package amazonscraper;

import org.json.JSONArray;
import org.json.JSONObject;

public class JsonOrders {

	final JSONObject object = new JSONObject();
	
	JSONArray orders = new JSONArray();

	public JsonOrders(JsonOrder... expectedOrders) {
		object.put("orders", orders);
		for (JsonOrder expectedOrder : expectedOrders) {
			orders.put(expectedOrder.object);
		}
	}
	
	public JsonOrders addOrder(JsonOrder order) {
		orders.put(order.object);
		return this;
	}

}
