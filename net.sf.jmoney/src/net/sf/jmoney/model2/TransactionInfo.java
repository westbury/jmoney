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

import java.util.Date;

import net.sf.jmoney.fields.DateControlFactory;
import net.sf.jmoney.fields.TextControlFactory;
import net.sf.jmoney.isolation.IObjectKey;
import net.sf.jmoney.isolation.IValues;
import net.sf.jmoney.isolation.ListKey;
import net.sf.jmoney.isolation.ObjectCollection;
import net.sf.jmoney.resources.Messages;

/**
 * This class is a listener class to the net.sf.jmoney.fields
 * extension point.  It implements an extension.
 * <P>
 * This extension registers the Entry properties.  By registering
 * the properties, every one can know how to display, edit, and store
 * the properties.
 * <P>
 * These properties are supported in the JMoney base code, so everyone
 * including plug-ins will know about these properties.  However, to
 * follow the Eclipse paradigm (every one should be treated equal,
 * including oneself), these are registered through the same extension
 * point that plug-ins must also use to register their properties.
 * 
 * @author Nigel Westbury
 * @author Johann Gyger
 */
public class TransactionInfo implements IPropertySetInfo {

	private static ExtendablePropertySet<Transaction> propertySet = PropertySet.addBaseFinalPropertySet(Transaction.class, Messages.TransactionInfo_Description, new IExtendableObjectConstructors<Transaction>() {

		@Override
		public Transaction construct(IObjectKey objectKey, ListKey<? super Transaction,?> parentKey) {
			return new Transaction(objectKey, parentKey);
		}

		@Override
		public Transaction construct(IObjectKey objectKey,
				ListKey<? super Transaction,?> parentKey, IValues<Transaction> values) {
			return new Transaction(
					objectKey, 
					parentKey, 
					values.getListManager(objectKey, TransactionInfo.getEntriesAccessor()),
					values.getScalarValue(TransactionInfo.getDateAccessor()),
					values.getScalarValue(TransactionInfo.getTypeAccessor()),
					values
			);
		}
	});


	private static ScalarPropertyAccessor<Date,Transaction> dateAccessor = null;
	private static ScalarPropertyAccessor<String,Transaction> typeAccessor = null;
	private static ListPropertyAccessor<Entry,Transaction> entriesAccessor = null;

	@Override
	public PropertySet registerProperties() {
		IListGetter<Transaction, Entry> entryGetter = new IListGetter<Transaction, Entry>() {
			@Override
			public ObjectCollection<Entry> getList(Transaction parentObject) {
				return parentObject.getEntryCollection();
			}
		};
		
        IPropertyControlFactory<Transaction,Date> dateControlFactory = new DateControlFactory<Transaction>();
		IPropertyControlFactory<Transaction,String> textControlFactory = new TextControlFactory<Transaction>();
		
		entriesAccessor = propertySet.addPropertyList("entry", Messages.TransactionInfo_Entry, EntryInfo.getPropertySet(), entryGetter); //$NON-NLS-1$
		dateAccessor = propertySet.addProperty("date", Messages.TransactionInfo_Date, Date.class, 0, 74, dateControlFactory, null); //$NON-NLS-1$
		typeAccessor = propertySet.addProperty("type", Messages.TransactionInfo_Type, String.class, 5, 100, textControlFactory, null); //$NON-NLS-1$
		
		return propertySet;
	}

	/**
	 * @return
	 */
	public static ExtendablePropertySet<Transaction> getPropertySet() {
		return propertySet;
	}

	/**
	 * @return
	 */
	public static ListPropertyAccessor<Entry,Transaction> getEntriesAccessor() {
		return entriesAccessor;
	}	

	/**
	 * @return
	 */
	public static ScalarPropertyAccessor<Date,Transaction> getDateAccessor() {
		return dateAccessor;
	}	

	/**
	 * @return
	 */
	public static ScalarPropertyAccessor<String,Transaction> getTypeAccessor() {
		return typeAccessor;
	}

}
