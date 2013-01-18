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

import net.sf.jmoney.fields.AccountControlFactory;
import net.sf.jmoney.fields.CheckBoxControlFactory;
import net.sf.jmoney.importer.resources.Messages;
import net.sf.jmoney.isolation.IObjectKey;
import net.sf.jmoney.isolation.IValues;
import net.sf.jmoney.isolation.ObjectCollection;
import net.sf.jmoney.model2.CapitalAccount;
import net.sf.jmoney.model2.CapitalAccountInfo;
import net.sf.jmoney.model2.ExtensionPropertySet;
import net.sf.jmoney.model2.IExtensionObjectConstructors;
import net.sf.jmoney.model2.IListGetter;
import net.sf.jmoney.model2.IPropertySetInfo;
import net.sf.jmoney.model2.IncomeExpenseAccount;
import net.sf.jmoney.model2.ListPropertyAccessor;
import net.sf.jmoney.model2.PropertySet;
import net.sf.jmoney.model2.ReferencePropertyAccessor;
import net.sf.jmoney.model2.ScalarPropertyAccessor;

import org.eclipse.osgi.util.NLS;

/**
 * Provides the metadata for the extra properties added to each capital account
 * by this plug-in.
 * 
 * @author Nigel Westbury
 */
public class PatternMatcherAccountInfo implements IPropertySetInfo {

	private static ExtensionPropertySet<PatternMatcherAccount,CapitalAccount> propertySet = PropertySet.addExtensionPropertySet(PatternMatcherAccount.class, CapitalAccountInfo.getPropertySet(), new IExtensionObjectConstructors<PatternMatcherAccount,CapitalAccount>() {

		@Override
		public PatternMatcherAccount construct(CapitalAccount extendedObject) {
			return new PatternMatcherAccount(extendedObject);
		}

		@Override
		public PatternMatcherAccount construct(CapitalAccount extendedObject, IValues<CapitalAccount> values) {
			return new PatternMatcherAccount(
					extendedObject, 
					values.getScalarValue(getReconcilableAccessor()),
					values.getListManager(extendedObject.getObjectKey(), getPatternsAccessor()),
					values.getReferencedObjectKey(getDefaultCategoryAccessor()) 
			);
		}
	});
	
	private static ScalarPropertyAccessor<Boolean,CapitalAccount> reconcilableAccessor = null;
	private static ReferencePropertyAccessor<IncomeExpenseAccount,CapitalAccount> defaultCategoryAccessor = null;
	private static ListPropertyAccessor<MemoPattern,CapitalAccount> patternsAccessor = null;
	
	@Override
	public PropertySet<PatternMatcherAccount,CapitalAccount> registerProperties() {
		AccountControlFactory<PatternMatcherAccount,IncomeExpenseAccount> accountControlFactory = new AccountControlFactory<PatternMatcherAccount,IncomeExpenseAccount>() {
			@Override
			public IObjectKey getObjectKey(PatternMatcherAccount parentObject) {
				return parentObject.defaultCategoryKey;
			}
		};

		IListGetter<PatternMatcherAccount, MemoPattern> patternListGetter = new IListGetter<PatternMatcherAccount, MemoPattern>() {
			@Override
			public ObjectCollection<MemoPattern> getList(PatternMatcherAccount parentObject) {
				return parentObject.getPatternCollection();
			}
		};
	
		reconcilableAccessor = propertySet.addProperty("reconcilable", Messages.Account_Import, Boolean.class, 1, 5, new CheckBoxControlFactory(), null);
		patternsAccessor = propertySet.addPropertyList("patterns", NLS.bind(Messages.Account_Import_Patterns, null), MemoPatternInfo.getPropertySet(), patternListGetter);
		defaultCategoryAccessor = propertySet.addProperty("defaultCategory", NLS.bind(Messages.Account_Import_DefaultCategory, null), IncomeExpenseAccount.class, 1, 20, accountControlFactory, null);
		
		return propertySet;
	}

	/**
	 * @return
	 */
	public static ExtensionPropertySet<PatternMatcherAccount,CapitalAccount> getPropertySet() {
		return propertySet;
	}

	/**
	 * @return
	 */
	public static ScalarPropertyAccessor<Boolean,CapitalAccount> getReconcilableAccessor() {
		return reconcilableAccessor;
	}

	/**
	 * @return
	 */
	public static ListPropertyAccessor<MemoPattern,CapitalAccount> getPatternsAccessor() {
		return patternsAccessor;
	}	

	/**
	 * @return
	 */
	public static ReferencePropertyAccessor<IncomeExpenseAccount,CapitalAccount> getDefaultCategoryAccessor() {
		return defaultCategoryAccessor;
	}	
}
