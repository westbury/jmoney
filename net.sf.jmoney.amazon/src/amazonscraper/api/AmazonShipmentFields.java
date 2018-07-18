package amazonscraper.api;

import java.util.List;

public interface AmazonShipmentFields {

	String getMovieName();

	boolean isGiftcardPurchase();

	String getGiftcardMessage();

	String getItemAmount();

	String getRecipient();

	String getExpectedDate();

	String getDeliveryDate();

	boolean isNotDispatched();

	boolean isReturned();

	boolean isExchanged();

	List<AmazonItemFields> getItems();

}
