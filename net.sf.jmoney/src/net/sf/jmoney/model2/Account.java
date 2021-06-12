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

import java.util.Collection;
import java.util.Collections;
import java.util.Vector;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.SectionPart;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.handlers.IHandlerService;

import net.sf.jmoney.JMoneyPlugin;
import net.sf.jmoney.isolation.IModelObject;
import net.sf.jmoney.isolation.IObjectKey;
import net.sf.jmoney.isolation.IValues;
import net.sf.jmoney.isolation.ListKey;
import net.sf.jmoney.isolation.ObjectCollection;
import net.sf.jmoney.pages.entries.EntriesSection;

/**
 * An implementation of the Account interface
 */
public abstract class Account extends ExtendableObject implements Comparable<Account> {

	protected String name;

	protected Account(
			IObjectKey objectKey,
			ListKey parentKey,
			String name,
			IValues<? extends Account> extensionValues) {
		super(objectKey, parentKey, extensionValues);
		this.name = name;
	}

	protected Account(
			IObjectKey objectKey,
			ListKey parentKey) {
		super(objectKey, parentKey);
		this.name = null;
	}

	/**
	 * @return the name of this account.
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param aName the name of this account.
	 */

	public void setName(String newName) {
		String oldName = name;
		name = newName;

		// Notify the change manager.
		processPropertyChange(AccountInfo.getNameAccessor(), oldName, newName);
	}

	public String getFullAccountName() {
		return getName();
	}

	public Account getParent() {
		IModelObject parent = parentKey.getParentKey().getObject();
		if (parent instanceof Account) {
			return (Account)parent;
		} else {
			return null;
		}
	}

	/**
	 * Returns the commodity that the amount in an entry
	 * represents.  If the account for the entry is an account
	 * that can store only one commodity (usually a currency)
	 * then the commodity is a property of the account.  If,
	 * however, the account can hold multiple commodities (such
	 * as a stock account) then information from the entry is
	 * required in order to get the commodity involved.
	 *
	 * @return Commodity for the given entry
	 */
	public abstract Commodity getCommodity(Entry entry);

	public abstract ObjectCollection<? extends Account> getSubAccountCollection();

	public Collection<? extends Account> getAllSubAccounts() {
	    Collection<Account> all = new Vector<Account>();
	    for (Account a: getSubAccountCollection()) {
	        all.add(a);
	        all.addAll(a.getAllSubAccounts());
	    }
		return all;
	}

	/**
	 * Get the entries in the account.
	 *
	 * @return A read-only collection with elements of
	 * 				type <code>Entry</code>
	 */
	public Collection<Entry> getEntries() {
		Collection<Entry> accountEntries = getDataManager().getEntries(this);
		return Collections.unmodifiableCollection(accountEntries);
	}

	/**
	 * @return true if there are any entries in this account,
	 * 			false if no entries are in this account
	 */
	public boolean hasEntries() {
		return getDataManager().hasEntries(this);
	}

    /**
	 * This method is used for debugging purposes only.
	 */
	@Override
	public String toString() {
		return getName();
	}

	@Override
	public int compareTo(Account other) {
		return getName().compareTo(other.getName());
	}

    public int getLevel () {
        int level;
        if (getParent() == null)
            level = 0;
        else
            level = getParent().getLevel() + 1;
        if (JMoneyPlugin.DEBUG) System.out.println("Level from " + this.name + ", child of " + getParent() +" is " + level); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        return level;
    }

    /**
     * Sometimes an account implementation may want to control the entries list in child accounts.  For example
     * a stock account may want to show stock transaction types in cash accounts that are sub-accounts of the
     * stock account.
     * <P>
     * @return the entries list implementation to use for the given child account, or null if the usual implementation
     * 		is to be used for the child account 
     */
	public SectionPart createEntriesSection(Composite parent, Account account, FormToolkit toolkit,
			IHandlerService handlerService) {
		return null;
	}
}
