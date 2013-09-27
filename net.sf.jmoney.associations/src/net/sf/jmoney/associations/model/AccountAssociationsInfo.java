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

package net.sf.jmoney.associations.model;

import net.sf.jmoney.isolation.IValues;
import net.sf.jmoney.isolation.ObjectCollection;
import net.sf.jmoney.model2.CapitalAccount;
import net.sf.jmoney.model2.CapitalAccountInfo;
import net.sf.jmoney.model2.ExtensionPropertySet;
import net.sf.jmoney.model2.IExtensionObjectConstructors;
import net.sf.jmoney.model2.IListGetter;
import net.sf.jmoney.model2.IPropertySetInfo;
import net.sf.jmoney.model2.ListPropertyAccessor;
import net.sf.jmoney.model2.PropertySet;

/**
 * Provides the metadata for the extra properties added to each
 * currency account by this plug-in.
 *
 * @author Nigel Westbury
 */
public class AccountAssociationsInfo implements IPropertySetInfo {

	private static ExtensionPropertySet<AccountAssociationsExtension,CapitalAccount> propertySet = PropertySet.addExtensionPropertySet(AccountAssociationsExtension.class, CapitalAccountInfo.getPropertySet(), new IExtensionObjectConstructors<AccountAssociationsExtension,CapitalAccount>() {

		@Override
		public AccountAssociationsExtension construct(CapitalAccount extendedObject) {
			return new AccountAssociationsExtension(extendedObject);
		}

		@Override
		public AccountAssociationsExtension construct(CapitalAccount extendedObject, IValues<CapitalAccount> values) {
			return new AccountAssociationsExtension(
					extendedObject,
					values.getListManager(extendedObject.getObjectKey(), getAssociationsAccessor())
			);
		}
	});

	private static ListPropertyAccessor<AccountAssociation,CapitalAccount> associationsAccessor = null;

	@Override
	public PropertySet<AccountAssociationsExtension,CapitalAccount> registerProperties() {

		IListGetter<AccountAssociationsExtension, AccountAssociation> associationListGetter = new IListGetter<AccountAssociationsExtension, AccountAssociation>() {
			@Override
			public ObjectCollection<AccountAssociation> getList(AccountAssociationsExtension parentObject) {
				return parentObject.getAssociationCollection();
			}
		};

		associationsAccessor = propertySet.addPropertyList("associations", "Associations", AccountAssociationInfo.getPropertySet(), associationListGetter);

		return propertySet;
	}

	/**
	 * @return
	 */
	public static ExtensionPropertySet<AccountAssociationsExtension,CapitalAccount> getPropertySet() {
		return propertySet;
	}

	/**
	 * @return
	 */
	public static ListPropertyAccessor<AccountAssociation,CapitalAccount> getAssociationsAccessor() {
		return associationsAccessor;
	}
}
