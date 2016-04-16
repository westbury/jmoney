/*
 *
 *  JMoney - A Personal Finance Manager
 *  Copyright (c) 2002 Johann Gyger <johann.gyger@switzerland.org>
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

package net.sf.jmoney.toshl;

import net.sf.jmoney.importer.matcher.IPatternMatcher;
import net.sf.jmoney.importer.model.MemoPattern;
import net.sf.jmoney.importer.model.PatternMatcherAccountInfo;
import net.sf.jmoney.isolation.IListManager;
import net.sf.jmoney.isolation.IObjectKey;
import net.sf.jmoney.isolation.IValues;
import net.sf.jmoney.isolation.ListKey;
import net.sf.jmoney.isolation.ObjectCollection;
import net.sf.jmoney.model2.Account;
import net.sf.jmoney.model2.ExtendableObject;
import net.sf.jmoney.model2.IncomeExpenseAccount;


/**
 * This class represents a Toshl account.  Many users may only have a single
 * Toshl account.
 */
public class ToshlAccount extends ExtendableObject implements IPatternMatcher {

	private String toshlAccountName;
	
	IObjectKey accountKey = null;
	
	protected IListManager<MemoPattern> patterns;
	
	IObjectKey defaultCategoryKey = null;
	
    /**
     * Constructor used by datastore plug-ins to create
     * a currency object.
     */
	public ToshlAccount(
				IObjectKey objectKey,
				ListKey parentKey,
				String toshlAccountName,
				IObjectKey accountKey,
				IListManager<MemoPattern> patterns,
				IObjectKey defaultCategoryKey,
				IValues<ToshlAccount> extensionValues) {
		super(objectKey, parentKey, extensionValues);

		this.toshlAccountName = toshlAccountName;
		this.accountKey = accountKey;
		this.patterns = patterns;
		this.defaultCategoryKey = defaultCategoryKey;
	}

    /**
     * Constructor used by datastore plug-ins to create
     * a currency object.
     */
	public ToshlAccount(
			IObjectKey objectKey,
			ListKey parentKey) {
		super(objectKey, parentKey);
		this.patterns = objectKey.constructListManager(PatternMatcherAccountInfo.getPatternsAccessor());
	}

    @Override
	protected String getExtendablePropertySetId() {
		return "net.sf.jmoney.toshl.toshlAccount"; //$NON-NLS-1$
	}

	/**
	 * @return the name of this account.
	 */
	public String getToshlAccountName() {
		return toshlAccountName;
	}

	/**
	 * @param newName the name given to the account in the Toshl datastore.
	 */
	public void setToshlAccountName(String newName) {
		String oldName = toshlAccountName;
		toshlAccountName = newName;

		// Notify the change manager.
		processPropertyChange(ToshlAccountInfo.getToshlAccountNameAccessor(), oldName, newName);
	}

	/**
	 * Returns the account that corresponds to the Toshl account.
	 * All Toshl entries are put into this account.
	 * 
	 * @return the income and expense account for
	 * 			all entries imported for this account
	 */
	public IncomeExpenseAccount getAccount() {
        return accountKey == null
		? null
				: (IncomeExpenseAccount)accountKey.getObject();
	}
		
	/**
	 * Sets the default category.
	 */
	public void setAccount(IncomeExpenseAccount account) {
        IncomeExpenseAccount oldAccount = getAccount();
        this.accountKey = 
        	account == null
        		? null 
        		: account.getObjectKey();

		// Notify the change manager.
		processPropertyChange(ToshlAccountInfo.getAccountAccessor(), oldAccount, account);
	}

	public ObjectCollection<MemoPattern> getPatternCollection() {
		return new ObjectCollection<MemoPattern>(patterns, this, ToshlAccountInfo.getPatternsAccessor());
	}

	/**
	 * Returns the default income and expense account.
	 * 
	 * @return the income and expense account to be given initially
	 * 			to all entries imported for this account
	 */
	public IncomeExpenseAccount getDefaultCategory() {
        return defaultCategoryKey == null
		? null
				: (IncomeExpenseAccount)defaultCategoryKey.getObject();
	}
		
	/**
	 * Sets the default category.
	 */
	public void setDefaultCategory(IncomeExpenseAccount defaultCategory) {
        IncomeExpenseAccount oldDefaultCategory = getDefaultCategory();
        this.defaultCategoryKey = 
        	defaultCategory == null
        		? null 
        		: defaultCategory.getObjectKey();

		// Notify the change manager.
		processPropertyChange(ToshlAccountInfo.getDefaultCategoryAccessor(), oldDefaultCategory, defaultCategory);
	}

	@Override
	public String getName() {
		return "Toshl account '" + toshlAccountName + "'";
	}

	@Override
	public boolean isReconcilable() {
		return true;
	}

	@Override
	public Account getBaseObject() {
		return getAccount();
	}
}

