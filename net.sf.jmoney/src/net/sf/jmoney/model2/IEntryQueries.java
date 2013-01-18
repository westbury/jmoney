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
import java.util.Date;

/**
 * Interface containing methods that execute queries against the datastore.
 * <P>
 * Plug-ins that implement a datastore may optionally implement this interface.
 * Consumers obtain this interface through the getAdapter method in the session
 * object.
 * <P>
 * The queries in this interface are all require access to the list of entries
 * and therefore it is a good idea for datastore implementations to implement
 * this interface if the entries are not cached in memory.
 * <P>
 * @author Nigel Westbury
 */
public interface IEntryQueries {

	/**
	 * Sum the amount for all entries in the given range
	 * (inclusive of the 'from' and 'to' dates).
	 * 
	 * @param account
	 * @param fromDate
	 * @param toDate
	 * @return
	 */
	long sumOfAmounts(CurrencyAccount account, Date fromDate, Date toDate);

	/**
	 * Return a sorted collection of Entry objects.
	 * The returned Collection is be implemented by the datastore.
	 * The implementation is therefore optimized by the datastore.
	 * For example, if the datastore is a JDBC database then the size()
	 * method will get the count from the database by executing a command
	 * of the form "select count(*) from entries where account = ?".
	 * The entries themselves will not be read from the database unless
	 * the Collection is iterated or the toArray method is called.
	 * If the datastore is backed by a database that supports an
	 * ORDER BY clause then the sorting will generally be passed on
	 * the the database.
	 * 
	 * @param account The account whose entries are to be returned.
	 * @param sortProperty The property used for the sort.  This property
	 * 			may be a property in the Entry object, a property in
	 * 			the Transaction object, or a property in the Account object.
	 * @param descending if true, sort in descending order
	 * @return
	 */
	Collection<Entry> getSortedEntries(CapitalAccount account, PropertyAccessor sortProperty, boolean descending);

	/**
	 * Return the total amount of entries in the account in each month.
	 * The range of months are given by the startYear, startMonth, and
	 * numberOfMonths parameters.
	 * 
	 * @param startYear
	 * @param startMonth
	 * @param numberOfMonths
	 * @param includeSubAccounts
	 * @return An array with a size equal to the numberOfMonths
	 */
	long[] getEntryTotalsByMonth(CapitalAccount account, int startYear, int startMonth, int numberOfMonths, boolean includeSubAccounts);
}
