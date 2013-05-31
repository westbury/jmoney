/*
 *
 *  JMoney - A Personal Finance Manager
 *  Copyright (c) 2004, 2009 Nigel Westbury <westbury@users.sourceforge.net>
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

package net.sf.jmoney.ofx.model;

import net.sf.jmoney.fields.NonEditableTextControlFactory;
import net.sf.jmoney.isolation.IValues;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.EntryInfo;
import net.sf.jmoney.model2.ExtensionPropertySet;
import net.sf.jmoney.model2.IExtensionObjectConstructors;
import net.sf.jmoney.model2.IPropertySetInfo;
import net.sf.jmoney.model2.PropertySet;
import net.sf.jmoney.model2.ScalarPropertyAccessor;
import net.sf.jmoney.ofx.resources.Messages;

/**
 * Provides the metadata for the extra properties added to each
 * entry by this plug-in.
 *
 * @author Nigel Westbury
 */
public class OfxEntryInfo implements IPropertySetInfo {

	private static ExtensionPropertySet<OfxEntry,Entry> propertySet = PropertySet.addExtensionPropertySet(OfxEntry.class, EntryInfo.getPropertySet(), new IExtensionObjectConstructors<OfxEntry,Entry>() {

		@Override
		public OfxEntry construct(Entry extendedObject) {
			return new OfxEntry(extendedObject);
		}

		@Override
		public OfxEntry construct(Entry extendedObject, IValues<Entry> values) {
			return new OfxEntry(
					extendedObject,
					values.getScalarValue(getFitidAccessor())
			);
		}
	});

	private static ScalarPropertyAccessor<String,Entry> fitidAccessor = null;

	@Override
	public PropertySet<OfxEntry,Entry> registerProperties() {
		fitidAccessor  = propertySet.addProperty("fitid", Messages.Entry_Fitid, String.class, 1, 80, new NonEditableTextControlFactory<Entry>(), null);

		return propertySet;
	}

	/**
	 * @return
	 */
	public static ExtensionPropertySet<OfxEntry,Entry> getPropertySet() {
		return propertySet;
	}

	/**
	 * @return
	 */
	public static ScalarPropertyAccessor<String,Entry> getFitidAccessor() {
		return fitidAccessor;
	}
}
