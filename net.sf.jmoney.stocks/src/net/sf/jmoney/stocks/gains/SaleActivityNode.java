package net.sf.jmoney.stocks.gains;

import java.util.Date;


class SaleActivityNode extends ActivityNode {

	public SaleActivityNode(Date date, long newStockBalance, long saleQuantity,	long saleProceeds) {
		super(date, newStockBalance);

		this.quantity = saleQuantity;
		this.cost = saleProceeds;
	}

	@Override
	protected int getOrderSequence() {
		return 2;
	}
}