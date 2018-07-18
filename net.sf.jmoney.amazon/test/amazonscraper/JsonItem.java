package amazonscraper;

import org.json.JSONObject;

public class JsonItem {

	final JSONObject object = new JSONObject();
	
	public JsonItem(long netCost) {
		object.put("itemAmount", netCost);
	}

	public JsonItem setDescription(String description) {
		object.put("description", description);
		return this;
	}

	public JsonItem setCategory(String category) {
		object.put("category", category);
		return this;
	}

	public JsonItem setQuantity(int quantity) {
		object.put("quantity", quantity);
		return this;
	}

	public JsonItem isMovie(boolean isMovie) {
		object.put("isMovie", isMovie);
		return this;
	}

	public JsonItem setOrderNumber(String orderNumber) {
		object.put("orderNumber", orderNumber);
		return this;
	}

}
