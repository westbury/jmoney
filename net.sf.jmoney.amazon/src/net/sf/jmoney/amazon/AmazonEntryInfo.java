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

package net.sf.jmoney.amazon;

import java.util.Date;

import net.sf.jmoney.fields.DateControlFactory;
import net.sf.jmoney.fields.IBlob;
import net.sf.jmoney.fields.ImageControlFactory;
import net.sf.jmoney.fields.TextControlFactory;
import net.sf.jmoney.isolation.IValues;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.EntryInfo;
import net.sf.jmoney.model2.ExtensionPropertySet;
import net.sf.jmoney.model2.IExtensionObjectConstructors;
import net.sf.jmoney.model2.IPropertyControlFactory;
import net.sf.jmoney.model2.IPropertyDependency;
import net.sf.jmoney.model2.IPropertySetInfo;
import net.sf.jmoney.model2.IncomeExpenseAccount;
import net.sf.jmoney.model2.PropertySet;
import net.sf.jmoney.model2.ScalarPropertyAccessor;

/**
 * Add extra properties to the Entry objects that are entries that have
 * been imported from Amazon.
 */
public class AmazonEntryInfo implements IPropertySetInfo {

	private static ExtensionPropertySet<AmazonEntry,Entry> propertySet = PropertySet.addExtensionPropertySet(AmazonEntry.class, EntryInfo.getPropertySet(), new IExtensionObjectConstructors<AmazonEntry,Entry>() {

		@Override
		public AmazonEntry construct(Entry extendedObject) {
			return new AmazonEntry(extendedObject);
		}

		@Override
		public AmazonEntry construct(Entry extendedObject, IValues<Entry> values) {
			return new AmazonEntry(
					extendedObject, 
					values.getScalarValue(getOrderIdAccessor()),
					values.getScalarValue(getTrackingNumberAccessor()),
					values.getScalarValue(getShipmentDateAccessor()),
					values.getScalarValue(getAsinOrIsbnAccessor()),
					values.getScalarValue(getPictureAccessor())
			);
		}
	});
	
	private static ScalarPropertyAccessor<String,Entry> orderIdAccessor;
	private static ScalarPropertyAccessor<String,Entry> trackingNumberAccessor;
	private static ScalarPropertyAccessor<Date,Entry> shipmentDateAccessor;
	private static ScalarPropertyAccessor<String,Entry> asinOrIsbnAccessor;
	private static ScalarPropertyAccessor<IBlob,Entry> pictureAccessor;
		
	@Override
	public PropertySet<AmazonEntry,Entry> registerProperties() {
		IPropertyControlFactory<Entry,String> textPropertyControlFactory = new TextControlFactory<Entry>();
		IPropertyControlFactory<Entry,Date> datePropertyControlFactory = new DateControlFactory<Entry>();
		IPropertyControlFactory<Entry,IBlob> imagePropertyControlFactory = new ImageControlFactory<Entry>();
		
		IPropertyDependency<Entry> x = new IPropertyDependency<Entry>() {
			@Override
			public boolean isApplicable(Entry entry) {
				return entry.getAccount() instanceof IncomeExpenseAccount;
			}
		};
		
		orderIdAccessor = propertySet.addProperty("orderId", "Amazon Order Id", String.class, 0, 80, textPropertyControlFactory, null);
		trackingNumberAccessor = propertySet.addProperty("trackingNumber", "Amazon Carrier & Tracking Number", String.class, 0, 80, textPropertyControlFactory, null);
		shipmentDateAccessor = propertySet.addProperty("shipmentDate", "Shipment Date", Date.class, 0, 100, datePropertyControlFactory, null);
		asinOrIsbnAccessor = propertySet.addProperty("asinOrIsbn", "ASIN/ISBN", String.class, 0, 80, textPropertyControlFactory, null);
		pictureAccessor = propertySet.addProperty("picture", "Item Image", IBlob.class, 0, 80, imagePropertyControlFactory, x);
		
		return propertySet;
	}

	/**
	 * @return
	 */
	public static ExtensionPropertySet<AmazonEntry,Entry> getPropertySet() {
		return propertySet;
	}

	public static ScalarPropertyAccessor<String,Entry> getOrderIdAccessor() {
		return orderIdAccessor;
	}

	public static ScalarPropertyAccessor<String,Entry> getTrackingNumberAccessor() {
		return trackingNumberAccessor;
	}

	public static ScalarPropertyAccessor<Date,Entry> getShipmentDateAccessor() {
		return shipmentDateAccessor;
	}	

	public static ScalarPropertyAccessor<String,Entry> getAsinOrIsbnAccessor() {
		return asinOrIsbnAccessor;
	}	

	public static ScalarPropertyAccessor<IBlob,Entry> getPictureAccessor() {
		return pictureAccessor;
	}	
}
