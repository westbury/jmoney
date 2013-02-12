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

package net.sf.jmoney.stocks.model;

import java.util.Date;

import net.sf.jmoney.fields.AmountControlFactory;
import net.sf.jmoney.fields.AmountEditor;
import net.sf.jmoney.fields.CurrencyControlFactory;
import net.sf.jmoney.fields.DateControlFactory;
import net.sf.jmoney.fields.IntegerControlFactory;
import net.sf.jmoney.isolation.IModelObject;
import net.sf.jmoney.isolation.IObjectKey;
import net.sf.jmoney.isolation.IScalarPropertyAccessor;
import net.sf.jmoney.isolation.IValues;
import net.sf.jmoney.isolation.ListKey;
import net.sf.jmoney.isolation.SessionChangeAdapter;
import net.sf.jmoney.model2.Commodity;
import net.sf.jmoney.model2.CommodityInfo;
import net.sf.jmoney.model2.Currency;
import net.sf.jmoney.model2.ExtendableObject;
import net.sf.jmoney.model2.ExtendablePropertySet;
import net.sf.jmoney.model2.IExtendableObjectConstructors;
import net.sf.jmoney.model2.IPropertyControl;
import net.sf.jmoney.model2.IPropertyControlFactory;
import net.sf.jmoney.model2.IPropertySetInfo;
import net.sf.jmoney.model2.IReferenceControlFactory;
import net.sf.jmoney.model2.PropertySet;
import net.sf.jmoney.model2.ReferencePropertyAccessor;
import net.sf.jmoney.model2.ScalarPropertyAccessor;
import net.sf.jmoney.stocks.StocksPlugin;

import org.eclipse.swt.widgets.Composite;

/**
 * The class descriptor for the Bond class.
 * <P>
 * @author Nigel Westbury
 * 
 */
public class BondInfo implements IPropertySetInfo {

	private static ExtendablePropertySet<Bond> propertySet = PropertySet.addDerivedFinalPropertySet(Bond.class, "Bond", SecurityInfo.getPropertySet(), new IExtendableObjectConstructors<Bond>() {

		@Override
		public Bond construct(IObjectKey objectKey, ListKey parentKey) {
			return new Bond(objectKey, parentKey);
		}

		@Override
		public Bond construct(IObjectKey objectKey,	ListKey<? super Bond,?> parentKey, IValues<Bond> values) {
			return new Bond(
					objectKey,
					parentKey,
					values.getScalarValue(CommodityInfo.getNameAccessor()),
					values.getScalarValue(SecurityInfo.getCusipAccessor()),
					values.getScalarValue(SecurityInfo.getSymbolAccessor()),
					values.getReferencedObjectKey(BondInfo.getCurrencyAccessor()),
					values.getScalarValue(BondInfo.getInterestRateAccessor()),
					values.getScalarValue(BondInfo.getMaturityDateAccessor()),
					values.getScalarValue(BondInfo.getRedemptionValueAccessor()),
					values);
		}
	});

	private static ReferencePropertyAccessor<Currency,Bond> currencyAccessor;
	private static ScalarPropertyAccessor<Integer,Bond> interestRateAccessor;
	private static ScalarPropertyAccessor<Date,Bond> maturityDateAccessor;
	private static ScalarPropertyAccessor<Long,Bond> redemptionValueAccessor;

	@Override
	public PropertySet registerProperties() {

		IPropertyControlFactory<Integer> integerControlFactory = new IntegerControlFactory();

		IPropertyControlFactory<Date> dateControlFactory = new DateControlFactory();

		IReferenceControlFactory<Bond,Currency> currencyControlFactory = new CurrencyControlFactory<Bond>() {
			@Override
			public IObjectKey getObjectKey(Bond parentObject) {
				return parentObject.currencyKey;
			}
		};

		IPropertyControlFactory<Long> amountControlFactory = new AmountControlFactory() {

			@Override
			protected Commodity getCommodity(ExtendableObject extendableObject) {
				/*
				 * All properties in this object that are amounts are in the
				 * currency in which this bond is denominated. We therefore
				 * return the currency in which this bond is denominated.
				 */
				return ((Bond)extendableObject).getCurrency();
			}

			@Override
			public IPropertyControl createPropertyControl(Composite parent,
					ScalarPropertyAccessor<Long,?> propertyAccessor) {
				final AmountEditor editor = new AmountEditor(parent, propertyAccessor, this);

				// The format of the amount will change if either
				// the currency property of this bond changes or if
				// any of the properties in the currency change.
				editor.setListener(new SessionChangeAdapter() {
					@Override
					public void objectChanged(IModelObject changedObject, IScalarPropertyAccessor changedProperty, Object oldValue, Object newValue) {
						Bond bond = (Bond)editor.getObject();
						if (bond == null) {
							return;
						}
						// Has the currency property of the bond changed?
						if (changedObject == bond
								&& changedProperty == BondInfo.getCurrencyAccessor()) {
							editor.updateCommodity(bond.getCurrency());
						}
						// If any property in the commodity object changed then
						// the format of the amount might also change.
						if (changedObject == bond.getCurrency()) {
							editor.updateCommodity(bond.getCurrency());
						}
					}
				});

				return editor;
			}
		};

		currencyAccessor = propertySet.addProperty("currency", StocksPlugin.getResourceString("PropertyDesc.currency"), Currency.class, 2, 20, currencyControlFactory, null);
		interestRateAccessor = propertySet.addProperty("interestRate", StocksPlugin.getResourceString("PropertyDesc.interestRate"), Integer.class, 1, 15, integerControlFactory, null);
		maturityDateAccessor = propertySet.addProperty("maturityDate", StocksPlugin.getResourceString("PropertyDesc.maturityDate"), Date.class, 1, 20, dateControlFactory, null);
		redemptionValueAccessor = propertySet.addProperty("redemptionValue", StocksPlugin.getResourceString("PropertyDesc.redemptionValue"), Long.class, 2, 20, amountControlFactory, null);

		return propertySet;
	}

	/**
	 * @return
	 */
	public static ExtendablePropertySet<Bond> getPropertySet() {
		return propertySet;
	}

	/**
	 * @return
	 */
	public static ReferencePropertyAccessor<Currency,Bond> getCurrencyAccessor() {
		return currencyAccessor;
	}

	/**
	 * @return
	 */
	public static ScalarPropertyAccessor<Integer,Bond> getInterestRateAccessor() {
		return interestRateAccessor;
	}

	/**
	 * @return
	 */
	public static ScalarPropertyAccessor<Date,Bond> getMaturityDateAccessor() {
		return maturityDateAccessor;
	}

	/**
	 * @return
	 */
	public static ScalarPropertyAccessor<Long,Bond> getRedemptionValueAccessor() {
		return redemptionValueAccessor;
	}
}
