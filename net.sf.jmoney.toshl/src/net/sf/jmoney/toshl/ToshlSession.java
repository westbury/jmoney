/*
 *
 *  JMoney - A Personal Finance Manager
 *  Copyright (c) 2006, 2016 Nigel Westbury <westbury@users.sourceforge.net>
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

package net.sf.jmoney.toshl;

import net.sf.jmoney.isolation.IListManager;
import net.sf.jmoney.isolation.ObjectCollection;
import net.sf.jmoney.model2.ExtendableObject;
import net.sf.jmoney.model2.ExtensionObject;
import net.sf.jmoney.model2.Session;

/**
 * An extension object that extends IncomeExpenseAccount objects.
 * This extension object maintains the values of the properties
 * that have been added by this plug-in.
 * 
 * @author Nigel Westbury
 */
public class ToshlSession extends ExtensionObject {
	
	protected IListManager<ToshlAccount> toshlAccounts = null;
	
	/**
	 * A default constructor is mandatory for all extension objects.
	 * The default constructor sets the extension properties to
	 * appropriate default values.
	 */
	public ToshlSession(ExtendableObject extendedObject) {
		super(extendedObject);
	}
	
	/**
	 * A Full constructor is mandatory for all extension objects.
	 * This constructor is called by the datastore to construct
	 * the extension objects when loading data.
	 * 
	 */
	public ToshlSession(
			ExtendableObject extendedObject,
			IListManager<ToshlAccount> toshlAccounts) {
		super(extendedObject);
		this.toshlAccounts = toshlAccounts;
	}
	
	public ObjectCollection<ToshlAccount> getToshlAccountCollection() {
		return new ObjectCollection<ToshlAccount>(toshlAccounts, (Session)baseObject, ToshlSessionInfo.getPatternsAccessor());
	}
}
