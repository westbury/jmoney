package ebayscraper;

import java.util.ArrayList;
import java.util.List;

import ebayscraper.api.EbayItemFields;
import ebayscraper.api.EbayOrderDetailFields;
import txr.matchers.MatchResults;

public class EbayOrderDetailFieldExtractor implements EbayOrderDetailFields {

	protected MatchResults orderBindings;
	
	public EbayOrderDetailFieldExtractor(MatchResults orderBindings) {
		this.orderBindings = orderBindings;
	}

	@Override
	public String getOrderDate() {
		return orderBindings.getVariable("orderdate").text;
	}

	@Override
	public String getOrderNumber() {
		return orderBindings.getVariable("ordernumber").text;
	}

	@Override
	public String getTotal() {
		// Is this the same amount as getTotal ("totalamount") from
		// the orders listing page?
		return orderBindings.getVariable("amount").text;
	}

	@Override
	public String getDescription() {
		return orderBindings.getVariable("description").text;
	}

	@Override
	public String getSeller() {
		return orderBindings.getVariable("seller").text;
	}

	@Override
	public List<EbayItemFields> getItems() {
		List<EbayItemFields> result = new ArrayList<>();
		
		for (MatchResults itemBindings : orderBindings.getCollections(0)) {
			result.add(
				new EbayItemFields() {

					@Override
					public String getDescription() {
						return itemBindings.getVariable("description2").text;
					}

					@Override
					public String getUnitPrice() {
						// TODO this is currently not available so same as item price
						return itemBindings.getVariable("itemprice").text;
					}

					@Override
					public String getQuantity() {
						return "1";
//						return itemBindings.getVariable("quantity").text;
					}

					@Override
					public String getSellerName() {
						return itemBindings.getVariable("soldby").text;
					}

					@Override
					public String getItemNumber() {
						return itemBindings.getVariable("itemnumber").text;
					}

					@Override
					public String getPaidDate() {
						return itemBindings.getVariable("paiddate").text;
					}

					@Override
					public String getShipDate() {
						return itemBindings.getVariable("shipdate").text;
					}
					
					@Override
					public String getItemPrice() {
						return itemBindings.getVariable("itemprice").text;
					}
					
				});
		}

		return result;
	}

	// These fields are available only from the details page.
	
	@Override
	public String getSubtotal() {
		return orderBindings.getVariable("subtotal").text;
	}

	@Override
	public String getGiftcardAmount() {
		return orderBindings.getVariable("giftcard").text;
	}

	@Override
	public String getPromotionAmount() {
		return orderBindings.getVariable("promotion").text;
	}

	@Override
	public String getImportFeesDepositAmount() {
		return orderBindings.getVariable("importfeesdeposit").text;
	}

	@Override
	public String getGrandTotal() {
		return orderBindings.getVariable("grandtotal").text;
	}

	@Override
	public String getRefundTotal() {
		return orderBindings.getVariable("refundtotal").text;
	}

	@Override
	public String getLastFourDigits() {
		return orderBindings.getVariable("lastfourdigits").text;
	}

	@Override
	public String getPostageAndPackaging() {
		return orderBindings.getVariable("postageandpackaging").text;
	}

}
