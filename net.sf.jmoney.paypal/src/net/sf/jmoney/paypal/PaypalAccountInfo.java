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

package net.sf.jmoney.paypal;

import net.sf.jmoney.fields.AccountControlFactory;
import net.sf.jmoney.isolation.IObjectKey;
import net.sf.jmoney.isolation.IValues;
import net.sf.jmoney.isolation.ListKey;
import net.sf.jmoney.model2.AccountInfo;
import net.sf.jmoney.model2.BankAccount;
import net.sf.jmoney.model2.CapitalAccountInfo;
import net.sf.jmoney.model2.CurrencyAccountInfo;
import net.sf.jmoney.model2.ExtendablePropertySet;
import net.sf.jmoney.model2.IExtendableObjectConstructors;
import net.sf.jmoney.model2.IPropertySetInfo;
import net.sf.jmoney.model2.IReferenceControlFactory;
import net.sf.jmoney.model2.IncomeExpenseAccount;
import net.sf.jmoney.model2.PropertySet;
import net.sf.jmoney.model2.ReferencePropertyAccessor;
import net.sf.jmoney.paypal.resources.Messages;

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
public class PaypalAccountInfo implements IPropertySetInfo {

	private static ExtendablePropertySet<PaypalAccount> propertySet = PropertySet.addDerivedFinalPropertySet(PaypalAccount.class, Messages.PaypalAccountInfo_ObjectDescription, CurrencyAccountInfo.getPropertySet(), new IExtendableObjectConstructors<PaypalAccount>() {

		@Override
		public PaypalAccount construct(IObjectKey objectKey, ListKey<? super PaypalAccount,?> parentKey) {
			return new PaypalAccount(
					objectKey, 
					parentKey
			);
		}

		@Override
		public PaypalAccount construct(IObjectKey objectKey,
				ListKey<? super PaypalAccount,?> parentKey, IValues<PaypalAccount> values) {
			return new PaypalAccount(
					objectKey, 
					parentKey, 
					values.getScalarValue(AccountInfo.getNameAccessor()),
					values.getListManager(objectKey, CapitalAccountInfo.getSubAccountAccessor()),
					values.getScalarValue(CapitalAccountInfo.getAbbreviationAccessor()),
					values.getScalarValue(CapitalAccountInfo.getCommentAccessor()),
					values.getReferencedObjectKey(CurrencyAccountInfo.getCurrencyAccessor()),
					values.getScalarValue(CurrencyAccountInfo.getStartBalanceAccessor()),
					values.getReferencedObjectKey(PaypalAccountInfo.getTransferBankAccountAccessor()),
					values.getReferencedObjectKey(PaypalAccountInfo.getTransferCreditCardAccountAccessor()),
					values.getReferencedObjectKey(PaypalAccountInfo.getSaleAndPurchaseAccountAccessor()),
					values.getReferencedObjectKey(PaypalAccountInfo.getPaypalFeesAccountAccessor()),
					values.getReferencedObjectKey(PaypalAccountInfo.getDonationAccountAccessor()),
					values
			);
		}
	});
	
	private static ReferencePropertyAccessor<BankAccount,PaypalAccount> transferBankAccountAccessor = null;
	private static ReferencePropertyAccessor<BankAccount,PaypalAccount> transferCreditCardAccountAccessor = null;
	private static ReferencePropertyAccessor<IncomeExpenseAccount,PaypalAccount> saleAndPurchaseAccountAccessor = null;
	private static ReferencePropertyAccessor<IncomeExpenseAccount,PaypalAccount> paypalFeesAccountAccessor = null;
	private static ReferencePropertyAccessor<IncomeExpenseAccount,PaypalAccount> donationAccountAccessor = null;

    @Override
	public ExtendablePropertySet<PaypalAccount> registerProperties() {
    	
        IReferenceControlFactory<PaypalAccount,PaypalAccount,BankAccount> bankAccountControlFactory = new AccountControlFactory<PaypalAccount,PaypalAccount,BankAccount>() {
			@Override
			public IObjectKey getObjectKey(PaypalAccount parentObject) {
				return parentObject.transferBankAccountKey;
			}
		};
		
        IReferenceControlFactory<PaypalAccount,PaypalAccount,BankAccount> creditCardAccountControlFactory = new AccountControlFactory<PaypalAccount,PaypalAccount,BankAccount>() {
			@Override
			public IObjectKey getObjectKey(PaypalAccount parentObject) {
				return parentObject.transferCreditCardAccountKey;
			}
		};
		
        IReferenceControlFactory<PaypalAccount,PaypalAccount,IncomeExpenseAccount> saleAndPurchaseAccountControlFactory = new AccountControlFactory<PaypalAccount,PaypalAccount,IncomeExpenseAccount>() {
			@Override
			public IObjectKey getObjectKey(PaypalAccount parentObject) {
				return parentObject.saleAndPurchaseAccountKey;
			}
		};
		
        IReferenceControlFactory<PaypalAccount,PaypalAccount,IncomeExpenseAccount> paypalFeesAccountControlFactory = new AccountControlFactory<PaypalAccount,PaypalAccount,IncomeExpenseAccount>() {
			@Override
			public IObjectKey getObjectKey(PaypalAccount parentObject) {
				return parentObject.paypalFeesAccountKey;
			}
		};
		
        IReferenceControlFactory<PaypalAccount,PaypalAccount,IncomeExpenseAccount> donationAccountControlFactory = new AccountControlFactory<PaypalAccount,PaypalAccount,IncomeExpenseAccount>() {
			@Override
			public IObjectKey getObjectKey(PaypalAccount parentObject) {
				return parentObject.donationAccountKey;
			}
		};
		
		transferBankAccountAccessor       = propertySet.addProperty("transferBank", Messages.PaypalAccountInfo_TransferBankAccount, BankAccount.class, 2, 70,  bankAccountControlFactory, null); //$NON-NLS-1$
		transferCreditCardAccountAccessor = propertySet.addProperty("transferCreditCard", Messages.PaypalAccountInfo_TransferCreditCardAccount, BankAccount.class, 2, 70,  creditCardAccountControlFactory, null); //$NON-NLS-1$
		saleAndPurchaseAccountAccessor    = propertySet.addProperty("saleAndPurchaseAccount", Messages.PaypalAccountInfo_SaleAndPurchaseAccount, IncomeExpenseAccount.class, 2, 70,  saleAndPurchaseAccountControlFactory, null); //$NON-NLS-1$
		paypalFeesAccountAccessor         = propertySet.addProperty("paypalFeesAccount", Messages.PaypalAccountInfo_PaypalFeesAccount, IncomeExpenseAccount.class, 2, 70,  paypalFeesAccountControlFactory, null); //$NON-NLS-1$
		donationAccountAccessor           = propertySet.addProperty("donationAccount", Messages.PaypalAccountInfo_DonationAccount, IncomeExpenseAccount.class, 2, 70,  donationAccountControlFactory, null); //$NON-NLS-1$
		
		return propertySet;
	}

	/**
	 * @return
	 */
	public static ExtendablePropertySet<PaypalAccount> getPropertySet() {
		return propertySet;
	}

	public static ReferencePropertyAccessor<BankAccount,PaypalAccount> getTransferBankAccountAccessor() {
		return transferBankAccountAccessor;
	}

	public static ReferencePropertyAccessor<BankAccount,PaypalAccount> getTransferCreditCardAccountAccessor() {
		return transferCreditCardAccountAccessor;
	}

	public static ReferencePropertyAccessor<IncomeExpenseAccount,PaypalAccount> getSaleAndPurchaseAccountAccessor() {
		return saleAndPurchaseAccountAccessor;
	}

	public static ReferencePropertyAccessor<IncomeExpenseAccount,PaypalAccount> getPaypalFeesAccountAccessor() {
		return paypalFeesAccountAccessor;
	}	

	public static ReferencePropertyAccessor<IncomeExpenseAccount,PaypalAccount> getDonationAccountAccessor() {
		return donationAccountAccessor;
	}	
}
