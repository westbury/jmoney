package analyzer;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

class ItemBuilder {

	EbayOrder order;

	Set<EbayOrderItem> preexistingItems;

	public ItemBuilder(EbayOrder order, List<EbayOrderItem> items) {
		this.order = order;
		preexistingItems = new HashSet<>(items);
	}

	public EbayOrderItem get(String itemNumber, String description, long netCost/*, OrderObject shipmentObject*/) {
		// Find the matching entry
		if (preexistingItems.isEmpty()) {
			EbayOrderItem item = order.createNewItem(itemNumber, description, netCost);
			return item;
		} else {
			EbayOrderItem[] matches = preexistingItems.stream().filter(item -> item.getNetCost() == netCost).toArray(EbayOrderItem[]::new);
			if (matches.length > 1) {
				matches = Stream.of(matches).filter(item -> item.getEbayDescription().equals(description)).toArray(EbayOrderItem[]::new);
			} else {
				/* This may be an overseas order or some other complex order in which we don't
				 * know the item price.  So try matching on description only.
				 * We want to match on the sale or return as appropriate so we do check the sign
				 * of the net cost.   
				 */
				if (matches.length == 0) {
					matches = preexistingItems.stream().filter(item -> item.getEbayDescription().equals(description) && (item.getNetCost() > 0) == (netCost > 0)).toArray(EbayOrderItem[]::new);
				}
			}
			if (matches.length != 1) {
				throw new RuntimeException("Existing transaction for order does not match up.");
			}
			EbayOrderItem matchingItem = matches[0];

			// Need to update item price if overseas processing happened...
			matchingItem.setNetCost(netCost);
			
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
		Optional<String> items = preexistingItems.stream().map(item -> item.toString()).reduce((arg0, arg1) -> arg0 + ", " + arg1); 
		return items.isPresent() ? items.get() : "no items";
	}
}