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

import net.sf.jmoney.isolation.IListManager;
import net.sf.jmoney.isolation.IObjectKey;
import net.sf.jmoney.isolation.IValues;
import net.sf.jmoney.isolation.ListKey;
import net.sf.jmoney.isolation.ObjectCollection;
import net.sf.jmoney.isolation.ReferenceViolationException;


/**
 *
 * @author  Nigel
 */
public class Transaction extends ExtendableObject {

    protected Date date = null;

	protected String type = null;

    protected IListManager<Entry> entries;

	public Transaction(
			IObjectKey objectKey,
			ListKey parentKey,
    		IListManager<Entry> entries,
    		Date date,
    		String type,
			IValues<Transaction> extensionValues) {
		super(objectKey, parentKey, extensionValues);

		this.entries = entries;
		this.date = date;
		this.type = type;
	}

	public Transaction(
			IObjectKey objectKey,
			ListKey parentKey) {
		super(objectKey, parentKey);

		this.entries = objectKey.constructListManager(TransactionInfo.getEntriesAccessor());

		// TODO: Check that this sets the date to the current date.
		this.date = new Date();
		this.type = null;
	}

    @Override
	protected String getExtendablePropertySetId() {
		return "net.sf.jmoney.transaction"; //$NON-NLS-1$
	}

    /**
     * Returns the date.
     */
    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        Date oldDate = this.date;
        this.date = date;

		// Notify the change manager.
		processPropertyChange(TransactionInfo.getDateAccessor(), oldDate, date);
    }

	/**
	 * Returns the entry type.
	 */
	public String getType() {
		return type;
	}

	/**
	 * See @ for a description of entry types.
	 */
	public void setType(String type) {
		String oldType = this.type;
		this.type = (type != null && type.length() == 0) ? null : type;

		// Notify the change manager.
		processPropertyChange(TransactionInfo.getTypeAccessor(), oldType, type);
	}

    public Entry createEntry() {
    	return new EntryCollection(entries, this, TransactionInfo.getEntriesAccessor()).createEntry();
	}

    public EntryCollection getEntryCollection() {
    	return new EntryCollection(entries, this, TransactionInfo.getEntriesAccessor());
    }

    public void deleteEntry(Entry entry) {
    	new EntryCollection(entries, this, TransactionInfo.getEntriesAccessor()).deleteEntry(entry);
    }

    // Some helper methods:

    public boolean hasTwoEntries() {
        return entries.size() == 2;
    }

    public boolean hasMoreThanTwoEntries() {
        return entries.size() > 2;
    }

    /**
     * Given an entry in the transaction, return the other entry.
     * <P>
     * If there are more than two entries in the transaction then null is returned.
     * If there is only one entry in the transaction then an
     * exception is throw.  If the given entry is not in the transaction then
     * an exception will be thrown.
     *
     * @param thisEntry an entry in the transaction
     * @return the other entry in the transaction or null if more than one other entry
     * 				is in the transaction
     */
    public Entry getOther(Entry thisEntry) {
    	boolean thisEntryFound = false;
    	Entry anotherEntry = null;
        for (Entry entry: entries) {
        	if (!entry.equals(thisEntry)) {
                if (anotherEntry != null) {
                	// There is more than one entry other than the given entry
                	return null;
                }
                anotherEntry = entry;
        	} else {
        		thisEntryFound = true;
        	}
        }

        if (!thisEntryFound) {
        	throw new RuntimeException("Double entry error"); //$NON-NLS-1$
        }

        if (anotherEntry == null) {
        	throw new RuntimeException("Double entry error"); //$NON-NLS-1$
        }

        return anotherEntry;
    }

    /**
     * This class adds a little tighter typing to ObjectCollection,
     * but it is barely worth while having this class.
	 */
	public class EntryCollection extends ObjectCollection<Entry> {
		EntryCollection(IListManager<Entry> listManager, Transaction parent, ListPropertyAccessor<Entry,Transaction> listPropertyAccessor) {
			super(listManager, parent, listPropertyAccessor);
		}

	    /**
	     * Identical to <code>remove</remove> but tighter typing
		 */
		public void deleteEntry(Entry entry) {
	    	try {
				deleteElement(entry);
			} catch (ReferenceViolationException e) {
				/*
				 * There are no known properties that reference entries, so this
				 * exception cannot happen.
				 */
				throw new RuntimeException("Internal error", e);
			}
		}

	    /**
	     * Identical to <code>createNewElement</remove> but tighter typing
		 */
		public Entry createEntry() {
			return createNewElement(EntryInfo.getPropertySet());
		}
	}
}
