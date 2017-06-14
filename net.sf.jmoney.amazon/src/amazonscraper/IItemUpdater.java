package amazonscraper;

public interface IItemUpdater {

	long getAmount();

	String getAmazonDescription();

	String getAsinOrIsbn();

	void setIntoReturnedItemAccount();

	String getImageCode();

	void setAsinOrIsbn(String asin);

	void setDescription(String description);

	void setOrderNumber(String orderNumber);

	void setQuantity(int quantity);

}
