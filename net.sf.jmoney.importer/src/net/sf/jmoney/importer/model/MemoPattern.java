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

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import net.sf.jmoney.isolation.IObjectKey;
import net.sf.jmoney.isolation.IValues;
import net.sf.jmoney.isolation.ListKey;
import net.sf.jmoney.model2.Account;
import net.sf.jmoney.model2.Currency;
import net.sf.jmoney.model2.ExtendableObject;

import org.eclipse.core.databinding.observable.map.IObservableMap;
import org.eclipse.core.databinding.observable.map.WritableMap;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.internal.databinding.observable.MapEntryObservableValue;

/**
 * The data model for an entry.
 */
public final class MemoPattern extends ExtendableObject {
	
	protected int orderingIndex = 0;
	
	protected String pattern = null;
	
	protected String description = null;
	
	/**
	 * Element: Account
	 */
	IObjectKey accountKey = null;
	
	protected String memo = null;

	/**
	 * Applicable only if the account is an IncomeExpenseAccount
	 * and the multi-currency property in the account is set.
	 * <P>
	 * Element: Currency
	 */
	// TODO: same comment as for account above.
	public IObjectKey incomeExpenseCurrencyKey = null; 
	
	/**
	 * The id of the transaction type.
	 * <P>
	 * The transaction type might be, for example, a stock sale,
	 * or a stock split, or a simple expense item.  Each transaction
	 * type has an id and it is used here to indicate the transaction
	 * type for all import items that match this pattern.
	 */
	private String transactionTypeId = null;
	
	/**
	 * String containing a list of parameterized values.  The values
	 * may be fixed for this pattern, or may be extracted from the
	 * text that matched.
	 * <P>
	 * Text may contain {n} or {description,n}.  The first form allowed only
	 * if there is a single text field in the input. 
	 * <P>
	 * Values are of the form name=value.  Each pair is separated by a new-line.
	 */
	private String transactionParameterValues = null;
	
	/**
	 * Extracted values from transactionParameterValues
	 */
	private IObservableMap<String, String> transactionParameterValueMap;

	private Map<String, String> patternMap;

	private Map<String, Pattern> compiledPatternMap;

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
			String     transactionTypeId,
			String     transactionParameterValues, 
    		String     description,
    		IObjectKey accountKey,
    		String     memo,
    		IObjectKey incomeExpenseCurrencyKey,
    		IValues<MemoPattern>    extensionValues) {
		super(objectKey, parentKey, extensionValues);

		this.orderingIndex = orderingIndex;
		this.pattern = pattern;
		this.transactionTypeId = transactionTypeId;
		this.transactionParameterValues = transactionParameterValues;
		this.description = description;
		this.accountKey = accountKey;
		this.memo = memo;
		this.incomeExpenseCurrencyKey = incomeExpenseCurrencyKey;
	
		patternMap = new HashMap<String, String>();
		compiledPatternMap = new HashMap<String, Pattern>();
		if (pattern != null) {
			extractPatterns();
 		}
		
		extractParameterValues();
	}
	
    private void extractPatterns() {
    	patternMap = new WritableMap<String, String>();
    	compiledPatternMap = new WritableMap<String, Pattern>();
    	 
 		if (pattern != null) {
 			for (String pair : pattern.split("\n")) {
 				/*
 				 * We split at the first '=', taking into account
 				 * that the value may contain one or more '=' that
 				 * should not cause a split.
 				 */
 				String [] parts = pair.split("=");

 				String columnId;
 				String columnPattern;
 				int splitIndex = pair.indexOf("=");
 				if (splitIndex == -1) {
 					columnId = pair;
 					columnPattern = "bad value";
 				} else {
 					columnId = pair.substring(0, splitIndex);
 					columnPattern = pair.substring(splitIndex + 1);
 				}

 				putPattern(columnId, columnPattern);

 				try {
 					Pattern thisCompiledPattern = Pattern.compile(columnPattern, Pattern.CASE_INSENSITIVE);
 					compiledPatternMap.put(columnId, thisCompiledPattern);
 				} catch (PatternSyntaxException e) {
 					compiledPatternMap.remove(columnId);
 				}
 			}
 		}
	}

	private void putPattern(String columnId, String columnPattern) {
		if (columnPattern.indexOf("\n") != -1) {
			throw new Error("Newline characters somehow got into a pattern");
		}
		
		patternMap.put(columnId, columnPattern);
		
		try {
			Pattern thisCompiledPattern = Pattern.compile(columnPattern, Pattern.CASE_INSENSITIVE);
			compiledPatternMap.put(columnId, thisCompiledPattern);
		} catch (PatternSyntaxException e) {
			compiledPatternMap.remove(columnId);
		}

		// Update the underlying data too, as this is what counts when
		// saving, binding to changes etc.
		StringBuffer buffer = new StringBuffer();
		String separator = "";
		for (Entry<String, String> entry : patternMap.entrySet()) {
			buffer.append(separator)
			.append(entry.getKey())
			.append('=')
			.append(entry.getValue());
			separator = "\n";
		}

		String oldPattern = this.pattern;
		this.pattern = buffer.toString();
		
		// Notify the change manager.
		processPropertyChange(MemoPatternInfo.getPatternAccessor(), oldPattern, pattern);
		
	}

    private void extractParameterValues() {
   	 transactionParameterValueMap = new WritableMap<String, String>();
   	 
		if (transactionParameterValues != null) {
			for (String pair : transactionParameterValues.split("/n|\n")) {
				String [] parts = pair.split("=");
				String paramId = parts[0];
				String paramValue = parts[1];
				transactionParameterValueMap.put(paramId, paramValue);
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
		this.transactionTypeId = null;
		this.transactionParameterValues = null;
		this.description = null;
		this.accountKey = null;
		this.memo = null;
		this.incomeExpenseCurrencyKey = null;

		patternMap = new HashMap<String, String>();
		transactionParameterValueMap = new WritableMap<String, String>();
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
	 * Returns the transaction type id.
	 */
	public String getTransactionTypeId() {
		return transactionTypeId;
	}
	
	/**
	 * Returns the transaction parameter values.  The values are returned
	 * as a single serialized string being the format in which it is stored
	 * in the datastore.
	 */
	public String getTransactionParameterValues() {
		return transactionParameterValues;
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
		
		// Notify the change manager.
		processPropertyChange(MemoPatternInfo.getPatternAccessor(), oldPattern, pattern);
	}
	
	/**
	 * Sets the transaction type id.
	 */
	public void setTransactionTypeId(String transactionTypeId) {
		String oldTransactionTypeId = this.transactionTypeId;
		this.transactionTypeId = transactionTypeId;
		
		// Notify the change manager.
		processPropertyChange(MemoPatternInfo.getTransactionTypeIdAccessor(), oldTransactionTypeId, transactionTypeId);
	}
	
	/**
	 * Sets the transaction parameter values.
	 */
	public void setTransactionParameterValues(String transactionParameterValues) {
		String oldTransactionParameterValues = this.transactionParameterValues;
		this.transactionParameterValues = transactionParameterValues;
		
		extractParameterValues();
		
		// Notify the change manager.
		processPropertyChange(MemoPatternInfo.getTransactionParameterValuesAccessor(), oldTransactionParameterValues, transactionParameterValues);
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
	
	public void setTransactionParameterValue(String parameterId, String value) {
		if (value.indexOf("\n") != -1) {
			throw new Error("Newline characters somehow got into a parameter value");
		}
		
		transactionParameterValueMap.put(parameterId, value);
		
		StringBuffer buffer = new StringBuffer();
		String separator = "";
		for (Entry<String, String> entry : transactionParameterValueMap.entrySet()) {
			buffer.append(separator)
			.append(entry.getKey())
			.append('=')
			.append(entry.getValue());
			separator = "\n";
		}

		String oldTransactionParameterValues = this.transactionParameterValues;
		this.transactionParameterValues = buffer.toString();
		
		// Notify the change manager.
		processPropertyChange(MemoPatternInfo.getTransactionParameterValuesAccessor(), oldTransactionParameterValues, transactionParameterValues);
	}

	/**
	 * 
	 * @param parameterId
	 * @return the value, may be the empty string but never null, may
	 * 				contain {0}, {1} etc for substituting values extracted
	 * 				from the import entry properties
	 */
	public String getParameterValue(String parameterId) {
		String value = transactionParameterValueMap.get(parameterId);
		return value == null ? "" : value;
	}

	public IObservableValue<String> observeParameterValue(String parameterId) {
		return new MapEntryObservableValue<String, String>(transactionParameterValueMap, parameterId, String.class);
	}

	public String getPattern(String importEntryPropertyId) {
		return patternMap.get(importEntryPropertyId);
	}

	public void setPattern(String importEntryPropertyId, String value) {
		putPattern(importEntryPropertyId, value);
	}

	public Pattern getCompiledPattern(String importEntryPropertyId) {
		return compiledPatternMap.get(importEntryPropertyId);
	}
}
