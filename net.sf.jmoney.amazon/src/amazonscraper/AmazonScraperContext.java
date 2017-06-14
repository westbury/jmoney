package amazonscraper;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.eclipse.core.databinding.observable.list.IObservableList;
import org.eclipse.core.databinding.observable.list.WritableList;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.widgets.Display;

import net.sf.jmoney.importer.wizards.ImportException;
import txr.matchers.DocumentMatcher;
import txr.matchers.MatchResults;

public class AmazonScraperContext {

	IContextUpdater contextUpdater;
	
	// Lazily created
	private DocumentMatcher ordersMatcher = null;

	// Lazily created
	private DocumentMatcher detailsMatcher = null;

	public IObservableList<AmazonOrder> orders = new WritableList<>();

	public AmazonScraperContext(IContextUpdater contextUpdater) {
		this.contextUpdater = contextUpdater;
	}

	private MatchResults fetchOrderBindingsFromClipboard() {
		if (ordersMatcher == null) {
			ordersMatcher = createMatcherFromResource("amazon-orders.txr");
		}

		MatchResults bindings = doMatchingFromClipboard(ordersMatcher);

		if (bindings == null || bindings.getCollections(0).isEmpty()) {
			throw new RuntimeException("Data in clipboard does not appear to be copied from the orders page.");
		}

		return bindings;
	}

	public MatchResults fetchDetailsBindingsFromClipboard() {
		if (detailsMatcher == null) {
			detailsMatcher = createMatcherFromResource("amazon-details.txr");
		}

		MatchResults orderBindings = doMatchingFromClipboard(detailsMatcher);

		if (orderBindings == null) {
			throw new RuntimeException("Data in clipboard does not appear to be a details page.");
		}
		
		return orderBindings;
	}

	private MatchResults doMatchingFromClipboard(DocumentMatcher matcher) {
		Display display = Display.getCurrent();
		Clipboard clipboard = new Clipboard(display);
		String plainText = (String)clipboard.getContents(TextTransfer.getInstance());
		clipboard.dispose();        

		MatchResults bindings = matcher.process(plainText);

		return bindings;
	}

	private DocumentMatcher createMatcherFromResource(String resourceName) {
		ClassLoader classLoader = getClass().getClassLoader();
		URL resource = classLoader.getResource(resourceName);
		try (InputStream txrInputStream = resource.openStream()) {
			return new DocumentMatcher(txrInputStream, "UTF-8");
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	public void pasteOrdersFromClipboard() {
		MatchResults bindings = fetchOrderBindingsFromClipboard();

		DateFormat dateFormat = new SimpleDateFormat("d MMM yyyy");

		for (MatchResults orderBindings : bindings.getCollections(0)) {
			String orderDateAsString = orderBindings.getVariable("date").text;
			String orderNumber = orderBindings.getVariable("ordernumber").text;
			String orderTotalAsString = orderBindings.getVariable("totalamount").text;

			long orderTotal = new BigDecimal(orderTotalAsString).scaleByPowerOfTen(2).longValueExact();

			Date orderDate;
			try {
				orderDate = dateFormat.parse(orderDateAsString);
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
			
			for (MatchResults shipmentBindings : orderBindings.getCollections(0)) {
				String movieName = shipmentBindings.getVariable("moviename").text;
				if (movieName != null) {
					continue;
				}
				
				String expectedDateAsString = shipmentBindings.getVariable("expecteddate").text;
				String deliveryDateAsString = shipmentBindings.getVariable("deliverydate").text;

				Date deliveryDate;
				try {
					deliveryDate = parsePastDate(deliveryDateAsString);
				} catch (ParseException e) {
					// TODO Return as error to TXR when that is supported???
					e.printStackTrace();
					throw new RuntimeException("bad date");
				}

				String shipmentIsNotDispatched = shipmentBindings.getVariable("isnotdispatched").text;
				if ("true".equals(shipmentIsNotDispatched)) {
					areAllShipmentsDispatched = false;
				}

				boolean returned = false;
				String isReturned = shipmentBindings.getVariable("returned").text;
				if ("true".equals(isReturned)) {
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
					returned = true;
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

				for (MatchResults itemBindings : shipmentBindings.getCollections(0)) {
					String description = itemBindings.getVariable("description").text;
					String unitPriceAsString = itemBindings.getVariable("itemamount").text;
					String quantityAsString = itemBindings.getVariable("quantity").text;
					String soldBy = itemBindings.getVariable("soldby").text;
					String author = itemBindings.getVariable("author").text;
					String returnDeadline = itemBindings.getVariable("returndeadline").text;

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
					AmazonOrderItem item = itemBuilder.get(asin, description, quantityAsString, signedItemAmount, shipmentObject);

					if (returned) {
						returnedItems.add(item);
					}

					item.setUnitPrice(unitPrice);
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
				
				long itemAmount = -returnedItem.getUnderlyingItem().getAmount();
				String description = returnedItem.getUnderlyingItem().getAmazonDescription();

				ShipmentObject myShipmentObject = new ShipmentObject();
				myShipmentObject.shipment = originalSaleShipment;
				String quantityAsString = "1"; // TODO
				AmazonOrderItem saleItem = itemBuilder.get(returnedItem.getUnderlyingItem().getAsinOrIsbn(), description, quantityAsString, itemAmount, myShipmentObject);

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
				}
				
				saleItem.getUnderlyingItem().setIntoReturnedItemAccount();
				returnedItem.getUnderlyingItem().setIntoReturnedItemAccount();
			}

			// Must do this check after returns are processed, because the sale of the return may not otherwise
			// have been extracted.
			if (!itemBuilder.isEmpty()) {
				throw new RuntimeException("The imported items in the order do not match the previous set of imported items in order " + order.getOrderNumber() + ".  This should not happen and the code cannot cope with this situation.");
			}

			// Set the charge amounts if not already set for each shipment.
			for (AmazonShipment shipment : order.getShipments()) {
				// If charge amount is not set, add up the transaction to set it.
				if (shipment.getChargeAmount() == null) {
					shipment.setCalculatedChargeAmount();
				}
			}
			
			if (order.getShipments().isEmpty() && order.getOrderTotal() == 0) {
				// Probably just a free movie from Amazon Prime or something.
				// We're not interested in this order here.
				// TODO re-factor so this order is not added in the first place.
				orders.remove(order);
			}
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

		AmazonOrder newOrder = contextUpdater.createAmazonOrderUpdater(orderNumber, orderDate);
		orders.add(newOrder);
		return newOrder;
	}

	private Date parsePastDate(String dateAsString) throws ParseException {
		if (dateAsString == null) {
			return null;
		}

		DayOfWeek givenDayOfWeek = null;

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
		case "Monday":
			givenDayOfWeek = DayOfWeek.MONDAY;
			break;
		case "Tuesday":
			givenDayOfWeek = DayOfWeek.TUESDAY;
			break;
		case "Wednesday":
			givenDayOfWeek = DayOfWeek.WEDNESDAY;
			break;
		case "Thursday":
			givenDayOfWeek = DayOfWeek.THURSDAY;
			break;
		case "Friday":
			givenDayOfWeek = DayOfWeek.FRIDAY;
			break;
		case "Saturday":
			givenDayOfWeek = DayOfWeek.SATURDAY;
			break;
		case "Sunday":
			givenDayOfWeek = DayOfWeek.SUNDAY;
			break;
		}

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

		DateFormat dateFormat = new SimpleDateFormat("d MMM yyyy");
		try {
			return dateFormat.parse(dateAsString);
		} catch (ParseException e) {
			// TODO Return as error to TXR when that is supported???
			e.printStackTrace();
			throw new RuntimeException("bad date");
		}

	}

	public void pasteDetailsFromClipboard() throws UpsupportedImportDataException {
		MatchResults orderBindings = fetchDetailsBindingsFromClipboard();

		String orderDateAsString = orderBindings.getVariable("orderdate").text;
		String orderNumber = orderBindings.getVariable("ordernumber").text;
		String subTotalAsString = orderBindings.getVariable("subtotal").text;
		String orderTotalAsString = orderBindings.getVariable("total").text;
		String giftcardAsString = orderBindings.getVariable("giftcard").text;
		String promotionAsString = orderBindings.getVariable("promotion").text;
		String importFeesDepositAsString = orderBindings.getVariable("importfeesdeposit").text;
		String grandTotalAsString = orderBindings.getVariable("grandtotal").text;
		String refundTotalAsString = orderBindings.getVariable("refundtotal").text;
		String lastFourDigits = orderBindings.getVariable("lastfourdigits").text;
		String postageAndPackagingAsString = orderBindings.getVariable("postageandpackaging").text;

//		boolean returned = false;
//		if (refundTotalAsString != null) {
//			returned = true;
//		}
		
		DateFormat dateFormat = new SimpleDateFormat("d MMM yyyy");

		Date orderDate;
		try {
			orderDate = dateFormat.parse(orderDateAsString);
		} catch (ParseException e) {
			// TODO Return as error to TXR when that is supported???
			e.printStackTrace();
			throw new RuntimeException("bad date");
		}

		/** The order total as shown in the orders list page */
		long orderTotal = new BigDecimal(orderTotalAsString).scaleByPowerOfTen(2).longValueExact();
		if (importFeesDepositAsString != null) {
			/*
			 * The order total (as shown on the orders list page) is the total
			 * plus 'import Fees Deposit' but not including promotions or
			 * amounts paid using gift cards.
			 */
			long importFeesDeposit = new BigDecimal(importFeesDepositAsString).scaleByPowerOfTen(2).longValueExact();
			orderTotal += importFeesDeposit;
		}
		
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
		
		for (MatchResults shipmentBindings : orderBindings.getCollections(0)) {
			String expectedDateAsString = shipmentBindings.getVariable("expecteddate").text;
			String deliveryDateAsString = shipmentBindings.getVariable("deliverydate").text;

			boolean returned = "true".equalsIgnoreCase(shipmentBindings.getVariable("returned").text);
			String refundAsString = shipmentBindings.getVariable("refund").text;

			String shipmentIsNotDispatched = shipmentBindings.getVariable("isnotdispatched").text;
			if ("true".equals(shipmentIsNotDispatched)) {
				areAllShipmentsDispatched = false;
			}

			Date deliveryDate;
			try {
				deliveryDate = parsePastDate(deliveryDateAsString);
			} catch (ParseException e) {
				// TODO Return as error to TXR when that is supported???
				e.printStackTrace();
				throw new RuntimeException("bad date");
			}

			order.setOrderDate(orderDate);
			order.setOrderTotal(orderTotal);

			ShipmentObject shipmentObject = new ShipmentObject();

			boolean overseasShipment = false;
			
			for (MatchResults itemBindings : shipmentBindings.getCollections(0)) {
				String description = itemBindings.getVariable("description").text;
				String unitPriceAsString = itemBindings.getVariable("itemamount").text;
				String quantityAsString = itemBindings.getVariable("quantity").text;
				String soldBy = itemBindings.getVariable("soldby").text;
				String author = itemBindings.getVariable("author").text;
				boolean overseasItem = "true".equalsIgnoreCase(itemBindings.getVariable("overseas").text);

				overseasShipment |= overseas;
				
				int itemQuantity = 1;
				if (quantityAsString != null) {
					itemQuantity = Integer.parseInt(quantityAsString);
				}

			 long unitPrice = new BigDecimal(unitPriceAsString).scaleByPowerOfTen(2).longValueExact();
				long itemAmount = unitPrice * itemQuantity;

				long signedItemAmount = returned ? -itemAmount : itemAmount;

				String asin = null;

				AmazonOrderItem item = itemBuilder.get(asin, description, quantityAsString, signedItemAmount, shipmentObject);

				if (returned) {
					returnedItems.add(item);
				}

				// TODO any item data to merge here???

				item.setUnitPrice(unitPrice);
				if (itemQuantity != 1) {
					item.setQuantity(itemQuantity);
				}
				item.setAuthor(author);
				item.setSoldBy(soldBy);
			}

			// Now we have the items in this shipment,
			// we can access the actual shipment.
			AmazonShipment shipment = shipmentObject.shipment;

			shipment.setExpectedDate(expectedDateAsString);
			shipment.setDeliveryDate(deliveryDate);
			shipment.setLastFourDigitsOfAccount(lastFourDigits);
			shipment.setReturned(returned);
			shipment.overseas = overseasShipment;
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
			
			long itemAmount = -returnedItem.getUnderlyingItem().getAmount();
			String description = returnedItem.getUnderlyingItem().getAmazonDescription();

			ShipmentObject myShipmentObject = new ShipmentObject();
			myShipmentObject.shipment = originalSaleShipment;
			String quantityAsString = "1"; // TODO
			AmazonOrderItem saleItem = itemBuilder.get(returnedItem.getUnderlyingItem().getAsinOrIsbn(), description, quantityAsString, itemAmount, myShipmentObject);

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
			order.setOrderTotal(orderTotal - promotion);

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

		// If this is an overseas shipment then we adjust the item price to get the
		// order total to be correct.
		// (This fails if there is more than one item in the order)
		AmazonShipment[] overseasSaleShipments = order.getShipments().stream().filter(shipment -> !shipment.isReturn() && shipment.overseas).toArray(AmazonShipment[]::new);
		if (overseasSaleShipments.length > 1) {
			throw new UpsupportedImportDataException("Can't cope with this");
		}
		if (overseasSaleShipments.length == 1) {
			AmazonShipment saleShipment = overseasSaleShipments[0];
			if (saleShipment.getItems().size() != 1) {
				throw new UpsupportedImportDataException("Can't cope with this");
			}
			AmazonOrderItem item = saleShipment.getItems().get(0);
			if (item.getQuantity() != 1) {
				throw new UpsupportedImportDataException("Can't cope with quantity in overseas order");
			}
	
			/* Change the item price to make it all match.
			We are fudging the numbers here, altering the item price
			to make the order total match.  This is backwards from the usual process
			but is necessary because it is the only way we can get overseas transactions to
			balance.  Amazon seem to switch everything to the buyer's currency at varying
			rates which mean nothing seems to balance.
			*/
			saleShipment.setChargeAmount(-orderTotal);
			
			long total = orderTotal - saleShipment.getPostageAndPackaging();
			
			item.setUnitPrice(total); 
		}
		
		// Set the charge amounts if not already set for each shipment.
//		boolean areAllShipmentsDispatched = true;
		for (AmazonShipment shipment : order.getShipments()) {
			// If charge amount is not set, add up the transaction to set it.
			if (shipment.getChargeAmount() == null) {
				shipment.setCalculatedChargeAmount();
			}
		}
	}

}
