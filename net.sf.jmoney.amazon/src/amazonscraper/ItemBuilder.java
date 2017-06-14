package amazonscraper;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

class ItemBuilder {

	AmazonOrder order;

	Set<AmazonOrderItem> preexistingItems;

	public ItemBuilder(AmazonOrder order, List<AmazonOrderItem> items) {
		this.order = order;
		preexistingItems = new HashSet<>(items);
	}

	public AmazonOrderItem get(String asin, String description, String quantityAsString, long itemAmount, ShipmentObject shipmentObject) {
		// Find the matching entry
		if (preexistingItems.isEmpty()) {
			AmazonOrderItem item = order.createNewItem(description, quantityAsString, itemAmount, shipmentObject);
			if (asin != null) {
				item.getUnderlyingItem().setAsinOrIsbn(asin);
			}
			return item;
		} else {
			AmazonOrderItem[] matches = preexistingItems.stream().filter(item -> item.getUnderlyingItem().getAmount() == itemAmount).toArray(AmazonOrderItem[]::new);
			if (matches.length > 1) {
				matches = Stream.of(matches).filter(item -> item.getUnderlyingItem().getAmazonDescription().equals(description)).toArray(AmazonOrderItem[]::new);
			}
			if (matches.length != 1) {
				throw new RuntimeException("Existing transaction for order does not match up.");
			}
			AmazonOrderItem matchingItem = matches[0];

			/*
			 * Check the shipment splitting is consistent (i.e. has not changed).
			 * 
			 */
			AmazonShipment shipmentOfThisItem = matchingItem.getShipment();
			if (shipmentObject.shipment != null) {
				if (shipmentObject.shipment != shipmentOfThisItem) {
					throw new RuntimeException("Inconsistent shipment");
				}
			} else {
				shipmentObject.shipment = shipmentOfThisItem;
			}

			preexistingItems.remove(matchingItem);
			return matchingItem;
		}
	}

	/**
	 * This method is used only to assert that all the items matched,
	 * none were left unmatched.
	 * 
	 * @return
	 */
	public boolean isEmpty() {
		return preexistingItems.isEmpty();
	}
}