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

import net.sf.jmoney.fields.NonEditableTextControlFactory;
import net.sf.jmoney.importer.resources.Messages;
import net.sf.jmoney.isolation.IValues;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.EntryInfo;
import net.sf.jmoney.model2.ExtensionPropertySet;
import net.sf.jmoney.model2.IExtensionObjectConstructors;
import net.sf.jmoney.model2.IPropertySetInfo;
import net.sf.jmoney.model2.PropertySet;
import net.sf.jmoney.model2.ScalarPropertyAccessor;

/**
 * Provides the metadata for the extra properties added to each
 * entry by this plug-in.
 *
 * @author Nigel Westbury
 */
public class ReconciliationEntryInfo implements IPropertySetInfo {

	private static ExtensionPropertySet<ReconciliationEntry,Entry> propertySet = PropertySet.addExtensionPropertySet(ReconciliationEntry.class, EntryInfo.getPropertySet(), new IExtensionObjectConstructors<ReconciliationEntry,Entry>() {

		@Override
		public ReconciliationEntry construct(Entry extendedObject) {
			return new ReconciliationEntry(extendedObject);
		}

		@Override
		public ReconciliationEntry construct(Entry extendedObject, IValues<Entry> values) {
			return new ReconciliationEntry(
					extendedObject,
					values.getScalarValue(getUniqueIdAccessor())
			);
		}
	});

	private static ScalarPropertyAccessor<String,Entry> uniqueIdAccessor = null;

	@Override
	public PropertySet<ReconciliationEntry,Entry> registerProperties() {
		uniqueIdAccessor  = propertySet.addProperty("uniqueId", Messages.Entry_UniqueIdShort, String.class, 1, 80, new NonEditableTextControlFactory<Entry>(), null);

		return propertySet;
	}

	/**
	 * @return
	 */
	public static ExtensionPropertySet<ReconciliationEntry,Entry> getPropertySet() {
		return propertySet;
	}

	/**
	 * @return
	 */
	public static ScalarPropertyAccessor<String,Entry> getUniqueIdAccessor() {
		return uniqueIdAccessor;
	}
}
