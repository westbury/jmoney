package net.sf.jmoney.stocks.pages;

import net.sf.jmoney.model2.Account;
import net.sf.jmoney.stocks.model.StockAccount;

public interface IEntryType {

	/**
	 * This is the last part of each triple in the entry type.
	 * 
	 * @return
	 */
	String getId();

	boolean isCompulsory();

	Account getAssociatedAccount(StockAccount account);

}
