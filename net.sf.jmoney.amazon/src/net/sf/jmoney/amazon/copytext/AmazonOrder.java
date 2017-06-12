package net.sf.jmoney.amazon.copytext;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import net.sf.jmoney.amazon.AccountFinder;
import net.sf.jmoney.amazon.AmazonEntry;
import net.sf.jmoney.amazon.AmazonEntryInfo;
import net.sf.jmoney.importer.wizards.ImportException;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.IncomeExpenseAccount;
import net.sf.jmoney.model2.Session;
import net.sf.jmoney.model2.Transaction;

public class AmazonOrder {

	private AccountFinder accountFinder;
	
	private String orderNumber;

	private List<AmazonShipment> shipments = new ArrayList<>();

	private Date orderDate;
	
	private long orderTotal;

	private IncomeExpenseAccount unmatchedAccount;

	/**
	 * This form is used when no transaction exists in the session for this
	 * order.  A new transaction is created.
	 * 
	 * @param orderNumber
	 * @param transaction
	 * @throws ImportException 
	 */
	public AmazonOrder(String orderNumber, Session session, AccountFinder accountFinder) throws ImportException {
		this.orderNumber = orderNumber;
		this.accountFinder = accountFinder;

		unmatchedAccount = accountFinder.findUnmatchedAccount();
	}
	
	/**
	 * This form is used when a transaction already exists for this order.
	 * 
	 * @param orderNumber
	 * @param transaction
	 * @throws ImportException 
	 */
	public AmazonOrder(String orderNumber, Transaction[] transactions, AccountFinder accountFinder) throws ImportException {
		this.orderNumber = orderNumber;
		this.accountFinder = accountFinder;

		unmatchedAccount = accountFinder.findUnmatchedAccount();
		
		assert transactions.length >= 1;
		
		for (Transaction transaction : transactions) {
			AmazonShipment shipment = new AmazonShipment(this, transaction, accountFinder);
			shipments.add(shipment);
		}
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
			shipment.getTransaction().setDate(orderDate);
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
	public AmazonOrderItem createNewItem(String description, String quantityAsString, long itemAmount, ShipmentObject shipmentObject, Session session) throws ImportException {
		if (shipmentObject.shipment == null) {
			shipmentObject.shipment = new AmazonShipment(this, session, accountFinder);
			// Is this correct, or do this in setOrderDate?
			shipmentObject.shipment.getTransaction().setDate(orderDate);
		
			shipments.add(shipmentObject.shipment);
		}
		
		Entry entry = shipmentObject.shipment.getTransaction().createEntry();
		entry.setAmount(itemAmount);
		entry.setAccount(unmatchedAccount);
		AmazonEntry amazonEntry = entry.getExtension(AmazonEntryInfo.getPropertySet(), true);
		AmazonOrderItem item = new AmazonOrderItem(amazonEntry);
		if (quantityAsString == null) {
			item.getEntry().setMemo(description);
		} else {
			item.getEntry().setMemo(description + " x" + quantityAsString);
		}
		item.getEntry().setAmazonDescription(description);
		item.getEntry().setOrderId(orderNumber);
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
