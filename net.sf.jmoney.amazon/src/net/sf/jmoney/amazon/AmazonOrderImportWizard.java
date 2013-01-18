package net.sf.jmoney.amazon;

import java.text.SimpleDateFormat;
import java.util.Date;

import net.sf.jmoney.importer.MatchingEntryFinder;
import net.sf.jmoney.importer.matcher.EntryData;
import net.sf.jmoney.importer.model.ReconciliationEntryInfo;
import net.sf.jmoney.importer.wizards.CsvImportWizard;
import net.sf.jmoney.importer.wizards.ImportException;
import net.sf.jmoney.isolation.ReferenceViolationException;
import net.sf.jmoney.isolation.TransactionManager;
import net.sf.jmoney.model2.BankAccount;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.EntryInfo;
import net.sf.jmoney.model2.IncomeExpenseAccount;
import net.sf.jmoney.model2.ScalarPropertyAccessor;
import net.sf.jmoney.model2.Session;
import net.sf.jmoney.model2.Transaction;

import org.eclipse.ui.IImportWizard;

/**
 * Items are grouped based on order id and shipment date.  A single order may be split into multiple
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

	private ImportedDateColumn column_orderDate = new ImportedDateColumn("Order date", new SimpleDateFormat("MM-dd-yyyy"));
	private ImportedDateColumn column_shipmentDate = new ImportedDateColumn("Shipment date", new SimpleDateFormat("MM-dd-yyyy"));
	private ImportedTextColumn column_paymentCard = new ImportedTextColumn("Payment - last 4 digits");
	private ImportedTextColumn column_orderId = new ImportedTextColumn("Amazon order ID");
	private ImportedTextColumn column_status = new ImportedTextColumn("Shipment/order condition");
	private ImportedAmountColumn column_subtotal = new ImportedAmountColumn("Subtotal");
	private ImportedAmountColumn column_shippingAmount = new ImportedAmountColumn("Shipping");
	private ImportedAmountColumn column_promotion = new ImportedAmountColumn("Total Promotions");
	private ImportedAmountColumn column_totalCharged = new ImportedAmountColumn("Total Charged");


	@Override
	protected void startImport(TransactionManager transactionManager) throws ImportException {
	}

	@Override
	protected ImportedColumn[] getExpectedColumns() {
		return new ImportedColumn [] {
				column_orderDate,
				column_shipmentDate,
				column_paymentCard,
				column_orderId,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				column_status,
				column_subtotal,
				column_shippingAmount,
				null,
				column_promotion,
				null,
				column_totalCharged,
				null,
				null,
		};
	}

	@Override
	public void importLine(String[] line) throws ImportException {
		Date shipmentDate = column_shipmentDate.getDate();
		String orderId = column_orderId.getText();

		String status = column_status.getText();
		if (!status.equals("Shipped")) {
			if (status.equals("Cancelled")) {
				return;
			} else {
				throw new ImportException("The 'Shipment/order condition' is '" + status + "' but 'Shipped' or 'Cancelled' is expected.");
			}
		}

		/*
		 * Find the account to which this entry has been charged.
		 */
		String lastFourDigits = column_paymentCard.getText();
		if (lastFourDigits == null || lastFourDigits.length() != 4) {
			throw new ImportException("Last four digits of payment card not properly specified.");
		}

		BankAccount chargedAccount = AmazonItemImportWizard.findChargeAccount(getShell(), session, lastFourDigits);

		IncomeExpenseAccount unmatchedAccount = AmazonItemImportWizard.findUnmatchedAccount(session, chargedAccount.getCurrency());

		/*
		 * Look in the unmatched entries account for an entry that matches on order id and shipment date.
		 */
		AmazonEntry matchingEntry = AmazonItemImportWizard.findMatchingEntry(shipmentDate,	orderId, unmatchedAccount);

		long totalCharged = column_totalCharged.getAmount();
		
		long subtotal = column_subtotal.getAmount();
		long shipping = column_shippingAmount.getAmount();
		long promotion = column_promotion.getAmount();
		
		if (shipping == -promotion) {
			// Free shipping deal, so just ignore both
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

		if (matchingEntry == null) {
			// Create new transaction
			
			Transaction trans = session.createTransaction();
			trans.setDate(column_orderDate.getDate());

			// Create a single entry in the "unmatched entries" account
			AmazonEntry unmatchedEntry;

			if (matchedEntryInChargeAccount == null) {
				unmatchedEntry = trans.createEntry().getExtension(AmazonEntryInfo.getPropertySet(), true);

				// Create a single entry in the charge account
			AmazonEntry chargeAccountEntry = trans.createEntry().getExtension(AmazonEntryInfo.getPropertySet(), true);
			chargeAccountEntry.setAccount(chargedAccount);
			chargeAccountEntry.setAmount(-totalCharged);
			chargeAccountEntry.setShipmentDate(shipmentDate);
			chargeAccountEntry.setOrderId(orderId);
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
			unmatchedEntry.setShipmentDate(shipmentDate);
			unmatchedEntry.setOrderId(orderId);
		} else {
			// TODO is this line correct?
			matchingEntry.setValuta(importedDate);
			// Is there a unique id set?
			matchingEntry.setPropertyValue(ReconciliationEntryInfo.getUniqueIdAccessor(), entryData.uniqueId);
			
			// Replace this entry in this transaction
			matchingEntry.setAccount(chargedAccount);
			
			if (matchedEntryInChargeAccount != null) {
				if (matchedEntryInChargeAccount.getTransaction().hasMoreThanTwoEntries()) {
					throw new ImportException("matched entry in charge account has more than one other entry");
				}

				// Copy across the properties
				for (ScalarPropertyAccessor<?,?> propertyAccessor : EntryInfo.getPropertySet().getScalarProperties3()) {
					copyProperty(matchingEntry, matchedEntryInChargeAccount,
							propertyAccessor);
				}
				
				// Set our own properties
				matchingEntry.setShipmentDate(shipmentDate);
				matchingEntry.setOrderId(orderId);
				
				// Delete the original transaction from the charge account
				try {
					matchedEntryInChargeAccount.getSession().deleteTransaction(matchedEntryInChargeAccount.getTransaction());
				} catch (ReferenceViolationException e) {
					throw new ImportException("exception from database", e);
				}
			}
		}
	}

	private <T> void copyProperty(AmazonEntry destinationEntry,
			Entry sourceEntry,
			ScalarPropertyAccessor<T,?> propertyAccessor) {
		T value = sourceEntry.getPropertyValue(propertyAccessor);
		destinationEntry.setPropertyValue(propertyAccessor, value);
	}
}
