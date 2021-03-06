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
import net.sf.jmoney.fields.DateControlFactory;
import net.sf.jmoney.fields.IAmountFormatter;
import net.sf.jmoney.isolation.IObjectKey;
import net.sf.jmoney.isolation.IValues;
import net.sf.jmoney.model2.Commodity;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.EntryInfo;
import net.sf.jmoney.model2.ExtensionPropertySet;
import net.sf.jmoney.model2.IExtensionObjectConstructors;
import net.sf.jmoney.model2.IPropertyControlFactory;
import net.sf.jmoney.model2.IPropertySetInfo;
import net.sf.jmoney.model2.IReferenceControlFactory;
import net.sf.jmoney.model2.PropertySet;
import net.sf.jmoney.model2.ReferencePropertyAccessor;
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
public class StockEntryInfo implements IPropertySetInfo {

	private static ExtensionPropertySet<StockEntry,Entry> propertySet = PropertySet.addExtensionPropertySet(StockEntry.class, EntryInfo.getPropertySet(), new IExtensionObjectConstructors<StockEntry,Entry>() {

		@Override
		public StockEntry construct(Entry extendedObject) {
			return new StockEntry(extendedObject);
		}

		@Override
		public StockEntry construct(Entry extendedObject, IValues<Entry> values) {
			return new StockEntry(
					extendedObject,
					values.getReferencedObjectKey(getSecurityAccessor()),
					values.getScalarValue(getQuantityAccessor()),
					values.getScalarValue(getBargainDateAccessor())
			);
		}
	});

	private static ReferencePropertyAccessor<Security,Entry> securityAccessor;
	private static ScalarPropertyAccessor<Long,Entry> quantityAccessor;
	private static ScalarPropertyAccessor<Date,Entry> bargainDateAccessor;

	@Override
	public PropertySet registerProperties() {
		IReferenceControlFactory<StockEntry,Entry,Security> securityPropertyControlFactory = new SecurityControlFactory<StockEntry,Entry>() {
			@Override
			public IObjectKey getObjectKey(StockEntry parentObject) {
				return parentObject.securityKey;
			}
		};

		IPropertyControlFactory<Entry,Date> datePropertyControlFactory = new DateControlFactory<Entry>();

        IPropertyControlFactory<Entry,Long> quantityControlFactory = new AmountControlFactory<Entry>() {
        	/**
        	 * @trackedGetter
        	 */
		    @Override
			protected IAmountFormatter getCommodity(Entry object) {
				// If not enough information has yet been set to determine
				// the commodity of the amount in this entry, return
				// the default currency.
	    	    Commodity commodity = StockEntryInfo.securityAccessor.getValue(object);
	    	    if (commodity == null) {
	    	    	// TODO we should really use StockAccount.getQuantityFormatter().
	    	    	// However it is messy to get an instance of a StockAccount from here,
	    	    	// and it may be in practice there is always a security set in this object.
	    	    	commodity = object.getSession().getDefaultCurrency();
	    	    }
	    	    return commodity;
		    }
        };


		securityAccessor = propertySet.addProperty("security", "Security", Security.class, 2, 20, securityPropertyControlFactory, null);
		quantityAccessor = propertySet.addProperty("quantity", "Quantity on which Dividend Paid", Long.class, 0, 20, quantityControlFactory, null);
		bargainDateAccessor = propertySet.addProperty("bargainDate", "Bargain Date", Date.class, 0, 20, datePropertyControlFactory, null);

		return propertySet;
	}

	/**
	 * @return
	 */
	public static ExtensionPropertySet<StockEntry,Entry> getPropertySet() {
		return propertySet;
	}

	/**
	 * @return
	 */
	public static ReferencePropertyAccessor<Security,Entry> getSecurityAccessor() {
		return securityAccessor;
	}

	/**
	 * @return
	 */
	public static ScalarPropertyAccessor<Long, Entry> getQuantityAccessor() {
		return quantityAccessor;
	}

	/**
	 * @return
	 */
	public static ScalarPropertyAccessor<Date,Entry> getBargainDateAccessor() {
		return bargainDateAccessor;
	}
}
