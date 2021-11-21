package ebayscraper;

import java.util.Date;

import analyzer.EbayOrder;
import analyzer.EbayOrderItem;

public interface IContextUpdater {

	/**
	 * This is the usual way of getting information from the underlying
	 * datastore for an order.
	 * 
	 * @param orderNumber
	 * @param orderDate date provided for performance only, as underlying
	 * 			datastores are likely to have an index on the date but
	 * 			less likely to have an index on the Ebay order number
	 * @return
	 */
	EbayOrder createEbayOrder(String orderNumber, Date orderDate);

}
