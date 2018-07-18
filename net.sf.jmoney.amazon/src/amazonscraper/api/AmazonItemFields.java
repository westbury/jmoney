package amazonscraper.api;

public interface AmazonItemFields {

	String getDescription();

	String getUnitPrice();

	String getQuantity();

	String getSellerName();

	String getAuthor();

	String getReturnDeadline();

	boolean isItemOverseas();

}
