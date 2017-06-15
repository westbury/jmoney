package amazonscraper;

import java.util.Date;
import java.util.Set;

public interface IShipmentUpdater {

	void setOrderDate(Date orderDate);

	void setPostageAndPackaging(long postageAndPackagingAmount);

	long getChargeAmount();

	void setChargeAmount(long amount);

	void setGiftcardAmount(long giftcardAmount);

	void setPromotionAmount(long promotionAmount);

	void setLastFourDigitsOfAccount(String lastFourDigits);

	Long getPostageAndPackaging();

	IItemUpdater createNewItemUpdater(long itemAmount);

	Set<IItemUpdater> getItemUpdaters();

}
