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

import java.util.Collection;

import net.sf.jmoney.fields.IBlob;
import net.sf.jmoney.isolation.IModelObject;
import net.sf.jmoney.isolation.IScalarPropertyAccessor;
import net.sf.jmoney.isolation.SessionChangeAdapter;
import net.sf.jmoney.isolation.SessionChangeListener;
import net.sf.jmoney.isolation.TransactionManager;
import net.sf.jmoney.model2.Account;
import net.sf.jmoney.model2.Commodity;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.EntryInfo;
import net.sf.jmoney.model2.IDataManagerForAccounts;
import net.sf.jmoney.model2.ScalarPropertyAccessor;

import org.eclipse.core.databinding.observable.list.IObservableList;
import org.eclipse.core.databinding.observable.list.WritableList;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.IValueChangeListener;
import org.eclipse.core.databinding.observable.value.ValueChangeEvent;
import org.eclipse.core.databinding.observable.value.WritableValue;
import org.eclipse.core.runtime.Assert;

/**
 * Class representing a top level entry in the list.
 * <P>
 * Note that it is entries and not transactions that are listed. For example, if
 * a transaction has two entries in the account then that transaction will
 * appear twice in the list.
 * <P>
 * Note that this object knows about an entry in the context of a single data manager.
 * If the user is editing an existing row then there will be two instances of EntryData
 * involved, while if the user is editing a new row then ????? not sure if an EntryData
 * need exist with a null entry or not?????
 */
public class EntryData {
	/**
	 * The entry represented by this row.  This entry will never be null.
	 *  be null if the row
	 * represents a new entry that has never been committed to the datastore.
	 * Row controls will generally create a datastore transaction in which to
	 * edit this entry.  However, this entry will be the committed version of
	 * the entry.
	 */
	private final Entry entry;

	private final IDataManagerForAccounts dataManager;

	/**
	 * The balance before this entry is added in, so the balance shown to the
	 * user for this line is calculated by adding this balance property to the
	 * amount of this entry.
	 */
	private long balance;

	private int index;

	// bound to entry.getAmount()
	protected IObservableValue<Long> netAmount = new WritableValue<Long>();

	/*
	 * This listener updates all our writable values.
	 *
	 * The model keeps only a weak reference to this listener, removing it
	 * if no one else is referencing the listener.  We therefore must maintain
	 * a reference for as long as this object exists.
	 */
	// TODO: We may potentially have a large number of EntryData objects.  Most
	// will not be for visible entries.  Therefore it would be nice if this
	// listener were maintained by a custom IObservableValue that created this
	// listener only while it in turn has listeners.
	private SessionChangeListener modelNetAmountListener = new SessionChangeAdapter() {
		@Override
		public void objectChanged(IModelObject changedObject,
				IScalarPropertyAccessor changedProperty, Object oldValue,
				Object newValue) {
			if (changedObject == getEntry()
					&& changedProperty == EntryInfo.getAmountAccessor()) {
				Long newAmount = (Long)newValue;
				netAmount.setValue(newAmount);
			}
		}
	};

	/**
	 * @param entry
	 *            the entry to be edited, or null if a new entry is to be
	 *            created
	 * @param dataManager
	 *            the datastore manager into which the entry will be committed,
	 *            which must be the same as the datastore manager for the entry
	 *            parameter if the entry parameter is non-null
	 */
	public EntryData(Entry entry, IDataManagerForAccounts dataManager) {
		this.entry = entry;
		this.dataManager = dataManager;

		if (entry != null) {
			dataManager.addChangeListenerWeakly(modelNetAmountListener);

			netAmount.addValueChangeListener(new IValueChangeListener<Long>() {
				@Override
				public void handleValueChange(ValueChangeEvent<Long> event) {
					if (event.diff.getNewValue() == null) {
						getEntry().setAmount(0);
					} else {
						getEntry().setAmount(event.diff.getNewValue());
					}
				}
			});
		}
	}

	public IObservableValue<Long> netAmount() {
		return netAmount;
	}

	public long getNetAmount() {
		return netAmount.getValue() == null ? 0 : netAmount.getValue();
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

	/**
	 * 
	 * @return the entry represented by this row, which will never be null????
	 */
	public Entry getEntry() {
		return entry;
	}

	public IDataManagerForAccounts getBaseSessionManager() {
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
	private IObservableList<Entry> buildOtherEntriesList() {
		WritableList<Entry> otherEntries = new WritableList<Entry>();
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

		/*
		 * Copy all values that are numbers, flags, text, or references to accounts or commodities.
		 * We do not copy dates or statement numbers.
		 */
		Entry selectedEntryInTrans = transactionManager.getCopyInTransaction(selectedEntry);
		for (ScalarPropertyAccessor<?, ? super Entry> accessor : EntryInfo.getPropertySet().getScalarProperties3()) {
			copyValue(accessor, selectedEntryInTrans, newEntry);
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


			Entry origEntryInTransaction = transactionManager.getCopyInTransaction(origEntry);

			/*
			 * Copy all values that are numbers, flags, text, or references to accounts or commodities.
			 * We do not copy dates or statement numbers.
			 */
			for (ScalarPropertyAccessor<?, ? super Entry> accessor : EntryInfo.getPropertySet().getScalarProperties3()) {
				copyValue(accessor, origEntryInTransaction, thisEntry);
			}

			thisEntry = null;
		}
	}

	protected <V> void copyValue(ScalarPropertyAccessor<V, ? super Entry> accessor, Entry selectedEntry, Entry newEntry) {
		V value = accessor.getValue(selectedEntry);

		/*
		 * Copy all values that are numbers, flags, text, or references to accounts or commodities.
		 * We do not copy dates or statement numbers.
		 */
		// TODO is there a better way of deciding what to copy?  This code has never caused a problem
		// but it just seems a bit of a hack.
		if (value instanceof Long
				|| value instanceof Boolean
				|| value instanceof Integer
				|| value instanceof IBlob
				|| value instanceof String
				|| value instanceof Account
				|| value instanceof Commodity
				) {
			accessor.setValue(newEntry, value);
		}
	}

}
