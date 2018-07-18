package amazonscraper;

import org.json.JSONArray;
import org.json.JSONObject;

public class JsonOrder {

	final JSONObject object = new JSONObject();
	
	JSONArray shipments = new JSONArray();

	public JsonOrder(JsonShipment... expectedShipments) {
		object.put("shipments", shipments);
		for (JsonShipment expectedShipment : expectedShipments) {
			shipments.put(expectedShipment.object);
		}
	}

	public JsonOrder addShipment(JsonShipment shipment) {
		shipments.put(shipment.object);
		return this;
	}

}
