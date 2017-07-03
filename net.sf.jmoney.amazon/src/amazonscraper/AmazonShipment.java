package amazonscraper;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class AmazonShipment {

	private List<AmazonOrderItem> items = new ArrayList<>();

	private String expectedDate;

	private Date deliveryDate;

	private boolean chargeAmountStale;

	private AmazonOrder order;

	boolean returned;

	public boolean overseas = false;

	private IShipmentUpdater updater;

	private long promotionAmount;

	private long giftcardAmount;

	public AmazonShipment(AmazonOrder amazonOrder, IShipmentUpdater shipmentUpdater) {
		this.order = amazonOrder;
		this.updater = shipmentUpdater;

		chargeAmountStale = false;
		
		/*
		 * Create items from data that already exists in the accounting database.
		 */
		for (IItemUpdater itemUpdater : updater.getItemUpdaters()) {
			items.add(new AmazonOrderItem(this, itemUpdater));
		}
	}

	public void addItem(AmazonOrderItem item) {
		items.add(item);
		chargeAmountStale = true;
	}
	
	public List<AmazonOrderItem> getItems() {
		return items;
	}

	public void setExpectedDate(String expectedDate) {
		this.expectedDate = expectedDate;
	}

	public String getExpectedDate() {
		return expectedDate;
	}

	public void setDeliveryDate(Date deliveryDate) {
		this.deliveryDate = deliveryDate;
	}

	public Date getDeliveryDate() {
		return deliveryDate;
	}

	public void setPostageAndPackaging(long postageAndPackagingAmount) {
		updater.setPostageAndPackaging(postageAndPackagingAmount);
		chargeAmountStale = true;
	}

	/**
	 * 
	 * 
	 * @return the amount charged to the charge account or null if
	 * 				this has not been determined
	 */
	public Long getChargeAmount() {
		if (chargeAmountStale) {
			assert updater.getChargeAmount() == 0;
			return null;
		} else {
			long amount = updater.getChargeAmount();
			return amount==0 ? null : amount;
		}
	}

	public void setChargeAmount(long amount) {
		updater.setChargeAmount(amount);
		chargeAmountStale = false;
	}

	public void setGiftcardAmount(long giftcardAmount) {
		this.giftcardAmount = giftcardAmount; 
		updater.setGiftcardAmount(giftcardAmount);
		chargeAmountStale = true;
	}

	public void setPromotionAmount(long promotionAmount) {
		this.promotionAmount = promotionAmount; 
		updater.setPromotionAmount(promotionAmount);
		chargeAmountStale = true;
	}

	public void setLastFourDigitsOfAccount(String lastFourDigits) {
		// The underlying method should check if the account is being improperly changed
		// (entry was imported or reconciled from bank).
		updater.setLastFourDigitsOfAccount(lastFourDigits);
	}

	public void setReturned(boolean returned) {
		this.returned = returned;
	}

	public IShipmentUpdater getShipmentUpdater() {
		return updater;
	}

	public long getPostageAndPackaging() {
		return updater.getPostageAndPackaging();
	}

	public long getImportFeesDeposit() {
		return updater.getImportFeesDeposit();
	}

	public long getPromotionAmount() {
		return promotionAmount;
	}

	public void setImportFeesDeposit(long importFeesDepositAmount) {
		updater.setImportFeesDeposit(importFeesDepositAmount);
		chargeAmountStale = true;
	}

	public void setCalculatedChargeAmount() {
		long total = 0;
		for (AmazonOrderItem item : items) {
			total += item.getNetCost();
		}
		total += getPostageAndPackaging();
		total += getImportFeesDeposit();
		total -= promotionAmount;
		total -= giftcardAmount;

		if (updater.isChargeAmountFixed()) {
			if (-total != updater.getChargeAmount()) {
				throw new RuntimeException("Can't update the charge amount because it has been matched to other imports (e.g. import from bank).");
			}
		} else {
			setChargeAmount(-total);
		}
		
		chargeAmountStale = false;
	}

	public boolean isReturn() {
		return returned;
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

	public AmazonOrder getOrder() {
		return order;
	}

}
