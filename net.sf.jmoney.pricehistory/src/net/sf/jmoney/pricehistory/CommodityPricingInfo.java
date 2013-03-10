/*
 *
 *  JMoney - A Personal Finance Manager
 *  Copyright (c) 2010 Nigel Westbury <westbury@users.sourceforge.net>
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

package net.sf.jmoney.pricehistory;

import net.sf.jmoney.fields.CurrencyControlFactory;
import net.sf.jmoney.isolation.IObjectKey;
import net.sf.jmoney.isolation.IValues;
import net.sf.jmoney.isolation.ObjectCollection;
import net.sf.jmoney.model2.Commodity;
import net.sf.jmoney.model2.CommodityInfo;
import net.sf.jmoney.model2.Currency;
import net.sf.jmoney.model2.ExtensionPropertySet;
import net.sf.jmoney.model2.IExtensionObjectConstructors;
import net.sf.jmoney.model2.IListGetter;
import net.sf.jmoney.model2.IPropertySetInfo;
import net.sf.jmoney.model2.IReferenceControlFactory;
import net.sf.jmoney.model2.ListPropertyAccessor;
import net.sf.jmoney.model2.PropertySet;
import net.sf.jmoney.model2.ReferencePropertyAccessor;
import net.sf.jmoney.pricehistory.resources.Messages;

/**
 * Provides the metadata for the extra properties added to each commodity
 * by this plug-in.
 * 
 * @author Nigel Westbury
 */
public class CommodityPricingInfo implements IPropertySetInfo {

	private static ExtensionPropertySet<CommodityPricing,Commodity> propertySet = PropertySet.addExtensionPropertySet(CommodityPricing.class, CommodityInfo.getPropertySet(), new IExtensionObjectConstructors<CommodityPricing,Commodity>() {

		@Override
		public CommodityPricing construct(Commodity extendedObject) {
			return new CommodityPricing(extendedObject);
		}

		@Override
		public CommodityPricing construct(Commodity extendedObject, IValues<Commodity> values) {
			return new CommodityPricing(
					extendedObject, 
					values.getReferencedObjectKey(getCurrencyAccessor()),
					values.getListManager(extendedObject.getObjectKey(), getPricesAccessor())
			);
		}
	});
	
	private static ReferencePropertyAccessor<Currency,Commodity> currencyAccessor = null;
	private static ListPropertyAccessor<Price,Commodity> pricesAccessor = null;
	
	@Override
	public ExtensionPropertySet<CommodityPricing, Commodity> registerProperties() {
		IReferenceControlFactory<CommodityPricing,Commodity,Currency> currencyControlFactory = new CurrencyControlFactory<CommodityPricing, Commodity>() {
			@Override
			public IObjectKey getObjectKey(CommodityPricing parentObject) {
				return parentObject.currencyKey;
			}
		};

		IListGetter<CommodityPricing, Price> patternListGetter = new IListGetter<CommodityPricing, Price>() {
			@Override
			public ObjectCollection<Price> getList(CommodityPricing parentObject) {
				return parentObject.getPriceCollection();
			}
		};
	
		currencyAccessor = propertySet.addProperty("currency", Messages.CommodityPricingInfo_Currency, Currency.class, 1, 20, currencyControlFactory, null);
		pricesAccessor = propertySet.addPropertyList("prices", Messages.CommodityPricingInfo_Prices, PriceInfo.getPropertySet(), patternListGetter);
		
		return propertySet;
	}

	/**
	 * @return
	 */
	public static ExtensionPropertySet<CommodityPricing,Commodity> getPropertySet() {
		return propertySet;
	}

	/**
	 * @return
	 */
	public static ReferencePropertyAccessor<Currency,Commodity> getCurrencyAccessor() {
		return currencyAccessor;
	}	

	/**
	 * @return
	 */
	public static ListPropertyAccessor<Price,Commodity> getPricesAccessor() {
		return pricesAccessor;
	}	
}
