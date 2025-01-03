package ebayscraper.api;

import java.util.Map;

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
	 * order list only
	 * 
	 * @return
	 */
	Map<String, String> getDetail();

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

	String getSeller();

	String getDeliveryDate();

}
