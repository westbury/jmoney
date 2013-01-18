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

import net.sf.jmoney.isolation.ObjectCollection;


/**
 *
 * @author  Nigel
 *
 * To add fields and methods to an Account object, one should
 * derive a class on AccountExtension.  This mechanism
 * allows multiple extensions to an Account object to be added
 * and maintained at runtime.
 *
 */
public abstract class AccountExtension extends ExtensionObject {
    
    public AccountExtension(ExtendableObject extendedObject) {
    	super(extendedObject);
    }

    /*
     * All extensions to Account objects implement the Account interface.  This is for convenience
     * so the comsumer can get a single object that supports both the base Entry
     * methods and the extension methods.  All Account interface methods are passed
     * on to the base Account object.
     */
    
	/**
	 * @return the name of the category.
	 */
	public String getName() {
            return getBaseObject().getName();
        }

	/**
	 * @return the full qualified name of the category.
	 */
	public String getFullAccountName() {
		return getBaseObject().getFullAccountName();
	}
	
	public Account getParent() {
		return getBaseObject().getParent();
	}

	public ObjectCollection getSubAccountCollection() {
		return getBaseObject().getSubAccountCollection();
	}

	public Account getBaseObject() {
		return (Account)baseObject;
	}
}
