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

import net.sf.jmoney.fields.AmountInCurrencyAccountControlFactory;
import net.sf.jmoney.fields.CurrencyControlFactory;
import net.sf.jmoney.isolation.IObjectKey;
import net.sf.jmoney.resources.Messages;

/**
 * This class is a listener class to the net.sf.jmoney.fields
 * extension point.  It implements an extension.
 * <P>
 * This extension registers the CapitalAccount properties.  By registering
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
 * @author Johann Gyger
 */
public class CurrencyAccountInfo implements IPropertySetInfo {

	private static ExtendablePropertySet<CurrencyAccount> propertySet = PropertySet.addDerivedAbstractPropertySet(CurrencyAccount.class, Messages.CurrencyAccountInfo_Description, CapitalAccountInfo.getPropertySet());

	private static ReferencePropertyAccessor<Currency,CurrencyAccount> currencyAccessor = null;
	private static ScalarPropertyAccessor<Long,CurrencyAccount> startBalanceAccessor = null;

    @Override
	public PropertySet registerProperties() {
		IPropertyControlFactory<Long> amountControlFactory = new AmountInCurrencyAccountControlFactory();
		IReferenceControlFactory<CurrencyAccount,Currency> currencyControlFactory = new CurrencyControlFactory<CurrencyAccount>() {
			@Override
			public IObjectKey getObjectKey(CurrencyAccount parentObject) {
				return parentObject.currencyKey;
			}
		};
		
		currencyAccessor = propertySet.addProperty("currency", Messages.CurrencyAccountInfo_Currency, Currency.class, 3, 30, currencyControlFactory, null); //$NON-NLS-1$
		startBalanceAccessor = propertySet.addProperty("startBalance", Messages.CurrencyAccountInfo_StartBalance, Long.class, 2, 40, amountControlFactory, null); //$NON-NLS-1$
		
		return propertySet;
	}

	/**
	 * @return
	 */
	public static ExtendablePropertySet<CurrencyAccount> getPropertySet() {
		return propertySet;
	}

	/**
	 * @return
	 */
	public static ReferencePropertyAccessor<Currency,CurrencyAccount> getCurrencyAccessor() {
		return currencyAccessor;
	}	

	/**
	 * @return
	 */
	public static ScalarPropertyAccessor<Long,CurrencyAccount> getStartBalanceAccessor() {
		return startBalanceAccessor;
	}	
}
