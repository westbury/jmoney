package net.sf.jmoney.stocks.types;

import net.sf.jmoney.model2.Account;
import net.sf.jmoney.stocks.model.StockAccount;
import net.sf.jmoney.stocks.pages.IEntryType;

public enum TakeoverEntryType implements IEntryType {
	Cash("cash", true, account -> account),
	TakenOver("takenOver", true, account -> account),
	TakingOver("takingOver", false, account -> account);

	public final String id;
	public final boolean isCompulsory;
	private final AssociatedAccount associatedAccountGetter;
	
	TakeoverEntryType(String id, boolean isCompulsory, AssociatedAccount associatedAccountGetter) {
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