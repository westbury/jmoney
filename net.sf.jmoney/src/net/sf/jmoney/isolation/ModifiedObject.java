/*
*
*  JMoney - A Personal Finance Manager
*  Copyright (c) 2006 Nigel Westbury <westbury@users.sourceforge.net>
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

import java.util.HashMap;
import java.util.Map;

/**
 * An instance of this class contains the changes that have been made to an
 * model object. These changes consist of either a map containing an entry
 * for each property whose value has changed, or an indication that the object
 * has been deleted.
 * <P>
 * Instances of this class are kept in a map in the transaction manager. This
 * map maps object keys to objects of this class.
 * <P>
 * If a value of a property is a reference to another object then an
 * UncommittedObjectKey is stored as the value. By doing this, the referenced
 * object does not need to be materialized unless necessary.
 * 
 * @author Nigel Westbury
 */
public class ModifiedObject {

	/**
	 * The map of properties of the object that have been changed, or a null map
	 * reference to indicate that the object has been deleted altogether
	 */
	Map<IScalarPropertyAccessor, Object> modifiedProperties = new HashMap<IScalarPropertyAccessor, Object>();

	public void put(IScalarPropertyAccessor accessor, Object value) {
		/*
		 * If modifiedProperties is null then an attempt to change a property of
		 * an object that has been deleted. This indicates a bug in
		 * net.sf.jmoney.isolation package so we can assume the map is not null.
		 */
		modifiedProperties.put(accessor, value);
	}

	public boolean isDeleted() {
		return modifiedProperties == null;
	}

	/**
	 * Set this object to indicate that the object has been deleted
	 * in this transaction.  
	 *
	 * Note that there is no method to 'undelete' an object.
	 * Once an object is deleted it is gone (the consumer
	 * can always create a new object).
	 */
	public void setDeleted() {
		modifiedProperties = null;
	}

	public boolean isEmpty() {
		/*
		 * If modifiedProperties is null then the object has been deleted. This
		 * method is not defined in such a case and this indicates a bug in
		 * net.sf.jmoney.isolation package so we can assume the map is not null.
		 */
		return modifiedProperties.isEmpty();
	}

	public Map<IScalarPropertyAccessor, Object> getMap() {
		/*
		 * If modifiedProperties is null then the object has been deleted. This
		 * method is not defined in such a case and this indicates a bug in
		 * net.sf.jmoney.isolation package so we can assume the map is not null.
		 */
		return modifiedProperties;
	}
	
}
