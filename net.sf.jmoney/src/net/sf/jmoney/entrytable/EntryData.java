/*
 *
 *  JMoney - A Personal Finance Manager
 *  Copyright (c) 2007 Nigel Westbury <westbury@users.sf.net>
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

package net.sf.jmoney.entrytable;

import java.util.ArrayList;
import java.util.Collection;

import net.sf.jmoney.isolation.DataManager;
import net.sf.jmoney.isolation.TransactionManager;
import net.sf.jmoney.model2.Account;
import net.sf.jmoney.model2.Commodity;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.EntryInfo;
import net.sf.jmoney.model2.ExtendableObject;
import net.sf.jmoney.model2.ScalarPropertyAccessor;

import org.eclipse.core.runtime.Assert;

/**
 * Class representing a top level entry in the list.
 * <P>
 * Note that it is entries and not transactions that are listed. For example, if
 * a transaction has two entries in the account then that transaction will
 * appear twice in the list.
 */
public class EntryData {
	/**
	 * The entry represented by this row.  This entry will be null if the row
	 * represents a new entry that has never been committed to the datastore.
	 * Row controls will generally create a datastore transaction in which to
	 * edit this entry.  However, this entry will be the committed version of
	 * the entry.
	 */
	private final Entry entry;
	
	private final DataManager dataManager;

	/**
	 * The balance before this entry is added in, so the balance shown to the
	 * user for this line is calculated by adding this balance property to the
	 * amount of this entry.
	 */
	private long balance;
	
	private int index;

	/**
	 * @param entry
	 *            the entry to be edited, or null if a new entry is to be
	 *            created
	 * @param dataManager
	 *            the datastore manager into which the entry will be committed,
	 *            which must be the same as the datastore manager for the entry
	 *            parameter if the entry parameter is non-null
	 */
	public EntryData(Entry entry, DataManager dataManager) {
		this.entry = entry;
		this.dataManager = dataManager;
	}

	public void setIndex(int index) {
		this.index = index;
	}

	public int getIndex() {
		return index;
	}

	/**
	 * A transaction with split entries is a transaction that
	 * has entries in three or more accounts (where each account
	 * may be either a capital account or an income and
	 * expense category).
	 */
	public boolean hasSplitEntries() {
		return entry.getTransaction().getEntryCollection().size() >= 3;
	}

	/**
	 * @return
	 */
	public Entry getOtherEntry() {
		Assert.isTrue(!hasSplitEntries());
		return buildOtherEntriesList().get(0);
	}

	/**
	 * @return
	 */
	public Collection<Entry> getSplitEntries() {
		return buildOtherEntriesList();
	}

	/**
	 * @return the balance before this entry is added in, so
	 * 		the balance show to the user for this line is calculated
	 * 		by adding this balance property to the amount of this
	 * 		entry
	 */
	public long getBalance() {
		return balance;
	}

	/**
	 * @param balance the balance before this entry is added in, so
	 * 		the balance show to the user for this line is calculated
	 * 		by adding this balance property to the amount of this
	 * 		entry
	 */
	public void setBalance(long balance) {
		this.balance = balance;
	}

	public Entry getEntry() {
		return entry;
	}

	public DataManager getBaseSessionManager() {
		return dataManager;
	}

	/**
	 * Database reads may be necessary when getting the other entries.
	 * Furthermore, these are not needed unless an entry becomes visible. These
	 * are therefore fetched only when needed (not in the constructor of this
	 * object).
	 * 
	 * We must be careful with this cached list because it is not kept up to date.
	 */
	private ArrayList<Entry> buildOtherEntriesList() {
		ArrayList<Entry> otherEntries = new ArrayList<Entry>();
			for (Entry entry2 : entry.getTransaction().getEntryCollection()) {
				if (!entry2.equals(entry)) {
					otherEntries.add(entry2);
				}
			}
			return otherEntries;
	}

	public void copyFrom(EntryData sourceEntryData) {
		Entry selectedEntry = sourceEntryData.getEntry();
		
		Entry newEntry = getEntry();
		TransactionManager transactionManager = (TransactionManager)newEntry.getDataManager();
		
//		newEntry.setMemo(selectedEntry.getMemo());
//		newEntry.setAmount(selectedEntry.getAmount());

		/*
		 * Copy all values that are numbers, flags, text, or references to accounts or commodities.
		 * We do not copy dates or statement numbers.
		 */
		for (ScalarPropertyAccessor accessor : EntryInfo.getPropertySet().getScalarProperties3()) {
			Object value = selectedEntry.getPropertyValue(accessor);
			if (value instanceof Integer
					|| value instanceof Long
					|| value instanceof Boolean
					|| value instanceof String) {
				newEntry.setPropertyValue(accessor, value);
			}
			if (value instanceof Commodity
					|| value instanceof Account) {
				newEntry.setPropertyValue(accessor, transactionManager.getCopyInTransaction((ExtendableObject)value));
			}
		}
		
		/*
		 * In the bank account entries, the new entry row will always have a second entry created.
		 * In other entry types such as a stock entry, the new entry row will have only one row.
		 */
		Entry thisEntry = getSplitEntries().isEmpty()
		? null : getOtherEntry();

		for (Entry origEntry: sourceEntryData.getSplitEntries()) {
			if (thisEntry == null) {
				thisEntry = getEntry().getTransaction().createEntry();
			}
//			thisEntry.setAccount(transactionManager.getCopyInTransaction(origEntry.getAccount()));
//			thisEntry.setMemo(origEntry.getMemo());
//			thisEntry.setAmount(origEntry.getAmount());
			
			/*
			 * Copy all values that are numbers, flags, text, or references to accounts or commodities.
			 * We do not copy dates or statement numbers.
			 */
			for (ScalarPropertyAccessor accessor : EntryInfo.getPropertySet().getScalarProperties3()) {
				Object value = origEntry.getPropertyValue(accessor);
				if (value instanceof Integer
						|| value instanceof Long
						|| value instanceof Boolean
						|| value instanceof String) {
					thisEntry.setPropertyValue(accessor, value);
				}
				if (value instanceof Commodity
						|| value instanceof Account) {
					thisEntry.setPropertyValue(accessor, transactionManager.getCopyInTransaction((ExtendableObject)value));
				}
			}
			
			thisEntry = null;
		}
	}
}
