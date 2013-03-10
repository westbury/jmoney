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

import net.sf.jmoney.fields.AmountInCurrencyAccountControlFactory;
import net.sf.jmoney.fields.TextControlFactory;
import net.sf.jmoney.isolation.IObjectKey;
import net.sf.jmoney.isolation.IValues;
import net.sf.jmoney.isolation.ListKey;
import net.sf.jmoney.resources.Messages;

/**
 * This class is a listener class to the net.sf.jmoney.fields
 * extension point.  It implements an extension.
 * <P>
 * This extension registers the CapitalAccount properties.  By registering
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
public class BankAccountInfo implements IPropertySetInfo {

	private static ExtendablePropertySet<BankAccount> propertySet = PropertySet.addDerivedFinalPropertySet(BankAccount.class, Messages.BankAccountInfo_BankAccount, CurrencyAccountInfo.getPropertySet(), new IExtendableObjectConstructors<BankAccount>() { 

		@Override
		public BankAccount construct(IObjectKey objectKey, ListKey parentKey) {
			return new BankAccount(
					objectKey, 
					parentKey
			);
		}

		@Override
		public BankAccount construct(IObjectKey objectKey,
				ListKey<? super BankAccount,?> parentKey, IValues<BankAccount> values) {
			return new BankAccount(
					objectKey, 
					parentKey, 
					values.getScalarValue(AccountInfo.getNameAccessor()),
					values.getListManager(objectKey, CapitalAccountInfo.getSubAccountAccessor()),
					values.getScalarValue(CapitalAccountInfo.getAbbreviationAccessor()),
					values.getScalarValue(CapitalAccountInfo.getCommentAccessor()),
					values.getReferencedObjectKey(CurrencyAccountInfo.getCurrencyAccessor()),
					values.getScalarValue(CurrencyAccountInfo.getStartBalanceAccessor()),
					values.getScalarValue(BankAccountInfo.getBankAccessor()),
					values.getScalarValue(BankAccountInfo.getAccountNumberAccessor()),
					values.getScalarValue(BankAccountInfo.getMinBalanceAccessor()),
					values
			);
		}
	});
	
	private static ScalarPropertyAccessor<String,BankAccount> bankAccessor = null;
	private static ScalarPropertyAccessor<String,BankAccount> accountNumberAccessor = null;
	private static ScalarPropertyAccessor<Long,BankAccount> minBalanceAccessor = null;

    @Override
	public PropertySet registerProperties() {
		IPropertyControlFactory<BankAccount,String> textControlFactory = new TextControlFactory<BankAccount>();
		IPropertyControlFactory<BankAccount,Long> amountControlFactory = new AmountInCurrencyAccountControlFactory<BankAccount>();
		
		bankAccessor          = propertySet.addProperty("bank", Messages.BankAccountInfo_Bank, String.class, 5, 100, textControlFactory, null); //$NON-NLS-1$ 
		accountNumberAccessor = propertySet.addProperty("accountNumber", Messages.BankAccountInfo_AccountNumber, String.class, 2, 70, textControlFactory, null); //$NON-NLS-1$ 
		minBalanceAccessor    = propertySet.addProperty("minBalance", Messages.BankAccountInfo_MinimalBalance, Long.class, 2, 40, amountControlFactory, null); //$NON-NLS-1$ 
		
		return propertySet;
	}

	/**
	 * @return
	 */
	public static ExtendablePropertySet<BankAccount> getPropertySet() {
		return propertySet;
	}

	/**
	 * @return
	 */
	public static ScalarPropertyAccessor<String,BankAccount> getBankAccessor() {
		return bankAccessor;
	}	

	/**
	 * @return
	 */
	public static ScalarPropertyAccessor<String,BankAccount> getAccountNumberAccessor() {
		return accountNumberAccessor;
	}	

	/**
	 * @return
	 */
	public static ScalarPropertyAccessor<Long,BankAccount> getMinBalanceAccessor() {
		return minBalanceAccessor;
	}	
}
