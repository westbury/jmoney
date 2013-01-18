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

/**
 * Some properties are only applicable if another property is set. The other
 * property may be a boolean property that must be set to true or an enumerated
 * type that must be set to a certain value. For example, the 'check number'
 * property of an <code>Entry</code> object would only be applicable if the
 * account of the <code>Entry</code> object is a <code>BankAccount</code>
 * object.
 * <P>
 * When the applicability of a property depends on the value of other
 * properties, the property must provide an implementation of this
 * IPropertyDependency interface.
 * 
 * @author Nigel Westbury
 *
 * @param <E> the class of the object that contains this property, which
 * 				must be either ExtendableObject or ExtensionObject 
 */
public interface IPropertyDependency<E extends Object> {
    /**
	 * @param object
	 *            the object containing the property
	 * @return true if, given the current state of the given object, the
	 *         property is applicable, false if the property is not applicable
	 */
	boolean isApplicable(E extendableObject);
}
