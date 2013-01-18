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

package net.sf.jmoney.importer.model;

import net.sf.jmoney.model2.EntryExtension;
import net.sf.jmoney.model2.ExtendableObject;

/**
 * An extension object that extends Entry objects.
 * This extension object maintains the values of the properties
 * that have been added by this plug-in.
 * 
 * @author Nigel Westbury
 */
public class ReconciliationEntry extends EntryExtension {
	
	private String uniqueId = null;
	
	/**
	 * A default constructor is mandatory for all extension objects.
	 * The default constructor sets the extension properties to
	 * appropriate default values.
	 * 
	 * @param extendedObject 
	 */
	public ReconciliationEntry(ExtendableObject extendedObject) {
		super(extendedObject);
	}
	
	/**
	 * A Full constructor is mandatory for all extension objects.
	 * This constructor is called by the datastore to construct
	 * the extension objects when loading data.
	 * 
	 */
	public ReconciliationEntry(ExtendableObject extendedObject, String uniqueId) {
		super(extendedObject);
		this.uniqueId = uniqueId;
	}
	
	/**
	 * Returns a string that uniquely identifies this entry.
	 * <P>
	 * The bank may provide a unique id with each entry downloaded
	 * from the bank.  If so then this id can be used to check if an
	 * entry has already been imported.  If no unique id is provided
	 * then this property should be null.  A unique id is not required
	 * to be provided by the bank.  If none is provided then entries are
	 * matched based on amount, date etc.
	 */
	public String getUniqueId() {
		return uniqueId;
	}
	
	public void setUniqueId(String uniqueId) {
		String oldUniqueId = this.uniqueId;
		this.uniqueId = uniqueId;
		processPropertyChange(ReconciliationEntryInfo.getUniqueIdAccessor(), oldUniqueId, uniqueId);
	}
}
