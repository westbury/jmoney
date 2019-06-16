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

package net.sf.jmoney.model2;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.eclipse.core.databinding.observable.set.IObservableSet;
import org.eclipse.core.databinding.observable.set.WritableSet;

import net.sf.jmoney.isolation.IObjectKey;
import net.sf.jmoney.isolation.IValues;
import net.sf.jmoney.isolation.ListKey;
import net.sf.jmoney.resources.Messages;

/**
 * The data model for an entry.
 */
public final class Entry extends ExtendableObject {

	protected long creation = Calendar.getInstance().getTime().getTime();

	protected String check = null;

	protected Date valuta = null;

	/**
	 * Element: Account
	 */
	protected IObjectKey accountKey = null;

	protected long amount = 0;

	/**
	 * The currency or commodity represented by the amount in this entry,
	 * which may be null only if the account contains only a single currency
	 * or commodity
	 * <P>
	 * Element: Commodity
	 */
	protected IObjectKey commodityKey = null;

	protected String memo = null;

	protected String type = null;

	/**
	 * Applicable only if the account is an IncomeExpenseAccount
	 * and the multi-currency property in the account is set.
	 * <P>
	 * Element: Currency
	 */
	protected IObjectKey incomeExpenseCurrencyKey = null;

    /**
     * Constructor used by datastore plug-ins to create
     * an entry object.
     *
     * Note that the entry constructed by this constructor
     * may be invalid.  For example, it is possible that a
     * null account is set.  It is the callers responsibility
     * to ensure that an account is set before it relinquishes
     * control to other plug-ins.
     *
     * @param parent The key to a Transaction object.
     * 		This parameter must be non-null.
     * 		The getObject method must not be called on this
     * 		key from within this constructor because the
     * 		key may not yet be in a state in which it is
     * 		capable of materializing an object.
     */
	public Entry(
			IObjectKey objectKey,
			ListKey parentKey,
    		String     check,
    		IObjectKey accountKey,
    		Date       valuta,
    		String     memo,
    		long       amount,
    		String     type,
    		IObjectKey commodityKey,
    		long       creation,
    		IObjectKey incomeExpenseCurrencyKey,
    		IValues<Entry> extensionValues) {
		super(objectKey, parentKey, extensionValues);

		if (creation == 0) {
			this.creation = Calendar.getInstance().getTime().getTime();
		} else {
			this.creation = creation;
		}
		this.check = check;
		this.valuta = valuta;
		this.accountKey = accountKey;
		this.amount = amount;
		this.commodityKey = commodityKey;
		this.memo = memo;
		this.incomeExpenseCurrencyKey = incomeExpenseCurrencyKey;
	}

    /**
     * Constructor used by datastore plug-ins to create
     * an entry object.
     *
     * Note that the entry constructed by this constructor
     * may be invalid.  For example, it is possible that a
     * null account is set.  It is the callers responsibility
     * to ensure that an account is set before it relinquishes
     * control to other plug-ins.
     *
     * @param parent The key to a Transaction object.
     * 		This parameter must be non-null.
     * 		The getObject method must not be called on this
     * 		key from within this constructor because the
     * 		key may not yet be in a state in which it is
     * 		capable of materializing an object.
     */
	public Entry(
			IObjectKey objectKey,
			ListKey parentKey) {
		super(objectKey, parentKey);

		this.creation = Calendar.getInstance().getTime().getTime();
		this.check = null;
		this.valuta = null;
		this.accountKey = null;
		this.amount = 0;
		this.commodityKey = null;
		this.memo = null;
		this.incomeExpenseCurrencyKey = null;
	}

    @Override
	protected String getExtendablePropertySetId() {
		return "net.sf.jmoney.entry"; //$NON-NLS-1$
	}

	/**
	 * Returns the transaction.
	 */
	public Transaction getTransaction() {
		return (Transaction)parentKey.getParentKey().getObject();
	}

	/**
	 * Returns the creation.
	 */
	public long getCreation() {
		return creation;
	}

	/**
	 * Returns the check.
	 */
	public String getCheck() {
		return check;
	}

	/**
	 * Returns the valuta.
	 */
	public Date getValuta() {
		return valuta;
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

	public String getFullAccountName() {
		if (getTransaction().hasTwoEntries()) {
			Account category = getTransaction().getOther(this).getAccount();
			if (category == null) {
				return null;
			} else {
				return category.getFullAccountName();
			}
		} else if (getTransaction().hasMoreThanTwoEntries()) {
			// TODO: get rid of this message from here,
			// and move text from jmoney to jmoney.accountentriespanel
			return Messages.Entry_SplitEntry;
		} else {
			return null;
		}
	}

	/**
	 * Returns the amount.
	 */
	public long getAmount() {
		return amount;
	}

	/**
	 * Returns the commodity in which the amount in this entry is denominated.
	 * This property may be null only if the account contains only a single
	 * currency or commodity.
	 */
	public Commodity getCommodity() {
		if (commodityKey == null) {
			return null;
		} else {
			return (Commodity)commodityKey.getObject();
		}
	}

	/**
	 * Returns the memo.
	 */
	public String getMemo() {
		return memo;
	}

	/**
	 * @return The commodity for this entry, or null if not enough
	 * 			information has been set to determine the commodity.
	 */
	public Commodity getCommodityInternal() {
		if (getAccount() == null) {
			return null;
		} else {
			return getAccount().getCommodity(this);
		}
	}

	/**
	 * Sets the creation.
	 */
	public void setCreation(long aCreation) {
		long oldCreation = this.creation;
		creation = aCreation;

		// Notify the change manager.
		processPropertyChange(EntryInfo.getCreationAccessor(), new Long(oldCreation), new Long(creation));
	}

	/**
	 * Sets the check.
	 */
	public void setCheck(String aCheck) {
		String oldCheck = this.check;
		check = (aCheck != null && aCheck.length() == 0) ? null : aCheck;

		// Notify the change manager.
		processPropertyChange(EntryInfo.getCheckAccessor(), oldCheck, check);
	}

	/**
	 * Sets the valuta.
	 */
	public void setValuta(Date aValuta) {
		Date oldValuta = this.valuta;
		valuta = aValuta;

		// Notify the change manager.
		processPropertyChange(EntryInfo.getValutaAccessor(), oldValuta, valuta);
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
		processPropertyChange(EntryInfo.getAccountAccessor(), oldAccount, newAccount);
	}

	/**
	 * Sets the amount.
	 */
	public void setAmount(long anAmount) {
		long oldAmount = this.amount;
		amount = anAmount;

		// Notify the change manager.
		processPropertyChange(EntryInfo.getAmountAccessor(), oldAmount, amount);
	}

	/**
	 * Sets the memo.
	 */
	public void setMemo(String aMemo) {
		String oldMemo = this.memo;
		this.memo = (aMemo != null && aMemo.length() == 0) ? null : aMemo;

		// Notify the change manager.
		processPropertyChange(EntryInfo.getMemoAccessor(), oldMemo, memo);
	}

	/**
	 * Sets the currency in which the amount in this entry is denominated.
	 * This property is applicable if and only if the account for this entry
	 * is an IncomeExpenseAccount and the multi-currency property in the account
	 * is set.
	 */
	public void setCommodity(Commodity commodity) {
		Commodity oldCommodity =
			commodityKey == null
			? null
					: (Commodity)commodityKey.getObject();

		// TODO: This is not efficient.  Better would be to pass
		// an object key as the old value to the property change
		// method.  Then the object is materialized only if
		// necessary.
		commodityKey =
			commodity == null
			? null
					: commodity.getObjectKey();


		// Notify the change manager.
		processPropertyChange(EntryInfo.getCommodityAccessor(), oldCommodity, commodity);
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
		processPropertyChange(EntryInfo.getIncomeExpenseCurrencyAccessor(), oldIncomeExpenseCurrency, incomeExpenseCurrency);
	}
	
	// Helper methods
	
	/**
	 * Returns the entry type.
	 */
	public String getType() {
		return type;
	}

	/**
	 * See @ for a description of entry types.
	 */
	public void setType(String type) {
		String oldType = this.type;
		this.type = (type != null && type.length() == 0) ? null : type;

		// Notify the change manager.
		processPropertyChange(EntryInfo.getTypeAccessor(), oldType, type);
	}

	/**
	 * This is a helper method for setting an entry type.
	 * 
	 * The 'type' property of an entry is actually a comma-separated list of values.
	 * Each value in the list contains a transaction type (with an optional id) and an entry type.
	 * Almost always there will be only a single value in this list, but in case there are more
	 * than one value, callers should use this method whenever setting the transaction type.
	 * This method will add the entry type for the given transaction type and id, leaving all other
	 * values in the list alone.
	 */
	public void setType(String transactionType, String entryType) {
		String oldType = this.type;
		
		if (this.type == null) {
			if (entryType != null) {
				this.type = transactionType + ":" + entryType;
			}
		} else {
			String[] values = this.type.split(",");
			List<String> newValues = new ArrayList<>();
			boolean entryFound = false;
			for (String value : values) {
				String[] parts = value.split(":");
				if (transactionType == parts[0] + ":" + parts[1]) {
					if (entryType == null) {
						// Explicitly clearing an entry type is fine.
					} else {
						if (entryType != parts[2]) {
							// We're forcing the entry type to another type.
							// I'm not sure this is a good idea so throw an error for time being.
							throw new RuntimeException("Forcing entry type not allowed for time being");
						}
						newValues.add(value);
						entryFound = true;
					}
				} else {
					newValues.add(value);
				}
			}
			
			if (!entryFound && entryType != null) {
				newValues.add(transactionType + ":" + entryType);
			}
		
			if (newValues.isEmpty()) {
				this.type = null;
			} else {
				StringBuffer newValuesBuffer = new StringBuffer();
				String separator = "";
				for (String value : newValues) {
					newValuesBuffer.append(separator).append(value);
					separator = ",";
				}
			}
		}

		// Notify the change manager.
		processPropertyChange(EntryInfo.getTypeAccessor(), oldType, type);
	}

	public String getType(String transactionType) {
		if (this.type != null) {
			String[] values = this.type.split(",");
			for (String value : values) {
				String[] parts = value.split(":");
				if (transactionType == parts[0] + ":" + parts[1]) {
					return parts[2];
				}
			}
		}
		return null;
	}

	/**
	 * A transaction with split entries is a transaction that
	 * has entries in three or more accounts (where each account
	 * may be either a capital account or an income and
	 * expense category).
	 */
	public boolean hasSplitEntries() {
		return getTransaction().getEntryCollection().size() >= 3;
	}

	/**
	 * based on uncommitted data
	 * 
	 * @return
	 */
//	public Entry getOtherEntry() {
//		Assert./isTrue(!hasSplitEntries());
//		return buildOtherEntriesList().get(0);
//	}

	/**
	 * returns the other account of the transaction associated with this
	 * entry. If the transaction is a split one, there are several "other
	 * accounts", and the returned value is "null".
	 */
	public Entry getOtherEntry() {
		return getTransaction().getOther(this);
	}

	/**
	 * returns the other account of the transaction associated with this
	 * entry. If the transaction is a splitted one, there are several "other
	 * accounts", and the returned value is "null".
	 */
	public Account getOtherAccount () {
		if (getTransaction().hasTwoEntries()) {
			return getTransaction().getOther(this).getAccount();
		} else {
			return null;
		}
	}

	/**
	 * based on uncommitted data
	 * 
	 * @return
	 */
	public Set<Entry> getOtherEntries() {
		return buildOtherEntriesList();
	}

	/**
	 * Database reads may be necessary when getting the other entries.
	 * Furthermore, these are not needed unless an entry becomes visible. These
	 * are therefore fetched only when needed (not in the constructor of this
	 * object).
	 *
	 * We must be careful with this cached list because it is not kept up to date.
	 */
	// FIXME is this observable or not????
	private IObservableSet<Entry> buildOtherEntriesList() {
		WritableSet<Entry> otherEntries = new WritableSet<Entry>();
			for (Entry entry2 : getTransaction().getEntryCollection()) {
				if (!entry2.equals(this)) {
					otherEntries.add(entry2);
				}
			}


			return otherEntries;
	}

}
