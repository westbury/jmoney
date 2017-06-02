package net.sf.jmoney.amazon.csv;

import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import net.sf.jmoney.Helper;
import net.sf.jmoney.amazon.AccountFinder;
import net.sf.jmoney.amazon.AmazonEntry;
import net.sf.jmoney.amazon.AmazonEntryInfo;
import net.sf.jmoney.importer.MatchingEntryFinder;
import net.sf.jmoney.importer.wizards.CsvImportWizard;
import net.sf.jmoney.importer.wizards.CsvTransactionReader;
import net.sf.jmoney.importer.wizards.ImportException;
import net.sf.jmoney.isolation.ReferenceViolationException;
import net.sf.jmoney.model2.BankAccount;
import net.sf.jmoney.model2.CapitalAccount;
import net.sf.jmoney.model2.Currency;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.EntryInfo;
import net.sf.jmoney.model2.IncomeExpenseAccount;
import net.sf.jmoney.model2.Session;
import net.sf.jmoney.model2.Transaction;
import net.sf.jmoney.model2.TransactionManagerForAccounts;
import net.sf.jmoney.reconciliation.BankStatement;
import net.sf.jmoney.reconciliation.ReconciliationEntryInfo;

import org.eclipse.jface.dialogs.MessageDialog;
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
	private ImportedAmountColumn column_totalCharged = new ImportedAmountColumn("Total Charged");

	@Override
	protected void startImport(TransactionManagerForAccounts transactionManager) throws ImportException {
		this.session = transactionManager.getSession();
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
				null,
				null,
				null,
				null,
				column_totalCharged,
		};
	}

	@Override
	public void importLine(CsvTransactionReader reader) throws ImportException {
		String orderId = column_orderId.getText();
		String trackingNumber = column_trackingNumber.getText();
		Date shipmentDate = column_shipmentDate.getDate();

		if (orderId.equals("104-6557509-2207433")) {
			System.out.println("here");
		}
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

			// TODO figure out actual currency of gift certificate
			thisCurrency = session.getCurrencyForCode("USD");

			/*
			 * Look for an income and expense account that can be used by default for items where
			 * the 'Payment - Last 4 Digits' column contains 'Gift Certificate/Card'.
			 */
			BankAccount giftCardAccount = AccountFinder.findGiftcardAccount(session, thisCurrency);

			chargedAccount = giftCardAccount;
		} else {
			if (lastFourDigits.length() != 4) {
				throw new ImportException("Last four digits of payment card must be 4 digits or indicate a gift certificate.");
			}

			BankAccount chargedBankAccount = AccountFinder.findChargeAccount(getShell(), session, lastFourDigits);
			thisCurrency = chargedBankAccount.getCurrency();
			chargedAccount = chargedBankAccount;
		}

		IncomeExpenseAccount unmatchedAccount = AccountFinder.findUnmatchedAccount(session, thisCurrency);

		/*
		 * Look in the unmatched entries account for an entry that matches on order id, tracking number, and shipment date.
		 */
		AmazonEntry matchingEntry = AmazonItemImportWizard.findMatchingEntry(orderId, trackingNumber, shipmentDate, unmatchedAccount);

		long totalCharged = column_totalCharged.getAmount();

		/*
		 * Auto-match the new entry in the charge account the same way that any other
		 * entry would be auto-matched.  This combines the entry if the entry already exists in the
		 * charge account (typically because transactions have been downloaded from the bank and imported).
		 *
		 * An entry in the charge account has already been matched to an
		 * Amazon order if it has an order id set.  This matcher will not return
		 * entries that have already been matched.
		 *
		 * Although we have already eliminated orders that have already been imported,
		 * this test ensures we don't mess up when more than one order can match to the
		 * same debit in the charge account.  This is not likely but two orders of the same
		 * amount and the same or very close dates may cause this.
		 *
		 * Note that we search ten days ahead for a matching entry in the charge account.
		 * Although Amazon usually charge on the day of shipment there have been cases where
		 * the charge appears at the bank seven days later.
		 */
		MatchingEntryFinder matchFinder = new MatchingEntryFinder() {
			@Override
			protected boolean doNotConsiderEntryForMatch(Entry entry) {
				return AmazonEntryInfo.getOrderIdAccessor().getValue(entry) != null;
			}
		};
		Entry matchedEntryInChargeAccount = matchFinder.findMatch(chargedAccount, -totalCharged, shipmentDate, 10, null);

		/*
		 * Although Amazon never charge until the order is shipped, other retailers
		 * often charge when the order is made.  So if we don't find the entry in the
		 * charge account try again starting at the order date.
		 */
		if (matchedEntryInChargeAccount == null) {
			matchedEntryInChargeAccount = matchFinder.findMatch(chargedAccount, -totalCharged, column_orderDate.getDate(), 10, null);
		}

		if (matchingEntry == null) {
			// Create new transaction

			Transaction trans = session.createTransaction();
			trans.setDate(column_orderDate.getDate());

			// Create a single entry in the "unmatched entries" account
			AmazonEntry unmatchedEntry;

			if (matchedEntryInChargeAccount == null) {
				unmatchedEntry = trans.createEntry().getExtension(AmazonEntryInfo.getPropertySet(), true);

				// Create a single entry in the charge account
				matchedEntryInChargeAccount = trans.createEntry();
				AmazonEntry chargeAccountEntry = matchedEntryInChargeAccount.getExtension(AmazonEntryInfo.getPropertySet(), true);
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

			if (matchedEntryInChargeAccount != null) {
				if (matchedEntryInChargeAccount.getTransaction().hasMoreThanTwoEntries()) {
					/*
					 * In this case we just don't merge the transactions.  We leave both the original
					 * transaction with the user-entered data and the transaction imported from
					 * Amazon.
					 *
					 * We could try to match the entries in each transaction but that would
					 * be difficult and risky.  As long as users get into the habit of importing from
					 * Amazon before manually editing the data then this should not happen.
					 *
					 */
					DateFormat df = new SimpleDateFormat("dd-MMM-yyyy");
					MessageDialog.openWarning(getShell(), "Unmerged Transaction",
							MessageFormat.format(
									"A transaction was found in the {0} account on {1} that matches an Amazon import.  However that transaction has split entries and cannot be automatically merged with the Amazon data.",
									chargedAccount.getName(),
									df.format(matchedEntryInChargeAccount.getTransaction().getDate())));

					/* Put the Amazon transaction into the charge account, and reconcile
					 it in lieu of the original transaction.  The user then has the least
					amount of work to tidy this up.
					*/
					matchingEntry.setAccount(chargedAccount);

					// TODO make this an optional dependency on the reconciliation plugin.
					BankStatement statement = ReconciliationEntryInfo.getStatementAccessor().getValue(matchedEntryInChargeAccount);
					ReconciliationEntryInfo.getStatementAccessor().setValue(matchedEntryInChargeAccount, null);
					ReconciliationEntryInfo.getStatementAccessor().setValue(matchingEntry.getBaseObject(), statement);

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

					// We distribute the costs and discounts on the items transaction,
					// so set this accordingly.
					matchedEntryInChargeAccount = matchingEntry.getBaseObject();
				} else {
					// Copy across the properties
					Helper.copyScalarValues(
							EntryInfo.getPropertySet(),
							matchedEntryInChargeAccount,
							matchingEntry.getBaseObject());

					/*
					 * If the other entry has an account that is not the default
					 * then we want to keep that account and also anything in the
					 * memo. If the item import contained more than one item (so is
					 * a split transaction) then put into all entries.
					 */
					Entry otherEntry = matchedEntryInChargeAccount.getTransaction().getOther(matchedEntryInChargeAccount);
					if (!otherEntry.getAccount().getName().startsWith("unreconciled")) {
						for (Entry itemEntry : matchingEntry.getTransaction().getEntryCollection()) {
							if (itemEntry != matchingEntry.getBaseObject()) {
								itemEntry.setAccount(otherEntry.getAccount());
								itemEntry.setMemo("" + itemEntry.getMemo() + " - " + otherEntry.getMemo());
							}
						}
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
				}
			} else {
				/*
				 * Move the entry from the 'matched' to the charge account.
				 * Although the value date is almost always the shipment date,
				 * we don't set it because that is generally set when the entry
				 * is imported from the bank's server.
				 */
				matchingEntry.setAccount(chargedAccount);

				/*
				 * We also change the amount from the 'Subtotal' amount to
				 * the 'Total Charged' amount.  The subtotal is the total of all
				 * the items and that is what will have been put by the item import.
				 * The total charged adds in any shipping amount and deducts
				 * any promotional discount.  Entries for those two amounts are added
				 * later so the transaction should then still balance.
				 */
				matchingEntry.setAmount(-totalCharged);
			}

			matchedEntryInChargeAccount = matchingEntry.getBaseObject();
		}

		List<Entry> itemEntries = new ArrayList<Entry>();
		itemEntries.addAll(matchedEntryInChargeAccount.getTransaction().getEntryCollection());
		itemEntries.remove(matchedEntryInChargeAccount);
		distribute(matchedEntryInChargeAccount.getTransaction(), itemEntries);

		AccountFinder.assertValid(matchedEntryInChargeAccount.getTransaction());
	}

	/**
	 * We distribute the shipping and promotional discounts among the items in proportion
	 * to the price of each item.
	 * <P>
	 * An attempt had been made to separate out the shipping and promotional discounts into
	 * separate entries.  However that approach did not work out very well.  The various adjustments
	 * in the CSV files exported by Amazon just are not consistent and in some cases just don't add up.
	 *
	 * @throws ImportException
	 */
	public static void distribute(Transaction trans, Collection<Entry> itemEntries) throws ImportException {
		long transactionTotal = 0;
		for (Entry itemEntry : trans.getEntryCollection()) {
			transactionTotal += itemEntry.getAmount();
		}

		long netTotal = 0;
		for (Entry itemEntry : itemEntries) {
			if (itemEntry.getAmount() < 0) {
				throw new ImportException("Can refunds be supported here?");
			}
			netTotal += itemEntry.getAmount();
		}
		
		long toDistribute = - transactionTotal;
		long leftToDistribute = toDistribute;

		if (toDistribute != 0) {
			System.out.println("to distribute: " + toDistribute);
		}
		for (Entry itemEntry : itemEntries) {
			long amount = toDistribute * itemEntry.getAmount() / netTotal;
			itemEntry.setAmount(itemEntry.getAmount() + amount);
			leftToDistribute -= amount;
		}

		// We have rounded down, so we may be under.  We now distribute
		// a penny to each until we get a balanced transaction.
		for (Entry itemEntry : itemEntries) {
			if (leftToDistribute > 0) {
				itemEntry.setAmount(itemEntry.getAmount() + 1);
				leftToDistribute--;
			} else if (leftToDistribute < 0) {
				itemEntry.setAmount(itemEntry.getAmount() - 1);
				leftToDistribute++;
			}
		}

		assert(leftToDistribute == 0);
	}

	@Override
	protected String getDescription() {
		return "The selected CSV file will be imported.  The CSV file must have been exported from Amazon " +
				"as an 'order' export.  This can be done if orders are made through amazon.com.  If you ordered through amazon.co.uk then you will not be able to get an item report.  You must instead use screen scraping (Amazon HTML import) which is quite a pain. " +
				"You must import both orders and items into JMoney because not all the information is in either export.  JMoney will match the data in the imports to obtain a single transaction for each order.";
	}
}
