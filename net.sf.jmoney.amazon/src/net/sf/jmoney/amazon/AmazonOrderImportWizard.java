package net.sf.jmoney.amazon;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;

import net.sf.jmoney.importer.MatchingEntryFinder;
import net.sf.jmoney.importer.matcher.EntryData;
import net.sf.jmoney.importer.model.ReconciliationEntryInfo;
import net.sf.jmoney.importer.wizards.CsvImportWizard;
import net.sf.jmoney.importer.wizards.ImportException;
import net.sf.jmoney.importer.wizards.CsvImportWizard.ImportedTextColumn;
import net.sf.jmoney.isolation.ReferenceViolationException;
import net.sf.jmoney.model2.Account;
import net.sf.jmoney.model2.BankAccount;
import net.sf.jmoney.model2.CapitalAccount;
import net.sf.jmoney.model2.Currency;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.EntryInfo;
import net.sf.jmoney.model2.IncomeExpenseAccount;
import net.sf.jmoney.model2.ScalarPropertyAccessor;
import net.sf.jmoney.model2.Session;
import net.sf.jmoney.model2.Session.NoAccountFoundException;
import net.sf.jmoney.model2.Session.SeveralAccountsFoundException;
import net.sf.jmoney.model2.Transaction;
import net.sf.jmoney.model2.TransactionManagerForAccounts;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IImportWizard;

/**
 * Items are grouped based on order id, tracking number, and shipment date.  A single order may be split into multiple
 * shipments.  Items are charged as they are shipped, and it is assumed that all items shipped on the
 * same day are charged as a single charge.
 * <P>
 * Having grouped the items, we still may not know the amount charged.  That is because there may be
 * shipping and other costs.  Those other costs are not available in the item import.  Therefore we do
 * not try to match items to charges on a credit card.  It is the import of the orders that makes this
 * connection.  If items have been imported but the orders have not then the items are added up and charged
 * to a special 'pending Amazon charges' account.  When the orders are imported, these entries are matched
 * to the credit card or bank account entries as appropriate.
 * <P>
 * The idea is that the orders and items can both be imported separately.  Data is put into the datastore.
 * If only one is imported then the data from that one import is in the datastore, but if both are imported
 * then everything is matched.  It should not matter what order the Amazon items, the Amazon orders, and the
 * charge or bank account are imported.
 *  
 * @author westbury.nigel2
 *
 */
public class AmazonOrderImportWizard extends CsvImportWizard implements IImportWizard {

	private Session session;

	private ImportedDateColumn column_orderDate = new ImportedDateColumn("Order Date", new SimpleDateFormat("MM/dd/yy"));
	private ImportedTextColumn column_orderId = new ImportedTextColumn("Order ID");
	private ImportedTextColumn column_paymentCard = new ImportedTextColumn("Payment - Last 4 Digits");
	private ImportedDateColumn column_shipmentDate = new ImportedDateColumn("Shipment Date", new SimpleDateFormat("MM/dd/yy"));
	private ImportedTextColumn column_status = new ImportedTextColumn("Shipment/Order Condition");
	private ImportedTextColumn column_trackingNumber = new ImportedTextColumn("Carrier Name & Tracking Number");
	private ImportedAmountColumn column_subtotal = new ImportedAmountColumn("Subtotal");
	private ImportedAmountColumn column_shippingAmount = new ImportedAmountColumn("Shipping Charge");
	private ImportedAmountColumn column_promotion = new ImportedAmountColumn("Total Promotions");
	private ImportedAmountColumn column_totalCharged = new ImportedAmountColumn("Total Charged");

	private Account promotionalAccount;
	private Account shippingAccount;

	@Override
	protected void startImport(TransactionManagerForAccounts transactionManager) throws ImportException {
		this.session = transactionManager.getSession();

		try {
			promotionalAccount = session.getAccountByShortName("Amazon promotional discounts");
		} catch (NoAccountFoundException e) {
			MessageDialog.openError(Display.getDefault().getActiveShell(), "Account not Set Up", "No account exists called 'Amazon promotional discounts'");
			throw new RuntimeException(e); 
		} catch (SeveralAccountsFoundException e) {
			MessageDialog.openError(Display.getDefault().getActiveShell(), "Multiple Accounts Set Up", "Multiple accounts exists called 'Amazon promotional discounts'");
			throw new RuntimeException(e); 
		}

		try {
			shippingAccount = session.getAccountByShortName("Amazon shipping");
		} catch (NoAccountFoundException e) {
			MessageDialog.openError(Display.getDefault().getActiveShell(), "Account not Set Up", "No account exists called 'Amazon shipping'");
			throw new RuntimeException(e); 
		} catch (SeveralAccountsFoundException e) {
			MessageDialog.openError(Display.getDefault().getActiveShell(), "Multiple Accounts Set Up", "Multiple accounts exists called 'Amazon shipping'");
			throw new RuntimeException(e); 
		}
	}

	@Override
	protected ImportedColumn[] getExpectedColumns() {
		return new ImportedColumn [] {
				column_orderDate,
				column_orderId,
				column_paymentCard,
				null,
				null,
				column_shipmentDate,
				null,
				null,
				null,
				null,
				null,
				null,
				column_status,
				column_trackingNumber,
				column_subtotal,
				column_shippingAmount,
				null,
				column_promotion,
				null,
				column_totalCharged,
		};
	}

	@Override
	public void importLine(String[] line) throws ImportException {
		String orderId = column_orderId.getText();
		String trackingNumber = column_trackingNumber.getText();
		Date shipmentDate = column_shipmentDate.getDate();

		String status = column_status.getText();
		if (!status.equals("Shipped")) {
//			if (status.equals("Cancelled")) {
//				return;
//			} else {
				throw new ImportException("The 'Shipment/order condition' is '" + status + "' but 'Shipped' or 'Cancelled' is expected.");
//			}
		}

		/*
		 * Find the account to which this entry has been charged.
		 */
		String lastFourDigits = column_paymentCard.getText();
		if (lastFourDigits == null) {
			throw new ImportException("Last four digits of payment card cannot be blank.");
		}

		BankAccount chargedAccount;
		Currency thisCurrency;
		if (lastFourDigits.equals("Gift Certificate/Card")) {
			/*
			 * Look for an income and expense account that can be used by default for items where
			 * the 'Payment - Last 4 Digits' column contains 'Gift Certificate/Card'.
			 */
			// TODO need to check the currency
			BankAccount giftCardAccount = null;
			for (Iterator<CapitalAccount> iter = session.getCapitalAccountIterator(); iter.hasNext(); ) {
				CapitalAccount eachAccount = iter.next();
				if (eachAccount.getName().startsWith("Amazon gift")
						/* && eachAccount.getCurrency() == ???Account.getCurrency() */) {
					giftCardAccount = (BankAccount)eachAccount;
					break;
				}
			}
			if (giftCardAccount == null) {
				throw new ImportException("No account exists with a name that begins 'Amazon gift'.");
			}

			chargedAccount = giftCardAccount;
			
			// TODO figure out actual currency of gift certificate
			thisCurrency = session.getCurrencyForCode("USD");
		} else {
			if (lastFourDigits.length() != 4) {
				throw new ImportException("Last four digits of payment card must be 4 digits or indicate a gift certificate.");
			}

			BankAccount chargedBankAccount = AmazonItemImportWizard.findChargeAccount(getShell(), session, lastFourDigits);
			thisCurrency = chargedBankAccount.getCurrency();
			chargedAccount = chargedBankAccount;
		}

		IncomeExpenseAccount unmatchedAccount = AmazonItemImportWizard.findUnmatchedAccount(session, thisCurrency);

		/*
		 * Look in the unmatched entries account for an entry that matches on order id, tracking number, and shipment date.
		 */
		AmazonEntry matchingEntry = AmazonItemImportWizard.findMatchingEntry(orderId, trackingNumber, shipmentDate, unmatchedAccount);

		long totalCharged = column_totalCharged.getAmount();
		
		long shipping = column_shippingAmount.getAmount();
		long promotion = column_promotion.getAmount();
		
		/*
		 * If the promotional discount is the same as the shipping charge
		 * then it's just a free shipping deal.  They cancel each other out
		 * so we just ignore both (i.e. don't create an entry for either).
		 */
		if (shipping == -promotion) {
			shipping = 0;
			promotion = 0;
		}

		/*
		 * Auto-match the new entry in the charge account the same way that any other
		 * entry would be auto-matched.  This combines the entry if the entry already exists in the
		 * charge account (typically because transactions have been downloaded from the bank and imported).
		 */
		EntryData entryData = new EntryData();
		entryData.amount = -totalCharged;
		entryData.valueDate = column_shipmentDate.getDate();
		
		Date importedDate = (entryData.valueDate != null)
		? entryData.valueDate
				: entryData.clearedDate;

		// TODO is the alreadyMatched rule correct?
		MatchingEntryFinder matchFinder = new MatchingEntryFinder() {
			@Override
			protected boolean alreadyMatched(Entry entry) {
				return entry.getPropertyValue(ReconciliationEntryInfo.getUniqueIdAccessor()) != null;
			}
		};
		Entry matchedEntryInChargeAccount = matchFinder.findMatch(chargedAccount, entryData.amount, importedDate, null);

		Transaction trans;
		
		if (matchingEntry == null) {
			// Create new transaction
			
			trans = session.createTransaction();
			trans.setDate(column_orderDate.getDate());

			// Create a single entry in the "unmatched entries" account
			AmazonEntry unmatchedEntry;

			if (matchedEntryInChargeAccount == null) {
				unmatchedEntry = trans.createEntry().getExtension(AmazonEntryInfo.getPropertySet(), true);

				// Create a single entry in the charge account
				AmazonEntry chargeAccountEntry = trans.createEntry().getExtension(AmazonEntryInfo.getPropertySet(), true);
				chargeAccountEntry.setAccount(chargedAccount);
				chargeAccountEntry.setAmount(-totalCharged);
				chargeAccountEntry.setOrderId(orderId);
				chargeAccountEntry.setTrackingNumber(trackingNumber);
				chargeAccountEntry.setShipmentDate(shipmentDate);
			} else {
				// 
				if (matchedEntryInChargeAccount.getTransaction().hasMoreThanTwoEntries()) {
					throw new ImportException("matched entry in charge account has more than one other entry");
				}
				
				Entry otherMatchedEntry = matchedEntryInChargeAccount.getTransaction().getOther(matchedEntryInChargeAccount);
				// Any checks on the other entry before we delete it?
				matchedEntryInChargeAccount.getTransaction().deleteEntry(otherMatchedEntry);
				
				unmatchedEntry = matchedEntryInChargeAccount.getTransaction().createEntry().getExtension(AmazonEntryInfo.getPropertySet(), true);

				
			}

			unmatchedEntry.setAccount(unmatchedAccount);
			unmatchedEntry.setAmount(totalCharged);
			unmatchedEntry.setOrderId(orderId);
			unmatchedEntry.setTrackingNumber(trackingNumber);
			unmatchedEntry.setShipmentDate(shipmentDate);
		} else {
			/*
			 * The amount should match (though with opposite sign).  If it
			 * does not match then something is wrong.
			 */
			if (matchingEntry.getAmount() != -column_subtotal.getAmount()) {
				throw new ImportException("the total price of the items in the matching transaction does not match the 'Subtotal' amount in the order table.");
			}
			 
			trans = matchingEntry.getTransaction();
			
			if (matchedEntryInChargeAccount != null) {
				if (matchedEntryInChargeAccount.getTransaction().hasMoreThanTwoEntries()) {
					throw new ImportException("matched entry in charge account has more than one other entry");
				}

				// Copy across the properties
				for (ScalarPropertyAccessor<?,? super Entry> propertyAccessor : EntryInfo.getPropertySet().getScalarProperties3()) {
					copyProperty(matchingEntry, matchedEntryInChargeAccount,
							propertyAccessor);
				}
				
				// Set our own properties
				matchingEntry.setOrderId(orderId);
				matchingEntry.setTrackingNumber(trackingNumber);
				matchingEntry.setShipmentDate(shipmentDate);
				
				/*
				 * If the charge account entry does not have a 'value' date then
				 * we put the transaction date from the existing charge account
				 * entry as the 'value' date. This is done because that
				 * transaction was most likely created when the entries were
				 * imported from the bank's server and will therefore be the
				 * date that the charge was debited by the bank.
				 */
				if (matchingEntry.getValuta() == null) {
					matchingEntry.setValuta(matchedEntryInChargeAccount.getTransaction().getDate());
				}
				
				// Delete the original transaction from the charge account
				try {
					matchedEntryInChargeAccount.getSession().deleteTransaction(matchedEntryInChargeAccount.getTransaction());
				} catch (ReferenceViolationException e) {
					throw new ImportException("exception from database", e);
				}
			} else {
				/*
				 * Move the entry from the 'matched' to the charge account.
				 * Although the value date is almost always the shipment date,
				 * we don't set it because that is generally set when the entry
				 * is imported from the bank's server.
				 */
				matchingEntry.setAccount(chargedAccount);
			}
		}
		
		if (promotion != 0) {
			AmazonEntry promotionalEntry = trans.createEntry().getExtension(AmazonEntryInfo.getPropertySet(), true);
			promotionalEntry.setAmount(-promotion);
			promotionalEntry.setAccount(promotionalAccount);
			promotionalEntry.setOrderId(orderId);
			promotionalEntry.setTrackingNumber(trackingNumber);
			promotionalEntry.setShipmentDate(shipmentDate);
		}
		
		if (promotion != 0) {
			AmazonEntry shippingEntry = trans.createEntry().getExtension(AmazonEntryInfo.getPropertySet(), true);
			shippingEntry.setAmount(shipping);
			shippingEntry.setAccount(shippingAccount);
			shippingEntry.setOrderId(orderId);
			shippingEntry.setTrackingNumber(trackingNumber);
			shippingEntry.setShipmentDate(shipmentDate);
		}
	}

	private <T> void copyProperty(AmazonEntry destinationEntry,
			Entry sourceEntry,
			ScalarPropertyAccessor<T,? super Entry> propertyAccessor) {
		T value = sourceEntry.getPropertyValue(propertyAccessor);
		destinationEntry.setPropertyValue(propertyAccessor, value);
	}
}
