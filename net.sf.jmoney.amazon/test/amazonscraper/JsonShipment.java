package amazonscraper;

import org.json.JSONArray;
import org.json.JSONObject;

public class JsonShipment {

	final JSONObject object = new JSONObject();
	
	JSONArray items = new JSONArray(new JSONObject [] { });

	public JsonShipment(long chargeAmount, JsonItem... expectedItems) {
		object.put("chargeAmount", chargeAmount);
		object.put("items", items);
		for (JsonItem expectedItem : expectedItems) {
			items.put(expectedItem.object);
		}
	}

	public JsonShipment setChargeAmount(long amount) {
		object.put("chargeAmount", amount);
		return this;
	}

	public JsonShipment setImportFeesDepositAmount(long amount) {
		object.put("importFeesDepositAmount", amount);
		return this;
	}

	public JsonShipment setPostageAndPackagingAmount(long amount) {
		object.put("postageAndPackagingAmount", amount);
		return this;
	}

	public JsonShipment addItem(JsonItem item) {
		items.put(item.object);
		return this;
	}

	public JsonShipment setPromotionAmount(long amount) {
		object.put("promotionAmount", amount);
		return this;
	}

	public JsonShipment setGiftcardAmount(long amount) {
		object.put("giftcardAmount", amount);
		return this;
	}

}
