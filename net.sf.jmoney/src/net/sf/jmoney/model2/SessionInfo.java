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

import net.sf.jmoney.fields.CurrencyControlFactory;
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
public class SessionInfo implements IPropertySetInfo {

	private static ExtendablePropertySet<Session> propertySet = PropertySet.addBaseFinalPropertySet(Session.class, Messages.SessionInfo_Description, new IExtendableObjectConstructors<Session>() {

		@Override
		public Session construct(IObjectKey objectKey, ListKey parentKey) {
			return new Session(objectKey, parentKey);
		}

		@Override
		public Session construct(IObjectKey objectKey,
				ListKey<? super Session,?> parentKey, IValues<Session> values) {
			return new Session(
					objectKey, 
					parentKey, 
					values.getListManager(objectKey, SessionInfo.getCommoditiesAccessor()),
					values.getListManager(objectKey, SessionInfo.getAccountsAccessor()),
					values.getListManager(objectKey, SessionInfo.getTransactionsAccessor()),
					values.getReferencedObjectKey(SessionInfo.getDefaultCurrencyAccessor()),
					values
			);
		}
	});

	
	private static ListPropertyAccessor<Commodity,Session> commoditiesAccessor = null;
	private static ListPropertyAccessor<Account,Session> accountsAccessor = null;
	private static ListPropertyAccessor<Transaction,Session> transactionsAccessor = null;
	private static ReferencePropertyAccessor<Currency,Session> defaultCurrencyAccessor = null;

	@Override
	public PropertySet registerProperties() {
		IListGetter<Session, Commodity> commodityGetter = new IListGetter<Session, Commodity>() {
			@Override
			public ObjectCollection<Commodity> getList(Session parentObject) {
				return parentObject.getCommodityCollection();
			}
		};
		
		IListGetter<Session, Account> accountGetter = new IListGetter<Session, Account>() {
			@Override
			public ObjectCollection<Account> getList(Session parentObject) {
				return parentObject.getAccountCollection();
			}
		};
		
		IListGetter<Session, Transaction> transactionGetter = new IListGetter<Session, Transaction>() {
			@Override
			public ObjectCollection<Transaction> getList(Session parentObject) {
				return parentObject.getTransactionCollection();
			}
		};
		
		IReferenceControlFactory<Session,Currency> currencyControlFactory = new CurrencyControlFactory<Session>() {
			@Override
			public IObjectKey getObjectKey(Session parentObject) {
				return parentObject.defaultCurrencyKey;
			}
		};

		commoditiesAccessor = propertySet.addPropertyList("commodity", Messages.SessionInfo_Commodity, CommodityInfo.getPropertySet(), commodityGetter); //$NON-NLS-1$
		accountsAccessor = propertySet.addPropertyList("account", Messages.SessionInfo_Account, AccountInfo.getPropertySet(), accountGetter); //$NON-NLS-1$
		transactionsAccessor = propertySet.addPropertyList("transaction", Messages.SessionInfo_Transaction, TransactionInfo.getPropertySet(), transactionGetter); //$NON-NLS-1$
		
		defaultCurrencyAccessor = propertySet.addProperty("defaultCurrency", Messages.SessionInfo_DefaultCurrency, Currency.class, 2, 20, currencyControlFactory, null); //$NON-NLS-1$
		
		return propertySet;
	}

	/**
	 * @return
	 */
	public static ExtendablePropertySet<Session> getPropertySet() {
		return propertySet;
	}

	/**
	 * @return
	 */
	public static ListPropertyAccessor<Commodity,Session> getCommoditiesAccessor() {
		return commoditiesAccessor;
	}	

	/**
	 * @return
	 */
	public static ListPropertyAccessor<Account,Session> getAccountsAccessor() {
		return accountsAccessor;
	}	

	/**
	 * @return
	 */
	public static ListPropertyAccessor<Transaction,Session> getTransactionsAccessor() {
		return transactionsAccessor;
	}	

	/**
	 * @return
	 */
	public static ReferencePropertyAccessor<Currency,Session> getDefaultCurrencyAccessor() {
		return defaultCurrencyAccessor;
	}	
}
