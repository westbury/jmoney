package analyzer;

import java.util.Date;

import ebayscraper.IItemUpdater;

public class EbayOrderItem {

	EbayOrder order;
	
	private IItemUpdater updater;
	
	int quantity = 1;

	private String detail;
	
	private long netCost;

	private String ebayDescription;

	private String imageCode;
	
	public EbayOrderItem(EbayOrder order, IItemUpdater updater) {
		this.order = order;
		this.updater = updater;

		// No, we don't know the net cost from the database.  The database has
		// just the gross cost.  Net cost is set only from the imported data.
//		this.netCost = updater.getNetCost();

		this.ebayDescription = updater.getEbayDescription();
//???		this.quantity = updater.getQuantity();
	}

	/**
	 * If the quantity is more than one then this is the
	 * line item price, ie the unit price times
	 * the quantity.
	 */
	public void setNetCost(long netCost) {
		this.netCost = netCost;
	}

	/**
	 * If the quantity is more than one then this is the
	 * line item price, ie the unit price times
	 * the quantity.
	 * 
	 * @return
	 */
	public long getNetCost() {
		return netCost;
	}

	public void setGrossCost(long amount) {
		updater.setGrossCost(amount);
	}

	public void setQuantity(int quantity) {
		this.quantity = quantity;
		updater.setQuantity(quantity);
	}

	public int getQuantity() {
		return quantity;
	}


	public IItemUpdater getUnderlyingItem() {
		return updater;
	}

	public EbayOrder getOrder() {
		return order;
	}

	public String getItemNumber() {
		return updater.getItemNumber();
	}

	public String getEbayDescription() {
		return ebayDescription;
	}

	public void setEbayDescription(String ebayDescription) {
		this.ebayDescription = ebayDescription;
		updater.setDescription(ebayDescription);
	}

	public String toString() {
		return ebayDescription == null ? Long.toString(netCost) : ebayDescription;
	}

	public void setImageCode(String imageCode) {
		this.imageCode = imageCode;
		updater.setImageCode(imageCode);
	}

	public String getDetail() {
		return detail;
	}

	public void setDetail(String detail) {
		this.detail = detail;
	}
}
