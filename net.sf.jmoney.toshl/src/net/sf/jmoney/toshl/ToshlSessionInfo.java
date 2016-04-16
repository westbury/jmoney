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

import net.sf.jmoney.isolation.IValues;
import net.sf.jmoney.isolation.ObjectCollection;
import net.sf.jmoney.model2.ExtensionPropertySet;
import net.sf.jmoney.model2.IExtensionObjectConstructors;
import net.sf.jmoney.model2.IListGetter;
import net.sf.jmoney.model2.IPropertySetInfo;
import net.sf.jmoney.model2.Session;
import net.sf.jmoney.model2.SessionInfo;
import net.sf.jmoney.model2.ListPropertyAccessor;
import net.sf.jmoney.model2.PropertySet;

/**
 * Provides the metadata for the extra properties added to the session
 * by the Toshl plug-in.
 * 
 * @author Nigel Westbury
 */
public class ToshlSessionInfo implements IPropertySetInfo {

	private static ExtensionPropertySet<ToshlSession,Session> propertySet = PropertySet.addExtensionPropertySet(ToshlSession.class, SessionInfo.getPropertySet(), new IExtensionObjectConstructors<ToshlSession,Session>() {

		public ToshlSession construct(Session extendedObject) {
			return new ToshlSession(extendedObject);
		}

		public ToshlSession construct(Session extendedObject, IValues<Session> values) {
			return new ToshlSession(
					extendedObject, 
					values.getListManager(extendedObject.getObjectKey(), getPatternsAccessor()) 
			);
		}
	});
	
	private static ListPropertyAccessor<ToshlAccount,Session> patternsAccessor = null;
	
	public PropertySet<ToshlSession,Session> registerProperties() {
	
		IListGetter<ToshlSession, ToshlAccount> patternListGetter = new IListGetter<ToshlSession, ToshlAccount>() {
			@Override
			public ObjectCollection<ToshlAccount> getList(ToshlSession parentObject) {
				return parentObject.getToshlAccountCollection();
			}
		};
	
		patternsAccessor = propertySet.addPropertyList("patterns", "Toshl Accounts", ToshlAccountInfo.getPropertySet(), patternListGetter);
		
		return propertySet;
	}

	/**
	 * @return
	 */
	public static ExtensionPropertySet<ToshlSession,Session> getPropertySet() {
		return propertySet;
	}

	/**
	 * @return
	 */
	public static ListPropertyAccessor<ToshlAccount,Session> getPatternsAccessor() {
		return patternsAccessor;
	}	
}
