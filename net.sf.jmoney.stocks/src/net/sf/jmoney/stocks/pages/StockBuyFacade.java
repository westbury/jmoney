package net.sf.jmoney.stocks.pages;

import net.sf.jmoney.model2.Transaction;
import net.sf.jmoney.stocks.model.StockAccount;
import net.sf.jmoney.stocks.types.TransactionType;

public class StockBuyFacade extends StockBuyOrSellFacade {

	public StockBuyFacade(Transaction transaction, String transactionName, StockAccount stockAccount) {
		super(transaction, TransactionType.Buy, transactionName, stockAccount);

	}
}
