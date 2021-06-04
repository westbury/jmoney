package net.sf.jmoney.stocks.types;

import net.sf.jmoney.model2.Account;
import net.sf.jmoney.stocks.model.StockAccount;
import net.sf.jmoney.stocks.pages.IEntryType;

public enum DividendEntryType implements IEntryType {
	Cash("cash", true, account -> account),
	Dividend("dividend", true, account -> account.getDividendAccount()),
	WithholdingTax("withholding-tax", false, account -> account.getWithholdingTaxAccount());

	public final String id;
	public final boolean isCompulsory;
	private final AssociatedAccount associatedAccountGetter;
	
	DividendEntryType(String id, boolean isCompulsory, AssociatedAccount associatedAccountGetter) {
		this.id = id;
		this.isCompulsory = isCompulsory;
		this.associatedAccountGetter = associatedAccountGetter;
	}
	
	@Override
	public String getId() {
		return id;
	}

	@Override
	public boolean isCompulsory() {
		return isCompulsory;
	}
	
	@Override
	public Account getAssociatedAccount(StockAccount account) {
		return associatedAccountGetter.getAssociatedAccount(account);
	}
}