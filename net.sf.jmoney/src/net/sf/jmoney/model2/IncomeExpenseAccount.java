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

import net.sf.jmoney.isolation.IListManager;
import net.sf.jmoney.isolation.IObjectKey;
import net.sf.jmoney.isolation.IValues;
import net.sf.jmoney.isolation.ListKey;
import net.sf.jmoney.isolation.ObjectCollection;
import net.sf.jmoney.resources.Messages;


/**
 * An implementation of the IncomeExpenseAccount interface
 */
public class IncomeExpenseAccount extends Account {

	protected IListManager<IncomeExpenseAccount> subAccounts;

	/**
	 * True if the entries in this account can be in any account,
	 * false if all the entries must be in the same currency
	 * (in which case the currency property must be set).
	 */
	private boolean multiCurrency = true;

	/**
	 * The currency in which all entries in this account are denominated.
	 * This property is not applicable if multiCurrency is true.
	 */
	protected IObjectKey currencyKey = null;

	private String fullAccountName = null;

	public IncomeExpenseAccount(
			IObjectKey objectKey,
			ListKey parent,
			String name,
			IListManager<IncomeExpenseAccount> subAccounts,
			boolean multiCurrency,
			IObjectKey currencyKey,
			IValues<IncomeExpenseAccount> extensionValues) {
		super(objectKey, parent, name, extensionValues);

		this.subAccounts = subAccounts;
		this.multiCurrency = multiCurrency;
		this.currencyKey = currencyKey;
	}

	public IncomeExpenseAccount(
			IObjectKey objectKey,
			ListKey parentKey) {
		super(objectKey, parentKey);
		this.name = Messages.IncomeExpenseAccount_Name;
		this.subAccounts = objectKey.constructListManager(IncomeExpenseAccountInfo.getSubAccountAccessor());
	}

    @Override
	protected String getExtendablePropertySetId() {
		return "net.sf.jmoney.category"; //$NON-NLS-1$
	}

    @Override
	public String getFullAccountName() {
		if (fullAccountName == null) {
			fullAccountName = name;
			Account ancestorCategory = getParent();
			while (ancestorCategory != null) {
				fullAccountName = ancestorCategory.getName() + ":" + fullAccountName; //$NON-NLS-1$
				ancestorCategory = ancestorCategory.getParent();
			}
		}
		return fullAccountName;
	}

	// TODO: use debugger to see if this version is called
	// when the Method object references the version in the
	// abstract base class.  If not then this is broke.
    @Override
	public void setName(String name) {
		super.setName(name);
		fullAccountName = null;
	}

	public boolean isMultiCurrency() {
		return multiCurrency;
	}

	public Currency getCurrency() {
		return currencyKey == null
		? null
				: (Currency)currencyKey.getObject();
	}

	public void setMultiCurrency(boolean multiCurrency) {
        boolean oldMultiCurrency = this.multiCurrency;
		this.multiCurrency = multiCurrency;
/* No, this is not good code to have here.  Firstly we set the currency field but we
 * don't call processPropertyChange, so no listeners are told, the property may not be
 * stored in the database etc.
 *
 * But also, changing properties other than the property set by the user is bad news.
 * It is upto the user to ensure the properties are set to valid values.  We really need
 * to get the dependency code working, so the currency property is properly indicated
 * as being appropriate only if not multi-currency.
 *
		// When turning off multi-currency, we must set an appropriate
		// currency.
		if (!multiCurrency && currencyKey == null) {
			// TODO: Look at the entries in the account and set the
			// account as appropriate.
			// For time being, set to default currency.
			currencyKey = getSession().getDefaultCurrency().getObjectKey();
		}
*/
		// Notify the change manager.
		processPropertyChange(IncomeExpenseAccountInfo.getMultiCurrencyAccessor(), new Boolean(oldMultiCurrency), new Boolean(multiCurrency));


	}

	public void setCurrency(Currency currency) {
        Currency oldCurrency = getCurrency();
		this.currencyKey = currency.getObjectKey();

		// Notify the change manager.
		processPropertyChange(IncomeExpenseAccountInfo.getCurrencyAccessor(), oldCurrency, currency);
	}

	/**
	 * @return Commodity represented by the amount in the given entry
	 */
    @Override
	public Commodity getCommodity(Entry entry) {
		// Income and expense accounts may be either single currency or
		// multiple currency.
		if (!multiCurrency) {
			return getCurrency();
		} else {
			// This is a multi-currency account, so we look to the entry.
			return entry.getCommodity();
		}
	}

    @Override
	public ObjectCollection<IncomeExpenseAccount> getSubAccountCollection() {
		return new ObjectCollection<IncomeExpenseAccount>(subAccounts, this, IncomeExpenseAccountInfo.getSubAccountAccessor());
	}

	/**
	 * Creates an sub-income/expense category to this income/expense category.
	 *
	 * @return a new sub-account
	 */
    public IncomeExpenseAccount createSubAccount() {
    	return getSubAccountCollection().createNewElement(IncomeExpenseAccountInfo.getPropertySet());
    }
}
