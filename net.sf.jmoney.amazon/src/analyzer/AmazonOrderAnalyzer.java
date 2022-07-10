package analyzer;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import amazonscraper.IContextUpdater;
import amazonscraper.api.AmazonItemFields;
import amazonscraper.api.AmazonOrderDetailFields;
import amazonscraper.api.AmazonOrderFields;
import amazonscraper.api.AmazonShipmentFields;
import net.sf.jmoney.importer.wizards.ImportException;

public class AmazonOrderAnalyzer {

	/** Date format used by Amazon on its web pages */
	private static DateFormat amazonDateFormat = new SimpleDateFormat("d MMM yyyy");

	private Set<AmazonOrder> orders;
	private IContextUpdater contextUpdater;

	public AmazonOrderAnalyzer(Set<AmazonOrder> orders, IContextUpdater contextUpdater) {
		this.orders = orders;
		this.contextUpdater = contextUpdater;
	}

	public Date processAmazonOrder(AmazonOrderFields orderFields) {
		String orderDateAsString = orderFields.getOrderDate();
		String orderNumber = orderFields.getOrderNumber();
		String orderTotalAsString = orderFields.getTotal();

		long orderTotal = new BigDecimal(orderTotalAsString).scaleByPowerOfTen(2).longValueExact();

		Date orderDate;
		try {
			orderDate = amazonDateFormat.parse(orderDateAsString);
		} catch (ParseException e) {
			// TODO Return as error to TXR when that is supported???
			e.printStackTrace();
			throw new RuntimeException("bad date");
		}

		/*
		 * If no AmazonOrder exists yet in this view then create one.
		 * This will be either initially empty if the order does not yet
		 * exist in the session or it will be a wrapper for the existing order
		 * found in the session.
		 */
		AmazonOrder order = getAmazonOrderWrapper(orderNumber, orderDate);

		order.setOrderDate(orderDate);  // Done here and in above....
		order.setOrderTotal(orderTotal);

		ItemBuilder itemBuilder = new ItemBuilder(order, order.getItems());

		boolean areAllShipmentsDispatched = true;
		List<AmazonOrderItem> returnedItems = new ArrayList<>();
		List<AmazonOrderItem> exchangedItems = new ArrayList<>();

		for (AmazonShipmentFields shipmentFields : orderFields.getShipments()) {
			String movieName = shipmentFields.getMovieName();
			boolean isGiftcardPurchase = shipmentFields.isGiftcardPurchase();

			if (movieName != null) {
				// Special case: A movie
				/*
				 * All we are given is a movie name, so create a sale with a single
				 * shipment that has a single item, and set the price to be the
				 * order total.
				 */

				// If the order amount is zero then this is a free movie and we ignore it.
				// TODO is this correct?  Perhaps a giftcard was used for payment?
				if (orderTotal == 0) {
					continue;
				}

				ShipmentObject shipmentObject = new ShipmentObject();
				long signedItemAmount = orderTotal;
				AmazonOrderItem item = itemBuilder.get(null, movieName, signedItemAmount, shipmentObject);
				item.setMovie(true);
			} else if (isGiftcardPurchase) {
				// Special case: A purchase of a giftcard
				// It is unknown if such a purchase can occur with other items in the shipment
				// or with other shipments for the same order.  Txr assumes giftcard is only
				// item in the order.
				/*
				 * Create a sale with a single shipment that has a single item.
				 */

				String message = shipmentFields.getGiftcardMessage();
				String itemAmountAsString = shipmentFields.getItemAmount();
				String recipientAddress = shipmentFields.getRecipient();

				long unitPrice = new BigDecimal(itemAmountAsString).scaleByPowerOfTen(2).longValueExact();

				ShipmentObject shipmentObject = new ShipmentObject();

				// TODO what if multiple gift certificates are purchased at one time???
				long signedItemAmount = unitPrice;
				AmazonOrderItem item = itemBuilder.get(null, "gift voucher - " + recipientAddress, signedItemAmount, shipmentObject);
			} else {
				// General purchase

				String expectedDateAsString = shipmentFields.getExpectedDate();
				String deliveryDateAsString = shipmentFields.getDeliveryDate();

				Date deliveryDate = parsePastDate(deliveryDateAsString);

				boolean shipmentIsNotDispatched = shipmentFields.isNotDispatched();
				if (shipmentIsNotDispatched) {
					areAllShipmentsDispatched = false;
				}

				boolean returned = shipmentFields.isReturned();
				if (returned) {
					/* The purchase of this item may or may not have
					 * already been imported.  If it has already been imported
					 * then we need to match this item to the original shipment,
					 * then modify that shipment.
					 * 
					 * If it has not already been imported then it is even more
					 * complicated because we have to determine which shipment
					 * the item was charged to.  This is actually not possible
					 * (we don't have the information) unless the charge card statement
					 * has been imported and we can thus look to see what amounts were actually
					 * charged.
					 */
				}

				boolean exchanged = shipmentFields.isExchanged();
				if (exchanged) {
					/* When an item is exchanged, the purchase of the original item remains
					 * in the report.  The exchange occurs under a different order number.
					 * 
					 * We want to find the order for the sale of the original item so we
					 * can indicate that is has been returned.  We don't know the original
					 * order number but we do know the item price.  So we must interrogate the 
					 * datastore for item sales based on date range, item price, and maybe on
					 * 'sold by'.
					 */
				}

				/*
				 * If no AmazonShipment exists yet in this view then create one.
				 * 
				 * AmazonItem object created from datastore initially have no shipment.
				 * Only when matched here is a shipment assigned to the item.
				 * 
				 * Two types of shipment creators.  One creates a new shipment each time.
				 * 
				 * 
				 * This will be either initially empty if the order does not yet
				 * exist in the session or it will be a wrapper for the existing order
				 * found in the session.
				 */
				ShipmentObject shipmentObject = new ShipmentObject();

				boolean isShipmentOverseas = false;

				for (AmazonItemFields itemFields : shipmentFields.getItems()) {
					String description = itemFields.getDescription();
					String unitPriceAsString = itemFields.getUnitPrice();
					String quantityAsString = itemFields.getQuantity();
					String soldBy = itemFields.getSellerName();
					String author = itemFields.getAuthor();
					String returnDeadline = itemFields.getReturnDeadline();
					boolean isItemOverseas = itemFields.isItemOverseas();

					isShipmentOverseas |= isItemOverseas;

					int itemQuantity = 1;
					if (quantityAsString != null) {
						itemQuantity = Integer.parseInt(quantityAsString);
					}

					final long unitPrice = new BigDecimal(unitPriceAsString).scaleByPowerOfTen(2).longValueExact();
					long itemAmount = unitPrice * itemQuantity;

					String asin = null;

					/*
					 * Returns are a little complicated here.  
					 * 
					 * 1. It may be we have already processed the order
					 * before the item was returned.  In that case we will already have a transaction for the original
					 * sale.  We create a separate transaction for the return.
					 * 
					 * 2. It may also be that the order has not been processed at all.  In that case we want to create
					 * the original order as it would have been without the item.  Then we can create the return the
					 * same as in case 1.
					 * 
					 * Note that when items are returned, we keep both the original transaction with the sale and
					 * create a second transaction for the return.  In both cases the income/expense category for
					 * the item is set to a special 'returns' account (a special 'returns' account allows us to keep details of the
					 * item which we need to maintain the integrity of further imports, but also keep the reports of
					 * other income/expense accounts clean). 
					 * 
					 * We reverse the sign of the amount if a refund.  This ensures the item created matches the refund entry,
					 * not the original sale entry.
					 * 
					 * We add to a list of all refunded items.  This allows us to add, if necessary, the original sale entries
					 * for all refunded items.  This is done later because we must have all the shipments available so we
					 * can determine which shipment each returned item originally arrived in.
					 */
					long signedItemAmount = returned ? -itemAmount : itemAmount;
					AmazonOrderItem item = itemBuilder.get(asin, description, signedItemAmount, shipmentObject);

					if (returned) {
						returnedItems.add(item);
					}

					if (exchanged) {
						exchangedItems.add(item);
					}

					if (itemQuantity != 1) {
						item.setQuantity(itemQuantity);
					}
					item.setSoldBy(soldBy);
					item.setAuthor(author);
					item.setReturnDeadline(returnDeadline);
				}

				// Now we have the items in this shipment,
				// we can access the actual shipment.
				AmazonShipment shipment = shipmentObject.shipment;

				if (shipment == null) {
					throw new RuntimeException("Shipment with no items in order " + order.getOrderNumber());
				}
				shipment.setExpectedDate(expectedDateAsString);
				shipment.setDeliveryDate(deliveryDate);

				shipment.setReturned(returned);
				shipment.overseas = isShipmentOverseas;
			}
		}

		if (!areAllShipmentsDispatched) {
			// TODO We should probably be able to import the shipments from this
			// order that have dispatched.  Need to think about this.
			// (The order total does not include the amount and the charge is not
			// made until a shipment is dispatch).
			// For time being, don't import anything in an order until all shipments
			// have dispatched.

			// TODO re-factor so such orders are not created in the first place.
			//				orders.remove(order);
			//				continue;
		}

		for (AmazonOrderItem returnedItem : returnedItems) {
			/*
			 * See if the original exists in any shipment.  If it does not, add it
			 * to the 'not return' shipment.  If there is no 'not return' shipment then
			 * create a shipment with this item.  If there is more than one 'not return'
			 * shipment then error for the time being (we will need a real example
			 * to determine how to resolve this).
			 */
			AmazonShipment originalSaleShipment = null;
			AmazonShipment[] saleShipments = order.getShipments().stream().filter(shipment -> !shipment.isReturn()).toArray(AmazonShipment[]::new);
			if (saleShipments.length > 1) {
				throw new RuntimeException("Can't determine which original shipment contained a returned item");
			} else if (saleShipments.length == 1) {
				originalSaleShipment = saleShipments[0];
			} else {
				// Setting a null shipment will cause a new one to be created.
				originalSaleShipment = null;
			}

			long itemAmount = -returnedItem.getNetCost();
			String description = returnedItem.getAmazonDescription();

			ShipmentObject myShipmentObject = new ShipmentObject();
			myShipmentObject.shipment = originalSaleShipment;
			AmazonOrderItem saleItem = itemBuilder.get(returnedItem.getUnderlyingItem().getAsinOrIsbn(), description, itemAmount, myShipmentObject);

			if (originalSaleShipment == null) {
				/*
				 * A new shipment was created for the sale. We don't have
				 * the delivery date available to us once an item is
				 * returned, so if we didn't import the order from Amazon
				 * before the item was returned then we have lost the
				 * information. We put text in the 'expected delivery date'
				 * field to indicate this.
				 */
				myShipmentObject.shipment.setExpectedDate("shipment of items later returned");
				myShipmentObject.shipment.overseas = returnedItem.getShipment().overseas;
			}

			saleItem.getUnderlyingItem().setIntoReturnedItemAccount();
			returnedItem.getUnderlyingItem().setIntoReturnedItemAccount();
		}

		for (AmazonOrderItem exchangedItem : exchangedItems) {
			/*
			 * The item marked as 'exchanged' is the original item in the original sale.
			 * We set the category for this item as 'giftcard'.
			 */
			exchangedItem.getUnderlyingItem().setIntoGiftcardAccount();

			/*
			 * We look now for an order in which the replacement item was shipped.
			 * This order will have an item that matches in price and which also
			 * has an order total which is too small given the items shipped.
			 */
			// TODO really we should only do this if the item was not already
			// in the returned category. Or perhaps not, as the replacement item
			// may not have been found the previous time we examined this exchange.

//				AmazonOrderItem replacementItem = null;
//
//				long itemAmount = exchangedItem.getNetCost();
//				String soldBy = exchangedItem.getSoldBy();
//
//
//				/* First check orders already in the context for the sale of an item with the
//				same price as this one.  Note that we only look in other orders.
//				 */
//				AmazonOrderItem[] possibleReplacementItems = orders.stream().filter(eachOrder -> (eachOrder != order)).flatMap(eachOrder -> eachOrder.getItems().stream()).filter(eachItem -> eachItem.getNetCost() == itemAmount).toArray(AmazonOrderItem[]::new);
//
//				// TODO what if there is more than one match????
//
//
//				if (possibleReplacementItems.length == 0) {
//					replacementItem = contextUpdater.createAmazonItemForMatchingExchange(orderDate, itemAmount, soldBy);
//				} else {
//					replacementItem = possibleReplacementItems[0];
//				}
//
//				// If we don't find the replacement, no worries.  We simply leave things
//				// as they are.  It should all match if the replacement item order is later
//				// imported because the exchanged item will be in the 'returned items' category.
//				if (replacementItem != null) {
//
//					// Look to see if a 'return' item has already been created in
//					// this shipment.
//
//					AmazonOrderItem[] matchingReturnedItems = replacementItem.getShipment().getItems().stream().filter(item -> item.getNetCost() == -itemAmount).toArray(AmazonOrderItem[]::new);
//					if (matchingReturnedItems.length > 2) {
//						throw new RuntimeException("Messed up transaction");
//					}
//
//					AmazonOrderItem returnOfItem;
//					if (matchingReturnedItems.length == 0) {
//						IItemUpdater returnedItemUpdater = replacementItem.getShipment().getShipmentUpdater().createNewItemUpdater(-itemAmount);
//						returnOfItem = new AmazonOrderItem(replacementItem.getShipment(), returnedItemUpdater);
//						replacementItem.getShipment().addItem(returnOfItem);
//						/*
//						 * As the original item return was just created, we need to find the original item sale
//						 * and copy data across.
//						 */
//						
//						returnOfItem.setQuantity(-exchangedItem.getQuantity());
//						returnOfItem.setSoldBy(exchangedItem.getSoldBy());
//						returnOfItem.setAmazonDescription(exchangedItem.getAmazonDescription());
//						returnOfItem.getUnderlyingItem().setAsinOrIsbn(exchangedItem.getUnderlyingItem().getAsinOrIsbn());
//						
//						// But order number is the return order
//						returnOfItem.getUnderlyingItem().setOrderNumber(replacementItem.getShipment().getOrder().getOrderNumber());
//					} else {
//						returnOfItem = matchingReturnedItems[0];
//					}
//
//					returnOfItem.getUnderlyingItem().setIntoReturnedItemAccount();
//					
//					// Flush now, because this shipment is not in our order and it may already have been flushed....
//					replacementItem.getShipment().flush();
//				}
		}

		// Must do this check after returns are processed, because the sale of the return may not otherwise
		// have been extracted.
		if (!itemBuilder.isEmpty()) {
			throw new RuntimeException("The imported items in the order do not match the previous set of imported items in order " + order.getOrderNumber() + ".  This should not happen and the code cannot cope with this situation.");
		}

		/*
		 * Not all changes are immediately reflected in the underlying datastore.
		 * For example, we don't update the calculated charge amount each time some
		 * other property's amount changes.  That would prevent us, when the charge amount is fixed, from making changes to
		 * multiple amounts that together leave the charge amount unchanged.
		 */
		for (AmazonShipment shipment : order.getShipments()) {
			shipment.flush();
		}

		if (order.getShipments().isEmpty() && order.getOrderTotal() == 0) {
			// Probably just a free movie from Amazon Prime or something.
			// We're not interested in this order here.
			// TODO re-factor so this order is not added in the first place.
			orders.remove(order);
		}
		return orderDate;
	}

	public void processAmazonOrderDetails(AmazonOrderDetailFields orderFields)
			throws UnsupportedImportDataException {
		String orderDateAsString = orderFields.getOrderDate();
		String orderNumber = orderFields.getOrderNumber();
		String subTotalAsString = orderFields.getSubtotal();
		String totalAsString = orderFields.getTotal();  // Same as from listings page???
		String giftcardAsString = orderFields.getGiftcardAmount();
		String promotionAsString = orderFields.getPromotionAmount();
		String importFeesDepositAsString = orderFields.getImportFeesDepositAmount();
		String grandTotalAsString = orderFields.getGrandTotal();
		String refundTotalAsString = orderFields.getRefundTotal();
		String lastFourDigits = orderFields.getLastFourDigits();
		String postageAndPackagingAsString = orderFields.getPostageAndPackaging();

		Date orderDate;
		try {
			orderDate = amazonDateFormat.parse(orderDateAsString);
		} catch (ParseException e) {
			// TODO Return as error to TXR when that is supported???
			e.printStackTrace();
			throw new RuntimeException("bad date");
		}

		/** The order total as shown in the orders list page */
		//		long orderTotal = new BigDecimal(totalAsString).scaleByPowerOfTen(2).longValueExact();

		// For foreign orders, must be grand total and not total, because we must add in the
		// import fees deposit.
		long orderTotal = new BigDecimal(grandTotalAsString).scaleByPowerOfTen(2).longValueExact();

		long postageAndPackaging = new BigDecimal(postageAndPackagingAsString).scaleByPowerOfTen(2).longValueExact();

		/*
		 * If no AmazonOrder exists yet in this view then create one.
		 * This will be either initially empty if the order does not yet
		 * exist in the session or it will be a wrapper for the existing order
		 * found in the session.
		 */
		AmazonOrder order = getAmazonOrderWrapper(orderNumber, orderDate);

		ItemBuilder itemBuilder = new ItemBuilder(order, order.getItems());

		boolean areAllShipmentsDispatched = false;

		List<AmazonOrderItem> returnedItems = new ArrayList<>();

		boolean overseas = (importFeesDepositAsString != null);
		if (importFeesDepositAsString != null) {
			System.out.println("overseas");
		}

		for (AmazonShipmentFields shipmentFields : orderFields.getShipments()) {
			String expectedDateAsString = shipmentFields.getExpectedDate();
			String deliveryDateAsString = shipmentFields.getDeliveryDate();

			boolean returned = shipmentFields.isReturned();
			
			// This is not used????
//			String refundAsString = shipmentBindings.getVariable("refund").text;

			if (shipmentFields.isNotDispatched()) {
				areAllShipmentsDispatched = false;
			}

			Date deliveryDate = parsePastDate(deliveryDateAsString);

			order.setOrderDate(orderDate);
			order.setOrderTotal(orderTotal);

			ShipmentObject shipmentObject = new ShipmentObject();

			boolean overseasShipment = false;

			for (AmazonItemFields itemFields : shipmentFields.getItems()) {
				String description = itemFields.getDescription();
				String unitPriceAsString = itemFields.getUnitPrice();
				String quantityAsString = itemFields.getQuantity();
				String soldBy = itemFields.getSellerName();
				String author = itemFields.getAuthor();
//				String returnDeadline = itemFields.getReturnDeadline();
				boolean overseasItem = itemFields.isItemOverseas();

				overseasShipment |= overseas;

				int itemQuantity = 1;
				if (quantityAsString != null) {
					itemQuantity = Integer.parseInt(quantityAsString);
				}

				long itemNetCost;
				if (!overseas) {
					long unitPrice = new BigDecimal(unitPriceAsString).scaleByPowerOfTen(2).longValueExact();
					itemNetCost = unitPrice * itemQuantity;
				} else {
					// Is overseas.
					if (shipmentFields.getItems().size() > 1) {
						throw new UnsupportedImportDataException("Can't cope with multiple items in overseas shipment.");
					}

					long subTotal = new BigDecimal(subTotalAsString).scaleByPowerOfTen(2).longValueExact();
					itemNetCost = subTotal;
				}
				long signedItemAmount = returned ? -itemNetCost : itemNetCost;

				String asin = null;

				AmazonOrderItem item = itemBuilder.get(asin, description, signedItemAmount, shipmentObject);

				if (returned) {
					returnedItems.add(item);
				}

				// TODO any item data to merge here???
				// These may already be set, so should we check they are
				// not being changed????
				if (itemQuantity != 1) {
					item.setQuantity(itemQuantity);
				}
				item.setAuthor(author);
				item.setSoldBy(soldBy);
			}

			// Now we have the items in this shipment,
			// we can access the actual shipment.
			AmazonShipment shipment = shipmentObject.shipment;

//			if (expectedDateAsString != null) {
				shipment.setExpectedDate(expectedDateAsString);
//			} else {
//				shipment.setExpectedDate("shipped in prior year");
//			}
			shipment.setDeliveryDate(deliveryDate);
			shipment.setLastFourDigitsOfAccount(lastFourDigits);
			shipment.setReturned(returned);
			shipment.overseas = overseasShipment;

			if (overseasShipment && returned) {
				// Extract the 'import fees deposit' amount that was refunded.

				long refundTotal = new BigDecimal(refundTotalAsString).scaleByPowerOfTen(2).longValueExact();
				long subTotal = new BigDecimal(subTotalAsString).scaleByPowerOfTen(2).longValueExact();
				long importFeesDepositRefunded = refundTotal - subTotal;
				shipment.setImportFeesDeposit(-importFeesDepositRefunded);
			}
		}

		for (AmazonOrderItem returnedItem : returnedItems) {
			/*
			 * See if the original exists in any shipment.  If it does not, add it
			 * to the 'not return' shipment.  If there is no 'not return' shipment then
			 * create a shipment with this item.  If there is more than one 'not return'
			 * shipment then error for the time being (we will need a real example
			 * to determine how to resolve this).
			 */
			AmazonShipment originalSaleShipment = null;
			AmazonShipment[] saleShipments = order.getShipments().stream().filter(shipment -> !shipment.isReturn()).toArray(AmazonShipment[]::new);
			if (saleShipments.length > 1) {
				throw new RuntimeException("Can't determine which original shipment contained a returned item");
			} else if (saleShipments.length == 1) {
				originalSaleShipment = saleShipments[0];
			} else {
				// Setting a null shipment will cause a new one to be created.
				originalSaleShipment = null;
			}

			long itemAmount = -returnedItem.getNetCost();
			String description = returnedItem.getAmazonDescription();

			ShipmentObject myShipmentObject = new ShipmentObject();
			myShipmentObject.shipment = originalSaleShipment;
			AmazonOrderItem saleItem = itemBuilder.get(returnedItem.getUnderlyingItem().getAsinOrIsbn(), description, itemAmount, myShipmentObject);

			if (originalSaleShipment == null) {
				/*
				 * A new shipment was created for the sale. We don't have
				 * the delivery date available to us once an item is
				 * returned, so if we didn't import the order from Amazon
				 * before the item was returned then we have lost the
				 * information. We put text in the 'expected delivery date'
				 * field to indicate this.
				 */
				myShipmentObject.shipment.setExpectedDate("shipment of items later returned");
				myShipmentObject.shipment.setLastFourDigitsOfAccount(lastFourDigits);

				myShipmentObject.shipment.overseas = overseas;
			}

			saleItem.getUnderlyingItem().setIntoReturnedItemAccount();
			returnedItem.getUnderlyingItem().setIntoReturnedItemAccount();
		}

		if (!itemBuilder.isEmpty()) {
			throw new RuntimeException("The imported items in the order do not match the previous set of imported items in order " + order.getOrderNumber() + ".  This should not happen and the code cannot cope with this situation.");
		}

		if (!areAllShipmentsDispatched) {
			// Anything to do here?  Or is this a useless check in this case?
		}

		if (postageAndPackaging != 0) {
			AmazonShipment[] saleShipments = order.getShipments().stream().filter(shipment -> !shipment.isReturn()).toArray(AmazonShipment[]::new);
			if (saleShipments.length == 1) {
				AmazonShipment shipment = saleShipments[0]; 
				shipment.setPostageAndPackaging(postageAndPackaging);
			} else {
				throw new RuntimeException("p&p but multiple shipments.  We need to see an example of this to decide how to handle this.");
			}
		}

		if (giftcardAsString != null) {
			long giftcard = new BigDecimal(giftcardAsString).scaleByPowerOfTen(2).longValueExact();

			if (order.getShipments().size() == 1) {
				AmazonShipment shipment = order.getShipments().get(0); 
				shipment.setGiftcardAmount(giftcard);
			} else {
				throw new RuntimeException("giftcard but multiple shipments.  We need to see an example of this to decide how to handle this.");
			}
		}

		if (promotionAsString != null) {
			long promotion = new BigDecimal(promotionAsString).scaleByPowerOfTen(2).longValueExact();

			// The order total set here must match the order total seen on the orders page.
			// This is the amount after the promotion has been deducted.
			// (Need to re-check giftcard amounts)
			// Note we are re-setting the order total which was already set.
			
			// NO, grand total already has this deducted.
//			order.setOrderTotal(orderTotal - promotion);

			/*
			 * In the only example we have with a promotional discount, the promotion was applied to the first shipment
			 * in the order.  It is not known if this will always be the case.  If there is a case of the promotion
			 * being applied to a shipment other than the first then we can really only deal with this by seeing what charges
			 * are made to the charge account and figuring it out.  The problem with that approach is that it assumes the
			 * charge account data is imported before the Amazon data, which violates the JMoney principle that all the data
			 * gets merged and matched up correctly regardless of the order of imports.
			 */
			AmazonShipment firstShipment = order.getShipments().get(0); 
			firstShipment.setPromotionAmount(promotion);
		}

		/*
		 * If this is an overseas shipment then we set the item price to the
		 * given sub-total. This is the only way to get the transaction to
		 * balance because the price given for the item does not result in a
		 * balanced transaction. Most likely the item price is converted using
		 * the exchange rate at the time of the report and not the rate actually
		 * used when the item was purchased.
		 * 
		 * If there is more than one item in the order then this trick won't
		 * work. In this case we fail the import.
		 */
		AmazonShipment[] overseasSaleShipments = order.getShipments().stream().filter(shipment -> !shipment.isReturn() && shipment.overseas).toArray(AmazonShipment[]::new);
		if (overseasSaleShipments.length > 1) {
			throw new UnsupportedImportDataException("Can't cope with this");
		}
		if (overseasSaleShipments.length == 1) {
			AmazonShipment saleShipment = overseasSaleShipments[0];
			if (saleShipment.getItems().size() != 1) {
				throw new UnsupportedImportDataException("Can't cope with this");
			}
			AmazonOrderItem item = saleShipment.getItems().get(0);

			/** Total price of all items */
			long itemTotal = new BigDecimal(subTotalAsString).scaleByPowerOfTen(2).longValueExact();

			item.setNetCost(itemTotal); 

			long importFeesDeposit = new BigDecimal(importFeesDepositAsString).scaleByPowerOfTen(2).longValueExact();
			saleShipment.setImportFeesDeposit(importFeesDeposit);
		}

		/*
		 * Not all changes are immediately reflected in the underlying datastore.
		 * For example, we don't update the calculated charge amount each time some
		 * other property's amount changes.  That would prevent us, when the charge amount is fixed, from making changes to
		 * multiple amounts that together leave the charge amount unchanged.
		 */
		for (AmazonShipment shipment : order.getShipments()) {
			shipment.flush();
		}
	}

	/**
	 * Looks to see if this order is already in this view.  If not,
	 * creates the AmazonOrder object for this order.
	 * <P>
	 * Note that when an AmazonOrder object is created, the order may or may
	 * not already exist in the accounting datastore.  If the order did not
	 * already exist in the datastore then a new transaction is created.
	 * 
	 * @param orderNumber
	 * @param orderDate
	 * @param session
	 * @return
	 * @throws ImportException 
	 */
	private AmazonOrder getAmazonOrderWrapper(String orderNumber, Date orderDate) {
		// Look to see if this order is already in the view.
		Optional<AmazonOrder> order = orders.stream().filter(x -> x.getOrderNumber().equals(orderNumber))
				.findFirst();

		if (order.isPresent()) {
			return order.get();
		}

		AmazonOrder newOrder = contextUpdater.createAmazonOrder(orderNumber, orderDate);
		orders.add(newOrder);
		return newOrder;
	}

	private Date parsePastDate(String dateAsString) {
		if (dateAsString == null) {
			return null;
		}

		switch (dateAsString) {
		case "today":
			return new Date();
		case "yesterday":
		{
			Date currentDate = new Date();
			LocalDateTime localDateTime = currentDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
			localDateTime = localDateTime.minusDays(1);
			Date resultDate = Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
			return resultDate;
		}
		}
		
		/**
		 * The language used on the Amazon site.  This must be the language used by
		 * the Amazon site when producing the web pages, not the preferred language
		 * of the user.  This is used for identifying and extracting data from the
		 * web page and will only work if the process is looking for words in the
		 * correct language.
		 */
		Locale languageOnAmazonSite = Locale.ENGLISH;
		
		Map<String, DayOfWeek> dayOfWeekMap = new HashMap<>();
		for (DayOfWeek eachDay : DayOfWeek.values()) {
			dayOfWeekMap.put(eachDay.getDisplayName(TextStyle.FULL, languageOnAmazonSite), eachDay);
		}
		
		DayOfWeek givenDayOfWeek = dayOfWeekMap.get(dateAsString);
		
		if (givenDayOfWeek != null) {
			// Or should we always use London time?  Is that what amazon.co.uk uses?
			Date currentDate = new Date();
			LocalDateTime localDateTime = currentDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();

			while (localDateTime.getDayOfWeek().compareTo(givenDayOfWeek) != 0) {
				localDateTime = localDateTime.minusDays(1);
			}

			// convert LocalDateTime to date
			Date resultDate = Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
			return resultDate;
		}

		try {
			return amazonDateFormat.parse(dateAsString);
		} catch (ParseException e) {
			// TODO we need to decide how to deal with errors like this.
			//			throw new UpsupportedImportDataException("Date given as " + dateAsString + " is not understood.");
			e.printStackTrace();
			return null;
		}
	}

}
