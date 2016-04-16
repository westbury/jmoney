/*
 *
 *  JMoney - A Personal Finance Manager
 *  Copyright (c) 2006 Nigel Westbury <westbury@users.sourceforge.net>
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

import net.sf.jmoney.importer.matcher.IPatternMatcher;
import net.sf.jmoney.isolation.IListManager;
import net.sf.jmoney.isolation.IObjectKey;
import net.sf.jmoney.isolation.ObjectCollection;
import net.sf.jmoney.model2.CapitalAccountExtension;
import net.sf.jmoney.model2.ExtendableObject;
import net.sf.jmoney.model2.IncomeExpenseAccount;

/**
 * An extension object that extends CapitalAccount objects.
 * This extension object maintains the values of the properties
 * that have been added by this plug-in.
 * 
 * @author Nigel Westbury
 */
public class PatternMatcherAccount extends CapitalAccountExtension implements IPatternMatcher {
	
	protected boolean reconcilable = false;
	
	protected IListManager<MemoPattern> patterns;
	
	IObjectKey defaultCategoryKey = null;
	
	/**
	 * A default constructor is mandatory for all extension objects.
	 * The default constructor sets the extension properties to
	 * appropriate default values.
	 */
	public PatternMatcherAccount(ExtendableObject extendedObject) {
		super(extendedObject);
		this.patterns = extendedObject.getObjectKey().constructListManager(PatternMatcherAccountInfo.getPatternsAccessor());
	}
	
	/**
	 * A Full constructor is mandatory for all extension objects.
	 * This constructor is called by the datastore to construct
	 * the extension objects when loading data.
	 * 
	 */
	public PatternMatcherAccount(
			ExtendableObject extendedObject,
			boolean reconcilable, 
			IListManager<MemoPattern> patterns,
			IObjectKey defaultCategoryKey) {
		super(extendedObject);
		this.reconcilable = reconcilable;
		this.patterns = patterns;
		this.defaultCategoryKey = defaultCategoryKey;
	}
	
	/**
	 * Indicates if an account is reconcilable.  If it is not then none
	 * of the other properties are applicable.
	 */
	public boolean isReconcilable() {
		return reconcilable;
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
	 * Sets the reconcilable flag.
	 */
	public void setReconcilable(boolean reconcilable) {
		boolean oldReconcilable = this.reconcilable;
		this.reconcilable = reconcilable;
		processPropertyChange(PatternMatcherAccountInfo.getReconcilableAccessor(), new Boolean(oldReconcilable), new Boolean(reconcilable));
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
		processPropertyChange(PatternMatcherAccountInfo.getDefaultCategoryAccessor(), oldDefaultCategory, defaultCategory);
	}

	public ObjectCollection<MemoPattern> getPatternCollection() {
		return new ObjectCollection<MemoPattern>(patterns, getBaseObject(), PatternMatcherAccountInfo.getPatternsAccessor());
	}
}
