package amazonscraper;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class AmazonShipment {

	private List<AmazonOrderItem> items = new ArrayList<>();

	private String expectedDate;

	private Date deliveryDate;

	private boolean chargeAmountToBeDetermined;

	private AmazonOrder order;

	boolean returned;

	public boolean overseas = false;

	private IShipmentUpdater updater;

	public AmazonShipment(AmazonOrder amazonOrder, IShipmentUpdater shipmentUpdater) {
		this.order = amazonOrder;
		this.updater = shipmentUpdater;

		chargeAmountToBeDetermined = false;
	}

	public void addItem(AmazonOrderItem item) {
		items.add(item);
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

		// And don't forget we must keep the transaction balanced.
	// This assumes no giftcard amount previously was set
	// TODO this class should be responsible for checking if amount changed....
		if (!chargeAmountToBeDetermined) {
			setChargeAmount(getChargeAmount() - postageAndPackagingAmount);
		}
	}

	/**
	 * 
	 * 
	 * @return the amount charged to the charge account or null if
	 * 				this has not been determined
	 */
	public Long getChargeAmount() {
		if (chargeAmountToBeDetermined) {
			assert updater.getChargeAmount() == 0;
			return null;
		} else {
			long amount = updater.getChargeAmount();
			return amount==0 ? null : amount;
		}
	}

	public void setChargeAmount(long amount) {
		updater.setChargeAmount(amount);
		chargeAmountToBeDetermined = false;
	}

	public void setGiftcardAmount(long giftcardAmount) {
		updater.setGiftcardAmount(giftcardAmount);

			// And don't forget we must keep the transaction balanced.
		// This assumes no giftcard amount previously was set
		// TODO this class should be responsible for checking if amount changed....
			if (!chargeAmountToBeDetermined) {
				setChargeAmount(getChargeAmount() + giftcardAmount);
			}
	}

	public void setPromotionAmount(long promotionAmount) {
		updater.setPromotionAmount(promotionAmount);

			// And don't forget we must keep the transaction balanced.
		// This assumes no promotion amount previously was set
		// TODO this class should be responsible for checking if amount changed....
			if (!chargeAmountToBeDetermined) {
				setChargeAmount(getChargeAmount() + promotionAmount);
			}
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
		final Long postageAndPackaging = updater.getPostageAndPackaging();
		return postageAndPackaging == null ? 0 : postageAndPackaging;
	}

	public void setCalculatedChargeAmount() {
		long total = 0;
		for (AmazonOrderItem item : items) {
			total += item.getUnitPrice() * item.getQuantity();
		}
		total -= getPostageAndPackaging();
		
		setChargeAmount(-total);
	}

	public boolean isReturn() {
		return returned;
	}

}
