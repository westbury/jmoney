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

import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.ExtendableObject;
import net.sf.jmoney.model2.TransactionExtension;

/**
 * Property set implementation class for the properties added
 * to each Transaction object by this plug-in.
 * 
 * @author Nigel Westbury
 */
public class EbayTransaction extends TransactionExtension {

	private long shippingCost;

	private long discount;

	/**
	 * A default constructor is mandatory for all extension objects.
	 * The default constructor sets the extension properties to
	 * appropriate default values.
	 * 
	 * @param extendedObject 
	 */
	public EbayTransaction(ExtendableObject extendedObject) {
		super(extendedObject);
	}
	
	/**
	 * A Full constructor is mandatory for all extension objects.
	 * This constructor is called by the datastore to construct
	 * the extension objects when loading data.
	 */
	public EbayTransaction(ExtendableObject extendedObject, long shippingCost, long discount) {
		super(extendedObject);
		this.shippingCost = shippingCost;
		this.discount = discount;
	}
	
	public long getShippingCost() {
		return shippingCost;
	}
	
	public void setShippingCost(long shippingCost) {
		long oldShippingCost = this.shippingCost;
		this.shippingCost = shippingCost;

		// Notify the change manager.
		processPropertyChange(EbayTransactionInfo.getShippingCostAccessor(), oldShippingCost, shippingCost);
	}
	
	public long getDiscount() {
		return discount;
	}
	
	public void setDiscount(long discount) {
		long oldDiscount = this.discount;
		this.discount = discount;

		// Notify the change manager.
		processPropertyChange(EbayTransactionInfo.getDiscountAccessor(), oldDiscount, discount);
	}
}
