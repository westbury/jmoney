package ebayscraper;

import java.util.ArrayList;
import java.util.List;

import ebayscraper.api.EbayDetailItemFields;
import ebayscraper.api.EbayDetailOrderFields;
import ebayscraper.api.EbayDetailPaymentFields;
import txr.matchers.MatchResults;

public class EbayPaymentDetailFieldExtractor implements EbayDetailPaymentFields {

	protected MatchResults paymentBindings;
	
	public EbayPaymentDetailFieldExtractor(MatchResults paymentBindings) {
		this.paymentBindings = paymentBindings;
	}

	@Override
	public String getLastFourDigits() {
		return paymentBindings.getVariable("lastfourdigits").text;
	}

	@Override
	public String getItemTotal() {
		return paymentBindings.getVariable("itemtotal2").text;
	}

	@Override
	public String getDiscount() {
		return paymentBindings.getVariable("discount").text;
	}

	@Override
	public String getShippingCost() {
		return paymentBindings.getVariable("shipping").text;
	}

	@Override
	public String getAmountCharged() {
		return paymentBindings.getVariable("totalforday").text;
	}

	@Override
	public List<EbayDetailOrderFields> getOrders() {
		List<EbayDetailOrderFields> result = new ArrayList<>();
		
		for (MatchResults orderBindings : paymentBindings.getCollections(0)) {
//			result.add(new EbayOrderDetailFieldExtractor(itemBindings));
			result.add(
					new EbayDetailOrderFields() {

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
						public String getSeller() {
							return orderBindings.getVariable("seller").text;
						}
						
						@Override
						public String getCarrier() {
							return orderBindings.getVariable("carrier").text;
						}

						@Override
						public String getShippingService() {
							return orderBindings.getVariable("shippingservice").text;
						}

						@Override
						public String getTrackingNumber() {
							return orderBindings.getVariable("trackingnumber").text;
						}

						@Override
						public String getDayOfYearPaid() {
							return orderBindings.getVariable("dayofyearpaid").text;
						}

						@Override
						public String getDayOfYearShipped() {
							return orderBindings.getVariable("dayofyearshipped").text;
						}

						public String getDeliveryDate() {
							return orderBindings.getVariable("deliverydate").text;
						}

						@Override
						public List<EbayDetailItemFields> getItems() {
							List<EbayDetailItemFields> result = new ArrayList<>();
							
							for (MatchResults itemBindings : orderBindings.getCollections(0)) {
								result.add(
									new EbayDetailItemFields() {

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
											return itemBindings.getVariable("unitprice").text;
										}

										@Override
										public String getAmount() {
											return itemBindings.getVariable("amount").text;
										}

										@Override
										public String getDetail() {
											return itemBindings.getVariable("detail").text;
										}
									});
							}

							return result;
						}
					});
		}

		return result;
	}
}
