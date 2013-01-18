/*
 *
 *  JMoney - A Personal Finance Manager
 *  Copyright (c) 2002 Johann Gyger <johann.gyger@switzerland.org>
 *  Copyright (c) 2004 Nigel Westbury <westbury@users.sourceforge.net>
 *
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 */

package net.sf.jmoney.importer.model;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import net.sf.jmoney.isolation.IObjectKey;
import net.sf.jmoney.isolation.IValues;
import net.sf.jmoney.isolation.ListKey;
import net.sf.jmoney.model2.Account;
import net.sf.jmoney.model2.Currency;
import net.sf.jmoney.model2.ExtendableObject;

/**
 * The data model for an entry.
 */
public final class MemoPattern extends ExtendableObject {
	
	protected int orderingIndex = 0;
	
	protected String pattern = null;
	
	protected String check = null;
	
	protected String description = null;
	
	/**
	 * Element: Account
	 */
	IObjectKey accountKey = null;
	
	protected String memo = null;

	/*
	 * The compiled pattern.  This is not a property but
	 * it is compiled each time an object of this class is constructed.
	 */
	Pattern compiledPattern = null;
	
	/**
	 * Applicable only if the account is an IncomeExpenseAccount
	 * and the multi-currency property in the account is set.
	 * <P>
	 * Element: Currency
	 */
	// TODO: same comment as for account above.
	public IObjectKey incomeExpenseCurrencyKey = null; 
	
    /**
     * Constructor used by datastore plug-ins to create
     * a pattern object.
     *
     * @param parent The key to a Transaction object.
     * 		This parameter must be non-null.
     * 		The getObject method must not be called on this
     * 		key from within this constructor because the
     * 		key may not yet be in a state in which it is
     * 		capable of materializing an object.   
     */
	public MemoPattern(
			IObjectKey objectKey,
			ListKey<? super MemoPattern,?> parentKey,
			int        orderingIndex,
			String     pattern,
    		String     check,
    		String     description,
    		IObjectKey accountKey,
    		String     memo,
    		IObjectKey incomeExpenseCurrencyKey,
    		IValues<MemoPattern>    extensionValues) {
		super(objectKey, parentKey, extensionValues);

		this.orderingIndex = orderingIndex;
		this.pattern = pattern;
		this.check = check;
		this.description = description;
		this.accountKey = accountKey;
		this.memo = memo;
		this.incomeExpenseCurrencyKey = incomeExpenseCurrencyKey;
	
		if (pattern != null) {
			try {
				compiledPattern = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
			} catch (PatternSyntaxException e) {
				compiledPattern = null;
			}
 		}
	}
	
    /**
     * Constructor used by datastore plug-ins to create
     * a pattern object.
     *
     * @param parent The key to a Transaction object.
     * 		This parameter must be non-null.
     * 		The getObject method must not be called on this
     * 		key from within this constructor because the
     * 		key may not yet be in a state in which it is
     * 		capable of materializing an object.   
     */
	public MemoPattern(
			IObjectKey objectKey,
    		ListKey<? super MemoPattern,?> parentKey) {
		super(objectKey, parentKey);

		this.orderingIndex = 0;
		this.pattern = null;
		this.check = null;
		this.description = null;
		this.accountKey = null;
		this.memo = null;
		this.incomeExpenseCurrencyKey = null;
	}

	@Override
	protected String getExtendablePropertySetId() {
		return "net.sf.jmoney.reconciliation.pattern";
	}
	
	/**
	 * Returns the index which specifies the order in which
	 * patterns should be tried.
	 */
	public int getOrderingIndex() {
		return orderingIndex;
	}
	
	/**
	 * Returns the pattern.
	 */
	public String getPattern() {
		return pattern;
	}
	
	/**
	 * Returns the check.
	 */
	public String getCheck() {
		return check;
	}
	
	/**
	 * Returns the description.
	 */
	public String getDescription() {
		return description;
	}
	
	/**
	 * Returns the account.
	 */
	public Account getAccount() {
		if (accountKey == null) {
			return null;
		} else {
			return (Account)accountKey.getObject();
		}
	}
	
	/**
	 * Returns the currency in which the amount in this entry is denominated.
	 * This property is applicable if and only if the account for this entry
	 * is an IncomeExpenseAccount and the multi-currency property in the account
	 * is set.
	 */
	public Currency getIncomeExpenseCurrency() {
		if (incomeExpenseCurrencyKey == null) {
			return null;
		} else {
			return (Currency)incomeExpenseCurrencyKey.getObject();
		}
	}

	/**
	 * Returns the memo.
	 */
	public String getMemo() {
		return memo;
	}

	/**
	 * Sets the index which specifies the order in which
	 * patterns should be tried.
	 */
	public void setOrderingIndex(int orderingIndex) {
		int oldOrderingIndex = this.orderingIndex;
		this.orderingIndex = orderingIndex;
		
		// Notify the change manager.
		processPropertyChange(MemoPatternInfo.getOrderingIndexAccessor(), oldOrderingIndex, orderingIndex);
	}
	
	/**
	 * Sets the pattern.
	 */
	public void setPattern(String pattern) {
		String oldPattern = this.pattern;
		this.pattern = pattern;
		
		if (pattern != null) {
			try {
				compiledPattern = Pattern.compile(pattern);
			} catch (PatternSyntaxException e) {
				compiledPattern = null;
			}
		} else {
			compiledPattern = null;
		}
		
		// Notify the change manager.
		processPropertyChange(MemoPatternInfo.getPatternAccessor(), oldPattern, pattern);
	}
	
	/**
	 * Sets the check.
	 */
	public void setCheck(String aCheck) {
		String oldCheck = this.check;
		check = (aCheck != null && aCheck.length() == 0) ? null : aCheck;
		
		// Notify the change manager.
		processPropertyChange(MemoPatternInfo.getCheckAccessor(), oldCheck, check);
	}
	
	
	/**
	 * Sets the description.
	 */
	public void setDescription(String aDescription) {
		String oldDescription = this.description;
		description = (aDescription != null && aDescription.length() == 0) ? null : aDescription;
		
		// Notify the change manager.
		processPropertyChange(MemoPatternInfo.getDescriptionAccessor(), oldDescription, description);
	}
	
	/**
	 * Sets the account.
	 */
	public void setAccount(Account newAccount) {
		Account oldAccount =
			accountKey == null
			? null
					: (Account)accountKey.getObject();
		
		// TODO: This is not efficient.  Better would be to pass
		// an object key as the old value to the property change
		// method.  Then the object is materialized only if
		// necessary.
		// NOTE: Even though a null account is not valid, we support
		// the setting of it because code may potentially need to do this
		// in order to, say, delete the account before the new account
		// of the entry is known.
		accountKey = 
			newAccount == null
			? null
					: newAccount.getObjectKey();
		
		// Notify the change manager.
		processPropertyChange(MemoPatternInfo.getAccountAccessor(), oldAccount, newAccount);
	}
	
	/**
	 * Sets the memo.
	 */
	public void setMemo(String aMemo) {
		String oldMemo = this.memo;
		this.memo = (aMemo != null && aMemo.length() == 0) ? null : aMemo;
		
		// Notify the change manager.
		processPropertyChange(MemoPatternInfo.getMemoAccessor(), oldMemo, memo);
	}
	
	/**
	 * Sets the currency in which the amount in this entry is denominated.
	 * This property is applicable if and only if the account for this entry
	 * is an IncomeExpenseAccount and the multi-currency property in the account
	 * is set.
	 */
	public void setIncomeExpenseCurrency(Currency incomeExpenseCurrency) {
		Currency oldIncomeExpenseCurrency =
			incomeExpenseCurrencyKey == null
			? null
					: (Currency)incomeExpenseCurrencyKey.getObject();
		
		// TODO: This is not efficient.  Better would be to pass
		// an object key as the old value to the property change
		// method.  Then the object is materialized only if
		// necessary.
		incomeExpenseCurrencyKey =
			incomeExpenseCurrency == null
			? null
					: incomeExpenseCurrency.getObjectKey();
		
		
		// Notify the change manager.
		processPropertyChange(MemoPatternInfo.getIncomeExpenseCurrencyAccessor(), oldIncomeExpenseCurrency, incomeExpenseCurrency);
	}
	
	public Pattern getCompiledPattern() {
		return compiledPattern;
	}
}
