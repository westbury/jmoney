package ebayscraper.api;

import java.util.List;

/**
 * This interface contains the fields that are available for an order when
 * the order details page has been processed.
 * 
 * @author Nigel Westbury
 *
 */
public interface EbayDetailOrderFields {

	String getOrderDate();

	String getOrderNumber();

	String getTotal();

	String getSeller();

	String getCarrier();

	String getShippingService();

	String getTrackingNumber();

	String getDayOfYearPaid();

	String getDayOfYearShipped();

	String getDeliveryDate();

	List<EbayDetailItemFields> getItems();
}
