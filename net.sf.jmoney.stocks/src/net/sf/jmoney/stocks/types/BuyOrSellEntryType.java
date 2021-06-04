package net.sf.jmoney.stocks.types;

import net.sf.jmoney.model2.Account;
import net.sf.jmoney.stocks.model.StockAccount;
import net.sf.jmoney.stocks.pages.IEntryType;

public enum BuyOrSellEntryType implements IEntryType {
	Cash("cash", true, account -> account),
	AquisitionOrDisposal("acquisition-or-disposal", true, account -> account),
	Commission("commission", false, account -> account.getCommissionAccount()),
	Tax1("tax1", false, account -> account.getTax1Account()),
	Tax2("tax2", false, account -> account.getTax2Account());

	public final String id;
	public final boolean isCompulsory;
	private final AssociatedAccount associatedAccountGetter;
	
	BuyOrSellEntryType(String id, boolean isCompulsory, AssociatedAccount associatedAccountGetter) {
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