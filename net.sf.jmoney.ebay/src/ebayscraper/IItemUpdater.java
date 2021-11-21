package ebayscraper;

import java.util.Date;

public interface IItemUpdater {

	/**
	 * @return the price for the given quantity of the item,
	 * being the unit price multiplied by the quantity
	 */
	long getNetCost();

	/**
	 * @param itemPrice the price for the given quantity of the item,
	 * being the unit price multiplied by the quantity
	 */
	void setNetCost(long itemPrice);

	String getEbayDescription();

	String getSoldBy();

	String getImageCode();

	void setSoldBy(String soldBy);

	void setDescription(String description);

	void setOrderNumber(String orderNumber);

	void setQuantity(int quantity);

	void setPaidDate(Date paidDate);

	void setShipDate(Date shipDate);

}
