package net.sf.jmoney.amazon.htmlparser;


public class AmazonItem {

	public String description;
	public String seller;
	public String img;
	public String asin;
	public long price;


	public long getPrice() {
		return price;
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
