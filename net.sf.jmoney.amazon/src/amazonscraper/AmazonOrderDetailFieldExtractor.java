package amazonscraper;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import amazonscraper.api.AmazonOrderDetailFields;
import amazonscraper.api.AmazonShipmentFields;
import txr.matchers.MatchResults;

public class AmazonOrderDetailFieldExtractor implements AmazonOrderDetailFields {

	protected MatchResults orderBindings;
	
	public AmazonOrderDetailFieldExtractor(MatchResults orderBindings) {
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
		return orderBindings.getVariable("total").text;
	}

	@Override
	public List<AmazonShipmentFields> getShipments() {
		return orderBindings.getCollections(0).stream()
				.map(shipmentBindings -> new AmazonShipmentFieldExtractor(shipmentBindings))
				.collect(Collectors.toList());
		
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
