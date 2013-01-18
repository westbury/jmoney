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

package net.sf.jmoney.reconciliation;

import net.sf.jmoney.fields.CheckBoxControlFactory;
import net.sf.jmoney.isolation.IValues;
import net.sf.jmoney.model2.CapitalAccount;
import net.sf.jmoney.model2.CapitalAccountInfo;
import net.sf.jmoney.model2.ExtensionPropertySet;
import net.sf.jmoney.model2.IExtensionObjectConstructors;
import net.sf.jmoney.model2.IPropertySetInfo;
import net.sf.jmoney.model2.PropertySet;
import net.sf.jmoney.model2.ScalarPropertyAccessor;

/**
 * Provides the metadata for the extra properties added to each capital account
 * by this plug-in.
 * 
 * @author Nigel Westbury
 */
public class ReconciliationAccountInfo implements IPropertySetInfo {

	private static ExtensionPropertySet<ReconciliationAccount,CapitalAccount> propertySet = PropertySet.addExtensionPropertySet(ReconciliationAccount.class, CapitalAccountInfo.getPropertySet(), new IExtensionObjectConstructors<ReconciliationAccount,CapitalAccount>() {

		public ReconciliationAccount construct(CapitalAccount extendedObject) {
			return new ReconciliationAccount(extendedObject);
		}

		public ReconciliationAccount construct(CapitalAccount extendedObject, IValues<CapitalAccount> values) {
			return new ReconciliationAccount(
					extendedObject, 
					values.getScalarValue(getReconcilableAccessor())
			);
		}
	});
	
	private static ScalarPropertyAccessor<Boolean,CapitalAccount> reconcilableAccessor = null;
	
	public PropertySet registerProperties() {
		reconcilableAccessor = propertySet.addProperty("reconcilable", ReconciliationPlugin.getResourceString("Account.isReconcilable"), Boolean.class, 1, 5, new CheckBoxControlFactory(), null);
		
		return propertySet;
	}

	/**
	 * @return
	 */
	public static ExtensionPropertySet<ReconciliationAccount,CapitalAccount> getPropertySet() {
		return propertySet;
	}

	/**
	 * @return
	 */
	public static ScalarPropertyAccessor<Boolean,CapitalAccount> getReconcilableAccessor() {
		return reconcilableAccessor;
	}
}
