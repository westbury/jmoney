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

package net.sf.jmoney.serializeddatastore;

import net.sf.jmoney.JMoneyPlugin;
import net.sf.jmoney.isolation.AbstractDataManager;
import net.sf.jmoney.isolation.IExtendablePropertySet;
import net.sf.jmoney.isolation.IListManager;
import net.sf.jmoney.isolation.IListPropertyAccessor;
import net.sf.jmoney.isolation.IModelObject;
import net.sf.jmoney.isolation.IObjectKey;
import net.sf.jmoney.isolation.IScalarPropertyAccessor;
import net.sf.jmoney.isolation.ListKey;
import net.sf.jmoney.model2.Account;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.EntryInfo;

/**
 * This class provides the IObjectKey implementation.
 *
 * In this datastore implementation, the entire datastore is
 * read into memory when a session is opened.  The object key
 * implementation is therefore very simple - the object key is
 * simply a reference to the object.
 */
public class SimpleObjectKey implements IObjectKey {
	private SessionManager sessionManager;
	private IModelObject extendableObject;
	
	// TODO: make this default protection
	public SimpleObjectKey(SessionManager sessionManager) {
		this.sessionManager = sessionManager;
	}
	
	@Override
	public IModelObject getObject() {
		return extendableObject;
	}

	// TODO: make this default protection
	public void setObject(IModelObject extendableObject) {
		this.extendableObject = extendableObject;
	}

	@Override
	public void updateProperties(IExtendablePropertySet<?> actualPropertySet, Object[] oldValues, Object[] newValues) {
		// If the account property of an entry is changed then we
		// must update the lists of entries in each account.
		if (extendableObject instanceof Entry) {
			int i = 0;
			for (IScalarPropertyAccessor propertyAccessor2: actualPropertySet.getScalarProperties3()) {
				if (propertyAccessor2 == EntryInfo.getAccountAccessor()) {
					if (!JMoneyPlugin.areEqual(oldValues[i], newValues[i])) {
						if (oldValues[i] != null) {
							sessionManager.removeEntryFromList((Account)oldValues[i], (Entry)extendableObject);
						}
						if (newValues[i] != null) {
							sessionManager.addEntryToList((Account)newValues[i], (Entry)extendableObject);
						}
					}
					break;
				}
				i++;
			}
		}
		
		/*
		 * There is no back-end datastore that needs updating, so we have
		 * nothing more to do except to mark the session as modified.
		 */
		
		sessionManager.setModified();
	}

	@Override
	public AbstractDataManager getDataManager() {
		return sessionManager;
	}

	@Override
	public <E extends IModelObject, S extends IModelObject> IListManager<E> constructListManager(IListPropertyAccessor<E,S> listAccessor) {
		return new SimpleListManager<E>(sessionManager, new ListKey<E,S>(this, listAccessor));
	}
}
