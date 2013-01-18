package net.sf.jmoney.paypal;

public class ShoppingCartRow {

	public long grossAmount;
	public String memo;
	public Long netAmount;
	public Long fee;
	public String url;
	public Long insurance;
	public Long salesTax;

	/*
	 * This value is calculated by distributing the value
	 * from the transaction.
	 */
	public long shippingAndHandling = 0;

}
