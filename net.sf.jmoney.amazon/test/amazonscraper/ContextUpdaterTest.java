package amazonscraper;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;

final class ContextUpdaterTest implements IContextUpdater {
	private Set<OrderUpdaterTest> orders = new HashSet<>();

	@Override
	public AmazonOrder createAmazonOrder(String orderNumber, Date orderDate) {
		OrderUpdaterTest updater = new OrderUpdaterTest();
		orders.add(updater);
		return new AmazonOrder(orderNumber, updater);
	}

	@Override
	public AmazonOrderItem createAmazonItemForMatchingExchange(Date exchangeDate, long itemAmount, String soldBy) {
		OrderUpdaterTest updater = new OrderUpdaterTest();
		orders.add(updater);
		AmazonOrder amazonOrder = new AmazonOrder("test order number", updater);
		amazonOrder.setOrderDate(exchangeDate);
		
		IShipmentUpdater shipmentUpdater = updater.createNewShipmentUpdater();
		AmazonShipment shipment = new AmazonShipment(amazonOrder, shipmentUpdater);
		amazonOrder.addShipment(shipment);
		
		IItemUpdater itemUpdater = shipmentUpdater.createNewItemUpdater(itemAmount);
		AmazonOrderItem item = new AmazonOrderItem(shipment, itemUpdater);
		item.setSoldBy(soldBy);
		shipment.addItem(item);
		
		return item;
	}

	public JSONObject toJson() {
		JSONArray jsonOrders = new JSONArray();
		this.orders.stream().forEach(order -> jsonOrders.put(order.toJson()));
		
		JSONObject result = new JSONObject()
				.put("orders", jsonOrders);

		return result;
	}
}