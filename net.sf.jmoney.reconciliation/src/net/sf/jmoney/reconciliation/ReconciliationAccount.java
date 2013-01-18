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

package net.sf.jmoney.reconciliation;

import net.sf.jmoney.model2.CapitalAccountExtension;
import net.sf.jmoney.model2.ExtendableObject;

/**
 * An extension object that extends CapitalAccount objects.
 * This extension object maintains the values of the properties
 * that have been added by this plug-in.
 * 
 * @author Nigel Westbury
 */
public class ReconciliationAccount extends CapitalAccountExtension {
	
	protected boolean reconcilable = false;
	
	/**
	 * A default constructor is mandatory for all extension objects.
	 * The default constructor sets the extension properties to
	 * appropriate default values.
	 */
	public ReconciliationAccount(ExtendableObject extendedObject) {
		super(extendedObject);
	}
	
	/**
	 * A Full constructor is mandatory for all extension objects.
	 * This constructor is called by the datastore to construct
	 * the extension objects when loading data.
	 * 
	 */
	public ReconciliationAccount(
			ExtendableObject extendedObject,
			boolean reconcilable) {
		super(extendedObject);
		this.reconcilable = reconcilable;
	}
	
	/**
	 * Indicates if an account is reconcilable.  If it is not then none
	 * of the other properties are applicable.
	 */
	public boolean isReconcilable() {
		return reconcilable;
	}
	
	/**
	 * Sets the reconcilable flag.
	 */
	public void setReconcilable(boolean reconcilable) {
		boolean oldReconcilable = this.reconcilable;
		this.reconcilable = reconcilable;
		processPropertyChange(ReconciliationAccountInfo.getReconcilableAccessor(), new Boolean(oldReconcilable), new Boolean(reconcilable));
	}
}
