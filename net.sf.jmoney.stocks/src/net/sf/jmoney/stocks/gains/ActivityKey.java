package net.sf.jmoney.stocks.gains;

import java.util.Date;

/**
 * An immutable key for the activity node.
 * <P>
 * Technically it does not matter whether purchases on the same date are
 * presumed to have occurred before or after the sales. If the security was
 * owned before this date then the sales will be matched to the earliest
 * possible purchases, and if there is not sufficient security owned before
 * this date to cover the sales then they will be matched to the purchases
 * made on this date even if the purchases were later in the day (this being
 * a short-sale).
 */
public class ActivityKey implements Comparable<ActivityKey> {
	final Date date;
	final int orderSequence;

	ActivityKey(Date date, int orderSequence) {
		this.date = date;
		this.orderSequence = orderSequence;
	}

	@Override
	public int compareTo(ActivityKey otherNode) {
		int dateOrder = date.compareTo(otherNode.date);
		if (dateOrder != 0) {
			return dateOrder;
		}

		int typeOrder = orderSequence - otherNode.orderSequence;
		return typeOrder;
	}
}