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
import net.sf.jmoney.model2.CommodityInfo;
import net.sf.jmoney.model2.ExtendablePropertySet;
import net.sf.jmoney.model2.IPropertyControlFactory;
import net.sf.jmoney.model2.IPropertySetInfo;
import net.sf.jmoney.model2.PropertySet;
import net.sf.jmoney.model2.ScalarPropertyAccessor;
import net.sf.jmoney.stocks.StocksPlugin;

/**
 * The metadata for the Security class.
 *
 * @author Nigel Westbury
 */
public class SecurityInfo implements IPropertySetInfo {

	private static ExtendablePropertySet<Security> propertySet = PropertySet.addDerivedAbstractPropertySet(Security.class, "Security", CommodityInfo.getPropertySet());

	private static ScalarPropertyAccessor<String,Security> cusipAccessor;
	private static ScalarPropertyAccessor<String,Security> symbolAccessor;

	@Override
	public PropertySet registerProperties() {

		IPropertyControlFactory<String> textControlFactory = new TextControlFactory();

		cusipAccessor = propertySet.addProperty("cusip", StocksPlugin.getResourceString("PropertyDesc.cusip"), String.class, 2, 20, textControlFactory, null);
		symbolAccessor = propertySet.addProperty("symbol", StocksPlugin.getResourceString("PropertyDesc.symbol"), String.class, 2, 20, textControlFactory, null);

		return propertySet;
	}

	/**
	 * @return
	 */
	public static ExtendablePropertySet<Security> getPropertySet() {
		return propertySet;
	}

	/**
	 * @return
	 */
	public static ScalarPropertyAccessor<String,Security> getCusipAccessor() {
		return cusipAccessor;
	}

	/**
	 * @return
	 */
	public static ScalarPropertyAccessor<String,Security> getSymbolAccessor() {
		return symbolAccessor;
	}
}
