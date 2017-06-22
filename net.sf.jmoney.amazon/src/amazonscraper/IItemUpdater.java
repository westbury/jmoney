package amazonscraper;

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

	String getAmazonDescription();

	String getAsinOrIsbn();

	void setIntoReturnedItemAccount();

	String getImageCode();

	void setAsinOrIsbn(String asin);

	void setDescription(String description);

	void setOrderNumber(String orderNumber);

	void setQuantity(int quantity);

	/** set if this item is a streamed movie rental or purchase */
	void setMovie(boolean isMovie);

}
