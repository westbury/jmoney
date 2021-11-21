package analyzer;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import ebayscraper.IItemUpdater;
import ebayscraper.IOrderUpdater;
import net.sf.jmoney.importer.wizards.ImportException;

public class EbayOrder {

	private IOrderUpdater orderUpdater;
	
	private Date orderDate;
	
	private String orderNumber;

	private String seller;

	private List<EbayOrderItem> items = new ArrayList<>();

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
	public EbayOrder(String orderNumber, IOrderUpdater orderUpdater) {
		this.orderNumber = orderNumber;
		this.orderUpdater = orderUpdater;
	}
	
	public void addItem(EbayOrderItem item) {
		items.add(item);
	}
	
	public String getOrderNumber() {
		return orderNumber;
	}

	public String getSeller() {
		return seller;
	}

	public void setSeller(String seller) {
		this.seller = seller;
	}

	public List<EbayOrderItem> getItems() {
		return items;
	}

	public void setOrderDate(Date orderDate) {
		// TODO check same if already set
		this.orderDate = orderDate;
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
	public EbayOrderItem createNewItem(String itemNumber, String description, long netCost) {
		IItemUpdater itemUpdater = this.orderUpdater.createNewItemUpdater(netCost);
		
		itemUpdater.setOrderNumber(orderNumber);
		EbayOrderItem item = new EbayOrderItem(this, itemUpdater);
		
		// Must be set here, not in updater, so we can be more sure we have it even
		// if the updater does not process it.
		item.setEbayDescription(description);
		
		items.add(item);
		return item;
	}

}
