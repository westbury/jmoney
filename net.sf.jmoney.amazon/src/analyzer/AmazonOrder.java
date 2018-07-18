package analyzer;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import amazonscraper.IItemUpdater;
import amazonscraper.IOrderUpdater;
import amazonscraper.IShipmentUpdater;
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
	public AmazonOrder(String orderNumber, IOrderUpdater orderUpdater) {
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
	 * @param netCost
	 * @param shipmentObject
	 * @return
	 * @throws ImportException 
	 */
	public AmazonOrderItem createNewItem(String description, long netCost, ShipmentObject shipmentObject) {
		if (shipmentObject.shipment == null) {
			shipmentObject.shipment = new AmazonShipment(this, orderUpdater.createNewShipmentUpdater());
			// Is this correct, or do this in setOrderDate?
			IShipmentUpdater shipmentUpdater = shipmentObject.shipment.getShipmentUpdater();
			shipmentUpdater.setOrderDate(orderDate);
		
			shipments.add(shipmentObject.shipment);
		}
		
		IItemUpdater itemUpdater = shipmentObject.shipment.getShipmentUpdater().createNewItemUpdater(netCost);
		
		itemUpdater.setOrderNumber(orderNumber);
		AmazonOrderItem item = new AmazonOrderItem(shipmentObject.shipment, itemUpdater);
		
		// Must be set here, not in updater, so we can be more sure we have it even
		// if the updater does not process it.
		item.setAmazonDescription(description);
		
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
