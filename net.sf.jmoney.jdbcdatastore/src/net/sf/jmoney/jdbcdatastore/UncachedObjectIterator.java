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

package net.sf.jmoney.jdbcdatastore;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;

import net.sf.jmoney.isolation.IExtendablePropertySet;
import net.sf.jmoney.isolation.IModelObject;
import net.sf.jmoney.jdbcdatastore.SessionManager.DatabaseListKey;

/**
 * Class that iterates over a set of objects when the objects
 * are not cached and thus must be read from the database.
 * <P>
 * Objects of this class are constructed from a result set
 * that contains the properties for a set of objects to be iterated.
 * This class implements the Iterator interface and will return
 * the set of ExtendableObject objects.
 * <P>
 * Derivable property sets are not supported by this class; the
 * property set must be a final property set.
 * 
 * This method is also used for sets of objects that are not containing
 * lists.  For example the list of entries in an account.
 * 
 * @author Nigel Westbury
 */
public class UncachedObjectIterator<E extends IModelObject> implements Iterator<E> {
	private Statement stmt;
	private ResultSet resultSet;
	private IExtendablePropertySet<E> propertySet;
	private DatabaseListKey<? super E,?> listKey;
	private SessionManager sessionManager;
	private boolean isAnother;

	/**
	 * It is the responsibility of this iterator to close both the result set
	 * and the statement when the iterator is done. (Note that if the
	 * constructor throws an exception, the constructor will immediately close
	 * the statement).
	 * <P>
	 * The statement and result set are closed when the iteration has completed
	 * (when the last object has been obtained from the next() method). However,
	 * there is a problem in that if the user of this iterator does not do a
	 * complete iteration of all elements then the result set and statement
	 * would never be closed. We guard against this by closing these when this
	 * iterator is garbage collected.
	 * 
	 * @param stmt
	 *            a prepared statement which, when executed, returns the result
	 *            set to be used for this iteration
	 * @param propertySet
	 *            The property set for the objects in this list, which must be
	 *            final (cannot be a derivable property set)
	 * @param listKey
	 *            The caller may pass a null list key. In that case, a new list
	 *            key will be generated for each object in the list. If all the
	 *            objects in the list have the same parent then pass this
	 *            parent. If the objects in the list have different parents then
	 *            pass null.
	 * @param sessionManager
	 * @throws SQLException
	 */
	public UncachedObjectIterator(ResultSet resultSet, IExtendablePropertySet<E> propertySet, DatabaseListKey<? super E,?> listKey, SessionManager sessionManager) {
		this.propertySet = propertySet;
		this.listKey = listKey;
		this.sessionManager = sessionManager;
		
		// Position on first row.
		try {
		this.stmt = resultSet.getStatement();
		this.resultSet = resultSet;
		isAnother = resultSet.next();
		} catch (SQLException e) {
			// Should really attempt to close, but this is not a likely exception.
			throw new RuntimeException("SQL failure");
		}
	}
	
	@Override
	public boolean hasNext() {
		return isAnother;
	}
	
	@Override
	public E next() {
		try {
			/*
			 * If parentKey is null then objects in this collection have
			 * different parents. In that case we must build a parent key for
			 * each object. No additional database access is necessary to do
			 * this because the foreign keys to the parent rows will be in the
			 * result set.
			 */
			DatabaseListKey<? super E,?> listKey2;
			if (listKey == null) {
				listKey2 = sessionManager.buildParentKey(resultSet, propertySet);
			} else {
				listKey2 = listKey;
			}
			
			ObjectKey key = new ObjectKey(resultSet, propertySet, listKey2, sessionManager);
			E extendableObject = propertySet.getImplementationClass().cast(key.getObject());
			
			// Rowset must be left positioned on the following row.
			isAnother = resultSet.next();
			
			/*
			 * We have reached the end, so close the statement and result
			 * set now.  (It is the responsibility of this iterator to do
			 * so).  Because the finalizer also closes these (in case the
			 * caller does not finish the iteration), we set these to null
			 * after closing them to prevent trying to close twice.
			 */
			if (!isAnother) {
				resultSet.close();
				resultSet = null;
				stmt.close();
				stmt = null;
			}
			
			return extendableObject;
		} catch (SQLException e) {
			e.printStackTrace();
			throw new RuntimeException("internal error");
		}
	}
	
	@Override
	public void remove() {
		throw new RuntimeException("unimplemented method");
	}
	
	@Override
	public void finalize() {
		try {
			if (stmt != null) {
				if (resultSet != null) {
					resultSet.close();
				}
				stmt.close();
			}
		} catch (SQLException e) {
			// Don't worry if the close fails.
		}
	}
}