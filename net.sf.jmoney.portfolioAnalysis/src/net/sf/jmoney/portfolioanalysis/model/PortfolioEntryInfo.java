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

package net.sf.jmoney.portfolioanalysis.model;

import net.sf.jmoney.fields.TextControlFactory;
import net.sf.jmoney.model2.EntryInfo;
import net.sf.jmoney.model2.ExtendableObject;
import net.sf.jmoney.model2.ExtensionPropertySet;
import net.sf.jmoney.model2.IExtensionObjectConstructors;
import net.sf.jmoney.model2.IPropertySetInfo;
import net.sf.jmoney.model2.IValues;
import net.sf.jmoney.model2.PropertySet;
import net.sf.jmoney.model2.ScalarPropertyAccessor;
import net.sf.jmoney.portfolioanalysis.resources.Messages;

/**
 * Provides the metadata for the extra properties added to each
 * entry by this plug-in.
 * 
 * @author Nigel Westbury
 */
public class PortfolioEntryInfo implements IPropertySetInfo {

	private static ExtensionPropertySet<PortfolioEntry> propertySet = PropertySet.addExtensionPropertySet(PortfolioEntry.class, EntryInfo.getPropertySet(), new IExtensionObjectConstructors<PortfolioEntry>() {

		public PortfolioEntry construct(ExtendableObject extendedObject) {
			return new PortfolioEntry(extendedObject);
		}

		public PortfolioEntry construct(ExtendableObject extendedObject, IValues values) {
			return new PortfolioEntry(
					extendedObject, 
					values.getScalarValue(getPortfolioNameAccessor())
			);
		}
	});
	
	private static ScalarPropertyAccessor<String> portfolioNameAccessor = null;
	
	public PropertySet<PortfolioEntry> registerProperties() {
		portfolioNameAccessor  = propertySet.addProperty("portfolioName", Messages.Entry_portfolioName, String.class, 1, 80, new TextControlFactory(), null);
		
		return propertySet;
	}

	/**
	 * @return
	 */
	public static ExtensionPropertySet<PortfolioEntry> getPropertySet() {
		return propertySet;
	}

	/**
	 * @return
	 */
	public static ScalarPropertyAccessor<String> getPortfolioNameAccessor() {
		return portfolioNameAccessor;
	}
}
