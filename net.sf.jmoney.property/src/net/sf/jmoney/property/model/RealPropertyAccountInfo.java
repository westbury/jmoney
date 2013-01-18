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

package net.sf.jmoney.property.model;

import net.sf.jmoney.fields.CurrencyControlFactory;
import net.sf.jmoney.isolation.IObjectKey;
import net.sf.jmoney.isolation.IValues;
import net.sf.jmoney.isolation.ListKey;
import net.sf.jmoney.model2.AccountInfo;
import net.sf.jmoney.model2.CapitalAccountInfo;
import net.sf.jmoney.model2.Currency;
import net.sf.jmoney.model2.ExtendablePropertySet;
import net.sf.jmoney.model2.IExtendableObjectConstructors;
import net.sf.jmoney.model2.IPropertySetInfo;
import net.sf.jmoney.model2.IReferenceControlFactory;
import net.sf.jmoney.model2.PropertySet;
import net.sf.jmoney.model2.ReferencePropertyAccessor;
import net.sf.jmoney.property.StocksPlugin;

/**
 * This class implements an extension to the net.sf.jmoney.fields
 * extension point.  It registers the StockAccount model class.
 * 
 * @author Nigel Westbury 
 */
public class RealPropertyAccountInfo implements IPropertySetInfo {

	private static ExtendablePropertySet<RealPropertyAccount> propertySet = PropertySet.addDerivedFinalPropertySet(RealPropertyAccount.class, "Worldwide Property", CapitalAccountInfo.getPropertySet(), new IExtendableObjectConstructors<RealPropertyAccount>() {

		public RealPropertyAccount construct(IObjectKey objectKey, ListKey<? super RealPropertyAccount,?> parentKey) {
			return new RealPropertyAccount(
					objectKey, 
					parentKey
			);
		}

		public RealPropertyAccount construct(IObjectKey objectKey,
				ListKey<? super RealPropertyAccount,?> parentKey, IValues<RealPropertyAccount> values) {
			return new RealPropertyAccount(
					objectKey, 
					parentKey, 
					values.getScalarValue(AccountInfo.getNameAccessor()),
					values.getListManager(objectKey, CapitalAccountInfo.getSubAccountAccessor()),
					values.getScalarValue(CapitalAccountInfo.getAbbreviationAccessor()),
					values.getScalarValue(CapitalAccountInfo.getCommentAccessor()),
					values.getReferencedObjectKey(RealPropertyAccountInfo.getCurrencyAccessor()),
					values
			);
		}
	});

	private static ReferencePropertyAccessor<Currency,RealPropertyAccount> currencyAccessor = null;

	public PropertySet registerProperties() {

		IReferenceControlFactory<RealPropertyAccount,Currency> currencyControlFactory = new CurrencyControlFactory<RealPropertyAccount>() {
			public IObjectKey getObjectKey(RealPropertyAccount parentObject) {
				return parentObject.currencyKey;
			}
		};

		currencyAccessor = propertySet.addProperty("currency", StocksPlugin.getResourceString("PropertyDesc.currency"), Currency.class, 2, 20, currencyControlFactory, null);

		return propertySet;
	}

	public static ExtendablePropertySet<RealPropertyAccount> getPropertySet() {
		return propertySet;
	}

	/**
	 * @return
	 */
	public static ReferencePropertyAccessor<Currency,RealPropertyAccount> getCurrencyAccessor() {
		return currencyAccessor;
	}	
}
