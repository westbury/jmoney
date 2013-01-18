/*
 *
 *  JMoney - A Personal Finance Manager
 *  Copyright (c) 2002 Johann Gyger <johann.gyger@switzerland.org>
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

package net.sf.jmoney.property.model;

import net.sf.jmoney.fields.IAmountFormatter;
import net.sf.jmoney.isolation.IListManager;
import net.sf.jmoney.isolation.IObjectKey;
import net.sf.jmoney.isolation.IValues;
import net.sf.jmoney.isolation.ListKey;
import net.sf.jmoney.model2.CapitalAccount;
import net.sf.jmoney.model2.Commodity;
import net.sf.jmoney.model2.Currency;
import net.sf.jmoney.model2.Entry;

/**
 * The data model for an account that holds a piece of real estate.
 * It may seem unneccessary to create an 'account' to hold a piece of real
 * estate.  However the JMoney data model requires all assets, whether currency,
 * stock or anything else, to be held in an account.
 * 
 * The account contains information such as the income category for rental income and
 * the expense category for expenses.  It might make sense to put multiple properties in
 * one account if, for example, you own a block of flats that you rent out individually,
 * you don't want to consider the block as one asset because you occassionally sell off
 * an individual flat, but you don't want to create separate income and expense accounts
 * for each flat.
 */
public class RealPropertyAccount extends CapitalAccount {

	/**
	 * Guaranteed non-null because the session default currency is
	 * set by default.
	 */
	IObjectKey currencyKey;

	/**
	 * The full constructor for a PropertyAccount object.  This constructor is called
	 * only be the datastore when loading data from the datastore.  The properties
	 * passed to this constructor must be valid because datastores should only pass back
	 * values that were previously saved from a PropertyAccount object.  So, for example,
	 * we can be sure that a non-null currency is passed to this constructor.
	 * 
	 * @param name the name of the account
	 */
	public RealPropertyAccount(
			IObjectKey objectKey,
			ListKey parentKey,
			String name,
			IListManager<CapitalAccount> subAccounts,
			String abbreviation,
			String comment,
			IObjectKey currencyKey,
			IValues extensionValues) {
			
		super(objectKey, parentKey, name, subAccounts, abbreviation, comment, extensionValues);
		
		/*
		 * The currency for this account is not allowed to be null, because
		 * users of this class may assume it to be non-null and would not know
		 * how to handle this account if it were null.
		 * 
		 * If null is passed, set to the default currency for the session.
		 * This is guaranteed to be never null.
		 */
		if (currencyKey != null) {
			this.currencyKey = currencyKey;
		} else {
			this.currencyKey = objectKey.getDataManager().getSession().getDefaultCurrency().getObjectKey();
		}
	}

	public RealPropertyAccount(
			IObjectKey objectKey, 
			ListKey parent) { 
		super(objectKey, parent);
		
		// Overwrite the default name with our own default name.
		name = "New Stock Account";
		
		this.currencyKey = objectKey.getDataManager().getSession().getDefaultCurrency().getObjectKey();
	}

	@Override
	protected String getExtendablePropertySetId() {
		return "net.sf.jmoney.property.realPropertyAccount";
	}
	
	public Currency getCurrency() {
        return (Currency)currencyKey.getObject();
	}

	/**
	 * Returns the commodity represented by the amount in this
	 * entry.  If this entry represents an addition of stock to
	 * the account or a removal of stock from the account then
	 * the amount represents the amount of that stock.  Otherwise
	 * the amount represents an amount in the currency for the account.
	 */
	@Override
	public Commodity getCommodity(Entry entry) {
		/*
		 * If a stock is set as the commodity for an entry in a stock account
		 * then the entry represents a gain or loss of stock in the account.
		 * Note that the stock field may be set in income and expense accounts
		 * (such as a dividend account) and in those cases the entry does not
		 * represent a change in the amount of stock, only that it is associated
		 * with that stock.
		 */
		// TODO: remove this test at some time because getCommodity should never
		// return null.
		
		// TODO: This method is incorrect.  If this entry is a purchase or sale entry
		// but the stock has not yet been entered by the user then getCommodity will
		// return null and so currency will be returned.  This results in the wrong formatter
		// being used for the share quantity (it will be parsed as though it were a currency
		// amount).  This issue has been fixed by simply not using this method, but then
		// why bother to have this method at all?
		if (entry.getCommodity() != null) {
			return entry.getCommodity();
		} else {
			return getCurrency();
		}
	}
	
	public void setCurrency(Currency aCurrency) {
	    if (aCurrency == null) throw new IllegalArgumentException();
        Currency oldCurrency = getCurrency();
		currencyKey = aCurrency.getObjectKey();

		// Notify the change manager.
		processPropertyChange(RealPropertyAccountInfo.getCurrencyAccessor(), oldCurrency, aCurrency);
	}

	/**
	 * Returns an object that knows how to both format and parse property quantities.
	 * 
	 * The quantity of a property can only be +1 (if you own it or are buying it), 0 (if you have sold it),
	 * or -1 (if this is the transaction representing the sale).
	 */
	public IAmountFormatter getQuantityFormatter() {
		return new IAmountFormatter() {
			public String format(long amount) {
				return Long.toString(amount);
			}

			public long parse(String amountString) {
				return Long.parseLong(amountString);
			}
		};
	}
}
