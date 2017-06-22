package amazonscraper;

import java.util.Date;

public interface IContextUpdater {

	/**
	 * This is the usual way of getting information from the underlying
	 * datastore for an order.
	 * 
	 * @param orderNumber
	 * @param orderDate date provided for performance only, as underlying
	 * 			datastores are likely to have an index on the date but
	 * 			less likely to have an index on the Amazon order number
	 * @return
	 */
	AmazonOrder createAmazonOrder(String orderNumber, Date orderDate);

	/**
	 * This form is used when an item is exchanged.  This method will look for
	 * an item that is likely to be the sale of the original item that was returned.
	 * 
	 * @param exchangeDate
	 * @param itemAmount
	 * @param soldBy
	 * @return
	 */
	AmazonOrderItem createAmazonItemForMatchingExchange(Date exchangeDate, long itemAmount, String soldBy);

}
