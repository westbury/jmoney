package net.sf.jmoney.amazon.copytext;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;

import net.sf.jmoney.amazon.AccountFinder;
import net.sf.jmoney.amazon.AmazonEntry;
import net.sf.jmoney.amazon.AmazonEntryInfo;
import net.sf.jmoney.importer.wizards.ImportException;
import net.sf.jmoney.model2.Account;
import net.sf.jmoney.model2.CapitalAccount;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.IncomeExpenseAccount;
import net.sf.jmoney.model2.Session;
import net.sf.jmoney.model2.Transaction;

public class AmazonOrder {

	private String orderNumber;

	private List<AmazonShipment> shipments = new ArrayList<>();

	private Date orderDate;
	
	private long orderTotal;

	/**
	 * This form is used when no transaction exists in the session for this
	 * order.  A new transaction is created.
	 * 
	 * @param orderNumber
	 * @param transaction
	 */
	public AmazonOrder(String orderNumber, Session session) {
		this.orderNumber = orderNumber;
		
	}
	
	/**
	 * This form is used when a transaction already exists for this order.
	 * 
	 * @param orderNumber
	 * @param transaction
	 */
	public AmazonOrder(String orderNumber, Transaction[] transactions) {
		this.orderNumber = orderNumber;
		
		assert transactions.length >= 1;
		
		for (Transaction transaction : transactions) {
			AmazonShipment shipment = new AmazonShipment(transaction);
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

	public void setOrderTotal(String orderTotalAsString) {
		this.orderTotal = new BigDecimal(orderTotalAsString).scaleByPowerOfTen(2).longValueExact();
		
		if (shipments.size() == 1) {
			shipments.get(0).chargeEntry.setAmount(-orderTotal);
		} else {
			// TODO how do we handle this???
		}
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
	 */
	public AmazonOrderItem createNewItem(String description, long itemAmount, ShipmentObject shipmentObject, Session session) {
		IncomeExpenseAccount unmatchedAccount;
		try {
			unmatchedAccount = AccountFinder.findDefaultPurchaseAccount(session, session.getCurrencyForCode("GBP"));
		} catch (ImportException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		
		if (shipmentObject.shipment == null) {
			shipmentObject.shipment = new AmazonShipment(session);
			// Is this correct, or do this in setOrderDate?
			shipmentObject.shipment.getTransaction().setDate(orderDate);
		
			shipments.add(shipmentObject.shipment);
		}
		
		Entry entry = shipmentObject.shipment.getTransaction().createEntry();
		entry.setAmount(itemAmount);
		entry.setAccount(unmatchedAccount);
		AmazonEntry amazonEntry = entry.getExtension(AmazonEntryInfo.getPropertySet(), true);
		AmazonOrderItem item = new AmazonOrderItem(amazonEntry);
		item.getEntry().setMemo(description);
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
