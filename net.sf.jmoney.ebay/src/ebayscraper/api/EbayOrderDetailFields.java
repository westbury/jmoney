package ebayscraper.api;

/**
 * This interface contains the fields that are available for an order when
 * the order details page has been processed.  This interface extends {@link EbayOrderFields}
 * which contains the fields available for an order from the order listings page.
 * 
 * @author Nigel Westbury
 *
 */
public interface EbayOrderDetailFields extends EbayOrderFields {

	String getSubtotal();

	String getGiftcardAmount();

	String getPromotionAmount();

	String getImportFeesDepositAmount();

	String getGrandTotal();

	String getRefundTotal();

	String getPostageAndPackaging();

	String getLastFourDigits();

}
