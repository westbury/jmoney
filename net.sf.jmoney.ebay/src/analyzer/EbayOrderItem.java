package analyzer;

import java.util.Date;

import ebayscraper.IItemUpdater;

public class EbayOrderItem {

	EbayOrder order;
	
	private IItemUpdater updater;
	
	private String soldBy;

	int quantity = 1;

	private long netCost;

	private String ebayDescription;

	private Date paidDate;
	
	private Date shipDate;
	
	public EbayOrderItem(EbayOrder order, IItemUpdater updater) {
		this.order = order;
		this.updater = updater;
		
		this.netCost = updater.getNetCost();
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
		updater.setNetCost(netCost);
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

	public void setSoldBy(String soldBy) {
		this.soldBy = soldBy;
	}

	public String getSoldBy() {
		return soldBy;
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

	public Date getPaidDate() {
		return paidDate;
	}

	public void setPaidDate(Date paidDate) {
		this.paidDate = paidDate;
		updater.setPaidDate(paidDate);
	}

	public Date getShipDate() {
		return shipDate;
	}

	public void setShipDate(Date shipDate) {
		this.shipDate = shipDate;
		updater.setShipDate(shipDate);
	}
}
