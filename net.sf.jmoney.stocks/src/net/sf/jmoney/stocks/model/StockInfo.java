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

import net.sf.jmoney.fields.TextControlFactory;
import net.sf.jmoney.isolation.IObjectKey;
import net.sf.jmoney.isolation.IValues;
import net.sf.jmoney.isolation.ListKey;
import net.sf.jmoney.model2.CommodityInfo;
import net.sf.jmoney.model2.ExtendablePropertySet;
import net.sf.jmoney.model2.IExtendableObjectConstructors;
import net.sf.jmoney.model2.IPropertyControlFactory;
import net.sf.jmoney.model2.IPropertySetInfo;
import net.sf.jmoney.model2.PropertySet;
import net.sf.jmoney.model2.ScalarPropertyAccessor;
import net.sf.jmoney.stocks.StocksPlugin;

/**
 * The metadata for the Stock class.
 *
 * @author Nigel Westbury
 */
public class StockInfo implements IPropertySetInfo {

	private static ExtendablePropertySet<Stock> propertySet = PropertySet.addDerivedFinalPropertySet(Stock.class, "Stock", SecurityInfo.getPropertySet(), new IExtendableObjectConstructors<Stock>() {

		@Override
		public Stock construct(IObjectKey objectKey, ListKey parentKey) {
			return new Stock(objectKey, parentKey);
		}

		@Override
		public Stock construct(IObjectKey objectKey, ListKey<? super Stock,?> parentKey, IValues<Stock> values) {
			return new Stock(
					objectKey,
					parentKey,
					values.getScalarValue(CommodityInfo.getNameAccessor()),
					values.getScalarValue(SecurityInfo.getCusipAccessor()),
					values.getScalarValue(SecurityInfo.getSymbolAccessor()),
					values.getScalarValue(StockInfo.getNominalValueAccessor()),
					values);
		}
	});

	private static ScalarPropertyAccessor<String,Stock> nominalValueAccessor;

	@Override
	public PropertySet registerProperties() {

		IPropertyControlFactory<Stock,String> textControlFactory = new TextControlFactory<Stock>();

		nominalValueAccessor = propertySet.addProperty("nominalValue", StocksPlugin.getResourceString("PropertyDesc.nominalValue"), String.class, 2, 20, textControlFactory, null);

		return propertySet;
	}

	/**
	 * @return
	 */
	public static ExtendablePropertySet<Stock> getPropertySet() {
		return propertySet;
	}

	/**
	 * @return
	 */
	public static ScalarPropertyAccessor<String,Stock> getNominalValueAccessor() {
		return nominalValueAccessor;
	}
}
