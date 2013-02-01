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

package net.sf.jmoney.model2;

import java.util.Date;

import net.sf.jmoney.JMoneyPlugin;
import net.sf.jmoney.isolation.IListManager;
import net.sf.jmoney.isolation.IObjectKey;
import net.sf.jmoney.isolation.IValues;
import net.sf.jmoney.isolation.ListKey;
import net.sf.jmoney.resources.Messages;

/**
 * The data model for an account.
 */
public abstract class CurrencyAccount extends CapitalAccount {

	/**
	 * Guaranteed non-null because the session default currency is
	 * set by default.
	 */
	protected IObjectKey currencyKey;

	protected long startBalance = 0;

	/**
	 * The full constructor for a CurrencyAccount object.  This constructor is called
	 * only by the datastore plug-in when loading data from the datastore.  The properties
	 * passed to this constructor must be valid because datastores should only pass back
	 * values that were previously saved from a CapitalAccount object.  So, for example,
	 * we can be sure that a non-null name and currency are passed to this constructor.
	 * 
	 * @param name the name of the account
	 */
	public CurrencyAccount(
			IObjectKey objectKey, 
			ListKey parent,
			String name,
			IListManager<CapitalAccount> subAccounts,
			String abbreviation,
			String comment,
			IObjectKey currencyKey,
			long startBalance,
			IValues extensionValues) { 
		super(objectKey, parent, name, subAccounts, abbreviation, comment, extensionValues);
		
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
			this.currencyKey = getDataManager().getSession().getDefaultCurrency().getObjectKey();
		}

		this.startBalance = startBalance;
	}

	/**
	 * The default constructor for a CapitalAccount object.  This constructor is called
	 * when a new CapitalAccount object is created.  The properties are set to default
	 * values.  The list properties are set to empty lists.  The parameter list for this
	 * constructor is the same as the full constructor except that there are no parameters
	 * for the scalar properties.
	 */
	public CurrencyAccount(
			IObjectKey objectKey, 
			ListKey parent) { 
		super(objectKey, parent);
		
		// Set a default name.
		this.name = Messages.CurrencyAccount_Name;
		
		// Set the currency to the session default currency.
		this.currencyKey = getDataManager().getSession().getDefaultCurrency().getObjectKey();
		
        this.startBalance = 0;
	}

    @Override	
	protected String getExtendablePropertySetId() {
		return "net.sf.jmoney.currencyAccount"; //$NON-NLS-1$
	}
	
	/**
	 * @return the locale of this account.
	 */
	public String getCurrencyCode() {
		return getCurrency().getCode();
	}

	public Currency getCurrency() {
        return (Currency)currencyKey.getObject();
	}

    @Override	
	public Commodity getCommodity(Entry entry) {
		// All entries in this account must be in the
		// same currency, so return the currency for this
		// account.
	    return getCurrency();
	}
	
	/**
	 * @return the initial balance of this account.
	 */
	public long getStartBalance() {
		return startBalance;
	}

	public void setCurrency(Currency aCurrency) {
	    if (aCurrency == null) throw new IllegalArgumentException();
        Currency oldCurrency = getCurrency();
		currencyKey = aCurrency.getObjectKey();

		// Notify the change manager.
		processPropertyChange(CurrencyAccountInfo.getCurrencyAccessor(), oldCurrency, aCurrency);
	}

	/**
	 * Sets the initial balance of this account.
	 * @param startBalance the start balance
	 */
	public void setStartBalance(long startBalance) {
        long oldStartBalance = this.startBalance;
		this.startBalance = startBalance;

		// Notify the change manager.
		processPropertyChange(CurrencyAccountInfo.getStartBalanceAccessor(), oldStartBalance, startBalance);
	}

    @Override	
	public String toString() {
		return name;
	}

    @Override	
    public String getFullAccountName() {
    	if (getParent() == null) {
    		return name;
    	} else {
    		return getParent().getFullAccountName() + "." + this.name; //$NON-NLS-1$
    	}
    }
	
	/**
	 * Get the balance at a given date
	 * 
	 * @param date
	 * @return the balance
	 * @author Faucheux
	 */
	public long getBalance(Session session, Date fromDate, Date toDate) {
		if (JMoneyPlugin.DEBUG) System.out.println("Calculing the Balance for >" + name + "< (without sub-accounts) between " + fromDate + " and " + toDate); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		
		long bal = getStartBalance();

		IEntryQueries queries = (IEntryQueries)getSession().getAdapter(IEntryQueries.class);
    	if (queries != null) {
    		bal += queries.sumOfAmounts(this, fromDate, toDate);
    	} else {
    		// IEntryQueries has not been implemented in the datastore.
    		// We must therefore provide our own implementation.
    		
    		// Sum each entry the entry between the two dates 
    		for (Entry e: getEntries()) {
    			if ((e.getTransaction().getDate().compareTo(fromDate) >= 0)
    					&& e.getTransaction().getDate().compareTo(toDate) <= 0){
    				bal += e.getAmount();
    				
    			}
    		}
    	}
    	
		return bal;
	}
	
	/**
	 * Get the balance between two dates , inclusive sub-accounts
	 * 
	 * @param date
	 * @return the balance
	 * @author Faucheux
	 */
	public long getBalanceWithSubAccounts(Session session, Date fromDate, Date toDate) {
		if (JMoneyPlugin.DEBUG) System.out.println("Calculing the Balance for >" + name + "< (with sub-accounts) between " + fromDate + " and " + toDate); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		long bal = getBalance(session, fromDate, toDate);
		
		// This logic may not be quite right.  If a stock account is a sub account of
		// a currency account then the balance of the stock account cannot be added
		// into the total (stock accounts don't hold currency).  However, what if the
		// stock account has a sub account that is a currency account?  This code
		// will not include that currency account.
		// Also, even if the sub-accounts are currency accounts, they may be in a
		// different currency.
		// However, this logic is probably ok for most uses.
		for (CapitalAccount account: getSubAccountCollection()) {
			if (account instanceof CurrencyAccount) {
				bal += ((CurrencyAccount)account).getBalanceWithSubAccounts(session, fromDate, toDate);
			}
		}
		return bal;
	}
}