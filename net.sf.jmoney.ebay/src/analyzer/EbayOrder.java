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

	/** total before shipping and discount, so this is total of items, not charged amount */
//	private long itemTotal;

	/** set only if a detail page, as the order total is only available there.  Not currently used because calculated, but we should check the value */
	private long orderTotal;

	private Date paidDate;
	
	private Date shippingDate;

//	private IncomeExpenseAccount unmatchedAccount;

	private boolean chargeAmountStale;

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

		/*
		 * Create items from data that already exists in the accounting database.
		 */
		for (IItemUpdater itemUpdater : orderUpdater.getItemUpdaters()) {
			items.add(new EbayOrderItem(this, itemUpdater));
		}
		
		chargeAmountStale = false;
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

	/** the amount charged, set only from order detail page */
	public void setOrderTotal(long orderTotal) {
		this.orderTotal = orderTotal;
	}

	/** the amount charged, calculated if we have only imported order list */
	public long getOrderTotal() {
		long total = 0;
		for (EbayOrderItem item : items) {
			total += item.getNetCost();
		}

		return total + this.orderUpdater.getPostageAndPackaging() - this.orderUpdater.getDiscount();
	}

	public void setLastFourDigitsOfAccount(String lastFourDigits) {
		// The underlying method should check if the account is being improperly changed
		// (entry was imported or reconciled from bank).
		orderUpdater.setLastFourDigitsOfAccount(lastFourDigits);
	}

	public Date getPaidDate() {
		return paidDate;
	}

	public void setPaidDate(Date paidDate) {
		this.paidDate = paidDate;
		
		// TODO this needs to be set as the 'cleared' date in the charge account entry
//		updater.setPaidDate(paidDate);
	}

	public Date getShippingDate() {
		return shippingDate;
	}

	public void setShippingDate(Date shippingDate) {
		this.shippingDate = shippingDate;

		// Not persisted in database as not really important. This could be removed.
	}

	/**
	 * Mostly delivery date is an item property, and it is stored per-item.\
	 * However the order detail page contains the delivery date as a single value for
	 * the order, so we have this setter to support that.
	 *
	 */
	public void setDeliveryDate(Date deliveryDate) {
		for (EbayOrderItem item : items) {
			item.getUnderlyingItem().setDeliveryDate(deliveryDate);
		}
	}

	/**
	 * Shipping cost is distributed across the items, so there is no separate entry
	 * in the transaction.  However we do have a special amount field in the transaction object itself.
	 * 
	 * @return
	 */
	public long getPostageAndPackaging() {
		return this.orderUpdater.getPostageAndPackaging();
	}

	public void setPostageAndPackaging(long amount) {
		this.orderUpdater.setPostageAndPackaging(amount);
		setCalculatedChargeAmount();
	}

	/**
	 * A discount is distributed across the items, so there is no separate entry
	 * in the transaction.  However we do have a special amount field in the transaction object itself.
	 * 
	 * @return
	 */
	public long getDiscount() {
		return this.orderUpdater.getDiscount();
	}

	public void setDiscount(long amount) {
		this.orderUpdater.setDiscount(amount);
		setCalculatedChargeAmount();
	}

	/**
	 * 
	 * 
	 * @return the amount charged to the charge account or null if
	 * 				this has not been determined
	 */
	public Long getChargeAmount() {
		if (chargeAmountStale) {
			assert orderUpdater.getChargeAmount() == 0;
			return null;
		} else {
			long amount = orderUpdater.getChargeAmount();
			return amount==0 ? null : amount;
		}
	}

	public void setChargeAmount(long amount) {
		orderUpdater.setChargeAmount(amount);
		chargeAmountStale = false;
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
	public EbayOrderItem createNewItem(String itemNumber, String description) {
		IItemUpdater itemUpdater = this.orderUpdater.createNewItemUpdater();
		
		itemUpdater.setOrderNumber(orderNumber);
		itemUpdater.setItemNumber(itemNumber);
		EbayOrderItem item = new EbayOrderItem(this, itemUpdater);
		
		// Must be set here, not in updater, so we can be more sure we have it even
		// if the updater does not process it.
		item.setEbayDescription(description);
		
		items.add(item);
		chargeAmountStale = true;

		return item;
	}

	public IOrderUpdater getUnderlyingOrder() {
		return orderUpdater;
	}

	public void setCalculatedChargeAmount() {
		long total = 0;
		for (EbayOrderItem item : items) {
			total += item.getNetCost();
		}

		long adjustments = getPostageAndPackaging() - getDiscount();
		long orderTotal = total + adjustments;

		if (orderUpdater.isChargeAmountFixed()) {
			if (-orderTotal != orderUpdater.getChargeAmount()) {
				throw new RuntimeException("Can't update the charge amount because it has been matched to other imports (e.g. import from bank).");
			}
		} else {
			setChargeAmount(-orderTotal);
		}

		// Update the item gross prices.  This is done by pro-rating the shipping and discount across the items.
		long total2 = 0;
		for (EbayOrderItem item : items) {
			long proportion = (long)(((float)item.getNetCost()) / total * adjustments);
			total2 += proportion;
		}
		
		long error = adjustments - total2;

		for (EbayOrderItem item : items) {
			long proportion = (long)(((float)item.getNetCost()) / total * adjustments);
			long amount = item.getNetCost() + proportion;
			if (error-- > 0) {
				amount++;
			}
			item.setGrossCost(amount);
		}

		
		chargeAmountStale = false;
	}

	/**
	 * Flushes changes to the underlying datastore.
	 * <P>
	 * We need this method because not all changes are immediately reflected in the underlying datastore.
	 * For example, we don't update the calculated charge amount each time some
	 * other property's amount changes.  That would prevent us, when the charge amount is fixed, from making changes to
	 * multiple amounts that together leave the charge amount unchanged.
	 */
	public void flush() {
		// If charge amount is not set, add up the transaction to set it.
		if (chargeAmountStale) {
			setCalculatedChargeAmount();
		}
	}

}
