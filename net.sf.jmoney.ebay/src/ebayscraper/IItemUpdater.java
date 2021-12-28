package ebayscraper;

import java.util.Date;

public interface IItemUpdater {

	String getItemNumber();
	
	void setItemNumber(String itemNumber);

	/**
	 * @return the price for the given quantity of the item,
	 * being the item price adjusted by the shipping and discount amounts
	 * (pro-rated across all items in the order)
	 */
	long getGrossCost();

	/**
	 * @param itemPrice the price for the given quantity of the item,
	 * being the item price adjusted by the shipping and discount amounts
	 * (pro-rated across all items in the order)
	 */
	void setGrossCost(long itemPrice);

	String getEbayDescription();

	String getSoldBy();

	void setSoldBy(String soldBy);

	void setDescription(String description);

	void setOrderNumber(String orderNumber);

	void setQuantity(int quantity);

	void setPaidDate(Date paidDate);

	void setDeliveryDate(Date deliveryDate);

	String getImageCode();

	void setImageCode(String imageCode);

}
