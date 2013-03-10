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

import net.sf.jmoney.fields.AccountControlFactory;
import net.sf.jmoney.fields.TextControlFactory;
import net.sf.jmoney.importer.resources.Messages;
import net.sf.jmoney.isolation.IObjectKey;
import net.sf.jmoney.isolation.IValues;
import net.sf.jmoney.isolation.ListKey;
import net.sf.jmoney.model2.Account;
import net.sf.jmoney.model2.ExtendablePropertySet;
import net.sf.jmoney.model2.IExtendableObjectConstructors;
import net.sf.jmoney.model2.IPropertyControlFactory;
import net.sf.jmoney.model2.IPropertySetInfo;
import net.sf.jmoney.model2.IReferenceControlFactory;
import net.sf.jmoney.model2.PropertySet;
import net.sf.jmoney.model2.ReferencePropertyAccessor;
import net.sf.jmoney.model2.ScalarPropertyAccessor;

import org.eclipse.osgi.util.NLS;

/**
 * This class is a listener class to the net.sf.jmoney.fields
 * extension point.  It implements an extension.
 * <P>
 * This extension registers the Pattern properties.  By registering
 * the properties, every one can know how to display, edit, and store
 * the properties.
 * 
 * @author Nigel Westbury
 */
public class AccountAssociationInfo implements IPropertySetInfo {

	
	private static ExtendablePropertySet<AccountAssociation> propertySet = PropertySet.addBaseFinalPropertySet(AccountAssociation.class, "Account Association", new IExtendableObjectConstructors<AccountAssociation>() {

		@Override
		public AccountAssociation construct(IObjectKey objectKey, ListKey<? super AccountAssociation,?> parentKey) {
			return new AccountAssociation(objectKey, parentKey);
		}

		@Override
		public AccountAssociation construct(IObjectKey objectKey,
				ListKey<? super AccountAssociation,?> parentKey, IValues<AccountAssociation> values) {
			return new AccountAssociation(
					objectKey, 
					parentKey, 
					values.getScalarValue(AccountAssociationInfo.getIdAccessor()),
					values.getReferencedObjectKey(AccountAssociationInfo.getAccountAccessor()),
					values
			);
		}
	});
	
	private static ScalarPropertyAccessor<String,AccountAssociation> idAccessor = null;
	private static ReferencePropertyAccessor<Account,AccountAssociation> accountAccessor = null;

	@Override
	public ExtendablePropertySet<AccountAssociation> registerProperties() {

		IPropertyControlFactory<AccountAssociation,String> textControlFactory = new TextControlFactory<AccountAssociation>();

		IReferenceControlFactory<AccountAssociation,AccountAssociation,Account> accountControlFactory = new AccountControlFactory<AccountAssociation,AccountAssociation,Account>() {
			@Override
			public IObjectKey getObjectKey(AccountAssociation parentObject) {
				return parentObject.accountKey;
			}
		};

		idAccessor      = propertySet.addProperty("id",       NLS.bind(Messages.AccountAssociationInfo_Id, null),     String.class, 2, 100, textControlFactory,    null);
		accountAccessor = propertySet.addProperty("account",  NLS.bind(Messages.AccountAssociationInfo_Account, null), Account.class,2, 70,  accountControlFactory, null);
		
		return propertySet;
	}

	/**
	 * @return
	 */
	public static ExtendablePropertySet<AccountAssociation> getPropertySet() {
		return propertySet;
	}

	/**
	 * @return
	 */
	public static ScalarPropertyAccessor<String,AccountAssociation> getIdAccessor() {
		return idAccessor;
	}	

	/**
	 * @return
	 */
	public static ReferencePropertyAccessor<Account,AccountAssociation> getAccountAccessor() {
		return accountAccessor;
	}	
}
