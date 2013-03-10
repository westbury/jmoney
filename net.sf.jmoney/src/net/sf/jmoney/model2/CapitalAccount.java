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

import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import net.sf.jmoney.isolation.IListManager;
import net.sf.jmoney.isolation.IObjectKey;
import net.sf.jmoney.isolation.IValues;
import net.sf.jmoney.isolation.ListKey;
import net.sf.jmoney.isolation.ObjectCollection;
import net.sf.jmoney.isolation.ReferenceViolationException;
import net.sf.jmoney.resources.Messages;

/**
 * The data model for an account.
 */
public abstract class CapitalAccount extends Account {

	protected IListManager<CapitalAccount> subAccounts;

	protected String abbreviation = null;

	protected String comment = null;

	/**
	 * The full constructor for a CapitalAccount object.  This constructor is called
	 * only be the datastore when loading data from the datastore.  The properties
	 * passed to this constructor must be valid because datastores should only pass back
	 * values that were previously saved from a CapitalAccount object.  So, for example,
	 * we can be sure that a non-null name and currency are passed to this constructor.
	 *
	 * @param name the name of the account
	 */
	public CapitalAccount(
			IObjectKey objectKey,
			ListKey parent,
			String name,
			IListManager<CapitalAccount> subAccounts,
			String abbreviation,
			String comment,
			IValues<? extends CapitalAccount> extensionValues) {
		super(objectKey, parent, name, extensionValues);

		this.subAccounts = subAccounts;
        this.abbreviation = abbreviation;
        this.comment = comment;
	}

	/**
	 * The default constructor for a CapitalAccount object.  This constructor is called
	 * when a new CapitalAccount object is created.  The properties are set to default
	 * values.  The list properties are set to empty lists.  The parameter list for this
	 * constructor is the same as the full constructor except that there are no parameters
	 * for the scalar properties.
	 */
	public CapitalAccount(
			IObjectKey objectKey,
			ListKey parent) {
		super(objectKey, parent);

		this.name = Messages.CapitalAccount_Name;

		this.subAccounts = objectKey.constructListManager(CapitalAccountInfo.getSubAccountAccessor());
        this.abbreviation = null;
        this.comment = null;
	}

    @Override
	protected String getExtendablePropertySetId() {
		return "net.sf.jmoney.capitalAccount"; //$NON-NLS-1$
	}

	/**
	 * @return the abbreviation of this account.
	 */
	public String getAbbreviation() {
		return abbreviation;
	}

	/**
	 * @return the comment of this account.
	 */
	public String getComment() {
		return comment;
	}

    @Override
	public ObjectCollection<CapitalAccount> getSubAccountCollection() {
		return new ObjectCollection<CapitalAccount>(subAccounts, this, CapitalAccountInfo.getSubAccountAccessor());
	}

	/**
	 * Get the entries in this account sorted according to the given
	 * sort specification.  If the datastore plug-in has implemented
	 * the IEntryQueries interface then pass the request on to the
	 * datastore through the method of the same name in the IEntryQueries
	 * interface.  If the IEntryQueries interface has not been implemented
	 * by the datastore then evaluate ourselves.
	 * <P>
	 * @return A collection containing the entries of this account.
	 * 				The entries are sorted using the given property and
	 * 				given sort order.  The collection is a read-only
	 * 				collection.
	 */
	public Collection<Entry> getSortedEntries(final ScalarPropertyAccessor<?,?> sortProperty, final Comparator<Entry> entryComparator, boolean descending) {
		IEntryQueries queries = (IEntryQueries)getSession().getAdapter(IEntryQueries.class);
    	if (queries != null) {
    		return queries.getSortedEntries(this, sortProperty, descending);
    	} else {
    		// IEntryQueries has not been implemented in the datastore.
    		// We must therefore provide our own implementation.

    		List<Entry> sortedEntries = new LinkedList<Entry>(getEntries());

    		Comparator<Entry> correctlyOrderedComparator;
    		if (descending) {
    			correctlyOrderedComparator = new Comparator<Entry>() {
    				@Override
					public int compare(Entry entry1, Entry entry2) {
    					return entryComparator.compare(entry2, entry1);
    				}
    			};
    		} else {
    			correctlyOrderedComparator = entryComparator;
    		}

    		Collections.sort(sortedEntries, correctlyOrderedComparator);

    		return sortedEntries;
    	}
	}

	/**
	 * @param anAbbrevation the abbrevation of this account.
	 */

	public void setAbbreviation(String anAbbreviation) {
        String oldAbbreviation = this.abbreviation;
        this.abbreviation = anAbbreviation;

		// Notify the change manager.
        processPropertyChange(CapitalAccountInfo.getAbbreviationAccessor(), oldAbbreviation, anAbbreviation);
	}

	/**
	 * @param aComment the comment of this account.
	 */

	public void setComment(String aComment) {
        String oldComment = this.comment;
        this.comment = aComment;

		// Notify the change manager.
        processPropertyChange(CapitalAccountInfo.getCommentAccessor(), oldComment, aComment);
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
	 * Create a sub-account of this account.  This method is
	 * identical to calling
	 * <code>getSubAccountCollection().createNewElement(propertySet)</code>.
	 *
	 * @param propertySet a property set derived (directly or
	 * 			indirectly) from the CapitalAccount property set.
	 * 			This property set must not be derivable and is
	 * 			the property set for the type of capital account
	 * 			to be created.
	 */
	public <A extends CapitalAccount> A createSubAccount(ExtendablePropertySet<A> propertySet) {
		return getSubAccountCollection().createNewElement(propertySet);
	}

	/**
	 * Delete a sub-account of this account.  This method is
	 * identical to calling
	 * <code>getSubAccountCollection().deleteElement(subAccount)</code>.
	 */
	void deleteSubAccount(CapitalAccount subAccount) throws ReferenceViolationException {
		getSubAccountCollection().deleteElement(subAccount);
	}

	/**
	 * @param date
	 * @param date2
	 * @param includeSubAccounts
	 * @return
	 */
	public long [] getEntryTotalsByMonth(int startYear, int startMonth, int numberOfMonths, boolean includeSubAccounts) {
		IEntryQueries queries = (IEntryQueries)getSession().getAdapter(IEntryQueries.class);
    	if (queries != null) {
    		return queries.getEntryTotalsByMonth(this, startYear, startMonth, numberOfMonths, includeSubAccounts);
    	} else {
    		// IEntryQueries has not been implemented in the datastore.
    		// We must therefore provide our own implementation.

    		Vector<Entry> entriesList = new Vector<Entry>();
    		entriesList.addAll(getEntries());
    		if (includeSubAccounts) {
    			addEntriesFromSubAccounts(this, entriesList);
    		}

            Collections.sort(entriesList, new Comparator<Entry>() {
                @Override
				public int compare(Entry entry1, Entry entry2) {
                    return entry1.getTransaction().getDate().compareTo(
                            entry2.getTransaction().getDate());
                }
            });


    		long [] totals = new long[numberOfMonths];

    		Calendar calendar = Calendar.getInstance();



    		// calculate the sum for each month
    		int year = startYear;
    		int month = startMonth;
            for (int i=0; i<numberOfMonths; i++) {
    			calendar.clear();
    			calendar.setLenient(false);
    			calendar.set(year, month - 1, 1, 0, 0, 0);
    			Date startOfMonth = calendar.getTime();
            	// Date startOfMonth = new Date(year - 1900, month, 1);

    			month++;
            	if (month == 13) {
            		year++;
            		month = 1;
            	}

    			calendar.clear();
    			calendar.setLenient(false);
    			calendar.set(year, month - 1, 1, 0, 0, 0);
    			Date endOfMonth = calendar.getTime();
            	// Date endOfMonth = new Date(year - 1900, month, 1);

            	int total = 0;
            	for (Entry entry: entriesList) {
            		if (entry.getTransaction().getDate().compareTo(startOfMonth) >= 0
            		 && entry.getTransaction().getDate().compareTo(endOfMonth) < 0) {
            			total += entry.getAmount();
            		}
            	}
            	totals[i] = total;
            }

            return totals;
    	}
	}

	public void addEntriesFromSubAccounts(CapitalAccount a, Collection<Entry> entriesList) {
		for (CapitalAccount subAccount: a.getSubAccountCollection()) {
			entriesList.addAll(subAccount.getEntries());
			addEntriesFromSubAccounts(subAccount, entriesList);
		}
	}

}
