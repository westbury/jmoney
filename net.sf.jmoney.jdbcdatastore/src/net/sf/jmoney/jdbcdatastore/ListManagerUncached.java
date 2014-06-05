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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Iterator;

import net.sf.jmoney.isolation.IExtendablePropertySet;
import net.sf.jmoney.isolation.IListManager;
import net.sf.jmoney.isolation.IModelObject;
import net.sf.jmoney.isolation.IValues;
import net.sf.jmoney.isolation.ReferenceViolationException;
import net.sf.jmoney.jdbcdatastore.SessionManager.DatabaseListKey;
import net.sf.jmoney.model2.ListPropertyAccessor;
import net.sf.jmoney.model2.SessionInfo;

/**
 * Every datastore implementation must provide an implementation
 * of the IListManager interface.  This implementation supplies
 * an implementation that executes each method by submitting
 * an SQL statement to the database.
 *
 * @author Nigel Westbury
 */
public class ListManagerUncached<E extends IModelObject, S extends IModelObject> implements IListManager<E> {
	SessionManager sessionManager;
	DatabaseListKey<E,S> listKey;
	
	public ListManagerUncached(SessionManager sessionManager, DatabaseListKey<E,S> listKey) {
		this.sessionManager = sessionManager;
		this.listKey = listKey;
	}
	
	@Override
	public <F extends E> F createNewElement(IExtendablePropertySet<F> propertySet) {
 		/*
		 * First build the in-memory object. Even though the object is not
		 * cached in the parent list property, the object must be constructed to
		 * get the default values to be written to the database and the object
		 * must be constructed so it can be returned to the caller.
		 */
		ObjectKey objectKey = new ObjectKey(sessionManager);
		
		/*
		 * Constructing the object means constructing the object key. Both
		 * contain a reference to the other, so they have the same lifetime.
		 */
		// TODO: remove constructWithCachedList parameter
		F extendableObject = sessionManager.constructExtendableObject(propertySet, objectKey, sessionManager.constructListKey(listKey));
		objectKey.setObject(extendableObject);
		
		// Insert the new object into the tables.
		
		int rowId = sessionManager.insertIntoDatabase(propertySet, extendableObject, listKey);
		objectKey.setRowId(rowId);
		
		return extendableObject;
	}
	
	@Override
	public <F extends E> F createNewElement(IExtendablePropertySet<F> propertySet, IValues<F> values) {
 		/*
		 * First build the in-memory object. Even though the object is not
		 * cached in the parent list property, the object must be constructed to
		 * get the default values to be written to the database and the object
		 * must be constructed so it can be returned to the caller.
		 */
		ObjectKey objectKey = new ObjectKey(sessionManager);
		
		/*
		 * Constructing the object means constructing the object key. Both
		 * contain a reference to the other, so they have the same lifetime.
		 */
		F extendableObject = sessionManager.constructExtendableObject(propertySet, objectKey, listKey, values);
		objectKey.setObject(extendableObject);
		
		// Insert the new object into the tables.
		
		int rowId = sessionManager.insertIntoDatabase(propertySet, extendableObject, listKey);
		objectKey.setRowId(rowId);
		
		return extendableObject;
	}

	@Override
	public void deleteElement(E extendableObject) throws ReferenceViolationException {
		// Delete this object from the database.
		IDatabaseRowKey key = (IDatabaseRowKey)extendableObject.getObjectKey();
		sessionManager.deleteFromDatabase(key);
	}

	@Override
	public <F extends E> void moveElement(F extendableObject, IListManager<? super F> originalListManager) {
		sessionManager.reparentInDatabase(extendableObject, listKey);
	}

	@Override
	public int size() {
		try {
			ListPropertyAccessor<?,?> listPropertyAccessor2 = (ListPropertyAccessor<?,?>)listKey.listPropertyAccessor;
			
			String tableName = listPropertyAccessor2.getElementPropertySet().getId().replace('.', '_');

			
			/* If the containing list property is a property in one of the three
			 * lists in the session object
			 * then, as an optimization, there is no parent column.
			 */
			String sql = "SELECT COUNT(*) FROM " + tableName;
			if (listPropertyAccessor2.getPropertySet() != SessionInfo.getPropertySet()) {
				String columnName = listPropertyAccessor2.getName().replace('.', '_');
				sql += " WHERE \"" + columnName + "\" = ?";
			}
			
			System.out.println(sql);
			PreparedStatement stmt = sessionManager.getConnection().prepareStatement(sql);
			try {
				// Set parent id (unless it's the session in which case it's optimized out)
				if (listPropertyAccessor2.getPropertySet() != SessionInfo.getPropertySet()) {
					stmt.setInt(1, listKey.parentKey.getRowId());
				}
				
				ResultSet resultSet = stmt.executeQuery();
				resultSet.next();
				int size = resultSet.getInt(1);
				resultSet.close();
				return size;
			} finally {
				stmt.close();
			}
		} catch (SQLException e) {
			e.printStackTrace();
			throw new RuntimeException("internal error");
		}
	}
	
	@Override
	public boolean isEmpty() {
		throw new RuntimeException("method not implemented");
	}

	@Override
	public boolean contains(Object o) {
		throw new RuntimeException("method not implemented");
	}

	@Override
	public Iterator<E> iterator() {
		/*
		 * We execute a SQL statement and pass the result set to an
		 * UncachedObjectIterator object which will return the entries in the
		 * result set. However, we must create a new statement because the
		 * iterator is being returned from this method call.
		 * 
		 * This class only supports lists where the element type is a final
		 * property set. Therefore we know the exact type of every element in
		 * the list before we execute any query. This saves us from having to
		 * iterate over the final property sets (like the ListManagerCached
		 * object has to).
		 * 
		 * The UnchachedObjectIterator is responsible for closing the result set
		 * and the associated statement.
		 */		
		ResultSet rs = sessionManager.runWithReconnect(new IRunnableSql<ResultSet>() {
			@Override
			public ResultSet execute(Connection connection) throws SQLException {
				// Although the connection is passed, it is not really necessary because it
				// is taken from the session manager, and that would be the same connection.
				PreparedStatement stmt = sessionManager.executeListQuery(listKey, listKey.listPropertyAccessor.getElementPropertySet());
				return stmt.executeQuery();
			}
		});

		return new UncachedObjectIterator<E>(rs, listKey.listPropertyAccessor.getElementPropertySet(), listKey, sessionManager);
	}

	@Override
	public Object[] toArray() {
		throw new RuntimeException("method not implemented");
	}

	@Override
	public <T> T[] toArray(T[] arg0) {
		throw new RuntimeException("method not implemented");
	}

	@Override
	public boolean add(E extendableObject) {
		/*
		 * This list is not cached so there is nothing to do. The object has
		 * already been added to the database so the list will be
		 * correct if it is fetched.
		 */
		return true;
	}

	@Override
	public boolean remove(Object o) {
		/*
		 * This list is not cached so there is nothing to do. The object has
		 * already been removed from the database so the list will be
		 * correct if it is fetched.
		 */
		return true;
	}

	@Override
	public boolean containsAll(Collection<?> arg0) {
		throw new RuntimeException("method not implemented");
	}

	@Override
	public boolean addAll(Collection<? extends E> arg0) {
		throw new RuntimeException("method not implemented");
	}

	@Override
	public boolean removeAll(Collection<?> arg0) {
		throw new RuntimeException("method not implemented");
	}

	@Override
	public boolean retainAll(Collection<?> arg0) {
		throw new RuntimeException("method not implemented");
	}

	@Override
	public void clear() {
		throw new RuntimeException("method not implemented");
	}
}
