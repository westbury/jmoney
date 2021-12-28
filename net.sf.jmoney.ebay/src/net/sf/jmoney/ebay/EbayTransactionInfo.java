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

import net.sf.jmoney.fields.AmountControlFactory;
import net.sf.jmoney.isolation.IValues;
import net.sf.jmoney.model2.Commodity;
import net.sf.jmoney.model2.ExtensionPropertySet;
import net.sf.jmoney.model2.IExtensionObjectConstructors;
import net.sf.jmoney.model2.IPropertyControlFactory;
import net.sf.jmoney.model2.IPropertySetInfo;
import net.sf.jmoney.model2.PropertySet;
import net.sf.jmoney.model2.ScalarPropertyAccessor;
import net.sf.jmoney.model2.Transaction;
import net.sf.jmoney.model2.TransactionInfo;
import net.sf.jmoney.resources.Messages;

/**
 * Add extra properties to the Transaction objects that are entries that have
 * been imported from Ebay.
 */
public class EbayTransactionInfo implements IPropertySetInfo {

	private static ExtensionPropertySet<EbayTransaction,Transaction> propertySet = PropertySet.addExtensionPropertySet(EbayTransaction.class, TransactionInfo.getPropertySet(), new IExtensionObjectConstructors<EbayTransaction,Transaction>() {

		@Override
		public EbayTransaction construct(Transaction extendedObject) {
			return new EbayTransaction(extendedObject);
		}

		@Override
		public EbayTransaction construct(Transaction extendedObject, IValues<Transaction> values) {
			return new EbayTransaction(
					extendedObject, 
					values.getScalarValue(getShippingCostAccessor()),
					values.getScalarValue(getDiscountAccessor())
			);
		}
	});
	
	private static ScalarPropertyAccessor<Long,Transaction> shippingCostAccessor = null;
	private static ScalarPropertyAccessor<Long,Transaction> discountAccessor = null;
		
	@Override
	public PropertySet<EbayTransaction,Transaction> registerProperties() {
        IPropertyControlFactory<Transaction,Long> amountControlFactory = new AmountControlFactory<Transaction>() {
        	/**
        	 * @trackedGetter
        	 */
		    @Override
			protected Commodity getCommodity(Transaction object) {
				// For now, assume ebay account is Sterling
	    	    return object.getSession().getCurrencyForCode("GBP");
		    }
        };

		shippingCostAccessor = propertySet.addProperty("shippingCost", "Shipping Cost", Long.class, 2, 70,  amountControlFactory, null); //$NON-NLS-1$
		discountAccessor = propertySet.addProperty("discount", "Discount", Long.class, 2, 70,  amountControlFactory, null); //$NON-NLS-1$
		
		return propertySet;
	}

	/**
	 * @return
	 */
	public static ExtensionPropertySet<EbayTransaction,Transaction> getPropertySet() {
		return propertySet;
	}

	public static ScalarPropertyAccessor<Long,Transaction> getShippingCostAccessor() {
		return shippingCostAccessor;
	}

	public static ScalarPropertyAccessor<Long,Transaction> getDiscountAccessor() {
		return discountAccessor;
	}
}
