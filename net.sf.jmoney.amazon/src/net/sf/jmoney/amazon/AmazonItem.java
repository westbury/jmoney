package net.sf.jmoney.amazon;


public class AmazonItem {

	public String description;
	public String seller;
	public String img;
	public String asin;
	public String price;


	public long getPrice() {
		String x = price.substring(1).replaceFirst("\\.", "");
		return Long.valueOf(x);
	}

	@Override
	public String toString() {
		return new StringBuffer()
		.append(", ASIN: ").append(asin)
		.append(", Image: ").append(img)
		.append(", Description: ").append(description)
		.append(", Seller: ").append(seller)
		.toString();
	}

}
