package net.sf.jmoney.amazon.htmlparser;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

public class AmazonShipment {

	public String dispatchDate;

	public String deliveryDate;

	public long total;

	public String trackingNumber;

	public List<AmazonItem> items = new ArrayList<AmazonItem>();

	public String carrier;

	public long totalPaidInCash;

	public long totalPaidInGiftCertificate;

	@Override
	public String toString() {
		return new StringBuffer()
		.append("Date: ").append(deliveryDate)
		.toString();
	}

	public long getTotal() {
		return total;
	}

	private static DateFormat df = new SimpleDateFormat("dd MMM yyyy");

	public Date getDispatchDate() {
		try {
			return dispatchDate == null ? null : df.parse(dispatchDate);
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}
	}

	public Date getDeliveryDate() {
		try {
			return deliveryDate == null ? null : df.parse(deliveryDate);
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * The given set may contain entries with "?".  These match any asin.
	 * 
	 * NO, Actually this is a shipment list so it can't....
	 * The collection may contain duplicates of the same item.  Normally orders
	 * of an item with a quantity of more than one appear as the same entry.  However
	 * if the items are split across shipments then there will be multiple entries for
	 * the same item.  That is why we use a List and not a Set.
	 * 
	 * @param itemsInShipment
	 * @return true if and only if the given set of ASIN numbers exactly matches
	 * 		the set of items in this shipment.
	 */
	public boolean containsExactly(Set<String> asins) {
		if (items.size() != asins.size()) {
			return false;
		}
		for (AmazonItem item : items) {
			if (!asins.contains(item.asin)) {
				// This code is not correct.  Just one "?" will
				// cause any set to match.
				if (!asins.contains("?")) { 
					return false;
				}
			}
		}
		return true;
	}
}
