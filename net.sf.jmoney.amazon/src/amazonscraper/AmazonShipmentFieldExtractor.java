package amazonscraper;

import java.util.ArrayList;
import java.util.List;

import amazonscraper.api.AmazonItemFields;
import amazonscraper.api.AmazonShipmentFields;
import txr.matchers.MatchResults;

final class AmazonShipmentFieldExtractor implements AmazonShipmentFields {
	private final MatchResults shipmentBindings;

	AmazonShipmentFieldExtractor(MatchResults shipmentBindings) {
		this.shipmentBindings = shipmentBindings;
	}

	@Override
	public String getMovieName() {
		return shipmentBindings.getVariable("moviename").text;
	}

	@Override
	public boolean isGiftcardPurchase() {
		return "true".equals(shipmentBindings.getVariable("giftcardpurchase").text);
	}

	@Override
	public String getGiftcardMessage() {
		return shipmentBindings.getVariable("giftcardmessage").text;
	}

	@Override
	public String getItemAmount() {
		return shipmentBindings.getVariable("itemamount").text;
	}

	@Override
	public String getRecipient() {
		return shipmentBindings.getVariable("recipient").text;
	}

	@Override
	public String getExpectedDate() {
		return shipmentBindings.getVariable("expecteddate").text;
	}

	@Override
	public String getDeliveryDate() {
		return shipmentBindings.getVariable("deliverydate").text;
	}

	@Override
	public boolean isNotDispatched() {
		String asText = shipmentBindings.getVariable("isnotdispatched").text;
		return "true".equals(asText);
	}

	@Override
	public boolean isReturned() {
		String asText = shipmentBindings.getVariable("returned").text;
		return "true".equals(asText);
	}

	@Override
	public boolean isExchanged() {
		String asText = shipmentBindings.getVariable("exchanged").text;
		return "true".equals(asText);
	}

	@Override
	public List<AmazonItemFields> getItems() {
		List<AmazonItemFields> result = new ArrayList<>();
		
		for (MatchResults itemBindings : shipmentBindings.getCollections(0)) {
			result.add(
					new AmazonItemFields() {

						@Override
						public String getDescription() {
							return itemBindings.getVariable("description").text;
						}

						@Override
						public String getUnitPrice() {
							return itemBindings.getVariable("itemamount").text;
						}

						@Override
						public String getQuantity() {
							return itemBindings.getVariable("quantity").text;
						}

						@Override
						public String getSellerName() {
							return itemBindings.getVariable("soldby").text;
						}

						@Override
						public String getAuthor() {
							return itemBindings.getVariable("author").text;
						}

						@Override
						public String getReturnDeadline() {
							return itemBindings.getVariable("returndeadline").text;
						}

						@Override
						public boolean isItemOverseas() {
							return "true".equals(itemBindings.getVariable("overseas").text);
						}
						
					});
		}

		return result;
	}
}