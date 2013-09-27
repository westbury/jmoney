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

import net.sf.jmoney.isolation.IObjectKey;
import net.sf.jmoney.isolation.IValues;
import net.sf.jmoney.isolation.ListKey;
import net.sf.jmoney.model2.Account;
import net.sf.jmoney.model2.ExtendableObject;

/**
 * The data model for an entry.
 */
public final class AccountAssociation extends ExtendableObject {
	
	String id = null;

	/**
	 * Element: Account
	 */
	IObjectKey accountKey = null;
	
    /**
     * Constructor used by datastore plug-ins to create
     * a pattern object.
     *
     * @param parent The key to a CapitalAccount object.
     * 		This parameter must be non-null.
     * 		The getObject method must not be called on this
     * 		key from within this constructor because the
     * 		key may not yet be in a state in which it is
     * 		capable of materializing an object.   
     */
	public AccountAssociation(
			IObjectKey objectKey,
			ListKey<? super AccountAssociation,?> parentKey,
			String     id,
    		IObjectKey accountKey,
    		IValues<AccountAssociation>    extensionValues) {
		super(objectKey, parentKey, extensionValues);

		this.id = id;
		this.accountKey = accountKey;
	}
	
    /**
     * Constructor used by datastore plug-ins to create
     * a pattern object.
     *
     * @param parent The key to a CapitaAccount object.
     * 		This parameter must be non-null.
     * 		The getObject method must not be called on this
     * 		key from within this constructor because the
     * 		key may not yet be in a state in which it is
     * 		capable of materializing an object.   
     */
	public AccountAssociation(
			IObjectKey objectKey,
    		ListKey<? super AccountAssociation,?> parentKey) {
		super(objectKey, parentKey);

		this.id = null;
		this.accountKey = null;
	}

	@Override
	protected String getExtendablePropertySetId() {
		return "net.sf.jmoney.ameritrade.reconciliation.pattern";
	}
	
	/**
	 * Returns the id of the association.
	 */
	public String getId() {
		return id;
	}
	
	/**
	 * Returns the account.
	 */
	public Account getAccount() {
		if (accountKey == null) {
			return null;
		} else {
			return (Account)accountKey.getObject();
		}
	}
	
	/**
	 * Sets the id of this association.
	 */
	public void setId(String id) {
		String oldId = this.id;
		this.id = id;
		
		// Notify the change manager.
		processPropertyChange(AccountAssociationInfo.getIdAccessor(), oldId, id);
	}
	
	/**
	 * Sets the account.
	 */
	public void setAccount(Account newAccount) {
		Account oldAccount =
			accountKey == null
			? null
					: (Account)accountKey.getObject();
		
		// TODO: This is not efficient.  Better would be to pass
		// an object key as the old value to the property change
		// method.  Then the object is materialized only if
		// necessary.
		// NOTE: Even though a null account is not valid, we support
		// the setting of it because code may potentially need to do this
		// in order to, say, delete the account before the new account
		// of the entry is known.
		accountKey = 
			newAccount == null
			? null
					: newAccount.getObjectKey();
		
		// Notify the change manager.
		processPropertyChange(AccountAssociationInfo.getAccountAccessor(), oldAccount, newAccount);
	}
}
