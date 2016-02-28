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

import java.security.InvalidParameterException;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

import net.sf.jmoney.isolation.IListManager;
import net.sf.jmoney.isolation.IObjectKey;
import net.sf.jmoney.isolation.IValues;
import net.sf.jmoney.isolation.ListKey;
import net.sf.jmoney.isolation.ObjectCollection;
import net.sf.jmoney.isolation.ReferenceViolationException;

/**
 * Holds the fields that will be saved in a file.
 */
public class Session extends ExtendableObject {

    IObjectKey defaultCurrencyKey;

    private IListManager<Commodity> commodities;

    private IListManager<Account> accounts;  // Only the Accounts of Level 0

    private IListManager<Transaction> transactions;

    /**
     * A map cache of currencies, used to get a currency object given
     * a currency code.  This cache must be updated as currencies are
     * added, removed, or when a currency's code changes.
     */
    Hashtable<String, Currency> currencies = new Hashtable<String, Currency>();

	/**
     * Constructor used by datastore plug-ins to create
     * a session object.
     */
    public Session(
    		IObjectKey objectKey,
			ListKey parentKey,
    		IListManager<Commodity> commodities,
			IListManager<Account> accounts,
			IListManager<Transaction> transactions,
			IObjectKey defaultCurrencyKey,
    		IValues<Session> extensionValues) {
    	super(objectKey, parentKey, extensionValues);

    	this.commodities = commodities;
    	this.accounts = accounts;
    	this.transactions = transactions;
    	this.defaultCurrencyKey = defaultCurrencyKey;

        /*
		 * Load the currencies into our cached map.
		 */
    	for (Commodity commodity: commodities) {
    		if (commodity instanceof Currency) {
    			Currency currency = (Currency)commodity;
    			if (currency.getCode() != null) {
    				this.currencies.put(currency.getCode(), currency);
    			}
    		}
    	}
    }

    /**
     * Constructor used by datastore plug-ins to create
     * a session object.
     */
    public Session(
    		IObjectKey objectKey,
			ListKey parentKey) {
    	super(objectKey, parentKey);

    	this.commodities = objectKey.constructListManager(SessionInfo.getCommoditiesAccessor());
    	this.accounts = objectKey.constructListManager(SessionInfo.getAccountsAccessor());
    	this.transactions = objectKey.constructListManager(SessionInfo.getTransactionsAccessor());
   		this.defaultCurrencyKey = null;
    }

    @Override
	protected String getExtendablePropertySetId() {
		return "net.sf.jmoney.session"; //$NON-NLS-1$
	}

    public Currency getDefaultCurrency() {
        return defaultCurrencyKey == null
		? null
				: (Currency)defaultCurrencyKey.getObject();
    }

    /**
     *
     * @param defaultCurrency the default currency, which cannot
     * 			be null because a default currency must always
     * 			be set for a session
     */
    public void setDefaultCurrency(Currency defaultCurrency) {
        Currency oldDefaultCurrency = getDefaultCurrency();
        this.defaultCurrencyKey = defaultCurrency.getObjectKey();

		// Notify the change manager.
		processPropertyChange(SessionInfo.getDefaultCurrencyAccessor(), oldDefaultCurrency, defaultCurrency);
    }

	/**
	 * @param code the currency code.
	 * @return the corresponding currency.
	 */
	public Currency getCurrencyForCode(String code) {
		return currencies.get(code);
	}

	public Collection<Account> getAllAccounts() {
        Vector<Account> all = new Vector<Account>();
        for (Account a: getAccountCollection()) {
            all.add(a);
            all.addAll(a.getAllSubAccounts());
        }
        return all;
    }

    public Iterator<CapitalAccount> getCapitalAccountIterator() {
        return new Iterator<CapitalAccount>() {
        	Iterator<Account> iter = accounts.iterator();
        	CapitalAccount element;

			@Override
			public boolean hasNext() {
				while (iter.hasNext()) {
					Account account = iter.next();
					if (account instanceof CapitalAccount) {
						element = (CapitalAccount)account;
						return true;
					}
				}
				return false;
			}
			@Override
			public CapitalAccount next() {
				return element;
			}
			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
        };
    }

    public Iterator<IncomeExpenseAccount> getIncomeExpenseAccountIterator() {
        return new Iterator<IncomeExpenseAccount>() {
        	Iterator<Account> iter = accounts.iterator();
        	IncomeExpenseAccount element;

			@Override
			public boolean hasNext() {
				while (iter.hasNext()) {
					Account account = iter.next();
					if (account instanceof IncomeExpenseAccount) {
						element = (IncomeExpenseAccount)account;
						return true;
					}
				}
				return false;
			}
			@Override
			public IncomeExpenseAccount next() {
				return element;
			}
			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
        };
    }

    public ObjectCollection<Commodity> getCommodityCollection() {
    	return new ObjectCollection<Commodity>(commodities, this, SessionInfo.getCommoditiesAccessor());
    }

    public ObjectCollection<Account> getAccountCollection() {
    	return new ObjectCollection<Account>(accounts, this, SessionInfo.getAccountsAccessor());
    }

    public ObjectCollection<Transaction> getTransactionCollection() {
    	return new ObjectCollection<Transaction>(transactions, this, SessionInfo.getTransactionsAccessor());
    }

	/**
	 * Create a new account.  Accounts are abstract, so
	 * a property set derived from the account property
	 * set must be passed to this method.  When an account
	 * is created, various objects, such as the objects that manage collections within
	 * the account, must be created.  The implementation of
	 * these objects depends on the datastore and must be passed
	 * to the constructor, so the actual construction of the object
	 * is delegated to the collection object that will hold this
	 * new account.  (The implementation of the collection object
	 * is provided by the datastore plug-in, so this object knows
	 * how to create an object in a way that is appropriate for
	 * the datastore).
	 *
	 * The collection object will get the properties for the new
	 * object from the given interface.  Scalar properties are
	 * simply set.  References to other objects are likewise set.
	 * This means any referenced object must have been fetched by
	 * the datastore.
	 *
	 * @param accountPropertySet
	 * @param account
	 * @return
	 */
	public <A extends Account> A createAccount(ExtendablePropertySet<A> propertySet) {
		return getAccountCollection().createNewElement(propertySet);
	}

	/**
	 * Create a new commodity.  Commodities are abstract, so
	 * a property set derived from the commodity property
	 * set must be passed to this method.  When an commodity
	 * is created, various objects, such as the objects that manage collections within
	 * the commodity, must be created.  The implementation of
	 * these objects depends on the datastore and must be passed
	 * to the constructor, so the actual construction of the object
	 * is delegated to the collection object that will hold this
	 * new commodity.  (The implementation of the collection object
	 * is provided by the datastore plug-in, so this object knows
	 * how to create an object in a way that is appropriate for
	 * the datastore).
	 *
	 * The collection object will get the properties for the new
	 * object from the given interface.  Scalar properties are
	 * simply set.  References to other objects are likewise set.
	 * This means any referenced object must have been fetched by
	 * the datastore.
	 *
	 * @param commodityPropertySet
	 * @param commodity
	 * @return
	 */
	public <E extends Commodity> E createCommodity(ExtendablePropertySet<E> propertySet) {
		return getCommodityCollection().createNewElement(propertySet);
	}

	public Transaction createTransaction() {
		return getTransactionCollection().createNewElement(TransactionInfo.getPropertySet());
	}

    public void deleteCommodity(Commodity commodity) throws ReferenceViolationException {
   		getCommodityCollection().deleteElement(commodity);
    }

	/**
	 * Helper method to delete the given account.
	 * <P>
	 * Note that accounts may be sub-accounts. Only top level accounts are in
	 * this session's account list. Sub-accounts must be removed by removing
	 * from the list of sub-accounts of the parent account.
	 *
	 * @param account
	 *            account to be removed from this collection, must not be null
	 * @return true if the account was present, false if the account was not
	 *         present in the collection
	 */
    public void deleteAccount(Account account) throws ReferenceViolationException {
        Account parent = account.getParent();
        if (parent == null) {
        	getAccountCollection().deleteElement(account);
        } else if (parent instanceof IncomeExpenseAccount) {
        	// Pass the request on to the parent account.
    		((IncomeExpenseAccount)parent).getSubAccountCollection().deleteElement((IncomeExpenseAccount)account);
        } else {
        	// Pass the request on to the parent account.
    		((CapitalAccount)parent).getSubAccountCollection().deleteElement((CapitalAccount)account);
        }
    }

   	public void deleteTransaction(Transaction transaction) throws ReferenceViolationException {
   		getTransactionCollection().deleteElement(transaction);
    }

    /**
     * @author Faucheux
     * TODO: Faucheux - not the better algorythm!
     */
	public Account getAccountByFullName(String name) {
		for (Account a: getAllAccounts()) {
	        if (a.getFullAccountName().equals(name))
	            return a;
	    }
	    return null;
	}

    /**
     * @throws InvalidParameterException
     * @author Faucheux
     * TODO: Faucheux - not the better algorythm!
     */
	public Account getAccountByShortName(String name) throws SeveralAccountsFoundException, NoAccountFoundException{
	    Account foundAccount = null;
	    Iterator it = getAllAccounts().iterator();
	    while (it.hasNext()) {
	        Account a = (Account) it.next();
	        if (a.getName().equals(name)) {
	            if (foundAccount != null) {
	                throw new SeveralAccountsFoundException ();
	            } else {
	                foundAccount = a;
	            }
	        }
	    }
	    if (foundAccount == null) throw new NoAccountFoundException();
	    return foundAccount;
	}

    public class NoAccountFoundException extends Exception {
		private static final long serialVersionUID = -6022196945540827504L;
	}

	public class SeveralAccountsFoundException extends Exception {
		private static final long serialVersionUID = -6427097946645258873L;
	}

}
