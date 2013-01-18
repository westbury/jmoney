/*
 *
 *  JMoney - A Personal Finance Manager
 *  Copyright (c) 2002 Johann Gyger <johann.gyger@switzerland.org>
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

import net.sf.jmoney.isolation.IListManager;
import net.sf.jmoney.isolation.IObjectKey;
import net.sf.jmoney.isolation.IValues;
import net.sf.jmoney.isolation.ListKey;
import net.sf.jmoney.model2.BankAccount;
import net.sf.jmoney.model2.CapitalAccount;
import net.sf.jmoney.model2.CurrencyAccount;
import net.sf.jmoney.model2.IncomeExpenseAccount;
import net.sf.jmoney.paypal.resources.Messages;

import org.eclipse.osgi.util.NLS;

/**
 * The data model for an bank account.
 */
public class PaypalAccount extends CurrencyAccount {

	protected IObjectKey transferBankAccountKey;

	protected IObjectKey transferCreditCardAccountKey;

	protected IObjectKey saleAndPurchaseAccountKey;

	protected IObjectKey paypalFeesAccountKey;

	protected IObjectKey donationAccountKey;

	/**
	 * The full constructor for a PaypalAccount object.  This constructor is called
	 * only by the datastore when loading data from the datastore.  The properties
	 * passed to this constructor must be valid because datastores should only pass back
	 * values that were previously saved from a CapitalAccount object.  So, for example,
	 * we can be sure that a non-null name and currency are passed to this constructor.
	 * 
	 * @param name the name of the account
	 */
	public PaypalAccount(
			IObjectKey objectKey, 
			ListKey<? super PaypalAccount,?> parent,
			String name,
			IListManager<CapitalAccount> subAccounts,
			String abbreviation,
			String comment,
			IObjectKey currencyKey,
			long startBalance,
			IObjectKey transferBankAccountKey,
			IObjectKey transferCreditCardAccountKey,
			IObjectKey saleAndPurchaseAccountKey,
			IObjectKey paypalFeesAccountKey,
			IObjectKey donationAccountKey,
			IValues<PaypalAccount> extensionValues) { 
		super(objectKey, parent, name, subAccounts, abbreviation, comment, currencyKey, startBalance, extensionValues);
		
        this.transferBankAccountKey = transferBankAccountKey;
        this.transferCreditCardAccountKey = transferCreditCardAccountKey;
        this.saleAndPurchaseAccountKey = saleAndPurchaseAccountKey;
        this.paypalFeesAccountKey = paypalFeesAccountKey;
        this.donationAccountKey = donationAccountKey;
	}

	/**
	 * The default constructor for a BankAccount object.  This constructor is called
	 * when a new BankAccount object is created.  The properties are set to default
	 * values.  The list properties are set to empty lists.  The parameter list for this
	 * constructor is the same as the full constructor except that there are no parameters
	 * for the scalar properties.
	 */
	public PaypalAccount(
			IObjectKey objectKey, 
			ListKey<? super PaypalAccount,?> parent) { 
		super(objectKey, parent);
		
		// Overwrite the default name with our own default name.
		this.name = NLS.bind(Messages.PaypalAccount_NewAccount, null);
		
        this.transferBankAccountKey = null;
        this.transferCreditCardAccountKey = null;
        this.saleAndPurchaseAccountKey = null;
        this.paypalFeesAccountKey = null;
        this.donationAccountKey = null;
	}

	// TODO: remove this.  If we could get the property set, typed
	// with the correct type as the generic parameter, then that would
	// be great.  Otherwise this method is no use because we can get
	// the property set from the map.
    @Override	
	protected String getExtendablePropertySetId() {
		return "net.sf.jmoney.bankAccount";
	}

	public BankAccount getTransferBank() {
        return transferBankAccountKey == null
        ? null
        		: (BankAccount)transferBankAccountKey.getObject();
	}

	public void setTransferBank(BankAccount transferBankAccount) {
		BankAccount oldAccount = getTransferBank();
		transferBankAccountKey = (transferBankAccount == null) ? null : transferBankAccount.getObjectKey();

		// Notify the change manager.
		processPropertyChange(PaypalAccountInfo.getTransferBankAccountAccessor(), oldAccount, transferBankAccount);
	}

	public BankAccount getTransferCreditCard() {
        return transferCreditCardAccountKey == null
        ? null
        		: (BankAccount)transferCreditCardAccountKey.getObject();
	}

	public void setTransferCreditCard(BankAccount transferCreditCardAccount) {
		BankAccount oldAccount = getTransferCreditCard();
		transferCreditCardAccountKey = (transferCreditCardAccount == null) ? null : transferCreditCardAccount.getObjectKey();

		// Notify the change manager.
		processPropertyChange(PaypalAccountInfo.getTransferCreditCardAccountAccessor(), oldAccount, transferCreditCardAccount);
	}

	public IncomeExpenseAccount getSaleAndPurchaseAccount() {
        return saleAndPurchaseAccountKey == null
        ? null
        		: (IncomeExpenseAccount)saleAndPurchaseAccountKey.getObject();
	}

	public void setSaleAndPurchaseAccount(IncomeExpenseAccount saleAndPurchaseAccount) {
		IncomeExpenseAccount oldAccount = getSaleAndPurchaseAccount();
		saleAndPurchaseAccountKey = (saleAndPurchaseAccount == null) ? null : saleAndPurchaseAccount.getObjectKey();

		// Notify the change manager.
		processPropertyChange(PaypalAccountInfo.getSaleAndPurchaseAccountAccessor(), oldAccount, saleAndPurchaseAccount);
	}

	public IncomeExpenseAccount getPaypalFeesAccount() {
        return paypalFeesAccountKey == null
        ? null
        		: (IncomeExpenseAccount)paypalFeesAccountKey.getObject();
	}

	public void setPaypalFeesAccount(IncomeExpenseAccount paypalFeesAccount) {
		IncomeExpenseAccount oldAccount = getPaypalFeesAccount();
		paypalFeesAccountKey = (paypalFeesAccount == null) ? null : paypalFeesAccount.getObjectKey();

		// Notify the change manager.
		processPropertyChange(PaypalAccountInfo.getPaypalFeesAccountAccessor(), oldAccount, paypalFeesAccount);
	}

	public IncomeExpenseAccount getDonationAccount() {
        return donationAccountKey == null
        ? null
        		: (IncomeExpenseAccount)donationAccountKey.getObject();
	}

	public void setDonationAccount(IncomeExpenseAccount donationAccount) {
		IncomeExpenseAccount oldAccount = getDonationAccount();
		donationAccountKey = (donationAccount == null) ? null : donationAccount.getObjectKey();

		// Notify the change manager.
		processPropertyChange(PaypalAccountInfo.getDonationAccountAccessor(), oldAccount, donationAccount);
	}
}
