/*
 *
 *  JMoney - A Personal Finance Manager
 *  Copyright (c) 2002 Johann Gyger <johann.gyger@switzerland.org>
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

import java.util.Date;

import net.sf.jmoney.isolation.IObjectKey;
import net.sf.jmoney.isolation.IValues;
import net.sf.jmoney.isolation.ListKey;
import net.sf.jmoney.model2.ExtendableObject;

/**
 * The data model for an object that represents the market price of a given
 * commodity on a given date.  The currency is stored with the commodity, this
 * plug-in supporting pricing in only one currency for each commodity.
 * <P>
 * Just a single price per day can be stored.  The price tracking support in this
 * plug-in is designed to be used for tax purposes, such as capital gains tax calculations
 * and also for viewing of investment performance.  If you require more detailed information
 * such as intra-day highs and lows then you are better interfacing directly to plug-ins that
 * can provide these data such as Eclipse Trader (Technium).
 */
public final class Price extends ExtendableObject {
	
	protected Date date = null;
	
	protected long price = 0;
	
    /**
     * Constructor used by datastore plug-ins to create
     * a pattern object.
     *
     * @param parent The key to a Transaction object.
     * 		This parameter must be non-null.
     * 		The getObject method must not be called on this
     * 		key from within this constructor because the
     * 		key may not yet be in a state in which it is
     * 		capable of materializing an object.   
     */
	public Price(
			IObjectKey objectKey,
			ListKey<? super Price,?> parentKey,
			Date       date,
			long       price,
    		IValues<Price>    extensionValues) {
		super(objectKey, parentKey, extensionValues);

		this.date = date;
		this.price = price;
	}
	
    /**
     * Constructor used by datastore plug-ins to create
     * a price object.
     *
     * @param parent The key to a Commodity object.
     * 		This parameter must be non-null.
     * 		The getObject method must not be called on this
     * 		key from within this constructor because the
     * 		key may not yet be in a state in which it is
     * 		capable of materializing an object.   
     */
	public Price(
			IObjectKey objectKey,
    		ListKey<? super Price,?> parentKey) {
		super(objectKey, parentKey);

		this.date = null;
		this.price = 0;
	}

	@Override
	protected String getExtendablePropertySetId() {
		return "net.sf.jmoney.reconciliation.pattern";
	}
	
	public Date getDate() {
		return date;
	}
	
	public long getPrice() {
		return price;
	}

	public void setDate(Date date) {
		Date oldDate = this.date;
		this.date = date;
		
		// Notify the change manager.
		processPropertyChange(PriceInfo.getDateAccessor(), oldDate, date);
	}
	
	public void setPrice(long price) {
		long oldPrice = this.price;
		this.price = price;
		
		// Notify the change manager.
		processPropertyChange(PriceInfo.getPriceAccessor(), oldPrice, price);
	}
}
