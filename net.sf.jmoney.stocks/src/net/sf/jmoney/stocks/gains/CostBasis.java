package net.sf.jmoney.stocks.gains;

import java.util.Date;

public class CostBasis {

	long quantity;
	Date date;
	long basis;

	public CostBasis(long quantity, Date date, long basis) {
		this.quantity = quantity;
		this.date = date;
		this.basis = basis;
	}
}