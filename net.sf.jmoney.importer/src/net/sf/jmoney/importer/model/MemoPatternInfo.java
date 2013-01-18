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

package net.sf.jmoney.importer.model;

import net.sf.jmoney.fields.AccountControlFactory;
import net.sf.jmoney.fields.CurrencyControlFactory;
import net.sf.jmoney.fields.IntegerControlFactory;
import net.sf.jmoney.fields.TextControlFactory;
import net.sf.jmoney.importer.resources.Messages;
import net.sf.jmoney.isolation.IObjectKey;
import net.sf.jmoney.isolation.IValues;
import net.sf.jmoney.isolation.ListKey;
import net.sf.jmoney.model2.Account;
import net.sf.jmoney.model2.Currency;
import net.sf.jmoney.model2.ExtendablePropertySet;
import net.sf.jmoney.model2.IExtendableObjectConstructors;
import net.sf.jmoney.model2.IPropertyControlFactory;
import net.sf.jmoney.model2.IPropertySetInfo;
import net.sf.jmoney.model2.IReferenceControlFactory;
import net.sf.jmoney.model2.PropertySet;
import net.sf.jmoney.model2.ReferencePropertyAccessor;
import net.sf.jmoney.model2.ScalarPropertyAccessor;

import org.eclipse.osgi.util.NLS;

/**
 * This class is a listener class to the net.sf.jmoney.fields
 * extension point.  It implements an extension.
 * <P>
 * This extension registers the Pattern properties.  By registering
 * the properties, every one can know how to display, edit, and store
 * the properties.
 * 
 * @author Nigel Westbury
 */
public class MemoPatternInfo implements IPropertySetInfo {

	
	private static ExtendablePropertySet<MemoPattern> propertySet = PropertySet.addBaseFinalPropertySet(MemoPattern.class, "Account Import Entry Pattern", new IExtendableObjectConstructors<MemoPattern>() {

		@Override
		public MemoPattern construct(IObjectKey objectKey, ListKey<? super MemoPattern,?> parentKey) {
			return new MemoPattern(objectKey, parentKey);
		}

		@Override
		public MemoPattern construct(IObjectKey objectKey,
				ListKey<? super MemoPattern,?> parentKey, IValues<MemoPattern> values) {
			return new MemoPattern(
					objectKey, 
					parentKey, 
					values.getScalarValue(MemoPatternInfo.getOrderingIndexAccessor()),
					values.getScalarValue(MemoPatternInfo.getPatternAccessor()),
					values.getScalarValue(MemoPatternInfo.getCheckAccessor()),
					values.getScalarValue(MemoPatternInfo.getDescriptionAccessor()),
					values.getReferencedObjectKey(MemoPatternInfo.getAccountAccessor()),
					values.getScalarValue(MemoPatternInfo.getMemoAccessor()),
					values.getReferencedObjectKey(MemoPatternInfo.getIncomeExpenseCurrencyAccessor()),
					values
			);
		}
	});
	
	private static ScalarPropertyAccessor<Integer,MemoPattern> orderingIndexAccessor = null;
	private static ScalarPropertyAccessor<String,MemoPattern> patternAccessor = null;
	private static ScalarPropertyAccessor<String,MemoPattern> checkAccessor = null;
	private static ScalarPropertyAccessor<String,MemoPattern> descriptionAccessor = null;
	private static ReferencePropertyAccessor<Account,MemoPattern> accountAccessor = null;
	private static ScalarPropertyAccessor<String,MemoPattern> memoAccessor = null;
	private static ReferencePropertyAccessor<Currency,MemoPattern> incomeExpenseCurrencyAccessor = null;

	@Override
	public ExtendablePropertySet<MemoPattern> registerProperties() {
		IPropertyControlFactory<Integer> integerControlFactory = new IntegerControlFactory();

		IPropertyControlFactory<String> textControlFactory = new TextControlFactory();

		IReferenceControlFactory<MemoPattern,Account> accountControlFactory = new AccountControlFactory<MemoPattern,Account>() {
			@Override
			public IObjectKey getObjectKey(MemoPattern parentObject) {
				return parentObject.accountKey;
			}
		};

		IReferenceControlFactory<MemoPattern,Currency> currencyControlFactory = new CurrencyControlFactory<MemoPattern>() {
			@Override
			public IObjectKey getObjectKey(MemoPattern parentObject) {
				return parentObject.incomeExpenseCurrencyKey;
			}
		};

		orderingIndexAccessor = propertySet.addProperty("orderingIndex", "Ordering Index",                                    Integer.class,1, 20,  integerControlFactory, null);
		patternAccessor       = propertySet.addProperty("pattern",       "Pattern",                                           String.class, 2, 50,  textControlFactory,    null);
		checkAccessor         = propertySet.addProperty("check",         NLS.bind(Messages.MemoPatternInfo_EntryCheck, null),       String.class, 2, 50,  textControlFactory,    null);
		descriptionAccessor   = propertySet.addProperty("description",   NLS.bind(Messages.MemoPatternInfo_EntryDescription, null), String.class, 5, 100, textControlFactory,    null);
		accountAccessor       = propertySet.addProperty("account",       NLS.bind(Messages.MemoPatternInfo_EntryCategory, null),    Account.class,2, 70,  accountControlFactory, null);
		memoAccessor          = propertySet.addProperty("memo",          NLS.bind(Messages.MemoPatternInfo_EntryMemo, null),        String.class, 5, 100, textControlFactory,    null);
		incomeExpenseCurrencyAccessor = propertySet.addProperty("incomeExpenseCurrency",    NLS.bind(Messages.MemoPatternInfo_EntryCurrency, null), Currency.class, 2, 70, currencyControlFactory, null);
		
		return propertySet;
	}

	/**
	 * @return
	 */
	public static ExtendablePropertySet<MemoPattern> getPropertySet() {
		return propertySet;
	}

	/**
	 * @return
	 */
	public static ScalarPropertyAccessor<Integer,MemoPattern> getOrderingIndexAccessor() {
		return orderingIndexAccessor;
	}	

	/**
	 * @return
	 */
	public static ScalarPropertyAccessor<String,MemoPattern> getPatternAccessor() {
		return patternAccessor;
	}	

	/**
	 * @return
	 */
	public static ScalarPropertyAccessor<String,MemoPattern> getCheckAccessor() {
		return checkAccessor;
	}	

	/**
	 * @return
	 */
	public static ScalarPropertyAccessor<String,MemoPattern> getDescriptionAccessor() {
		return descriptionAccessor;
	}	

	/**
	 * @return
	 */
	public static ReferencePropertyAccessor<Account,MemoPattern> getAccountAccessor() {
		return accountAccessor;
	}	

	/**
	 * @return
	 */
	public static ScalarPropertyAccessor<String,MemoPattern> getMemoAccessor() {
		return memoAccessor;
	}	


	/**
	 * @return
	 */
	public static ReferencePropertyAccessor<Currency,MemoPattern> getIncomeExpenseCurrencyAccessor() {
		return incomeExpenseCurrencyAccessor;
	}	
}
