package amazonscraper;

import java.util.HashSet;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;

public class OrderUpdaterTest implements IOrderUpdater {

	private Set<ShipmentUpdaterTest> shipments = new HashSet<>();


	@Override
	public IShipmentUpdater createNewShipmentUpdater() {
		ShipmentUpdaterTest updater = new ShipmentUpdaterTest();
		shipments.add(updater);
		return updater;
	}


	public JSONObject toJson() {
		JSONArray jsonShipments = new JSONArray();
		this.shipments.stream().forEach(shipment -> jsonShipments.put(shipment.toJson()));
		
		JSONObject result = new JSONObject()
				.put("shipments", jsonShipments);

		return result;
	}
}
