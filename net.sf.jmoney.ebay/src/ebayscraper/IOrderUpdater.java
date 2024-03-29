package ebayscraper;

import java.util.Set;

public interface IOrderUpdater {

	IItemUpdater createNewItemUpdater();

	Set<? extends IItemUpdater> getItemUpdaters();

	long getPostageAndPackaging();

	void setPostageAndPackaging(long postageAndPackagingAmount);

	long getDiscount();

	void setDiscount(long discountAmount);

	long getChargeAmount();

	void setChargeAmount(long amount);

	/**
	 * It may be that the charge entry for this shipment has already been matched to an entry
	 * imported from the bank, or from some other source.  In such a situation, the charge amount
	 * cannot be adjusted when further data becomes available from Amazon.
	 * 
	 * @return <code>true</code> if the amount charged for this shipment cannot be changed,
	 * 			<code>false</code> if the charge amount has not been matched to anything outside
	 * 			Amazon import data and thus can be adjusted 
	 */
	boolean isChargeAmountFixed();

	void setLastFourDigitsOfAccount(String lastFourDigits);

	/**
	 * This is done after charge account has been determined, including after details page has
	 * been pasted, but before finally committing.
	 */
	void matchChargeEntry();

}
