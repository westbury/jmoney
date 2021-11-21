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

import ebayscraper.IContextUpdater;
import ebayscraper.api.EbayItemFields;
import ebayscraper.api.EbayOrderDetailFields;
import ebayscraper.api.EbayOrderFields;
import net.sf.jmoney.importer.wizards.ImportException;

public class EbayOrderAnalyzer {

	/** Date format used by Ebay on its web pages */
	private static DateFormat ebayDateFormat = new SimpleDateFormat("MMM dd, yyyy");
	private static DateFormat ebayPaidDateFormat = new SimpleDateFormat("MMM d, yyyy");
	private static DateFormat ebayShipDateFormat = new SimpleDateFormat("MM/dd/yyyy");

	private Set<EbayOrder> orders;
	private IContextUpdater contextUpdater;

	public EbayOrderAnalyzer(Set<EbayOrder> orders, IContextUpdater contextUpdater) {
		this.orders = orders;
		this.contextUpdater = contextUpdater;
	}

	public Date processEbayOrder(EbayOrderFields orderFields) {
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

		order.setOrderDate(orderDate);  // Done here and in above....
		order.setSeller(seller);
		order.setOrderTotal(orderTotal);

		ItemBuilder itemBuilder = new ItemBuilder(order, order.getItems());

		for (EbayItemFields itemFields : orderFields.getItems()) {

			String paidDateAsString = itemFields.getPaidDate();
			String shipDateAsString = itemFields.getShipDate();

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
			String unitPriceAsString = itemFields.getUnitPrice();
			String quantityAsString = itemFields.getQuantity();
			String itemNumber = itemFields.getItemNumber();

			Date paidDate = null;
			try {
				if (paidDateAsString != null) {
					paidDate = ebayPaidDateFormat.parse(paidDateAsString);
				}
			} catch (ParseException e) {
				e.printStackTrace();
				throw new RuntimeException("bad date");
			}

			Date shipDate = null;
			if (paidDateAsString != null) {
				try {
					shipDate = ebayShipDateFormat.parse(shipDateAsString);
				} catch (ParseException e) {
					e.printStackTrace();
					throw new RuntimeException("bad date");
				}
			}
			
			int itemQuantity = 1;
			if (quantityAsString != null) {
				itemQuantity = Integer.parseInt(quantityAsString);
			}

			final long unitPrice = new BigDecimal(unitPriceAsString).scaleByPowerOfTen(2).longValueExact();
			long itemAmount = unitPrice * itemQuantity;

//				if (itemQuantity != 1) {
//					itemFields.setQuantity(itemQuantity);
//				}
			long netAmount = unitPrice * itemQuantity;
			
			EbayOrderItem item = itemBuilder.get(itemNumber, description, netAmount);

			item.setPaidDate(paidDate);
			item.setShipDate(shipDate);
		}

//				// Now we have the items in this order,
//				// we can access the actual order.
//				EbayShipment order = shipmentObject.shipment;
//
//				if (order == null) {
//					throw new RuntimeException("Shipment with no items in order " + order.getOrderNumber());
//				}

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
//		for (EbayShipment order : order.getShipments()) {
//			order.flush();
//		}

		return orderDate;
	}

	public void processEbayOrderDetails(EbayOrderDetailFields orderFields)
			throws UnsupportedImportDataException {
		String orderDateAsString = orderFields.getOrderDate();
		String orderNumber = orderFields.getOrderNumber();
		String totalAsString = orderFields.getTotal();  // Same as from listings page???
		String grandTotalAsString = orderFields.getGrandTotal();
		String lastFourDigits = orderFields.getLastFourDigits();
		String postageAndPackagingAsString = orderFields.getPostageAndPackaging();

		Date orderDate;
		try {
			orderDate = ebayDateFormat.parse(orderDateAsString);
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
		 * If no EbayOrder exists yet in this view then create one.
		 * This will be either initially empty if the order does not yet
		 * exist in the session or it will be a wrapper for the existing order
		 * found in the session.
		 */
		EbayOrder order = getEbayOrderWrapper(orderNumber, orderDate);

		ItemBuilder itemBuilder = new ItemBuilder(order, order.getItems());

		for (EbayItemFields itemFields : orderFields.getItems()) {

				String description = itemFields.getDescription();
				String unitPriceAsString = itemFields.getUnitPrice();
				String quantityAsString = itemFields.getQuantity();
				String soldBy = itemFields.getSellerName();
				String itemNumber = itemFields.getItemNumber();

//				EbayOrderItem item = itemBuilder.get("", description, signedItemAmount, orderObject);
//
//				// TODO any item data to merge here???
//				// These may already be set, so should we check they are
//				// not being changed????
////				if (itemQuantity != 1) {
////					item.setQuantity(itemQuantity);
////				}
//				item.setAuthor(author);
//				item.setSoldBy(soldBy);
		}

		if (!itemBuilder.isEmpty()) {
			throw new RuntimeException("The imported items in the order do not match the previous set of imported items in order " + order.getOrderNumber() + ".  This should not happen and the code cannot cope with this situation.");
		}

		if (postageAndPackaging != 0) {
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
		for (EbayOrderItem item : order.getItems()) {
			// TODO
//			order.flush();
		}
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
