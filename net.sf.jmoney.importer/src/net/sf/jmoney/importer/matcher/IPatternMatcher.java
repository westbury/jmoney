package net.sf.jmoney.importer.matcher;

import net.sf.jmoney.importer.model.MemoPattern;
import net.sf.jmoney.isolation.ObjectCollection;
import net.sf.jmoney.model2.Account;
import net.sf.jmoney.model2.IncomeExpenseAccount;

public interface IPatternMatcher {

	ObjectCollection<MemoPattern> getPatternCollection();

	IncomeExpenseAccount getDefaultCategory();

	/**
	 * Used to identify patterns to user
	 * 
	 * @return
	 */
	String getName();

	// Almost always true.  False for Ameritrade.  Ameritrade should be
	// cleaned-up and this method removed.
	boolean isReconcilable();

	// The account into which we are importing
	Account getBaseObject();

	void setDefaultCategory(IncomeExpenseAccount defaultCategory);

}
