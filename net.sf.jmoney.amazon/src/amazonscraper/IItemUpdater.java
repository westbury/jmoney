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

	/**
	 * This method is called for items that were returned
	 * for a cash refund.
	 */
	void setIntoReturnedItemAccount();

	/**
	 * This method is called for items that were returned
	 * for an exchange item.  
	 * <P>
	 * For exchanges, Amazon show the
	 * shipment of the replacement item as a separate order that
	 * was paid for using a gift-card.  Therefore, to ensure everything
	 * balances, we put the returned exchange item also into the gift-card
	 * account.
	 * 
	 */
	void setIntoGiftcardAccount();

	String getImageCode();

	void setAsinOrIsbn(String asin);

	void setDescription(String description);

	void setOrderNumber(String orderNumber);

	void setQuantity(int quantity);

	/** set if this item is a streamed movie rental or purchase */
	void setMovie(boolean isMovie);

}
