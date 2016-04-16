/*
 *
 *  JMoney - A Personal Finance Manager
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

package net.sf.jmoney.toshl;

import net.sf.jmoney.fields.AccountControlFactory;
import net.sf.jmoney.fields.TextControlFactory;
import net.sf.jmoney.importer.model.MemoPattern;
import net.sf.jmoney.importer.model.MemoPatternInfo;
import net.sf.jmoney.isolation.IObjectKey;
import net.sf.jmoney.isolation.IValues;
import net.sf.jmoney.isolation.ListKey;
import net.sf.jmoney.isolation.ObjectCollection;
import net.sf.jmoney.model2.ExtendablePropertySet;
import net.sf.jmoney.model2.IExtendableObjectConstructors;
import net.sf.jmoney.model2.IListGetter;
import net.sf.jmoney.model2.IPropertyControlFactory;
import net.sf.jmoney.model2.IPropertySetInfo;
import net.sf.jmoney.model2.IncomeExpenseAccount;
import net.sf.jmoney.model2.ListPropertyAccessor;
import net.sf.jmoney.model2.PropertySet;
import net.sf.jmoney.model2.ReferencePropertyAccessor;
import net.sf.jmoney.model2.ScalarPropertyAccessor;
import net.sf.jmoney.resources.Messages;

/**
 * This class is an implementation for the net.sf.jmoney.fields
 * extension point.  It implements an extension.
 * <P>
 *
 * @author Nigel Westbury
 */
public class ToshlAccountInfo implements IPropertySetInfo {

	private static ExtendablePropertySet<ToshlAccount> propertySet = PropertySet.addBaseFinalPropertySet(ToshlAccount.class, Messages.CurrencyInfo_Description, new IExtendableObjectConstructors<ToshlAccount>() {

		@Override
		public ToshlAccount construct(IObjectKey objectKey, ListKey parentKey) {
			return new ToshlAccount(
					objectKey,
					parentKey
			);
		}

		@Override
		public ToshlAccount construct(IObjectKey objectKey,
				ListKey<? super ToshlAccount,?> parentKey, IValues<ToshlAccount> values) {
			return new ToshlAccount(
					objectKey,
					parentKey,
					values.getScalarValue(getToshlAccountNameAccessor()),
					values.getReferencedObjectKey(getAccountAccessor()), 
					values.getListManager(objectKey, getPatternsAccessor()),
					values.getReferencedObjectKey(getDefaultCategoryAccessor()), 
					values
			);
		}
	});

	private static ScalarPropertyAccessor<String,ToshlAccount> toshlAccountNameAccessor = null;

	private static ReferencePropertyAccessor<IncomeExpenseAccount,ToshlAccount> accountAccessor = null;

	private static ListPropertyAccessor<MemoPattern,ToshlAccount> patternsAccessor = null;

	private static ReferencePropertyAccessor<IncomeExpenseAccount,ToshlAccount> defaultCategoryAccessor = null;


	@Override
	public PropertySet registerProperties() {
		IPropertyControlFactory<ToshlAccount,String> textControlFactory = new TextControlFactory<ToshlAccount>();

		toshlAccountNameAccessor = propertySet.addProperty("toshlAccountName", "Toshl Account", String.class, 3, 150, textControlFactory, null); //$NON-NLS-1$

		AccountControlFactory<ToshlAccount,ToshlAccount,IncomeExpenseAccount> accountControlFactory1 = new AccountControlFactory<ToshlAccount,ToshlAccount,IncomeExpenseAccount>() {
			@Override
			public IObjectKey getObjectKey(ToshlAccount parentObject) {
				return parentObject.accountKey;
			}
		};

		accountAccessor = propertySet.addProperty("account", "Account for Toshl Account", IncomeExpenseAccount.class, 1, 20, accountControlFactory1, null);

		IListGetter<ToshlAccount, MemoPattern> patternListGetter = new IListGetter<ToshlAccount, MemoPattern>() {
			@Override
			public ObjectCollection<MemoPattern> getList(ToshlAccount parentObject) {
				return parentObject.getPatternCollection();
			}
		};
	
		AccountControlFactory<ToshlAccount,ToshlAccount,IncomeExpenseAccount> accountControlFactory2 = new AccountControlFactory<ToshlAccount,ToshlAccount,IncomeExpenseAccount>() {
			@Override
			public IObjectKey getObjectKey(ToshlAccount parentObject) {
				return parentObject.defaultCategoryKey;
			}
		};

		patternsAccessor = propertySet.addPropertyList("patterns", "Patterns", MemoPatternInfo.getPropertySet(), patternListGetter);
		defaultCategoryAccessor = propertySet.addProperty("defaultCategory", "Default Category", IncomeExpenseAccount.class, 1, 20, accountControlFactory2, null);

		return propertySet;
	}

	/**
	 * @return
	 */
	public static ExtendablePropertySet<ToshlAccount> getPropertySet() {
		return propertySet;
	}

	/**
	 * @return
	 */
	public static ScalarPropertyAccessor<String,ToshlAccount> getToshlAccountNameAccessor() {
		return toshlAccountNameAccessor;
	}	

	/**
	 * @return
	 */
	public static ReferencePropertyAccessor<IncomeExpenseAccount,ToshlAccount> getAccountAccessor() {
		return accountAccessor;
	}	

	/**
	 * @return
	 */
	public static ListPropertyAccessor<MemoPattern,ToshlAccount> getPatternsAccessor() {
		return patternsAccessor;
	}	

	/**
	 * @return
	 */
	public static ReferencePropertyAccessor<IncomeExpenseAccount,ToshlAccount> getDefaultCategoryAccessor() {
		return defaultCategoryAccessor;
	}	
}
