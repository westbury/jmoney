package ebayscraper.api;

import java.util.List;

/**
 * This interface contains the fields that are available for the payment information in the details page.
 * All details pages have only a single set of these fields, even when multiple orders appear on the page.
 * 
 * @author Nigel Westbury
 *
 */
public interface EbayDetailPaymentFields {

	String getLastFourDigits();

	String getItemTotal();
	
	String getDiscount();

	String getShippingCost();
	
	String getAmountCharged();
	
	List<EbayDetailOrderFields> getOrders();

}
