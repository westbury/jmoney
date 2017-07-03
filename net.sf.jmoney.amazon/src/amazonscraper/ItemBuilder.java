package amazonscraper;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BinaryOperator;
import java.util.stream.Stream;

class ItemBuilder {

	AmazonOrder order;

	Set<AmazonOrderItem> preexistingItems;

	public ItemBuilder(AmazonOrder order, List<AmazonOrderItem> items) {
		this.order = order;
		preexistingItems = new HashSet<>(items);
	}

	public AmazonOrderItem get(String asin, String description, long netCost, ShipmentObject shipmentObject) {
		// Find the matching entry
		if (preexistingItems.isEmpty()) {
			AmazonOrderItem item = order.createNewItem(description, netCost, shipmentObject);
			if (asin != null) {
				item.getUnderlyingItem().setAsinOrIsbn(asin);
			}
			return item;
		} else {
			AmazonOrderItem[] matches = preexistingItems.stream().filter(item -> item.getNetCost() == netCost).toArray(AmazonOrderItem[]::new);
			if (matches.length > 1) {
				matches = Stream.of(matches).filter(item -> item.getAmazonDescription().equals(description)).toArray(AmazonOrderItem[]::new);
			} else {
				/* This may be an overseas order or some other complex order in which we don't
				 * know the item price.  So try matching on description only.
				 * We want to match on the sale or return as appropriate so we do check the sign
				 * of the net cost.   
				 */
				if (matches.length == 0) {
					matches = preexistingItems.stream().filter(item -> item.getAmazonDescription().equals(description) && (item.getNetCost() > 0) == (netCost > 0)).toArray(AmazonOrderItem[]::new);
				}
			}
			if (matches.length != 1) {
				throw new RuntimeException("Existing transaction for order does not match up.");
			}
			AmazonOrderItem matchingItem = matches[0];

			// Need to update item price if overseas processing happened...
			matchingItem.setNetCost(netCost);
			
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
	
	public String toString() {
//		Optional<String> items = preexistingItems.stream().map(item -> item.toString()).reduce(new BinaryOperator<String>() {
//			@Override
//			public String apply(String arg0, String arg1) {
//				return arg0 + ", " + arg1;
//			}}); 
		Optional<String> items = preexistingItems.stream().map(item -> item.toString()).reduce((arg0, arg1) -> arg0 + ", " + arg1); 
		return items.isPresent() ? items.get() : "no items";
	}
}