package net.sf.jmoney.stocks.pages;

import net.sf.jmoney.model2.Transaction;
import net.sf.jmoney.stocks.model.StockAccount;
import net.sf.jmoney.stocks.pages.StockEntryRowControl.TransactionType;

public class StockSellFacade extends StockBuyOrSellFacade {

	public StockSellFacade(Transaction transaction, String transactionName, StockAccount stockAccount) {
		super(transaction, TransactionType.Sell, transactionName, stockAccount);

	}
}
