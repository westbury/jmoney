package net.sf.jmoney.stocks.gains;

import java.util.Date;

import net.sf.jmoney.stocks.model.Stock;

public class StockPurchaseAndSale {
	private Stock stock;
	private long quantity;
	private Date buyDate;
	private long basis;
	private Date sellDate;
	private long proceeds;

	public StockPurchaseAndSale(Stock stock, long quantity, Date buyDate,
			long basis, Date sellDate, long proceeds) {
		this.stock = stock;
		this.quantity = quantity;
		this.buyDate = buyDate;
		this.basis = basis;
		this.sellDate = sellDate;
		this.proceeds = proceeds;
	}

	public Stock getStock() {
		return stock;
	}

	public long getQuantity() {
		return quantity;
	}

	public Date getBuyDate() {
		return buyDate;
	}

	public long getBasis() {
		return basis;
	}

	public Date getSellDate() {
		return sellDate;
	}

	public long getProceeds() {
		return proceeds;
	}
}