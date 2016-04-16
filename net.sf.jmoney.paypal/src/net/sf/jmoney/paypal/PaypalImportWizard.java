/*
 *
 *  JMoney - A Personal Finance Manager
 *  Copyright (c) 2004,2009 Nigel Westbury <westbury@users.sourceforge.net>
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

import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import net.sf.jmoney.associations.AssociationMetadata;
import net.sf.jmoney.importer.MatchingEntryFinder;
import net.sf.jmoney.importer.matcher.EntryData;
import net.sf.jmoney.importer.wizards.CsvImportToAccountWizard;
import net.sf.jmoney.importer.wizards.CsvTransactionReader;
import net.sf.jmoney.importer.wizards.ImportException;
import net.sf.jmoney.importer.wizards.MultiRowTransaction;
import net.sf.jmoney.model2.Account;
import net.sf.jmoney.model2.BankAccount;
import net.sf.jmoney.model2.CapitalAccount;
import net.sf.jmoney.model2.Currency;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.IDatastoreManager;
import net.sf.jmoney.model2.IncomeExpenseAccount;
import net.sf.jmoney.model2.Session;
import net.sf.jmoney.model2.Session.NoAccountFoundException;
import net.sf.jmoney.model2.Session.SeveralAccountsFoundException;
import net.sf.jmoney.model2.Transaction;
import net.sf.jmoney.reconciliation.ReconciliationEntryInfo;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWizard;

/**
 * A wizard to import data from a comma-separated file that has been downloaded
 * from Paypal.
 */
public class PaypalImportWizard extends CsvImportToAccountWizard implements IWorkbenchWizard {

	
	private ImportedDateColumn   column_date                = new ImportedDateColumn("Date", new SimpleDateFormat("M/d/yyyy"));
	private ImportedTextColumn   column_payeeName           = new ImportedTextColumn("Name");
	private ImportedTextColumn   column_type                = new ImportedTextColumn("Type");
	private ImportedTextColumn   column_status              = new ImportedTextColumn("Status");
	private ImportedTextColumn   column_currency            = new ImportedTextColumn("Currency");
	private ImportedAmountColumn column_grossAmount         = new ImportedAmountColumn("Gross");
	private ImportedAmountColumn column_fee                 = new ImportedAmountColumn("Fee");
	private ImportedAmountColumn column_netAmount           = new ImportedAmountColumn("Net");
	private ImportedTextColumn   column_payerEmail          = new ImportedTextColumn("From Email Address");
	private ImportedTextColumn   column_payeeEmail          = new ImportedTextColumn("To Email Address");
	private ImportedTextColumn   column_transactionId       = new ImportedTextColumn("Transaction ID");
	private ImportedTextColumn   column_memo                = new ImportedTextColumn("Item Title");
	private ImportedAmountColumn column_shippingAndHandling = new ImportedAmountColumn("Shipping and Handling Amount");
	private ImportedAmountColumn column_insurance           = new ImportedAmountColumn("Insurance Amount");
	private ImportedAmountColumn column_salesTax            = new ImportedAmountColumn("Sales Tax");
	private ImportedTextColumn   column_itemUrl             = new ImportedTextColumn("Item URL");
	private ImportedTextColumn   column_quantity            = new ImportedTextColumn("Quantity");
	private ImportedAmountColumn column_balance             = new ImportedAmountColumn("Balance");

	/**
	 * Account inside transaction
	 */
	private PaypalAccount paypalAccount;


	Collection<RefundRow> refunds = new ArrayList<RefundRow>();
	Collection<ReversalRow> reversals = new ArrayList<ReversalRow>();

	/**
	 * Currency of the Paypal account, being the currency in which
	 * amounts are to be formatted when put in the memo
	 */
	Currency currency;
	
	private Session session;

	public PaypalImportWizard() {
		// TODO check these dialog settings are used by the base class
		// so the default filename location is separate for each import type.
		IDialogSettings workbenchSettings = Activator.getDefault().getDialogSettings();
		IDialogSettings section = workbenchSettings.getSection("PaypalImportWizard");//$NON-NLS-1$
		if (section == null) {
			section = workbenchSettings.addNewSection("PaypalImportWizard");//$NON-NLS-1$
		}
		setDialogSettings(section);
	}

	/**
	 * This form of this method is called when the wizard is initiated from the
	 * 'import' menu.  No account is available from the context so we search
	 * for a Paypal account.  If there is more than one then we fail (it would
	 * be better to add a page that prompts the user for the Paypal account to
	 * use, or perhaps try to identify the account from the imported file).
	 */
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
		Account account;

		IDatastoreManager sessionManager = (IDatastoreManager)window.getActivePage().getInput();
		
		session = sessionManager.getSession();
		
		if (selection.size() == 1
				&& selection.getFirstElement() instanceof PaypalAccount) {
			account = (PaypalAccount)selection.getFirstElement();
		} else {
			try {
				account = session.getAccountByShortName("Paypal");
			} catch (NoAccountFoundException e) {
				MessageDialog.openError(Display.getDefault().getActiveShell(), "Account not Set Up", "No account exists called 'Paypal'");
				throw new RuntimeException(e); 
			} catch (SeveralAccountsFoundException e) {
				MessageDialog.openError(Display.getDefault().getActiveShell(), "Multiple Accounts Set Up", "Multiple accounts exists called 'Paypal'");
				throw new RuntimeException(e); 
			}
		}

		if (!(account instanceof PaypalAccount)) {
			MessageDialog.openError(Display.getDefault().getActiveShell(), "Account not Set Up", "The account called 'Paypal' must be a Paypal account.");
			throw new RuntimeException("Paypal account not a Paypal account"); 
		}
		
		init(window, account);
	}

	@Override
	protected void setAccount(Account accountInsideTransaction)	throws ImportException {
		if (!(accountInsideTransaction instanceof PaypalAccount)) {
			throw new ImportException("Bad configuration: This import can be used for Paypal accounts only.");
		}

		this.paypalAccount = (PaypalAccount)accountInsideTransaction;
		this.session = accountInsideTransaction.getSession();

		currency = paypalAccount.getCurrency();

//		try {
//			interestAccount = session.getAccountByShortName("Interest - Ameritrade");
//		} catch (NoAccountFoundException e) {
//			MessageDialog.openError(Display.getDefault().getActiveShell(), "Account not Set Up", "No account exists called 'Interest - Ameritrade'");
//			throw new RuntimeException(e);
//		} catch (SeveralAccountsFoundException e) {
//			MessageDialog.openError(Display.getDefault().getActiveShell(), "Multiple Accounts Set Up", "Multiple accounts exists called 'Interest - Ameritrade'");
//			throw new RuntimeException(e);
//		}
	}

	@Override
	public void importLine(CsvTransactionReader reader) throws ImportException {
		String rowType = column_type.getText();

		/*
		 * We are not interested in our own e-mail.  The merchant
		 * e-mail may be either in the 'from' or the 'to' e-mail address
		 * column, depending on the row type.
		 */
		String rowMerchantEmail;
		if (rowType.equals("Refund")
				|| rowType.equals("Reversal")
				|| rowType.equals("Payment Received")
				|| rowType.equals("eBay Payment Received")) {
			rowMerchantEmail = column_payerEmail.getText();
		} else {
			rowMerchantEmail = column_payeeEmail.getText();
		}

		if (rowType.equals("Shopping Cart Payment Sent")) {
			/**
			 * Shopping cart entries are split across multiple rows, with a 'Payment Sent' row
			 * following by one or more 'Item' rows.  These must be combined into a single
			 * transaction.  To enable us to do this, this class is used to put each row into,
			 * and it can then output the transaction when a row is found that is in the
			 * next transaction.
			 */
			MultiRowTransaction thisMultiRowProcessor = new ShoppingCartPaymentSent(column_date.getDate(), column_shippingAndHandling.getAmount());
			currentMultiRowProcessors.add(thisMultiRowProcessor);
		} else if (rowType.equals("Shopping Cart Item")) {
			throw new ImportException("'Shopping Cart Item' row found but it is not preceeded by a 'Shopping Cart Payment Sent', 'Express Checkout Payment Sent', or 'eBay Payment Sent' row.");
		} else if (rowType.equals("Refund")) {
			/*
			 * Refunds are combined with the original transaction.
			 *
			 * Because the input file is in reverse date order, we find
			 * the refund first. We save the refund information in a
			 * collection. Whenever a 'Shopping Cart Payment Sent' or a
			 * 'eBayPaymentSent' or 'Express Checkout Payment Sent' is
			 * found with a status of 'Partially Refunded' or 'Refunded'
			 * and the payee name exactly matches then we add the refund
			 * as another pair of split entries in the same transaction.
			 */
			RefundRow refundRow = new RefundRow();
			refundRow.payeeName = this.column_payeeName.getText();
			refundRow.grossAmount = this.column_grossAmount.getNonNullAmount();
			refundRow.date = this.column_date.getDate();
			refundRow.merchantEmail = this.column_payerEmail.getText();
			refundRow.transactionId = this.column_transactionId.getText();
			refunds.add(refundRow);
		} else if (rowType.equals("Reversal")) {
			/*
			 * Reversals are processed in a similar way to refunds.  We keep
			 * and list and match them to later entries.
			 */
			reversals.add(new ReversalRow());
		} else if (rowType.equals("eBay Payment Sent")
				|| rowType.equals("eBay Payment Received")
				|| rowType.equals("Payment Received")
				|| rowType.equals("Payment Sent")
				|| rowType.equals("Preapproved Payment Sent")
				|| rowType.equals("Web Accept Payment Sent")) {

			if (column_status.getText().equals("Refunded")) {
				/*
				 * Find the refund entry.  We create a single transaction with two entries both
				 * in this Paypal account.
				 */
				RefundRow match = null;
				for (RefundRow refund : refunds) {
					if (refund.payeeName.equals(column_payeeName.getText())
							&& refund.grossAmount == -column_grossAmount.getAmount()) {
						match = refund;
						break;
					}
				}
				if (match == null) {
					throw new ImportException("An entry was found that says it was refunded, but no matching 'Refund' entry was found.");
				}
				refunds.remove(match);

				createRefundTransaction(match);
			} else if (column_status.getText().equals("Reversed")) {
				/*
				 * Find the reversal entry.  We don't create anything if an
				 * entry was reversed.
				 */
				ReversalRow match = null;
				for (ReversalRow reversal : reversals) {
					if (reversal.payeeName.equals(column_payeeName.getText())
							&& reversal.grossAmount == -column_grossAmount.getAmount()) {
						match = reversal;
						break;
					}
				}
				if (match == null) {
					throw new ImportException("An entry was found that says it was reversed, but no matching 'Reversal' entry was found.");
				}
				reversals.remove(match);
			} else if (rowType.equals("eBay Payment Sent")) {
				/*
				 * Rows have been found where there is a row of type 'eBay Payment Sent' followed
				 * by a row of type 'Shopping Cart Item'.  This seems very strange because 'Shopping
				 * Cart Item' rows normally follow a 'Shopping Cart Payment Sent' row.  Furthermore,
				 * the 'Shopping Cart Item' row does not have any data in it of any use except for
				 * a quantity (in known cases always 1, but presumably this would be some other number
				 * if the quantity were not 1).  The 'eBay Payment Sent' row does not have a quantity
				 * so we use the quantity from the 'Shopping Cart Item' row.
				 *
				 * Also, ebay Payment Sent may be followed by a currency exchange.
				 */
				MultiRowTransaction thisMultiRowProcessor = new EbayPaymentSent();
				currentMultiRowProcessors.add(thisMultiRowProcessor);
			} else if (rowType.equals("Payment Sent")) {
				/*
				 * 'Payment Sent' may have a currency conversion but may not have itemized entries,
				 * so use EbayPaymentSent, not ShoppingCartPaymentSent.
				 */
				MultiRowTransaction thisMultiRowProcessor = new EbayPaymentSent();
				currentMultiRowProcessors.add(thisMultiRowProcessor);
			} else {
				Transaction trans = session.createTransaction();
				trans.setDate(column_date.getDate());

				PaypalEntry mainEntry = trans.createEntry().getExtension(PaypalEntryInfo.getPropertySet(), true);
				mainEntry.setAccount(paypalAccount);
				mainEntry.setAmount(column_netAmount.getAmount());
				mainEntry.setMemo("payment - " + column_payeeName.getText());
				mainEntry.setValuta(column_date.getDate());
				mainEntry.setMerchantEmail(rowMerchantEmail);
				ReconciliationEntryInfo.getUniqueIdAccessor().setValue(mainEntry.getBaseObject(), column_transactionId.getText());

				/**
				 * The memo, being set initially to the memo from the input file but may
				 * be modified
				 */
				String rowMemo = column_memo.getText();
				long rowNetAmount = column_netAmount.getAmount();
				long rowGrossAmount = column_grossAmount.getAmount();
				long rowFee = column_fee.getAmount();

				if (column_status.getText().equals("Partially Refunded")) {
					/*
					 * Look for a refunds that match.  Put them in this transaction.
					 * If the transaction is not itemized then we reduce the expense entry
					 * by the amount of the refund.  If the transaction is itemized then we
					 * create a separate entry for the total amount refunded.
					 *
					 * (Though currently we have no cases of itemized transactions here so this
					 * is not supported.  We probably need to merge this with "Shopping Cart Payment Sent"
					 * processing).
					 */

					long refundAmount = 0;

					for (Iterator<RefundRow> iter = refunds.iterator(); iter.hasNext(); ) {
						RefundRow refund = iter.next();
						if (refund.payeeName.equals(column_payeeName.getText())) {
							/*
							 * Create the refund entry in the Paypal account
							 */
							PaypalEntry refundEntry = trans.createEntry().getExtension(PaypalEntryInfo.getPropertySet(), true);
							refundEntry.setAccount(paypalAccount);
							refundEntry.setAmount(refund.grossAmount);
							refundEntry.setMemo("refund - " + refund.payeeName);
							refundEntry.setValuta(refund.date);
							refundEntry.setMerchantEmail(refund.merchantEmail);
							ReconciliationEntryInfo.getUniqueIdAccessor().setValue(refundEntry.getBaseObject(), refund.transactionId);

							refundAmount += refund.grossAmount;

							iter.remove();
						}
					}

					if (-rowNetAmount - refundAmount == column_shippingAndHandling.getAmount()) {
						// All was refunded except s&h, so indicate accordingly in the memo
						rowMemo = rowMemo + " (s&h not refunded after return)";
					} else {
						// Indicate the original amount paid and refund amount in the memo
						rowMemo = rowMemo + " ($" + currency.format(-rowNetAmount) + " less $" + currency.format(refundAmount) + " refunded)";
					}

					// Note that the amounts in the row will be negative, which is
					// why we add the refund amount when it may seem we should deduct
					// the refund amount.
					rowNetAmount += refundAmount;
					rowGrossAmount += refundAmount;
				}

				if (rowFee != 0) {
					// For non-sale transfers, treat the Paypal fee as a bank service
					// charge.  For E-bay sales, absorb in the price or proceeds.

					if (rowType.equals("Payment Received")
							|| rowType.equals("Payment Sent")) {
						if (paypalAccount.getPaypalFeesAccount() == null) {
							throw new ImportException("A Paypal fee has been found in the imported data.  However, no category has been configured in the properties for this Paypal account for such fees.");
						}

						// Note that fee shows up as a negative amount, and we want
						// a positive amount in the category account to be used for the fee.
						Entry feeEntry = trans.createEntry();
						feeEntry.setAccount(paypalAccount.getPaypalFeesAccount());
						feeEntry.setAmount(-column_fee.getAmount());
						feeEntry.setMemo("Paypal");
						// Set fee to zero so it does not appear in the memo
						rowFee = 0L;
						rowNetAmount = rowGrossAmount;
					}
				}

				if (rowMemo.length() == 0) {
					// Certain transactions don't have memos, so we fill one in
					if (rowType.equals("Payment Received")) {
						rowMemo = column_payeeName.getText() + " - gross payment";
					}
					if (rowType.equals("Payment Sent")) {
						rowMemo = column_payeeName.getText() + " - payment";
					}
				}
				
				/*
				 * Shopping cart items have positive amounts in the 'gross amount' field
				 * only, others have negative amounts that are in both the 'gross
				 * amount' and the 'net amount' fields. We want to set a positive amount
				 * in the category. (Though signs may be opposite if a refund or
				 * something).
				 */
				long amount;
				if (column_type.getText().equals("Shopping Cart Item")) {
					
					amount = rowGrossAmount;
					throw new RuntimeException("should never happen");
				} else {
					amount = -rowNetAmount;
				}

				createCategoryEntry(trans, rowMemo, amount, column_shippingAndHandling.getAmount(), column_insurance.getAmount(), column_salesTax.getAmount(), rowFee, column_itemUrl.getText(), column_quantity.getText(), paypalAccount.getSaleAndPurchaseAccount());

				assertValid(trans);
			}
		} else if (rowType.equals("Donation Sent")) {
			if (paypalAccount.getDonationAccount() == null) {
				throw new ImportException("A donation has been found in the imported data.  However, no category was set for donations.  Please go to the Paypal account properties and select a category to be used for donations.");
			}

			// Donations do not have memos set, so the payee name is used as the memo in the
			// expense category entry.
			createTransaction("donation sent", paypalAccount.getDonationAccount(), column_payeeName.getText());
		} else if (rowType.equals("Add Funds from a Bank Account")) {
			if (paypalAccount.getTransferBank() == null) {
				throw new ImportException("A bank account transfer has been found in the imported data.  However, no bank account has been set in the properties for this Paypal account.");
			}
			// TODO: check this is US dollars, or determine correct paypal account for the currency.
			createTransaction("transfer from bank", paypalAccount, paypalAccount.getTransferBank(), "transfer to Paypal");
		} else if (rowType.equals("Update to eCheck Sent")) {
			// Updates do not involve a financial transaction
			// so nothing to import.
		} else if (rowType.equals("Update to eCheck Received")) {
			// Updates do not involve a financial transaction
			// so nothing to import.
		} else if (rowType.equals("Update to Payment Received")) {
			// Updates do not involve a financial transaction
			// so nothing to import.
		} else if (rowType.equals("Update to Add Funds from a Bank Account")) {
			// Updates do not involve a financial transaction
			// so nothing to import.
		} else if (rowType.equals("eCheck Sent")) {
			if (paypalAccount.getSaleAndPurchaseAccount() == null) {
				throw new ImportException("An eCheck entry has been found in the imported data.  However, no sale and purchase account has been set in the properties for this Paypal account.");
			}
			createTransaction("payment by transfer", paypalAccount.getSaleAndPurchaseAccount(), "transfer from Paypal");
		} else if (rowType.equals("Express Checkout Payment Sent")) {
			if (paypalAccount.getSaleAndPurchaseAccount() == null) {
				throw new ImportException("An 'Express Checkout' entry has been found in the imported data.  However, no sale and purchase account has been set in the properties for this Paypal account.");
			}

			if (column_status.getText().equals("Refunded")) {
				/*
				 * Find the refund entry.  We create a single transaction with two entries both
				 * in this Paypal account.
				 */
				RefundRow match = null;
				for (RefundRow refund : refunds) {
					if (refund.payeeName.equals(column_payeeName.getText())
							&& refund.grossAmount == -column_grossAmount.getAmount()) {
						match = refund;
						break;
					}
				}
				if (match == null) {
					throw new ImportException("An entry was found that says it was refunded, but no matching 'Refund' entry was found.");
				}
				refunds.remove(match);

				createRefundTransaction(match);
			} else {
				/**
				 * Shopping cart entries are split across multiple rows, with a 'Payment Sent' row
				 * following by one or more 'Item' rows.  These must be combined into a single
				 * transaction.  To enable us to do this, this class is used to put each row into,
				 * and it can then output the transaction when a row is found that is in the
				 * next transaction.
				 */
				MultiRowTransaction thisMultiRowProcessor = new ShoppingCartPaymentSent(column_date.getDate(), column_shippingAndHandling.getAmount());
				currentMultiRowProcessors.add(thisMultiRowProcessor);
			}
		} else if (rowType.equals("Charge From Credit Card")) {
			String currency = column_currency.getText();
			BankAccount creditCard;
			CapitalAccount paypalAccountForCurrency;
			if (currency.equals("USD")) {
				paypalAccountForCurrency = paypalAccount;
				creditCard = paypalAccount.getTransferCreditCard();
			} else if (currency.equals("GBP")) {
				try {
					paypalAccountForCurrency = (CapitalAccount)session.getAccountByShortName("Paypal (£)");
				} catch (NoAccountFoundException e) {
					MessageDialog.openError(Display.getDefault().getActiveShell(), "Account not Set Up", "No account exists called 'Paypal (£)'");
					throw new RuntimeException(e); 
				} catch (SeveralAccountsFoundException e) {
					MessageDialog.openError(Display.getDefault().getActiveShell(), "Multiple Accounts Set Up", "Multiple accounts exists called 'Paypal (£)'");
					throw new RuntimeException(e); 
				}

				creditCard = (BankAccount)getAssociatedAccount("net.sf.jmoney.paypal.creditcard.GBP");
			} else {
				throw new ImportException("unsupported currency");
			}
			if (creditCard == null) {
				throw new ImportException("A credit card charge has been found in the imported data.  However, no credit card account has been set in the properties for this Paypal account.");
			}
			createTransaction("payment from credit card", paypalAccountForCurrency, creditCard, "transfer to Paypal");
		} else if (rowType.equals("Credit to Credit Card")
				|| rowType.equals("PayPal card confirmation refund")) {
			if (paypalAccount.getTransferCreditCard() == null) {
				throw new ImportException("A credit card refund has been found in the imported data.  However, no credit card account has been set in the properties for this Paypal account.");
			}

			String currency = column_currency.getText();
			BankAccount creditCard;
			CapitalAccount paypalAccountForCurrency;
			if (currency.equals("USD")) {
				paypalAccountForCurrency = paypalAccount;
				creditCard = paypalAccount.getTransferCreditCard();
			} else if (currency.equals("GBP")) {
				try {
					paypalAccountForCurrency = (CapitalAccount)session.getAccountByShortName("Paypal (£)");
				} catch (NoAccountFoundException e) {
					MessageDialog.openError(Display.getDefault().getActiveShell(), "Account not Set Up", "No account exists called 'Paypal (£)'");
					throw new RuntimeException(e); 
				} catch (SeveralAccountsFoundException e) {
					MessageDialog.openError(Display.getDefault().getActiveShell(), "Multiple Accounts Set Up", "Multiple accounts exists called 'Paypal (£)'");
					throw new RuntimeException(e); 
				}

				creditCard = (BankAccount)getAssociatedAccount("net.sf.jmoney.paypal.creditcard.GBP");
			} else {
				throw new ImportException("unsupported currency");
			}
			if (creditCard == null) {
				throw new ImportException("A credit card charge has been found in the imported data.  However, no credit card account has been set in the properties for this Paypal account.");
			}

			
			createTransaction("refund to credit card", paypalAccountForCurrency, creditCard, "refund from Paypal");
		} else {
			throw new ImportException("Entry found with unknown type: '" + rowType + "'.");
		}
	}

	private void assertValid(Transaction trans) {
		long total = 0;
		for (Entry entry : trans.getEntryCollection()) {
			total += entry.getAmount();
		}
		if (total != 0) {
			System.out.println("unbalanced");
		}
		assert(total == 0);
	}

	/**
	 * The gross and net amounts differ only by the fee.  This method will
	 * absorb the fee into the proceeds (i.e. the amount shown in the accounts
	 * that the item was sold for will be reduced by the fee).  If this is not
	 * an item sale but a funds transfer then the fee is not absorbed.  It is
	 * accounted for as a separate split entry in the transaction.  In that case
	 * the caller will have zeroed out the fee and set the net amount to be the same
	 * as the gross amount.
	 *
	 * @param trans
	 * @param rowItem
	 * @param account
	 * @throws ImportException
	 */
	private void createCategoryEntry(Transaction trans, String memo, long amount, Long shippingAndHandling, Long insurance, Long salesTax, Long fee, String url, String quantityText, IncomeExpenseAccount account) {
		PaypalEntry lineItemEntry = trans.createEntry().getExtension(PaypalEntryInfo.getPropertySet(), true);

		/*
		 * Pattern matching will be used to select the income and expense account.
		 * However we do set the account.  This overrides the default account from
		 * the pattern matching but specific pattern matches will override this account.
		 * (But with a check that the account from the specific pattern matching
		 * does not have a different currency).
		 * 
		 * We add this entry to the list.
		 */
		lineItemEntry.setAccount(account);
		
		EntryData entryData = new EntryData();
		entryData.setEntry(lineItemEntry.getBaseObject());
		addEntryToBeProcessed(entryData);
		
		lineItemEntry.setAmount(amount);

		if (url.length() != 0) {
			try {
				URL itemUrl = new URL(url);
				lineItemEntry.setItemUrl(itemUrl);
			} catch (MalformedURLException e) {
				// Leave the URL blank
			}
		}

		StringBuffer adjustmentsBuffer = new StringBuffer();

		Currency currency = paypalAccount.getCurrency();
		String separator = "";
		long baseAmount = lineItemEntry.getAmount();
		String ourMemo = memo;

		if (quantityText.trim().length() != 0
				&& !quantityText.trim().equals("0")
				&& !quantityText.trim().equals("1")) {
			ourMemo = ourMemo + " x" + quantityText.trim();
		}

		if (shippingAndHandling != null && shippingAndHandling.longValue() != 0) {
			adjustmentsBuffer.append("s&h $")
			.append(currency.format(shippingAndHandling));
			separator = ", ";
			baseAmount -= shippingAndHandling;
		}
		if (insurance != null && insurance.longValue() != 0) {
			adjustmentsBuffer.append(separator)
			.append("insurance $")
			.append(currency.format(insurance));
			separator = ", ";
			baseAmount -= insurance;
		}
		if (salesTax != null && salesTax.longValue() != 0) {
			adjustmentsBuffer.append(separator)
			.append("tax $")
			.append(currency.format(salesTax));
			separator = ", ";
			baseAmount -= salesTax;
		}
		if (fee != null && fee.longValue() != 0) {
			adjustmentsBuffer.append(separator)
			.append("less Paypal fee $")
			.append(currency.format(fee));
			separator = ", ";
			baseAmount -= fee;
		}

		if (adjustmentsBuffer.length() == 0) {
			lineItemEntry.setMemo(ourMemo);
		} else {
			lineItemEntry.setMemo(ourMemo + " ($" + currency.format(baseAmount) + " + " + adjustmentsBuffer.toString() + ")");
		}
	}

	/**
	 * We distribute the shipping and handling among the items in proportion
	 * to the price of each item.  This is the preference of the author.
	 * If this is not your preference then please add a preference to the preferences
	 * to indicate if a separate line item should instead be created for the
	 * shipping and handling and implement it.
	 * 
	 * @throws ImportException
	 */
	private void distribute(long toDistribute, List<ShoppingCartRow> rowItems) throws ImportException {
		long netTotal = 0;
		for (ShoppingCartRow rowItem : rowItems) {
			if (rowItem.grossAmount <= 0) {
				throw new ImportException("Shopping Cart Item with zero or negative gross amount");
			}
			netTotal += rowItem.grossAmount;
		}

		long leftToDistribute = toDistribute;

		for (ShoppingCartRow rowItem : rowItems) {
			long amount = toDistribute * rowItem.grossAmount / netTotal;
			rowItem.shippingAndHandling = amount;
			leftToDistribute -= amount;
		}

		// We have rounded down, so we may be under.  We now distribute
		// a penny to each to get a balanced transaction.
		for (ShoppingCartRow rowItem : rowItems) {
			if (leftToDistribute > 0) {
				rowItem.shippingAndHandling++;
				leftToDistribute--;
			}
		}

		assert(leftToDistribute == 0);

		/*
		 * normally both the gross and net amounts have the s&h included. The
		 * itemized rows don't, and they have just the amount as a positive
		 * value in the 'gross amount' field (nothing in the 'net amount' field)
		 * so to make it consistent we adjust these amounts (which are positive
		 * amounts for normal sales) by the s&h amount.
		 */
		for (ShoppingCartRow rowItem : rowItems) {
			rowItem.grossAmount += rowItem.shippingAndHandling;
		}
	}

	/**
	 * This version will match against existing entries in the charge account and
	 * reconcile them.
	 * 
	 * Not sure if this is really necessary because the import for the other account
	 * should have put an entry into the Paypal account.  This does however mean that
	 * we don't need a Paypal account for each currency to hold entries that have been
	 * imported from the charge account but not yet imported from Paypal.
	 * 
	 * @param paypalAccountMemo
	 * @param otherAccount
	 * @param otherAccountMemo
	 * @throws ImportException
	 */
	private void createTransaction(String paypalAccountMemo, final CapitalAccount paypalAccountForCurrency, CapitalAccount otherAccount, String otherAccountMemo) throws ImportException {
		/*
		 * Auto-match the new entry in the charge account the same way that any other
		 * entry would be auto-matched.  This combines the entry if the entry already exists in the
		 * charge account (typically because transactions have been downloaded from the bank and imported).
		 *
		 * An entry in the charge account has already been matched to a
		 * Paypal payment if it has a transaction id set.  This matcher will not return
		 * entries that have already been matched.
		 *
		 * Although we have already eliminated orders that have already been imported,
		 * this test ensures we don't mess up when more than one order can match to the
		 * same debit in the charge account.  This is not likely but two orders of the same
		 * amount and the same or very close dates may cause this.
		 *
		 * Note that we search two days ahead for a matching entry in the charge account.
		 * Although we know the date Paypal charged the amount, it can sometimes appear up to
		 * two days later in the charge account.
		 */
		MatchingEntryFinder matchFinder = new MatchingEntryFinder() {
			@Override
			protected boolean doNotConsiderEntryForMatch(Entry entry) {
				if (entry.getOtherAccount() == paypalAccountForCurrency
						&& ReconciliationEntryInfo.getUniqueIdAccessor().getValue(entry.getTransaction().getOther(entry)) != null) {
						return true;
				} else {
					return false;
				}
			}
		};
		Entry matchedEntryInChargeAccount = matchFinder.findMatch(otherAccount, -column_grossAmount.getAmount(), column_date.getDate(), 2, null);

		/*
		 * Create an entry for the amount charged to the charge account.
		 * 
		 * Note that if a match is found but that entry has split entries then we don't
		 * merge the transactions.  We leave two transactions that must be manually merged.
		 * This is just because it is too hard otherwise to ensure we don't lose data. 
		 */
		Transaction trans;
		if (matchedEntryInChargeAccount == null
				|| matchedEntryInChargeAccount.getTransaction().hasMoreThanTwoEntries()) {
			
			if (matchedEntryInChargeAccount != null && matchedEntryInChargeAccount.getTransaction().hasMoreThanTwoEntries()) {
				MessageDialog.openInformation(getShell(), "Problem Transaction", "For amount " + (-column_grossAmount.getAmount()) + ", transaction already split so you must manually merge.");
			}

			trans = session.createTransaction();
			trans.setDate(column_date.getDate());

			Entry otherEntry = trans.createEntry();
			otherEntry.setAccount(otherAccount);
			otherEntry.setAmount(-column_grossAmount.getAmount());
			otherEntry.setMemo(otherAccountMemo);
		} else {
			trans = matchedEntryInChargeAccount.getTransaction();
			matchedEntryInChargeAccount.setMemo(otherAccountMemo);
			
			Entry otherMatchedEntry = matchedEntryInChargeAccount.getTransaction().getOther(matchedEntryInChargeAccount);
			// Any checks on the other entry before we delete it?
			matchedEntryInChargeAccount.getTransaction().deleteEntry(otherMatchedEntry);
		}

		Entry mainEntry = trans.createEntry();
		mainEntry.setAccount(paypalAccountForCurrency);
		mainEntry.setAmount(column_grossAmount.getAmount());
		mainEntry.setMemo(paypalAccountMemo);
		mainEntry.setValuta(column_date.getDate());
		ReconciliationEntryInfo.getUniqueIdAccessor().setValue(mainEntry, column_transactionId.getText());
	}

	private void createTransaction(String paypalAccountMemo, IncomeExpenseAccount otherAccount, String otherAccountMemo) throws ImportException {
		Transaction trans = session.createTransaction();
		trans.setDate(column_date.getDate());

		Entry mainEntry = trans.createEntry();
		mainEntry.setAccount(paypalAccount);
		mainEntry.setAmount(column_grossAmount.getAmount());
		mainEntry.setMemo(paypalAccountMemo);
		mainEntry.setValuta(column_date.getDate());
		ReconciliationEntryInfo.getUniqueIdAccessor().setValue(mainEntry, column_transactionId.getText());

		Entry otherEntry = trans.createEntry();
		otherEntry.setAccount(otherAccount);
		otherEntry.setAmount(-column_grossAmount.getAmount());
		otherEntry.setMemo(otherAccountMemo);
	}

	/**
	 * This is a helper method that creates a transaction where there are just two entries
	 * and both are in the Paypal account.  This occurs when an entry is refunded in full.
	 * <P>
	 * The original row is always the current row.  The refunded row is passed in.
	 * @throws ImportException
	 */
	private void createRefundTransaction(RefundRow refundRow) throws ImportException {
		Transaction trans = session.createTransaction();
		trans.setDate(column_date.getDate());

		Entry mainEntry = trans.createEntry();
		mainEntry.setAccount(paypalAccount);
		mainEntry.setAmount(column_grossAmount.getAmount());
		mainEntry.setMemo(column_payeeName.getText());
		mainEntry.setValuta(column_date.getDate());
		ReconciliationEntryInfo.getUniqueIdAccessor().setValue(mainEntry, column_transactionId.getText());

		Entry refundEntry = trans.createEntry();
		refundEntry.setAccount(paypalAccount);
		refundEntry.setAmount(-column_grossAmount.getAmount());
		refundEntry.setMemo("refund - " + column_payeeName.getText());
		refundEntry.setValuta(refundRow.date);
		ReconciliationEntryInfo.getUniqueIdAccessor().setValue(refundEntry, refundRow.transactionId);
	}

	public class ShoppingCartPaymentSent implements MultiRowTransaction {

		private Date date;
		private String status;
		private long grossAmount;
		private long netAmount;

		/**
		 * Three letter code of the currency of the eBay payment.
		 */
		private String currencyOfPayment;

		private String payeeName;
		private String transactionId;
		private String merchantEmail;
		private long shippingAndHandlingAmount;
		private List<ShoppingCartRow> rowItems = new ArrayList<ShoppingCartRow>();

		/**
		 * This class handles the currency conversions.  Delegate
		 * currency conversion rows to this class.
		 */
		CurrencyConversionHandler currencyConversionHandler;

		private boolean done = false;

		/**
		 * Initial constructor called when we find one of:
		 * - "Shopping Cart Payment Sent"
		 * - "Express Checkout Payment Sent"
		 *
		 * @param date
		 * @param quantity
		 * @param stock
		 * @throws ImportException 
		 */
		public ShoppingCartPaymentSent(Date date, long shippingAndHandlingAmount) throws ImportException {
			this.date = date;
			this.status = column_status.getText();
			this.grossAmount = column_grossAmount.getAmount();
			this.netAmount = column_netAmount.getAmount();
			this.payeeName = column_payeeName.getText();
			this.transactionId = column_transactionId.getText();
			this.merchantEmail = column_payeeEmail.getText();
			this.shippingAndHandlingAmount = shippingAndHandlingAmount;
			
			this.currencyOfPayment = column_currency.getText();
			currencyConversionHandler = new CurrencyConversionHandler(-grossAmount, currencyOfPayment);
		}

		@Override
		public void createTransaction(Session session) throws ImportException {
			// Distribute the shipping and handling amount
			distribute(shippingAndHandlingAmount, rowItems);
			//        	long [] amounts = distribute(row.shippingAndHandlingAmount, rowItems);
			//        	for (int i = 0; i < rowItems.size(); i++) {
			//        		rowItems.get(i).shippingAndHandlingAmount = amounts[i];
			//        	}

			// Start a new transaction
			Transaction trans = session.createTransaction();
			trans.setDate(date);

			PaypalEntry mainEntry = trans.createEntry().getExtension(PaypalEntryInfo.getPropertySet(), true);
			mainEntry.setAccount(paypalAccount);
			if (!currencyConversionHandler.isConversion()) {
				// There was no currency conversion
				mainEntry.setAmount(netAmount);
			} else {
				// Currency conversion, so use amount that is in currency
				// of the PayPal account.
				mainEntry.setAmount(currencyConversionHandler.getFromAmount());
			}
			mainEntry.setMemo("payment - " + payeeName);
			mainEntry.setMerchantEmail(merchantEmail);
			ReconciliationEntryInfo.getUniqueIdAccessor().setValue(mainEntry.getBaseObject(), transactionId);

			
			IncomeExpenseAccount categoryAccount;
			if (currencyOfPayment == null || currencyOfPayment.equals(paypalAccount.getCurrency().getCode())) {
				// There was no currency conversion.
				if (currencyConversionHandler.isConversion()) {
					throw new ImportException("Currency conversion rows but payment was in same currency as Paypal account.");
				}
				categoryAccount = paypalAccount.getSaleAndPurchaseAccount();
			} else if (currencyOfPayment.equals("GBP")) {
				if (!currencyConversionHandler.isConversion()) {
					/*
					 * TODO: This is not the correct thing to do.  The Paypal account should be a
					 * multi-currency account that can contain more than one currency.  This appears to
					 * be how Paypal treat the account.  The 'balance' column actually appears to contain
					 * the balance of the currency which has just been credited or debited from the account,
					 * so may differ from row to row.  JMoney can support this with little effort because this
					 * is basically what stock accounts do.
					 * 
					 * For the time being, I don't have time to change the Paypal account at the moment so it
					 * is just put into a different Paypal account.
					 */
					// HACK
					Account ukAccount;
					try {
						ukAccount = session.getAccountByShortName("Paypal (£)");
					} catch (NoAccountFoundException e) {
						MessageDialog.openError(Display.getDefault().getActiveShell(), "Account not Set Up", "No account exists called 'Paypal (£)'");
						throw new RuntimeException(e); 
					} catch (SeveralAccountsFoundException e) {
						MessageDialog.openError(Display.getDefault().getActiveShell(), "Multiple Accounts Set Up", "Multiple accounts exists called 'Paypal (£)'");
						throw new RuntimeException(e); 
					}

					// Overwrite the Paypal account
					mainEntry.setAccount(ukAccount);

					// TODO: think of a way of avoiding this cast.
					categoryAccount = (IncomeExpenseAccount)getAssociatedAccount("net.sf.jmoney.paypal.purchases.GBP");
					if (categoryAccount == null) {
						throw new ImportException("A GBP purchase has been found.  This is a foreign exchange purchase but no account has been set up for GBP purchases.");
					}
				
//					throw new ImportException("No currency conversion rows found but payment was in different currency than Paypal account.");
				} else {
					// TODO: think of a way of avoiding this cast.
					categoryAccount = (IncomeExpenseAccount)getAssociatedAccount("net.sf.jmoney.paypal.purchases.GBP");
					if (categoryAccount == null) {
						throw new ImportException("A GBP purchase has been found.  This is a foreign exchange purchase but no account has been set up for GBP purchases.");
					}
				}
			} else {
				throw new ImportException(MessageFormat.format("Currency {0} is not supported.  Only transactions in USD or GBP are currently supported.", currencyOfPayment));
			}
			
			for (ShoppingCartRow rowItem2 : rowItems) {
				createCategoryEntry(trans, rowItem2.memo, rowItem2.grossAmount, rowItem2.shippingAndHandling, rowItem2.insurance, rowItem2.salesTax, rowItem2.fee, rowItem2.url, rowItem2.quantityText, categoryAccount);
			}

			/*
			 * Look for a refunds that match.  Move them into the cart so they can
			 * be processed as part of the same transaction.
			 */
			if (status.equals("Partially Refunded")) {
				long refundAmount = 0;
				for (Iterator<RefundRow> iter = refunds.iterator(); iter.hasNext(); ) {
					RefundRow refund = iter.next();
					if (refund.payeeName.equals(payeeName)) {
						/*
						 * Create the refund entry in the Paypal account
						 */
						PaypalEntry refundEntry = trans.createEntry().getExtension(PaypalEntryInfo.getPropertySet(), true);
						refundEntry.setAccount(paypalAccount);
						refundEntry.setAmount(refund.grossAmount);
						refundEntry.setMemo("refund - " + refund.payeeName);
						refundEntry.setValuta(refund.date);
						refundEntry.setMerchantEmail(refund.merchantEmail);
						ReconciliationEntryInfo.getUniqueIdAccessor().setValue(refundEntry.getBaseObject(), refund.transactionId);

						refundAmount += refund.grossAmount;

						iter.remove();
					}
				}

				// Create a single income entry with the total amount refunded
				PaypalEntry lineItemEntry = trans.createEntry().getExtension(PaypalEntryInfo.getPropertySet(), true);
				lineItemEntry.setAccount(paypalAccount.getSaleAndPurchaseAccount());
				lineItemEntry.setAmount(-refundAmount);
				lineItemEntry.setMemo(payeeName + " - amount refunded");
			}
			assertValid(trans);
		}

		@Override
		public boolean processCurrentRow(Session session) throws ImportException {

			String rowItemType = column_type.getText();
			Long itemShippingAndHandlingAmount = column_shippingAndHandling.getAmount();

			if (rowItemType.equals("Shopping Cart Item")) {
				if (itemShippingAndHandlingAmount != null && itemShippingAndHandlingAmount.longValue() != shippingAndHandlingAmount) {
					throw new ImportException("shipping and handling amounts in different rows in the same transaction do not match.");
				}

				ShoppingCartRow shoppingCartRow = new ShoppingCartRow();
				shoppingCartRow.memo = column_memo.getText();
				shoppingCartRow.grossAmount = column_grossAmount.getAmount();
				shoppingCartRow.netAmount = column_netAmount.getAmount();
				shoppingCartRow.insurance = column_insurance.getAmount();
				shoppingCartRow.salesTax = column_salesTax.getAmount();
				shoppingCartRow.fee = column_fee.getAmount();
				shoppingCartRow.url = column_itemUrl.getText();
				shoppingCartRow.quantityText = column_quantity.getText();

				rowItems.add(shoppingCartRow);
				return true;
			} else if (column_type.getText().equals("Currency Conversion")) {
				currencyConversionHandler.processCurrentRow(session);
				return true;
			} else {
				done = true;
			}

			return false;
		}

		@Override
		public boolean isDone() {
			return done;
		}
	}

	/**
	 * Rows have been found where there is a row of type 'eBay Payment Sent' followed
	 * by a row of type 'Shopping Cart Item'.  This seems very strange because 'Shopping
	 * Cart Item' rows normally follow a 'Shopping Cart Payment Sent' row.  Furthermore,
	 * the 'Shopping Cart Item' row does not have any data in it of any use except for
	 * a quantity (in known cases always 1, but presumably this would be some other number
	 * if the quantity were not 1).  The 'eBay Payment Sent' row does not have a quantity
	 * so we use the quantity from the 'Shopping Cart Item' row.
	 *
	 * Note: Paypal may have fixed this.  No occurrences of this have been seen for a while.
	 * 
	 * 'Currency Conversion' rows always appear in pairs.  Both rows of the pair are always
	 * consecutive in the exported CSV file.  Both rows have different transaction ids but
	 * they do have the same date and time.  The times appear are given to the nearest second
	 * and appear to be the same.  It is not known if in rare cases the two rows may be given
	 * a timestamp that differs by a second.

	 */
	public class EbayPaymentSent implements MultiRowTransaction {

		// The following fields are saved from the first row
		// (the 'Ebay Payment Set' row).

		private Date date;
		private String transactionId;
		private String memo;
		private String payeeName;
		
		/**
		 * Total amount paid in the currency of the eBay payment.
		 */
		private long grossAmount;
		
		/**
		 * Three letter code of the currency of the eBay payment.
		 */
		private String currencyOfPayment;

		private long netAmount;
		private long fee;
		private String merchantEmail = column_payeeEmail.getText();
		private String quantityString;
		private long shippingAndHandling;


		private boolean done = false;
		private Long insurance;
		private Long salesTax;
		private String url;
		private String quantityText;

		/**
		 * This class handles the currency conversions.  Delegate
		 * currency conversion rows to this class.
		 */
		CurrencyConversionHandler currencyConversionHandler;

		/**
		 * Initial constructor called when first "Ebay Payment Sent" row found.
		 *
		 * @param date
		 * @param quantity
		 * @param stock
		 * @throws ImportException
		 */
		public EbayPaymentSent() throws ImportException {
			this.date = column_date.getDate();
			this.transactionId = column_transactionId.getText();
			this.memo = column_memo.getText();
			this.payeeName = column_payeeName.getText();

			this.currencyOfPayment = column_currency.getText();

			this.grossAmount = column_grossAmount.getAmount();
			this.netAmount = column_netAmount.getAmount();
			this.shippingAndHandling = column_shippingAndHandling.getAmount() == null ? 0 : column_shippingAndHandling.getAmount();
			this.insurance = column_insurance.getAmount();
			this.salesTax = column_salesTax.getAmount();
			this.fee = column_fee.getAmount();
			this.url = column_itemUrl.getText();
			this.quantityText = column_quantity.getText();
			
			currencyConversionHandler = new CurrencyConversionHandler(-grossAmount, currencyOfPayment);
		}

		@Override
		public boolean processCurrentRow(Session session) throws ImportException {
			if (!column_date.getDate().equals(date)) {
				// Date has changed so not the same transaction.
				done = true;
				return false;
			}

			// We may not need this any more.  See comments above.
			
//			if (column_type.getText().equals("Shopping Cart Item")) {
//				quantityString = column_quantity.getText();
//				done = true;
//				return true;
//
//			} else 
				if (column_type.getText().equals("Currency Conversion")) {
					currencyConversionHandler.processCurrentRow(session);
				
				return true;
			}

			return false;
		}

		@Override
		public void createTransaction(Session session) throws ImportException {
			Transaction trans = session.createTransaction();
			trans.setDate(date);

			PaypalEntry mainEntry = trans.createEntry().getExtension(PaypalEntryInfo.getPropertySet(), true);
			mainEntry.setAccount(paypalAccount);
			if (!currencyConversionHandler.isConversion()) {
				// There was no currency conversion
				mainEntry.setAmount(netAmount);
			} else {
				// Currency conversion, so use amount that is in currency
				// of the PayPal account.
				mainEntry.setAmount(currencyConversionHandler.getFromAmount());
			}
			mainEntry.setMemo("payment - " + payeeName);
			mainEntry.setValuta(date);
			mainEntry.setMerchantEmail(merchantEmail);
			ReconciliationEntryInfo.getUniqueIdAccessor().setValue(mainEntry.getBaseObject(), transactionId);

			IncomeExpenseAccount categoryAccount;
			if (currencyOfPayment == null || currencyOfPayment.equals(paypalAccount.getCurrency().getCode())) {
				// There was no currency conversion.
				if (currencyConversionHandler.isConversion()) {
					throw new ImportException("Currency conversion rows but payment was in same currency as Paypal account.");
				}
				categoryAccount = paypalAccount.getSaleAndPurchaseAccount();
				createCategoryEntry(trans, memo, -netAmount, shippingAndHandling, insurance, salesTax, fee, url, quantityText, categoryAccount);
			} else if (currencyOfPayment.equals("GBP")) {
				if (!currencyConversionHandler.isConversion()) {
					throw new ImportException("No currency conversion rows found but payment was in different currency than Paypal account.");
				}
				// TODO: think of a way of avoiding this cast.
				categoryAccount = (IncomeExpenseAccount)getAssociatedAccount("net.sf.jmoney.paypal.purchases.GBP");
				if (categoryAccount == null) {
					throw new ImportException("A GBP purchase has been found.  This is a foreign exchange purchase but no account has been set up for GBP purchases.");
				}
				createCategoryEntry(trans, memo, -netAmount, shippingAndHandling, insurance, salesTax, fee, url, quantityText, categoryAccount);
			} else {
				throw new ImportException(MessageFormat.format("Currency {0} is not supported.  Only transactions in USD or GBP are currently supported.", currencyOfPayment));
			}

			assertValid(trans);
		}

		@Override
		public boolean isDone() {
			return done;
		}
	}

	public class CurrencyConversionHandler {

		long paymentAmount;
		
		/**
		 * Currency in which the payment was made.  This class
		 * will check that it matches 'to' currency in the currency
		 * conversion.
		 */
		String currencyOfPayment;
		
		/**
		* This is set to a non-null value if currency was converted
		* from the currency of the PayPal account.  This is the actual amount
		* deducted from the PayPal account.  It will differ from grossAmount
		* because it is in a different currency.
		*/
		private Long fromAmount = null;

		public CurrencyConversionHandler(long paymentAmount, String currencyOfPayment) {
			this.paymentAmount = paymentAmount;
			this.currencyOfPayment = currencyOfPayment;
		}

		/**
		 * Process the current row which MUST be a Currency Conversion row.
		 * 
		 * @param session
		 * @throws ImportException
		 */
		public void processCurrentRow(Session session) throws ImportException {
			if (column_grossAmount.getAmount().longValue() != column_netAmount.getAmount().longValue()) {
				throw new ImportException("Net and gross amounts differ in currency conversion.  This was not expected");
			};
			if (column_fee.getAmount().longValue() != 0) {
				throw new ImportException("Fee found for currency conversion.  This was not expected");
			};
			long amount = column_grossAmount.getAmount();
			
			String name = column_payeeName.getText();
			if (name.startsWith("From ")) {
				if (amount <= 0) {
					throw new ImportException("Amount should be positive for 'from' conversion as that is the 'to' amount.");
				}
				
				checkNull(fromAmount, "The 'from' currency has been specified twice.");
				String fromCurrency = getCurrencyCodeFromCurrencyName(name.substring(5));
				String toCurrency = column_currency.getText();

				/* The amount is the amount in the 'to' currency.
				 * Both the currency and the amount must match the values in
				 * the preceding 'eBay Payment Sent' row.
				 */
				if (amount != paymentAmount) {
					throw new ImportException("bad currency conversion");
				}
			
				if (!currencyOfPayment.equals(toCurrency)) {
					throw new ImportException("Currency converted to wrong currency - 'to' currency must be same as the currency of the eBay payment.");
				}
				
				if (!paypalAccount.getCurrency().getCode().equals(fromCurrency)) {
					throw new ImportException("Currency converted from wrong currency - 'from' currency must be same as currency of Paypal account.");
				}

			} else if (name.startsWith("To ")) {
				if (amount >= 0) {
					throw new ImportException("Amount should be negative for 'to' conversion as that is the 'from' amount.");
				}
				String toCurrencyFromName = getCurrencyCodeFromCurrencyName(name.substring(3));
				if (!toCurrencyFromName.equals(currencyOfPayment)) {
					throw new ImportException("'to' currencies don't match.");
				}
				if (!column_currency.getText().equals(paypalAccount.getCurrency().getCode())) {
					throw new ImportException("'from' currencies don't match.");
				}

				fromAmount = amount;
			} else {
				throw new ImportException("'Name' column expected to contain 'From ...' or 'To ...'.");
			}
		}

		private String getCurrencyCodeFromCurrencyName(String currencyName) throws ImportException {
			if (currencyName.equals("U.S. Dollar")) {
				return "USD";
			} else if (currencyName.equals("British Pound")) {
				return "GBP";
			} else {
				throw new ImportException(
						MessageFormat.format("Currency {0} in 'Name' column is not supported.", currencyName));
			}
		}

		/**
		 * 
		 * @return true if currency conversion rows were found, false
		 * 			if no currency conversion
		 */
		public boolean isConversion() {
			return fromAmount != null;
		}

		/**
		 * May be called only if there was a currency conversion.
		 * 
		 * @return the amount converted in the 'from' currency
		 */
		public long getFromAmount() {
			return fromAmount;
		}
	}

	@Override
	protected ImportedColumn[] getExpectedColumns() {
		return new ImportedColumn [] {
				column_date,
				null,
				null,
				column_payeeName,
				column_type,
				column_status,
				column_currency,
				column_grossAmount,
				column_fee,
				column_netAmount,
				column_payerEmail,
				column_payeeEmail,
				column_transactionId,
				null,
				null,
				column_memo,
				null,
				column_shippingAndHandling,
				column_insurance,
				column_salesTax,
				null,
				null,
				null,
				null,
				null,
				null,
				column_itemUrl,
				null,
				null,
				null,
				null,
				null,
				null,
				column_quantity,
				null,
				column_balance
		};
	}


	public void checkNull(Object object, String message) throws ImportException {
		if (object != null) {
			throw new ImportException(message);
		}
		
	}

	@Override
	protected String getSourceLabel() {
		return "Paypal";
	}

	@Override
	public AssociationMetadata[] getAssociationMetadata() {
		return new AssociationMetadata[] {
				new AssociationMetadata("net.sf.jmoney.paypal.interest", "Interest Account"),
				new AssociationMetadata("net.sf.jmoney.paypal.creditcard.USD", "Credit Card (USD)"),
				new AssociationMetadata("net.sf.jmoney.paypal.creditcard.GBP", "Credit Card (GBP)"),
				new AssociationMetadata("net.sf.jmoney.paypal.purchases.USD", "Purchases (USD)"),
				new AssociationMetadata("net.sf.jmoney.paypal.purchases.GBP", "Purchases (GBP)"),
		};
	}

	@Override
	protected String getDescription() {
		return "The selected CSV file will be imported.  As you have not selected an account into which the import is to be made, " +
				"a single Paypal account must exist and the data will be imported into that account. " +
				"The file must have been downloaded from Paypal for this import to work.  To download from Paypal, go to 'Download History' on the 'History' menu, choose 'Comma Delimited - All Activity'." +
				"You should also check the box 'Include shopping cart details' at the bottom to get itemized entries. " +
				"If entries have already been imported, this import will create duplicates but this needs to be fixed by incorporating this better into the reconciliation plug-in.";
	}
}
