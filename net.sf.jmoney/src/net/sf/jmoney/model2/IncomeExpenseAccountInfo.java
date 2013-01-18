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

package net.sf.jmoney.model2;

import net.sf.jmoney.JMoneyPlugin;
import net.sf.jmoney.fields.CheckBoxControlFactory;
import net.sf.jmoney.fields.CurrencyControlFactory;
import net.sf.jmoney.isolation.IObjectKey;
import net.sf.jmoney.isolation.IValues;
import net.sf.jmoney.isolation.ListKey;
import net.sf.jmoney.isolation.ObjectCollection;
import net.sf.jmoney.resources.Messages;

/**
 * This class is a listener class to the net.sf.jmoney.fields
 * extension point.  It implements an extension.
 * <P>
 * This extension registers the IncomeExpenseAccount properties.  By registering
 * the properties, every one can know how to display, edit, and store
 * the properties.
 * <P>
 * These properties are supported in the JMoney base code, so everyone
 * including plug-ins will know about these properties.  However, to
 * follow the Eclipse paradigm (every one should be treated equal,
 * including oneself), these are registered through the same extension
 * point that plug-ins must also use to register their properties.
 * 
 * @author Nigel Westbury
 */
public class IncomeExpenseAccountInfo implements IPropertySetInfo {

	private static ExtendablePropertySet<IncomeExpenseAccount> propertySet = PropertySet.addDerivedFinalPropertySet(IncomeExpenseAccount.class, Messages.IncomeExpenseAccountInfo_Description, AccountInfo.getPropertySet(), new IExtendableObjectConstructors<IncomeExpenseAccount>() {

		@Override
		public IncomeExpenseAccount construct(IObjectKey objectKey,
				ListKey parentKey) {
			return new IncomeExpenseAccount(objectKey, parentKey);
		}

		@Override
		public IncomeExpenseAccount construct(IObjectKey objectKey,
				ListKey<? super IncomeExpenseAccount,?> parentKey, IValues<IncomeExpenseAccount> values) {
			return new IncomeExpenseAccount(
					objectKey, 
					parentKey, 
					values.getScalarValue(AccountInfo.getNameAccessor()),
					values.getListManager(objectKey, IncomeExpenseAccountInfo.getSubAccountAccessor()),
					values.getScalarValue(IncomeExpenseAccountInfo.getMultiCurrencyAccessor()),
					values.getReferencedObjectKey(IncomeExpenseAccountInfo.getCurrencyAccessor()),
					values 
			);
		}
	});


	private static ListPropertyAccessor<IncomeExpenseAccount,IncomeExpenseAccount> subAccountAccessor = null;
	private static ScalarPropertyAccessor<Boolean,IncomeExpenseAccount> multiCurrencyAccessor = null;
	private static ReferencePropertyAccessor<Currency,IncomeExpenseAccount> currencyAccessor = null;
	
    @Override
	public PropertySet registerProperties() {
		IListGetter<IncomeExpenseAccount, IncomeExpenseAccount> accountGetter = new IListGetter<IncomeExpenseAccount, IncomeExpenseAccount>() {
			@Override
			public ObjectCollection<IncomeExpenseAccount> getList(IncomeExpenseAccount parentObject) {
				return parentObject.getSubAccountCollection();
			}
		};

		IReferenceControlFactory<IncomeExpenseAccount,Currency> currencyControlFactory = new CurrencyControlFactory<IncomeExpenseAccount>() {
			@Override
			public IObjectKey getObjectKey(IncomeExpenseAccount parentObject) {
				return parentObject.currencyKey;
			}
		};
		
		IPropertyDependency<IncomeExpenseAccount> onlyIfSingleCurrency = new IPropertyDependency<IncomeExpenseAccount>() {
			@Override
			public boolean isApplicable(IncomeExpenseAccount account) {
				return !account.isMultiCurrency();
			}
		};
		
		subAccountAccessor = propertySet.addPropertyList("subAccount", Messages.IncomeExpenseAccountInfo_SubAccount, IncomeExpenseAccountInfo.getPropertySet(), accountGetter); //$NON-NLS-1$
		multiCurrencyAccessor = propertySet.addProperty("multiCurrency", Messages.IncomeExpenseAccountInfo_MultiCurrency, Boolean.class, 0, 10, new CheckBoxControlFactory(), null);  //$NON-NLS-1$
		currencyAccessor = propertySet.addProperty("currency", Messages.IncomeExpenseAccountInfo_Currency, Currency.class, 2, 20, currencyControlFactory, onlyIfSingleCurrency); //$NON-NLS-1$
		
		// We should define something for the implied enumerated value
		// that is controlled by the derived class type.  This has not
		// been designed yet, so for time being we have nothing to do.
		
		propertySet.setIcon(JMoneyPlugin.createImageDescriptor("category.gif")); //$NON-NLS-1$
		
		return propertySet;
	}

	/**
	 * @return
	 */
	public static ExtendablePropertySet<IncomeExpenseAccount> getPropertySet() {
		return propertySet;
	}

    /**
	 * @return
	 */
	public static ListPropertyAccessor<IncomeExpenseAccount,IncomeExpenseAccount> getSubAccountAccessor() {
		return subAccountAccessor;
	}	

    /**
	 * @return
	 */
	public static ScalarPropertyAccessor<Boolean,IncomeExpenseAccount> getMultiCurrencyAccessor() {
		return multiCurrencyAccessor;
	}	

    /**
	 * @return
	 */
	public static ReferencePropertyAccessor<Currency,IncomeExpenseAccount> getCurrencyAccessor() {
		return currencyAccessor;
	}	
}
