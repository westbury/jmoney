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

import java.util.Date;

import net.sf.jmoney.model2.Transaction.EntryCollection;

/**
 *
 * @author  Nigel
 *
 * To add fields and methods to a Transaction object, one should
 * derive a class on TransactionExtension.  This mechanism
 * allows multiple extensions to a Transaction object to be added
 * and maintained at runtime.
 *
 */
public abstract class TransactionExtension extends ExtensionObject {
	
	public TransactionExtension(ExtendableObject extendedObject) {
		super(extendedObject);
	}
	
	/*
	 * All extensions implement the Transaction interface.  This is for convenience
	 * so the consumer can get a single object that supports both the base Transaction
	 * methods and the extension methods.  All Transaction interface methods are passed
	 * on to the base Transaction object.
	 */
	
	public Date getDate() {
		return getBaseObject().getDate();
	}
	
	public void setDate(Date date) {
		getBaseObject().setDate(date);
	}

	public EntryCollection getEntryCollection() {
		return getBaseObject().getEntryCollection();
	}

	public Entry createEntry() {
		return getBaseObject().createEntry();
	}

	public void deleteEntry(Entry entry) {
		getBaseObject().deleteEntry(entry);
	}
	
	public Transaction getBaseObject() {
		return (Transaction)baseObject;
	}
}
