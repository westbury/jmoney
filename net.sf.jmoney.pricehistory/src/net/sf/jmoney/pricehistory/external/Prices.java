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

package net.sf.jmoney.pricehistory.external;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.jmoney.model2.Commodity;
import net.sf.jmoney.pricehistory.CommodityPricing;
import net.sf.jmoney.pricehistory.CommodityPricingInfo;
import net.sf.jmoney.pricehistory.Price;
import net.sf.jmoney.pricehistory.PriceInfo;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;

public class Prices {

	private Commodity commodity;
	
	private IPriceFetcher fetcher;
	
	public Prices(Commodity commodity) {
		this.commodity = commodity;

		IExtensionRegistry registry = Platform.getExtensionRegistry();
		
		/**
		 * The only check that can be done from the XML without loading the plug-in
		 * is the 'instanceOf' attribute.  If this attribute is specified, the extension
		 * will be loaded only if the commodity is of that type.
		 */
		final Map<String, IConfigurationElement> extensionMap = new HashMap<String, IConfigurationElement>();
		
		List<IPriceFetcher> allFetchers = new ArrayList<IPriceFetcher>();
		
		for (IConfigurationElement element: registry.getConfigurationElementsFor("net.sf.jmoney.pricehistory.sources")) { //$NON-NLS-1$
			if (element.getName().equals("source")) { //$NON-NLS-1$
				String commodityClass = element.getAttribute("instanceOf"); //$NON-NLS-1$
				try {
					if (commodityClass == null
							|| Class.forName(commodityClass).isAssignableFrom(commodity.getClass())) {
						IPriceFetcher eachFetcher = (IPriceFetcher)element.createExecutableExtension("class");
						if (eachFetcher.canProcess(commodity)) {
							allFetchers.add(eachFetcher);
						}
					}
				} catch (ClassNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (CoreException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
		if (allFetchers.size() == 0) {
			// TODO: How do we want to handle this?
			throw new RuntimeException("no source for prices for commodity available");
		} else if (allFetchers.size() == 1) {
			fetcher = allFetchers.get(0);
		} else {
			// TODO: Open a dialog box to ask the user for
			// the preferred source and think about how or where
			// we want to remember the user's choice.
			
			// For time being, just use the first one found.
			fetcher = allFetchers.get(0);
		}
	}
	
	public Price getPrice(Date date) {
		CommodityPricing pricing = commodity.getExtension(CommodityPricingInfo.getPropertySet(), true);
		
		for (Price price : pricing.getPriceCollection()) {
			if (price.getDate().equals(date)) {
				return price;
			}
		}

		Long priceAmount = fetcher.getPrice(date);
		
		if (priceAmount != null) {
			Price newPrice = pricing.getPriceCollection().createNewElement(PriceInfo.getPropertySet());
			newPrice.setDate(date);
			newPrice.setPrice(priceAmount);
			return newPrice;
		}
		
		return null;
	}
	
	public Price[] getPrices(Date startDate, Date endDate) {
		throw new RuntimeException("not implemented");
	}
}
