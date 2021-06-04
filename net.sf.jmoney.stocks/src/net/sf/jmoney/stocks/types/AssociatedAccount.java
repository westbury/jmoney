package net.sf.jmoney.stocks.types;

import net.sf.jmoney.model2.Account;
import net.sf.jmoney.stocks.model.StockAccount;

public interface AssociatedAccount {
	public Account getAssociatedAccount(StockAccount account);
}