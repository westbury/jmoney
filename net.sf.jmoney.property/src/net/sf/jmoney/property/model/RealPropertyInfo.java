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

import net.sf.jmoney.fields.AccountControlFactory;
import net.sf.jmoney.isolation.IObjectKey;
import net.sf.jmoney.isolation.IValues;
import net.sf.jmoney.isolation.ListKey;
import net.sf.jmoney.model2.CommodityInfo;
import net.sf.jmoney.model2.Currency;
import net.sf.jmoney.model2.ExtendablePropertySet;
import net.sf.jmoney.model2.IExtendableObjectConstructors;
import net.sf.jmoney.model2.IPropertySetInfo;
import net.sf.jmoney.model2.IReferenceControlFactory;
import net.sf.jmoney.model2.IncomeExpenseAccount;
import net.sf.jmoney.model2.PropertySet;
import net.sf.jmoney.model2.ReferencePropertyAccessor;
import net.sf.jmoney.property.StocksPlugin;

/**
 * The class descriptor for the RealProperty class.
 * <P>
 * @author Nigel Westbury
 *
 */
public class RealPropertyInfo implements IPropertySetInfo {

	private static ExtendablePropertySet<RealProperty> propertySet = PropertySet.addDerivedFinalPropertySet(RealProperty.class, "Real Property", CommodityInfo.getPropertySet(), new IExtendableObjectConstructors<RealProperty>() {

		public RealProperty construct(IObjectKey objectKey, ListKey parentKey) {
			return new RealProperty(objectKey, parentKey);
		}

		public RealProperty construct(IObjectKey objectKey,	ListKey<? super RealProperty,?> parentKey, IValues<RealProperty> values) {
			return new RealProperty(
					objectKey,
					parentKey,
					values.getScalarValue(CommodityInfo.getNameAccessor()),
					values.getReferencedObjectKey(RealPropertyInfo.getRentalIncomeAccountAccessor()),
					values);
		}
	});

	private static ReferencePropertyAccessor<Currency,RealProperty> currencyAccessor;
	private static ReferencePropertyAccessor<IncomeExpenseAccount,RealProperty> rentalIncomeAccountAccessor = null;

	public PropertySet registerProperties() {

		IReferenceControlFactory<RealProperty,IncomeExpenseAccount> rentalIncomeAccountControlFactory = new AccountControlFactory<RealProperty,IncomeExpenseAccount>() {
			public IObjectKey getObjectKey(RealProperty parentObject) {
				return parentObject.rentalIncomeAccountKey;
			}
		};

		rentalIncomeAccountAccessor = propertySet.addProperty("rentalIncomeAccount", StocksPlugin.getResourceString("PropertyDesc.dividendAccount"), IncomeExpenseAccount.class, 2, 80, rentalIncomeAccountControlFactory, null);

		return propertySet;
	}

	/**
	 * @return
	 */
	public static ExtendablePropertySet<RealProperty> getPropertySet() {
		return propertySet;
	}

	/**
	 * @return
	 */
	public static ReferencePropertyAccessor<Currency,RealProperty> getCurrencyAccessor() {
		return currencyAccessor;
	}

	/**
	 * @return
	 */
	public static ReferencePropertyAccessor<IncomeExpenseAccount,RealProperty> getRentalIncomeAccountAccessor() {
		return rentalIncomeAccountAccessor;
	}
}
