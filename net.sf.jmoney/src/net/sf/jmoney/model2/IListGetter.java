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
package net.sf.jmoney.model2;

import net.sf.jmoney.isolation.ObjectCollection;

/**
 * An implementation of this interface must be provided for every list property.
 * The implementation provides a method for getting the list from a given parent
 * object. Code that wishes to process properties containing lists in a
 * generalized way (without knowing about all the properties that contain lists
 * at compile time) can thus do so.
 * 
 * @author Nigel Westbury
 * 
 * @param <P>
 * 			the class of objects that contain this property
 * @param <E>
 *          the class of objects contained as elements in this list property
 */
public interface IListGetter<P, E extends ExtendableObject> {

	ObjectCollection<E> getList(P parentObject);
}
