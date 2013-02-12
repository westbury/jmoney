package net.sf.jmoney.stocks.gains;

import java.util.Date;

class PurchaseActivityNode extends ActivityNode {

	public PurchaseActivityNode(Date date,long newStockBalance, long purchaseQuantity, long purchaseCost) {
		super(date, newStockBalance);

		this.quantity = purchaseQuantity;
		this.cost = purchaseCost;
	}

	@Override
	protected int getOrderSequence() {
		return 1;
	}
}