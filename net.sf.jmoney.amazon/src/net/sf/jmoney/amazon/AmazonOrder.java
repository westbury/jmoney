package net.sf.jmoney.amazon;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class AmazonOrder {

	public String orderDate;

	public String id2;
	public String id;

	public long total;

	public List<AmazonShipment> shipments = new ArrayList<AmazonShipment>();

	public String giftRecipient;

	/**
	 * true if there were so many items that they were not listed on
	 * the order list page.
	 */
	public boolean moreItems;

	@Override
	public String toString() {
		return new StringBuffer()
		.append("Order Date: ").append(orderDate)
		.append(", Order Id: ").append(id)
		.append(", Total: ").append(total)
		.toString();
	}

	private static DateFormat df = new SimpleDateFormat("dd MMM yyyy");

	public Date getOrderDate() {
		try {
			return df.parse(orderDate);
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}
	}

	public long getTotal() {
		return total;
	}
}
