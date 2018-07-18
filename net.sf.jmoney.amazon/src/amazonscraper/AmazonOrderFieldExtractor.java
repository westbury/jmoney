package amazonscraper;

import java.util.ArrayList;
import java.util.List;

import amazonscraper.api.AmazonOrderFields;
import amazonscraper.api.AmazonShipmentFields;
import txr.matchers.MatchResults;

public class AmazonOrderFieldExtractor implements AmazonOrderFields {

	protected MatchResults orderBindings;
	
	public AmazonOrderFieldExtractor(MatchResults orderBindings) {
		this.orderBindings = orderBindings;
	}

	@Override
	public String getOrderDate() {
		return orderBindings.getVariable("date").text;
	}

	@Override
	public String getOrderNumber() {
		return orderBindings.getVariable("ordernumber").text;
	}

	@Override
	public String getTotal() {
		return orderBindings.getVariable("totalamount").text;
	}

	@Override
	public List<AmazonShipmentFields> getShipments() {
		List<AmazonShipmentFields> result = new ArrayList<>();
	
		for (MatchResults shipmentBindings : orderBindings.getCollections(0)) {
			result.add(new AmazonShipmentFieldExtractor(shipmentBindings));
		}

		return result;
	}

}
