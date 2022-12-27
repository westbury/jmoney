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
			EbayOrderItem[] matches = {};
			if (itemNumber != null) {
				matches = preexistingItems.stream().filter(item -> itemNumber.equals(item.getItemNumber())).toArray(EbayOrderItem[]::new);
				if (matches.length > 1) {
					EbayOrderItem[] matchesOnDesc = Stream.of(matches).filter(item -> item.getEbayDescription().equals(description)).toArray(EbayOrderItem[]::new);
					if (matchesOnDesc.length >= 1) {
						matches = matchesOnDesc;
					}
				}
			}
			if (matches.length == 0) {
				// Orders page no longer shows item numbers, so filter on description
				matches = preexistingItems.stream().filter(item -> item.getEbayDescription().equals(description)).toArray(EbayOrderItem[]::new);
			}
			if (matches.length == 0) {
				throw new RuntimeException("Existing transaction for order does not match up.");
			}
			EbayOrderItem matchingItem = matches[0];

			// Orders page no longer shows item numbers, so may need to set
			if (itemNumber != null) {
				matchingItem.setItemNumber(itemNumber);
			}
			
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