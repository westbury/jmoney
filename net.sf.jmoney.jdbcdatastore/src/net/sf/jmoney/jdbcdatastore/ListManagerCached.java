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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Vector;

import net.sf.jmoney.isolation.IExtendablePropertySet;
import net.sf.jmoney.isolation.IListManager;
import net.sf.jmoney.isolation.IModelObject;
import net.sf.jmoney.isolation.IValues;
import net.sf.jmoney.isolation.ReferenceViolationException;
import net.sf.jmoney.jdbcdatastore.SessionManager.DatabaseListKey;
import net.sf.jmoney.model2.ListPropertyAccessor;

/**
 * Every datastore implementation must provide an implementation
 * of the IListManager interface.  This implementation simply
 * uses the Vector class to keep a list of objects.
 *
 * @author Nigel Westbury
 */
public class ListManagerCached<E extends IModelObject, S extends IModelObject> implements IListManager<E> {

	private static final long serialVersionUID = 867883048050895954L;

	private SessionManager sessionManager;
	private DatabaseListKey<E,S> listKey;
	
	/**
	 * the elements in this list if known, or null if the set of
	 * elements is not yet known because they have not yet been read
	 * from the database (an empty list indicates that we know there
	 * are no elements in this list)
	 */
	private Vector<E> elements = null;
	
	/**
	 * 
	 * @param sessionManager
	 * @param listKey
	 * @param isEmpty
	 *            true if the list is known to be empty (this is the case if the
	 *            parent object has been newly created), false if the list is
	 *            unknown (may or may not be empty, this is the case if the
	 *            parent object is being materialized from the database)
	 */
	public ListManagerCached(SessionManager sessionManager, DatabaseListKey<E,S> listKey, boolean isEmpty) {
		this.sessionManager = sessionManager;
		this.listKey = listKey;
		
		if (isEmpty) {
			this.elements = new Vector<E>();
		}
	}

	public <F extends E> F createNewElement(IExtendablePropertySet<F> propertySet) {
		// We must create the object before we persist it to the database.
		// The reason why we must do this, and not simply write the
		// default values, is that the constructor only uses the
		// default values array as a guide.  For example, the constructor
		// may replace a null timestamp with the current time, or
		// a null currency with a default currency.
		
 		// First we build the in-memory object.
		// This is done here because in this case the object is always cached
		// in memory.
		
		ObjectKey objectKey = new ObjectKey(sessionManager);
		
		F extendableObject = sessionManager.constructExtendableObject(propertySet, objectKey, sessionManager.constructListKey(listKey));

		objectKey.setObject(extendableObject);

		/*
		 * We can add elements without needed to build the list. If the list
		 * does ultimately need to be built from the database, this object will
		 * be included as it has been written to the database.
		 */
		if (elements != null) {
			elements.add(extendableObject);
		}
		
		// Now we insert the new row into the tables.

		int rowId = sessionManager.insertIntoDatabase(propertySet, extendableObject, listKey);
		objectKey.setRowId(rowId);

		return extendableObject;
	}

	public <F extends E> F createNewElement(IExtendablePropertySet<F> propertySet, IValues<F> values) {
		// We must create the object before we persist it to the database.
		// The reason why we must do this, and not simply write the
		// default values, is that the constructor only uses the
		// default values array as a guide.  For example, the constructor
		// may replace a null timestamp with the current time, or
		// a null currency with a default currency.
		
 		// First we build the in-memory object.
		// This is done here because in this case the object is always cached
		// in memory.
		
		ObjectKey objectKey = new ObjectKey(sessionManager);
		
		F extendableObject = sessionManager.constructExtendableObject(propertySet, objectKey, listKey, values);

		objectKey.setObject(extendableObject);
		
		/*
		 * We can add elements without needed to build the list. If the list
		 * does ultimately need to be built from the database, this object will
		 * be included as it has been written to the database.
		 */
		if (elements != null) {
			elements.add(extendableObject);
		}
		
		// Now we insert the new row into the tables.

		int rowId = sessionManager.insertIntoDatabase(propertySet, extendableObject, listKey);
		objectKey.setRowId(rowId);

		return extendableObject;
	}

	public void deleteElement(E extendableObject) throws ReferenceViolationException {
		if (elements == null) {
			buildCachedList();
		}

		boolean found = elements.remove(extendableObject);
		if (!found) {
			throw new RuntimeException("attempt to delete an element that did not exist");
		}
		
		// Delete this object from the database.
		IDatabaseRowKey key = (IDatabaseRowKey)extendableObject.getObjectKey();
		sessionManager.deleteFromDatabase(key);
	}

	public void moveElement(E extendableObject, IListManager originalListManager) {
		sessionManager.reparentInDatabase(extendableObject, listKey);
	}

	public boolean add(E extendableObject) {
		if (elements != null) {
			elements.add(extendableObject);
		} else {
			/*
			 * The list has not been fetched, so there is nothing to do. The
			 * object has already been added to the database so the list will be
			 * correct if it is fetched.
			 */
		}
		return true;
	}

	public boolean remove(Object o) {
		if (elements != null) {
			return elements.remove(o);
		} else {
			/*
			 * The list has not been fetched, so there is nothing to do. The
			 * object has already been removed from the database so the list
			 * will be correct if it is fetched. It's not worth the cost of
			 * checking if the object exists in this list. Assume that the
			 * object is in the list.
			 */
			return true;
		}
	}

	public boolean addAll(Collection<? extends E> arg0) {
		throw new RuntimeException("Method not supported");
	}

	public void clear() {
		throw new RuntimeException("Method not supported");
	}

	public boolean contains(Object arg0) {
		if (elements == null) {
			buildCachedList();
		}
		return elements.contains(arg0);
	}

	public boolean containsAll(Collection<?> arg0) {
		if (elements == null) {
			buildCachedList();
		}
		return elements.containsAll(arg0);
	}

	public boolean isEmpty() {
		if (elements == null) {
			buildCachedList();
		}
		return elements.isEmpty();
	}

	public Iterator<E> iterator() {
		if (elements == null) {
			buildCachedList();
		}
		return elements.iterator();
	}

	public boolean removeAll(Collection<?> arg0) {
		throw new RuntimeException("Method not supported");
	}

	public boolean retainAll(Collection<?> arg0) {
		throw new RuntimeException("Method not supported");
	}

	public int size() {
		if (elements == null) {
			buildCachedList();
		}
		return elements.size();
	}

	public Object[] toArray() {
		if (elements == null) {
			buildCachedList();
		}
		return elements.toArray();
	}

	public <T> T[] toArray(T[] arg0) {
		if (elements == null) {
			buildCachedList();
		}
		return elements.toArray(arg0);
	}


	private void buildCachedList() {
		elements = new Vector<E>();

		/*
		 * If the type of object held by the list is a type from which property
		 * sets must be derived then we execute a query for each final property
		 * set. This is necessary because different tables must be joined
		 * depending on the actual property set.
		 */		
		try {
			ListPropertyAccessor<?,?> accessor2 = (ListPropertyAccessor<?,?>)listKey.listPropertyAccessor;
			for (IExtendablePropertySet<?> finalPropertySet2 : accessor2.getElementPropertySet().getDerivedPropertySets()) {
				IExtendablePropertySet<? extends E> finalPropertySet = (IExtendablePropertySet<? extends E>)finalPropertySet2;
				PreparedStatement stmt = sessionManager.executeListQuery(listKey, finalPropertySet);
				try {
					ResultSet resultSet = stmt.executeQuery();
					try {
						while (resultSet.next()) {
							ObjectKey key = new ObjectKey(resultSet, finalPropertySet, listKey, sessionManager);
							E extendableObject = finalPropertySet.getImplementationClass().cast(key.getObject());
							elements.add(extendableObject);
						}
					} finally {
						resultSet.close();
					}
				} finally {
					stmt.close();
				}
			}
		} catch (SQLException e) {
			// TODO: We get an 08S01 here when the socket is reset.
			e.printStackTrace();
			throw new RuntimeException("internal error");
		}
	}
}
