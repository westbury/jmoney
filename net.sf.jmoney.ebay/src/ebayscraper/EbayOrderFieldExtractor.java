package ebayscraper;

import java.util.ArrayList;
import java.util.List;

import ebayscraper.api.EbayOrderListItemFields;
import ebayscraper.api.EbayOrderListOrderFields;
import txr.matchers.MatchResults;

public class EbayOrderFieldExtractor implements EbayOrderListOrderFields {

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
	public List<EbayOrderListItemFields> getItems() {
		List<EbayOrderListItemFields> result = new ArrayList<>();
	
		for (MatchResults itemBindings : orderBindings.getCollections(0)) {
			result.add(
				new EbayOrderListItemFields() {

					@Override
					public String getItemNumber() {
						return itemBindings.getVariable("itemnumber").text;
					}

					@Override
					public String getDescription() {
						return itemBindings.getVariable("description").text;
					}

					@Override
					public String getUnitPrice() {
						// TODO this is currently not available so same as item price
						return itemBindings.getVariable("itemprice").text;
					}

//					@Override
//					public String getDetail() {
//						return itemBindings.getVariable("detail").text;
//					}

					@Override
					public String getItemPrice() {
						return itemBindings.getVariable("itemprice").text;
					}

					@Override
					public String getAmount() {
						// This is available from the detail page only
						return null;
					}

					@Override
					public String getSeller() {
						return itemBindings.getVariable("seller").text;
					}

					@Override
					public String getDeliveryDate() {
						return itemBindings.getVariable("deliverydate").text;
					}
					
				});
		}

		return result;
	}

}
