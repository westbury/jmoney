package amazonscraper;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import net.sf.jmoney.importer.wizards.ImportException;

public class AmazonOrder {

	private IOrderUpdater orderUpdater;
	
	private String orderNumber;

	private List<AmazonShipment> shipments = new ArrayList<>();

	private Date orderDate;
	
	private long orderTotal;

//	private IncomeExpenseAccount unmatchedAccount;

	/**
	 * This form is used when no transaction exists in the session for this
	 * order.  A new transaction is created.
	 * 
	 * @param orderNumber
	 * @param transaction
	 * @throws ImportException 
	 */
	public AmazonOrder(String orderNumber, IOrderUpdater orderUpdater) throws ImportException {
		this.orderNumber = orderNumber;
		this.orderUpdater = orderUpdater;
	}
	
	public void addShipment(AmazonShipment shipment) {
		shipments.add(shipment);
	}
	
	public String getOrderNumber() {
		return orderNumber;
	}

	public List<AmazonShipment> getShipments() {
		return shipments;
	}

	public void setOrderDate(Date orderDate) {
		// TODO check same if already set
		this.orderDate = orderDate;
		// Is this correct place to do this???
		for (AmazonShipment shipment : shipments) {
			shipment.getShipmentUpdater().setOrderDate(orderDate);
		}
	}

	public Date getOrderDate() {
		return orderDate;
	}

	public void setOrderTotal(long orderTotal) {
		this.orderTotal = orderTotal;
	}

	public long getOrderTotal() {
		return orderTotal;
	}

	/**
	 * Creates a new item in the datastore
	 * 
	 * @param description
	 * @param itemAmount
	 * @param shipmentObject
	 * @return
	 * @throws ImportException 
	 */
	public AmazonOrderItem createNewItem(String description, String quantityAsString, long itemAmount, ShipmentObject shipmentObject) throws ImportException {
		if (shipmentObject.shipment == null) {
			shipmentObject.shipment = new AmazonShipment(this, orderUpdater.createNewShipmentUpdater());
			// Is this correct, or do this in setOrderDate?
			IShipmentUpdater shipmentUpdater = shipmentObject.shipment.getShipmentUpdater();
			shipmentUpdater.setOrderDate(orderDate);
		
			shipments.add(shipmentObject.shipment);
		}
		
		IItemUpdater itemUpdater = shipmentObject.shipment.getShipmentUpdater().createNewItemUpdater(itemAmount);
		if (quantityAsString != null) {
			itemUpdater.setQuantity(Integer.parseInt(quantityAsString));
		}
		itemUpdater.setDescription(description);
		itemUpdater.setOrderNumber(orderNumber);
		AmazonOrderItem item = new AmazonOrderItem(shipmentObject.shipment, itemUpdater);
		shipmentObject.shipment.addItem(item);
		return item;
	}

	public List<AmazonOrderItem> getItems() {
		List<AmazonOrderItem> result = new ArrayList<>();
		for (AmazonShipment shipment : shipments) {
			result.addAll(shipment.getItems());
		}
		return result;
	}

}
