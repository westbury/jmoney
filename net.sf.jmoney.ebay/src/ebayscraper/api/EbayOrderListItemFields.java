package ebayscraper.api;

public interface EbayOrderListItemFields {

	/**
	 * order list and order detail
	 * 
	 * @return
	 */
	String getItemNumber();

	/**
	 * order list and order detail
	 * 
	 * @return
	 * 
	 */
	String getDescription();

	/**
	 * order list
	 * 
	 * @return
	 */
	String getItemPrice();
	
	/**
	 * order detail only
	 * @return
	 */
	String getUnitPrice();

	/**
	 * order detail only
	 * @return
	 */
	String getAmount();

}
