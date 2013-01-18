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

import net.sf.jmoney.isolation.IListManager;
import net.sf.jmoney.isolation.IObjectKey;
import net.sf.jmoney.isolation.ObjectCollection;
import net.sf.jmoney.model2.CommodityExtension;
import net.sf.jmoney.model2.Currency;
import net.sf.jmoney.model2.ExtendableObject;

/**
 * An extension object that extends Commodity objects.
 * <P>
 * This extension object maintains the price history information for the
 * commodity.
 * 
 * @author Nigel Westbury
 */
public class CommodityPricing extends CommodityExtension {
	
	/**
	 * Element: Currency
	 */
	public IObjectKey currencyKey = null; 
	
	protected IListManager<Price> prices;
	
	/**
	 * A default constructor is mandatory for all extension objects.
	 * The default constructor sets the extension properties to
	 * appropriate default values.
	 */
	public CommodityPricing(ExtendableObject extendedObject) {
		super(extendedObject);
		this.prices = extendedObject.getObjectKey().constructListManager(CommodityPricingInfo.getPricesAccessor());
	}
	
	/**
	 * A Full constructor is mandatory for all extension objects.
	 * This constructor is called by the datastore to construct
	 * the extension objects when loading data.
	 * 
	 */
	public CommodityPricing(
			ExtendableObject extendedObject,
			IObjectKey currencyKey,
			IListManager<Price> prices) {
		super(extendedObject);
		this.currencyKey = currencyKey;
		this.prices = prices;
	}
	
	/**
	 * Returns the currency in which the price history for this commodity is kept.
	 * 
	 * @return the currency in which the price history for this commodity is kept
	 * 			or null if no price history is kept for this commodity
	 */
	public Currency getCurrency() {
        return currencyKey == null
		? null
				: (Currency)currencyKey.getObject();
	}
		
	/**
	 * Sets the default category.
	 */
	public void setCurrency(Currency currency) {
        Currency oldCurrency = getCurrency();
        this.currencyKey = 
        	currency == null
        		? null 
        		: currency.getObjectKey();

		// Notify the change manager.
		processPropertyChange(CommodityPricingInfo.getCurrencyAccessor(), oldCurrency, currency);
	}

	public ObjectCollection<Price> getPriceCollection() {
		return new ObjectCollection<Price>(prices, getBaseObject(), CommodityPricingInfo.getPricesAccessor());
	}
}
