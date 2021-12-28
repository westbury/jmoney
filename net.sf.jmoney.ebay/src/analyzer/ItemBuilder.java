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

	public EbayOrderItem get(String itemNumber, String description) {
		// Find the matching entry
		if (preexistingItems.isEmpty()) {
			EbayOrderItem item = order.createNewItem(itemNumber, description);
			return item;
		} else {
			EbayOrderItem[] matches = preexistingItems.stream().filter(item -> item.getItemNumber() == itemNumber).toArray(EbayOrderItem[]::new);
			if (matches.length > 1) {
				matches = Stream.of(matches).filter(item -> item.getEbayDescription().equals(description)).toArray(EbayOrderItem[]::new);
			}
			if (matches.length == 0) {
				throw new RuntimeException("Existing transaction for order does not match up.");
			}
			EbayOrderItem matchingItem = matches[0];

			// Remove this one.  This ensures that if there are multiple identical items, each one is matched once.
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