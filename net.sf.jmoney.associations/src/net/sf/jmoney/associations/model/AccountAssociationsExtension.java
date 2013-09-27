/*
*
*  JMoney - A Personal Finance Manager
*  Copyright (c) 2010 Nigel Westbury <westbury@users.sourceforge.net>
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

package net.sf.jmoney.associations.model;

import net.sf.jmoney.isolation.IListManager;
import net.sf.jmoney.isolation.ObjectCollection;
import net.sf.jmoney.model2.CapitalAccountExtension;
import net.sf.jmoney.model2.ExtendableObject;

/**
 * An extension object that extends BankAccount objects.
 * This extension object maintains the values of the properties
 * that have been added by this plug-in.
 * 
 * @author Nigel Westbury
 */
public class AccountAssociationsExtension extends CapitalAccountExtension {
	
	protected IListManager<AccountAssociation> associations;
	
	/**
	 * A default constructor is mandatory for all extension objects.
	 * The default constructor sets the extension properties to
	 * appropriate default values.
	 */
	public AccountAssociationsExtension(ExtendableObject extendedObject) {
		super(extendedObject);
		this.associations = extendedObject.getObjectKey().constructListManager(AccountAssociationsInfo.getAssociationsAccessor());
	}
	
	/**
	 * A Full constructor is mandatory for all extension objects.
	 * This constructor is called by the datastore to construct
	 * the extension objects when loading data.
	 * 
	 */
	public AccountAssociationsExtension(
			ExtendableObject extendedObject,
			IListManager<AccountAssociation> associations) {
		super(extendedObject);
		this.associations = associations;
	}
	
	public ObjectCollection<AccountAssociation> getAssociationCollection() {
		return new ObjectCollection<AccountAssociation>(associations, getBaseObject(), AccountAssociationsInfo.getAssociationsAccessor());
	}
}
