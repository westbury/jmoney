package analyzer;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import ebayscraper.IContextUpdater;
import ebayscraper.api.EbayDetailItemFields;
import ebayscraper.api.EbayDetailOrderFields;
import ebayscraper.api.EbayDetailPaymentFields;
import ebayscraper.api.EbayOrderListItemFields;
import ebayscraper.api.EbayOrderListOrderFields;
import net.sf.jmoney.ebay.copytext.EbayImportView;
import net.sf.jmoney.ebay.copytext.ItemUpdater;
import net.sf.jmoney.importer.wizards.ImportException;

public class EbayOrderAnalyzer {

	/** Date format used by Ebay on its web pages */
	private static DateFormat ebayDateFormat = new SimpleDateFormat("dd MMM, yyyy");
	private static DateFormat ebayDateNoCommaFormat = new SimpleDateFormat("dd MMM yyyy");
	private static DateFormat ebayPaidDateFormat = new SimpleDateFormat("d MMM, yyyy");
	private static DateFormat ebayShipDateFormat = new SimpleDateFormat("dd/MM/yyyy");
	private static DateFormat ebayMonthAndDayFormat = new SimpleDateFormat("d MMM");
	private static DateFormat ebayDeliveryDateFormat = new SimpleDateFormat("EEE, d MMM yyyy");
	private static DateFormat ebayShortDeliveryDateFormat = new SimpleDateFormat("EEE d MMM");
	
	private Set<EbayOrder> orders;
	private IContextUpdater contextUpdater;

	public EbayOrderAnalyzer(Set<EbayOrder> orders, IContextUpdater contextUpdater) {
		this.orders = orders;
		this.contextUpdater = contextUpdater;
	}

	public Date processEbayOrderList(EbayOrderListOrderFields orderFields) {
		String orderDateAsString = orderFields.getOrderDate();
		String orderNumber = orderFields.getOrderNumber();
		String seller = orderFields.getSeller();
		String orderTotalAsString = orderFields.getTotal();

		long orderTotal = orderTotalAsString != null
				? new BigDecimal(orderTotalAsString).scaleByPowerOfTen(2).longValueExact()
				: 0;

		Date orderDate;
		try {
			orderDate = ebayDateFormat.parse(orderDateAsString);
		} catch (ParseException e) {
			// TODO Return as error to TXR when that is supported???
			e.printStackTrace();
			throw new RuntimeException("bad date");
		}

		/*
		 * If no EbayOrder exists yet in this view then create one.
		 * This will be either initially empty if the order does not yet
		 * exist in the session or it will be a wrapper for the existing order
		 * found in the session.
		 */
		EbayOrder order = getEbayOrderWrapper(orderNumber, orderDate);

//		order.setOrderDate(orderDate);  // Done here and in above....
//		order.setItemTotal(orderTotal);  actually not available from order list

		ItemBuilder itemBuilder = new ItemBuilder(order, order.getItems());

		for (EbayOrderListItemFields itemFields : orderFields.getItems()) {

			/*
			 * If no EbayShipment exists yet in this view then create one.
			 * 
			 * EbayItem object created from datastore initially have no order.
			 * Only when matched here is a order assigned to the item.
			 * 
			 * Two types of order creators.  One creates a new order each time.
			 * 
			 * 
			 * This will be either initially empty if the order does not yet
			 * exist in the session or it will be a wrapper for the existing order
			 * found in the session.
			 */

			String description = itemFields.getDescription();
			String itemPriceAsString = itemFields.getUnitPrice();
			String itemNumber = itemFields.getItemNumber();

			long itemPrice = new BigDecimal(itemPriceAsString).scaleByPowerOfTen(2).longValueExact();

			EbayOrderItem item = itemBuilder.get(itemNumber, description);

			item.setNetCost(itemPrice);
			
			// Set the image
			if (itemNumber != null) {
				try {
					String imageCode = EbayImportView.getImageCodeFromItemNumber(itemNumber);
					if (imageCode != null) {
						// TODO delete setImageCode method altogether as not now used
						// In fact why can't more just be set in the entry directly?
	//					item.setImageCode(imageCode);
						EbayImportView.setImageIntoItem(imageCode, (ItemUpdater)item.getUnderlyingItem());	
					}
				} catch (RuntimeException e) {
					// Don't fail the entire import if this fails.  It just means we don't get an image for this item.
					e.printStackTrace(System.out);
				}
			}
			
			// As the seller is listed here separately for each item, set on each item (not on the order as is done elsewhere)
			String itemSeller = itemFields.getSeller();
			if (itemSeller != null) {
				item.setSeller(itemSeller);
			}
			
			String deliveryDateAsString = itemFields.getDeliveryDate();
			if (deliveryDateAsString != null) {
				try {
					Date deliveryDate = ebayShortDeliveryDateFormat.parse(deliveryDateAsString);

					// Set year for delivery date, because that was given as day and month only.
					Calendar orderedDay = Calendar.getInstance();
					orderedDay.setTime(orderDate);
					int orderedMonth = orderedDay.get(Calendar.MONTH);
					int orderedYear = orderedDay.get(Calendar.YEAR);

					Calendar deliveredCalendar = Calendar.getInstance();
					deliveredCalendar.setTime(deliveryDate);
					int deliveredMonth = deliveredCalendar.get(Calendar.MONTH);
					if (deliveredMonth < orderedMonth) {
						deliveredCalendar.set(Calendar.YEAR, orderedYear + 1);
					} else {
						deliveredCalendar.set(Calendar.YEAR, orderedYear);
					}
					item.setDeliveryDate(deliveredCalendar.getTime());
				} catch (ParseException e) {
					throw new RuntimeException("bad date", e);
				}
			}
			
			
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
		order.flush();

		return orderDate;
	}

	public void processEbayOrderDetails(EbayDetailPaymentFields paymentFields)
			throws UnsupportedImportDataException {
		
		String lastFourDigits = paymentFields.getLastFourDigits();
		String discountAsString = paymentFields.getDiscount();
		String shippingAsString = paymentFields.getShippingCost();

		for (EbayDetailOrderFields orderFields : paymentFields.getOrders()) {
		
			String orderDateAsString = orderFields.getOrderDate();
			String orderNumber = orderFields.getOrderNumber();
			String totalAsString = orderFields.getTotal();  // Same as from listings page???
			String soldBy = orderFields.getSeller();
			String paidDayOfYearAsString = orderFields.getDayOfYearPaid();
			String shippingDayOfYearAsString = orderFields.getDayOfYearShipped();
			String deliveryDateAsString = orderFields.getDeliveryDate();
	
			Date orderDate;
			Date paidDate;
			Date shippingDate;
			Date deliveryDate;
			try {
				orderDate = ebayDateNoCommaFormat.parse(orderDateAsString);
				paidDate = ebayMonthAndDayFormat.parse(paidDayOfYearAsString);
				if (shippingDayOfYearAsString != null) {
					shippingDate = ebayMonthAndDayFormat.parse(shippingDayOfYearAsString);
				} else {
					// This seems to happen when an order is made but the seller never ships but instead refunds payment
					shippingDate = null;
				}
				if (deliveryDateAsString != null) {
					deliveryDate = ebayDeliveryDateFormat.parse(deliveryDateAsString);
				} else {
					deliveryDate = null; // Sometimes this seems to be missing from the details page
				}
				
				// Set year for paid and ship dates, because those were given as day and month only.
				Calendar orderedDay = Calendar.getInstance();
				orderedDay.setTime(orderDate);
				int orderedMonth = orderedDay.get(Calendar.MONTH);
				int orderedYear = orderedDay.get(Calendar.YEAR);

				Calendar paidCalendar = Calendar.getInstance();
				paidCalendar.setTime(paidDate);
				int paidMonth = paidCalendar.get(Calendar.MONTH);
				if (paidMonth < orderedMonth) {
					paidCalendar.set(Calendar.YEAR, orderedYear + 1);
				} else {
					paidCalendar.set(Calendar.YEAR, orderedYear);
				}
				paidDate = paidCalendar.getTime();

				if (shippingDate != null) {
					Calendar shippedCalendar = Calendar.getInstance();
					shippedCalendar.setTime(shippingDate);
					int shippedMonth = paidCalendar.get(Calendar.MONTH);
					if (shippedMonth < orderedMonth) {
						shippedCalendar.set(Calendar.YEAR, orderedYear + 1);
					} else {
						shippedCalendar.set(Calendar.YEAR, orderedYear);
					}
					shippingDate = shippedCalendar.getTime();
				}
			} catch (ParseException e) {
				throw new RuntimeException("bad date", e);
			}
	
			long orderTotal = new BigDecimal(totalAsString).scaleByPowerOfTen(2).longValueExact();
	
			long discount = discountAsString == null ? 0 : new BigDecimal(discountAsString).scaleByPowerOfTen(2).longValueExact();
			if (!shippingAsString.startsWith("£") && !shippingAsString.equals("Free")) {
				throw new RuntimeException("bad shipping amount");
			}
			long shipping = (shippingAsString == null || shippingAsString.equals("Free")) ? 0 : new BigDecimal(shippingAsString.substring("£".length())).scaleByPowerOfTen(2).longValueExact();
	
			/*
			 * If no EbayOrder exists yet in this view then create one.
			 * This will be either initially empty if the order does not yet
			 * exist in the session or it will be a wrapper for the existing order
			 * found in the session.
			 */
			EbayOrder order = getEbayOrderWrapper(orderNumber, orderDate);
			order.setSeller(soldBy);
			order.setOrderTotal(orderTotal);
			order.setDiscount(discount);
			order.setPostageAndPackaging(shipping);
			order.setPaidDate(paidDate);
			order.setShippingDate(shippingDate);
			if (deliveryDate != null) {
				order.setDeliveryDate(deliveryDate);
			}

			order.setLastFourDigitsOfAccount(lastFourDigits);

			ItemBuilder itemBuilder = new ItemBuilder(order, order.getItems());
	
			for (EbayDetailItemFields itemFields : orderFields.getItems()) {
	
				String description = itemFields.getDescription();
				String unitPriceAsString = itemFields.getUnitPrice();
				String itemNumber = itemFields.getItemNumber();
				String itemDetail = itemFields.getDetail();

				long itemPrice = new BigDecimal(unitPriceAsString).scaleByPowerOfTen(2).longValueExact();

				EbayOrderItem item = itemBuilder.get(itemNumber, description);

				item.setNetCost(itemPrice);
				item.getUnderlyingItem().setSoldBy(soldBy);
				
				// Look for quantity in the detail
				int itemQuantity = 1;
				if (itemDetail != null) {
					Pattern quantityPattern1 = Pattern.compile("(.*)Qty\\s(\\d+)(.*)");
					Matcher m = quantityPattern1.matcher(itemDetail);
					if (m.matches()) {
						String q = m.group(2);
						itemQuantity = Integer.parseInt(q);
						itemDetail = m.group(1) + m.group(3);
						Pattern quantityPattern2 = Pattern.compile("(.*)quantity\\s" + q + "(.*)");
						Matcher m2 = quantityPattern2.matcher(itemDetail);
						if (m2.matches()) {
							itemDetail = m2.group(1) + m2.group(2);
						}
					}
					if (itemQuantity != 1) {
						item.setQuantity(itemQuantity);	
					}
				}
				
				// What's left, show that as 'detail' to the user.
				item.setDetail(itemDetail);
			}
	
			if (!itemBuilder.isEmpty()) {
				throw new RuntimeException("The imported items in the order do not match the previous set of imported items in order " + order.getOrderNumber() + ".  This should not happen and the code cannot cope with this situation.");
			}

			if (discount != 0) {
			}
			
			if (shipping != 0) {
	//			EbayShipment[] saleShipments = order.getShipments().stream().filter(order -> !order.isReturn()).toArray(EbayShipment[]::new);
	//			if (saleShipments.length == 1) {
	//				EbayShipment order = saleShipments[0]; 
	//				order.setPostageAndPackaging(postageAndPackaging);
	//			} else {
	//				throw new RuntimeException("p&p but multiple shipments.  We need to see an example of this to decide how to handle this.");
	//			}
			}

			/*
			 * Not all changes are immediately reflected in the underlying datastore.
			 * For example, we don't update the calculated charge amount each time some
			 * other property's amount changes.  That would prevent us, when the charge amount is fixed, from making changes to
			 * multiple amounts that together leave the charge amount unchanged.
			 */
			order.flush();

		}
		
		/*
		 * Not all changes are immediately reflected in the underlying datastore.
		 * For example, we don't update the calculated charge amount each time some
		 * other property's amount changes.  That would prevent us, when the charge amount is fixed, from making changes to
		 * multiple amounts that together leave the charge amount unchanged.
		 */
//		for (EbayOrderItem item : order.getItems()) {
//			// TODO
////			item.flush();
//		}
	}

	/**
	 * Looks to see if this order is already in this view.  If not,
	 * creates the EbayOrder object for this order.
	 * <P>
	 * Note that when an EbayOrder object is created, the order may or may
	 * not already exist in the accounting datastore.  If the order did not
	 * already exist in the datastore then a new transaction is created.
	 * 
	 * @param orderNumber
	 * @param orderDate
	 * @param session
	 * @return
	 * @throws ImportException 
	 */
	private EbayOrder getEbayOrderWrapper(String orderNumber, Date orderDate) {
		// Look to see if this order is already in the view.
		Optional<EbayOrder> order = orders.stream().filter(x -> x.getOrderNumber().equals(orderNumber))
				.findFirst();

		if (order.isPresent()) {
			return order.get();
		}

		EbayOrder newOrder = contextUpdater.createEbayOrder(orderNumber, orderDate);
		orders.add(newOrder);
		return newOrder;
	}

}
