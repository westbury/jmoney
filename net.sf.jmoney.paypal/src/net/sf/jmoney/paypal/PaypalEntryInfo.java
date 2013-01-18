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

package net.sf.jmoney.paypal;

import java.net.URL;

import net.sf.jmoney.fields.TextControlFactory;
import net.sf.jmoney.isolation.IValues;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.EntryInfo;
import net.sf.jmoney.model2.ExtensionPropertySet;
import net.sf.jmoney.model2.IExtensionObjectConstructors;
import net.sf.jmoney.model2.IPropertyControlFactory;
import net.sf.jmoney.model2.IPropertySetInfo;
import net.sf.jmoney.model2.PropertySet;
import net.sf.jmoney.model2.ScalarPropertyAccessor;

/**
 * Add extra properties to the Entry objects that are entries of amounts
 * of a stock or bond.
 * 
 * An entry for an amount of a stock or bond exists any time the number of
 * a particular stock in an account changes.  This can happen when a stock
 * is acquired in any way, disposed of in any way, or transfered from one
 * account to another.
 * <P>
 * The entry will contain the amount of the stock.  This amount is kept in
 * the base Amount property field.  The entry will also contain the particular
 * stock or bond concerned.  In the case of, for example, the bank account
 * entries, the currency is not kept in the entry but is kept in the account
 * object.  However, a stock account will usually contain stock in many different
 * companies (unless it is your employee stock purchase plan account). 
 * Therefore a reference to the stock Commodity
 * object is kept in the Entry object.
 * <P>
 * @author Nigel Westbury
 */
public class PaypalEntryInfo implements IPropertySetInfo {

	private static ExtensionPropertySet<PaypalEntry,Entry> propertySet = PropertySet.addExtensionPropertySet(PaypalEntry.class, EntryInfo.getPropertySet(), new IExtensionObjectConstructors<PaypalEntry,Entry>() {

		@Override
		public PaypalEntry construct(Entry extendedObject) {
			return new PaypalEntry(extendedObject);
		}

		@Override
		public PaypalEntry construct(Entry extendedObject, IValues<Entry> values) {
			return new PaypalEntry(
					extendedObject, 
					values.getScalarValue(getMerchantEmailAccessor()),
					values.getScalarValue(getItemUrlAccessor())
			);
		}
	});
	
	private static ScalarPropertyAccessor<String,Entry> merchantEmailAccessor;
	private static ScalarPropertyAccessor<URL,Entry> itemUrlAccessor;
	
	@Override
	public PropertySet<PaypalEntry,Entry> registerProperties() {
		IPropertyControlFactory<String> textPropertyControlFactory = new TextControlFactory();
		IPropertyControlFactory<URL> urlPropertyControlFactory = new UrlControlFactory();
		
		merchantEmailAccessor = propertySet.addProperty("merchantEmail", "Merchant E-Mail", String.class, 0, 80, textPropertyControlFactory, null);
		itemUrlAccessor = propertySet.addProperty("itemUrl", "Item URL", URL.class, 0, 100, urlPropertyControlFactory, null);
		
		return propertySet;
	}

	/**
	 * @return
	 */
	public static ExtensionPropertySet<PaypalEntry,Entry> getPropertySet() {
		return propertySet;
	}

	public static ScalarPropertyAccessor<String,Entry> getMerchantEmailAccessor() {
		return merchantEmailAccessor;
	}

	public static ScalarPropertyAccessor<URL,Entry> getItemUrlAccessor() {
		return itemUrlAccessor;
	}	
}
