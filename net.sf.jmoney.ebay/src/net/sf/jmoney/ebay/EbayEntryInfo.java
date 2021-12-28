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

package net.sf.jmoney.ebay;

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
 * been imported from Ebay.
 */
public class EbayEntryInfo implements IPropertySetInfo {

	private static ExtensionPropertySet<EbayEntry,Entry> propertySet = PropertySet.addExtensionPropertySet(EbayEntry.class, EntryInfo.getPropertySet(), new IExtensionObjectConstructors<EbayEntry,Entry>() {

		@Override
		public EbayEntry construct(Entry extendedObject) {
			return new EbayEntry(extendedObject);
		}

		@Override
		public EbayEntry construct(Entry extendedObject, IValues<Entry> values) {
			return new EbayEntry(
					extendedObject, 
					values.getScalarValue(getOrderNumberAccessor()),
					values.getScalarValue(getItemNumberAccessor()),
					values.getScalarValue(getTrackingNumberAccessor()),
					values.getScalarValue(getDeliveryDateAccessor()),
					values.getScalarValue(getEbayDescriptionAccessor()),
					values.getScalarValue(getSoldByAccessor()),
					values.getScalarValue(getImageCodeAccessor()),
					values.getScalarValue(getPictureAccessor())
			);
		}
	});
	
	private static ScalarPropertyAccessor<String,Entry> orderNumberAccessor;
	private static ScalarPropertyAccessor<String,Entry> itemNumberAccessor;
	private static ScalarPropertyAccessor<String,Entry> trackingNumberAccessor;
	private static ScalarPropertyAccessor<Date,Entry> deliveryDateAccessor;
	private static ScalarPropertyAccessor<String,Entry> ebayDescriptionAccessor;
	private static ScalarPropertyAccessor<String,Entry> soldByAccessor;
	private static ScalarPropertyAccessor<String,Entry> imageCodeAccessor;
	private static ScalarPropertyAccessor<IBlob,Entry> pictureAccessor;
		
	@Override
	public PropertySet<EbayEntry,Entry> registerProperties() {
		IPropertyControlFactory<Entry,String> textPropertyControlFactory = new TextControlFactory<Entry>();
		IPropertyControlFactory<Entry,Date> datePropertyControlFactory = new DateControlFactory<Entry>();
		IPropertyControlFactory<Entry,IBlob> imagePropertyControlFactory = new ImageControlFactory<Entry>();
		
		IPropertyDependency<Entry> x = new IPropertyDependency<Entry>() {
			@Override
			public boolean isApplicable(Entry entry) {
				return entry.getAccount() instanceof IncomeExpenseAccount;
			}
		};
		
		orderNumberAccessor = propertySet.addProperty("orderNumber", "Ebay Order Number", String.class, 0, 80, textPropertyControlFactory, null);
		itemNumberAccessor = propertySet.addProperty("itemNumber", "Ebay Item Number", String.class, 0, 80, textPropertyControlFactory, null);
		trackingNumberAccessor = propertySet.addProperty("trackingNumber", "Ebay Carrier & Tracking Number", String.class, 0, 80, textPropertyControlFactory, null);
		deliveryDateAccessor = propertySet.addProperty("deliveryDate", "Delivery Date", Date.class, 0, 100, datePropertyControlFactory, null);
		ebayDescriptionAccessor = propertySet.addProperty("ebayDescription", "Original Ebay Description", String.class, 0, 80, textPropertyControlFactory, null);
		soldByAccessor = propertySet.addProperty("soldBy", "Sold by", String.class, 0, 80, textPropertyControlFactory, null);
		imageCodeAccessor = propertySet.addProperty("imageCode", "Image Code", String.class, 0, 80, textPropertyControlFactory, null);
		pictureAccessor = propertySet.addProperty("picture", "Item Image", IBlob.class, 0, 80, imagePropertyControlFactory, x);
		
		return propertySet;
	}

	/**
	 * @return
	 */
	public static ExtensionPropertySet<EbayEntry,Entry> getPropertySet() {
		return propertySet;
	}

	public static ScalarPropertyAccessor<String,Entry> getOrderNumberAccessor() {
		return orderNumberAccessor;
	}

	public static ScalarPropertyAccessor<String,Entry> getItemNumberAccessor() {
		return itemNumberAccessor;
	}

	public static ScalarPropertyAccessor<String,Entry> getTrackingNumberAccessor() {
		return trackingNumberAccessor;
	}

	public static ScalarPropertyAccessor<Date,Entry> getDeliveryDateAccessor() {
		return deliveryDateAccessor;
	}	

	public static ScalarPropertyAccessor<String,Entry> getSoldByAccessor() {
		return soldByAccessor;
	}	

	public static ScalarPropertyAccessor<String,Entry> getEbayDescriptionAccessor() {
		return ebayDescriptionAccessor;
	}	

	public static ScalarPropertyAccessor<String,Entry> getImageCodeAccessor() {
		return imageCodeAccessor;
	}	

	public static ScalarPropertyAccessor<IBlob,Entry> getPictureAccessor() {
		return pictureAccessor;
	}	
}
