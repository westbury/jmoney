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

package net.sf.jmoney.isolation;

import java.util.Collection;


/**
 * This interface is the interface to all objects that manage
 * the sets of values that are the values of a multi-valued
 * property.  For example, the set of all accounts in a session
 * are managed by an object that implements this interface.
 * 
 * @param E type of the elements in this list
 */
public interface IListManager<E extends IModelObject> extends Collection<E> {

	/**
	 * This method creates a new object in this collection
	 * in the datastore.  The new object will be initialized
	 * with default values.
	 * 
	 * @param propertySet the property set of the object to create
	 * 			(this parameter is required because some lists
	 * 			contain objects of a derivable type, in which case
	 * 			the exact type of the object to create must be given).
	 * @return the newly created object.
	 */
	<F extends E> F createNewElement(IExtendablePropertySet<F> propertySet);

	/**
	 * This method creates a new object in this collection
	 * in the datastore.  The new object will be initialized
	 * with property values taken from the given interface.
	 * 
	 * @param values values to be set in the properties of the new object 
	 * @return the newly created object.
	 */
	<F extends E> F createNewElement(IExtendablePropertySet<F> propertySet, IValues<F> values);
	
	/**
	 * Deletes the given object from this list. Because objects are 'owned' by
	 * the lists, removing it from the list means deleting the object altogether
	 * from the data-store.
	 * <P>
	 * This method should return <code>false</code> if there are references to
	 * the object. If the data-store is backed by a relational database then it
	 * is cheaper to attempt the delete, catch the foreign key constraint
	 * violation, and return <code>false</code>. If the data-store is serialized
	 * to/from a file then the implementation must check for references before
	 * attempting the delete.
	 * 
	 * @param extendableObject
	 * @return true if the element was deleted, false if it could not be deleted
	 *         because there were references to it
	 */
	void deleteElement(E extendableObject) throws ReferenceViolationException;
	
	/**
	 * Moves the given object into this list, removing it from its
	 * original list.
	 * 
	 * @param extendableObject
	 * @param originalList 
	 */
	void moveElement(E extendableObject, IListManager originalList);
}
