package amazonscraper;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;

public class ShipmentUpdaterTest implements IShipmentUpdater {

	private Date orderDate;
	
	private Set<ItemUpdaterTest> items = new HashSet<>();

	private long postageAndPackagingAmount;

	private long chargeAmount;

	private boolean chargeAmountFixed;

	private String lastFourDigits;

	private long giftcardAmount;

	private long promotionAmount;

	private long importFeesDepositAmount;

	@Override
	public void setOrderDate(Date orderDate) {
		this.orderDate = orderDate;
	}

	@Override
	public IItemUpdater createNewItemUpdater(long itemAmount) {
		final ItemUpdaterTest updater = new ItemUpdaterTest(itemAmount);
		items.add(updater);
		return updater;
	}

	@Override
	public Set<? extends IItemUpdater> getItemUpdaters() {
		return items;
	}

	@Override
	public long getPostageAndPackaging() {
		return postageAndPackagingAmount;
	}

	@Override
	public void setPostageAndPackaging(long postageAndPackagingAmount) {
		this.postageAndPackagingAmount = postageAndPackagingAmount;
	}

	@Override
	public long getChargeAmount() {
		return chargeAmount;
	}

	@Override
	public void setChargeAmount(long chargeAmount) {
		this.chargeAmount = chargeAmount;
	}

	@Override
	public boolean isChargeAmountFixed() {
		return chargeAmountFixed;
	}

	@Override
	public void setLastFourDigitsOfAccount(String lastFourDigits) {
		this.lastFourDigits = lastFourDigits;
	}

	@Override
	public void setGiftcardAmount(long giftcardAmount) {
		this.giftcardAmount = giftcardAmount;
	}

	@Override
	public void setPromotionAmount(long promotionAmount) {
		this.promotionAmount = promotionAmount;
	}

	@Override
	public long getImportFeesDeposit() {
		return importFeesDepositAmount;
	}

	@Override
	public void setImportFeesDeposit(long importFeesDepositAmount) {
		this.importFeesDepositAmount = importFeesDepositAmount;
	}

	public JSONObject toJson() {
		JSONArray jsonItems = new JSONArray();
		this.items.stream().forEach(item -> jsonItems.put(item.toJson()));
		
		JSONObject result = new JSONObject()
				.put("items", jsonItems)
				.put("chargeAmount", chargeAmount)
				.put("chargeAmountFixed", chargeAmountFixed)
				.put("lastFourDigits", lastFourDigits)
				.put("postageAndPackagingAmount", postageAndPackagingAmount)
				.put("giftcardAmount", giftcardAmount)
				.put("promotionAmount", promotionAmount)
				.put("importFeesDepositAmount", importFeesDepositAmount);

		return result;
	}
}
