package net.sf.jmoney.amazon;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class AmazonShipment {

	public String dispatchDate;

	public String deliveryDate;

	public long total;

	public String trackingNumber;

	public List<AmazonItem> items = new ArrayList<AmazonItem>();

	public String carrier;

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
			return df.parse(deliveryDate);
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}
	}

	public Date getDeliveryDate() {
		try {
			return df.parse(deliveryDate);
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}
	}
}
