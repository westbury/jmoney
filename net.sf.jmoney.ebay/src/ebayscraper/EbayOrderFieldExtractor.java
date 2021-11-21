package ebayscraper;

import java.util.ArrayList;
import java.util.List;

import ebayscraper.api.EbayItemFields;
import ebayscraper.api.EbayOrderFields;
import txr.matchers.MatchResults;

public class EbayOrderFieldExtractor implements EbayOrderFields {

	protected MatchResults orderBindings;
	
	public EbayOrderFieldExtractor(MatchResults orderBindings) {
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
	public String getSeller() {
		return orderBindings.getVariable("seller").text;
	}

	@Override
	public String getTotal() {
		return orderBindings.getVariable("ordertotal").text;
	}

	@Override
	public String getDescription() {
		return orderBindings.getVariable("description").text;
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

}
