package net.sf.jmoney.amazon;

import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import net.sf.jmoney.importer.wizards.CsvImportWizard;
import net.sf.jmoney.importer.wizards.ImportException;
import net.sf.jmoney.importer.wizards.MultiRowTransaction;
import net.sf.jmoney.model2.Account;
import net.sf.jmoney.model2.BankAccount;
import net.sf.jmoney.model2.CapitalAccount;
import net.sf.jmoney.model2.Currency;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.IncomeExpenseAccount;
import net.sf.jmoney.model2.Session;
import net.sf.jmoney.model2.Transaction;
import net.sf.jmoney.model2.TransactionManagerForAccounts;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IImportWizard;

public class AmazonItemImportWizard extends CsvImportWizard implements IImportWizard {

	private Session session;

	/**
	 * Category used for Amazon purchases
	 */
	//	private IncomeExpenseAccount unknownAmazonPurchaseAccount;

	private ImportedDateColumn column_orderDate = new ImportedDateColumn("Order Date", new SimpleDateFormat("MM/dd/yy"));
	private ImportedTextColumn column_orderId = new ImportedTextColumn("Order ID");
	private ImportedTextColumn column_title = new ImportedTextColumn("Title");
	private ImportedTextColumn column_category = new ImportedTextColumn("Category");
	private ImportedTextColumn column_id = new ImportedTextColumn("ASIN/ISBN");
	private ImportedTextColumn column_condition = new ImportedTextColumn("Condition");
	private ImportedTextColumn column_seller = new ImportedTextColumn("Seller");
	private ImportedAmountColumn column_unitPrice = new ImportedAmountColumn("Per Unit Price");
	private ImportedNumberColumn column_quantity = new ImportedNumberColumn("Quantity");
	private ImportedTextColumn column_paymentCard = new ImportedTextColumn("Payment - Last 4 Digits");
	private ImportedDateColumn column_shipmentDate = new ImportedDateColumn("Shipment Date", new SimpleDateFormat("MM/dd/yy"));
	private ImportedTextColumn column_addressName = new ImportedTextColumn("Shipping Address Name");
	private ImportedTextColumn column_addressStreet1 = new ImportedTextColumn("Shipping Address Street 1");
	private ImportedTextColumn column_status = new ImportedTextColumn("Shipment/Order Condition");
	private ImportedTextColumn column_trackingNumber = new ImportedTextColumn("Carrier Name & Tracking Number");
	private ImportedAmountColumn column_subtotal = new ImportedAmountColumn("Item Subtotal");

	//	Pattern patternCheque;
	//	Pattern patternWithdrawalDate;

	//	private ImportMatcher matcher;

	//	@Override
	//	protected void setAccount(Account accountInsideTransaction)	throws ImportException {
	//		if (!(accountInsideTransaction instanceof BankAccount)) {
	//			throw new ImportException("Bad configuration: This import can be used for bank accounts only.");
	//		}
	//
	//		this.account = (BankAccount)accountInsideTransaction;
	//		this.session = accountInsideTransaction.getSession();
	//
	//		try {
	//			patternCheque = Pattern.compile("Cheque (\\d\\d\\d\\d\\d\\d)\\.");
	//			patternWithdrawalDate = Pattern.compile("(.*) Withdrawal Date (\\d\\d  [A-Z][a-z][a-z] 20\\d\\d)");
	//		} catch (PatternSyntaxException e) {
	//			throw new RuntimeException("pattern failed", e); 
	//		}
	//
	//		matcher = new ImportMatcher(account.getExtension(PatternMatcherAccountInfo.getPropertySet(), true));
	//	}

	@Override
	protected void startImport(TransactionManagerForAccounts transactionManager) throws ImportException {
		this.session = transactionManager.getSession();
	}

	@Override
	protected ImportedColumn[] getExpectedColumns() {
		return new ImportedColumn [] {
				column_orderDate,
				column_orderId,
				column_title,
				column_category,
				column_id,
				null,
				column_condition,
				column_seller,
				column_unitPrice,
				column_quantity,
				column_paymentCard,
				null,
				null,
				column_shipmentDate,
				column_addressName,
				column_addressStreet1,
				null,
				null,
				null,
				null,
				column_status,
				column_trackingNumber,
				column_subtotal,
		};
	}

	@Override
	public void importLine(String[] line) throws ImportException {

		String orderId = column_orderId.getText();
		String trackingNumber = column_trackingNumber.getText();
		Date shipmentDate = column_shipmentDate.getDate();

		DateFormat f = new SimpleDateFormat("dd-MMM-yyyy");
		System.out.println("Shipment date: " + (shipmentDate==null ? "null" : f.format(shipmentDate)) + ", order: " + orderId);
		
		/*
		 * If this order has not shipped then we don't process it.  It won't have been
		 * charged so we can't match against the charge account.  More importantly, the
		 * shipment may be split into multiple shipments and multiple charges thus made.
		 * Trying to import it now will really confuse things.
		 */
		String status = column_status.getText();
		if (!status.equals("Shipped")) {
			if (status.equals("Cancelled") || status.equals("Shipment planned")) {
				return;
			} else {
				throw new ImportException("The 'Shipment/order condition' is '" + status + "' but 'Shipped', 'Cancelled', or 'Shipment planned' is expected.");
			}
		}

		/*
		 * For all shipped items, the unit price and the quantity must be positive.
		 */
		if (column_quantity.getAmount() <= 0
				|| column_unitPrice.getAmount() <= 0) {
			throw new ImportException("If an item has shipped, both the unit price and the quantity are assumed to be positive (non-zero).");
		}
		
		/*
		 * Try each of the current row processors.  If any are 'done' then we
		 * create their transaction and remove the processor from our list.
		 * If any can process (and only one should be able to process) then
		 * we leave it at that.
		 */
		boolean processed = false;
		for (Iterator<MultiRowTransaction> iter = currentMultiRowProcessors.iterator(); iter.hasNext(); ) {
			MultiRowTransaction currentMultiRowProcessor = iter.next();
			
			boolean thisOneProcessed = currentMultiRowProcessor.processCurrentRow(session);
			
			if (thisOneProcessed) {
				if (processed) {
					throw new RuntimeException("Can't have two current processors that can process the same row");
				}
				processed = true;
			}
			
			if (currentMultiRowProcessor.isDone()) {
				currentMultiRowProcessor.createTransaction(session);
				iter.remove();
			}
		}

		if (!processed) {
			/*
			 * Find the account to which this entry has been charged.
			 */
			String lastFourDigits = column_paymentCard.getText();
			if (lastFourDigits == null) {
				throw new ImportException("Last four digits of payment card cannot be blank.");
			}

			Account chargedAccount;
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

				BankAccount chargedBankAccount = findChargeAccount(getShell(), session, lastFourDigits);
				thisCurrency = chargedBankAccount.getCurrency();
				chargedAccount = chargedBankAccount;
			}

			IncomeExpenseAccount unmatchedAccount = findUnmatchedAccount(session, thisCurrency);

			/*
			 * Look in the unmatched entries account for an entry that matches on order id and shipment date.
			 */
			AmazonEntry matchingEntry = findMatchingEntry(orderId,	trackingNumber, shipmentDate, unmatchedAccount);

			/*
			 * Look for an income and expense account that can be used by default for the purchases.
			 * The currency of this account must match the currency of the charge account. 
			 */
			IncomeExpenseAccount unknownAmazonPurchaseAccount = null;
			for (Iterator<IncomeExpenseAccount> iter = session.getIncomeExpenseAccountIterator(); iter.hasNext(); ) {
				IncomeExpenseAccount eachAccount = iter.next();
				if (eachAccount.getName().startsWith("Amazon purchase")
						&& eachAccount.getCurrency() == thisCurrency) {
					unknownAmazonPurchaseAccount = eachAccount;
					break;
				}
			}
			if (unknownAmazonPurchaseAccount == null) {
				throw new ImportException("No account exists with a name that begins 'Amazon purchase' and a currency of " + thisCurrency.getName() + ".");
			}

			// All rows are processed by this
			MultiRowTransaction thisMultiRowProcessor;
			if (matchingEntry == null) {
				/*
				 * We should check the charge account to make sure there is no entries.
				 * If both the items and the order has already been imported then we should
				 * find the matching entry in the charge account. 
				 */
				for (Entry entry : chargedAccount.getEntries()) {
					AmazonEntry amazonEntry = entry.getExtension(AmazonEntryInfo.getPropertySet(), false);
					if (amazonEntry != null) {
						if (orderId.equals(amazonEntry.getOrderId())
								&& shipmentDate.equals(amazonEntry.getShipmentDate())) {
							throw new ImportException("Items for this shipment have already been imported.");
						}
					}
				}

				thisMultiRowProcessor = new ItemsShippedTransactionUnmatched(unmatchedAccount, unknownAmazonPurchaseAccount, shipmentDate, orderId);
			} else {
				/*
				 * We have a matching entry.  Now if the amount is positive then it represents items
				 * that have not yet been imported, and if the amount is negative then it represents
				 * the charge account entry that cannot yet be matched.
				 */
				if (matchingEntry.getAmount() < 0) {
					throw new ImportException("Items for this shipment have already been imported.");
				}
				thisMultiRowProcessor = new ItemsShippedTransactionMatched(unmatchedAccount, unknownAmazonPurchaseAccount, matchingEntry);
			}

			thisMultiRowProcessor.processCurrentRow(session);
			currentMultiRowProcessors.add(thisMultiRowProcessor);
		}
	}

	public static AmazonEntry findMatchingEntry(String orderId, String trackingNumber, Date shipmentDate,
			IncomeExpenseAccount unmatchedAccount) {
		AmazonEntry matchingEntry = null;
		for (Entry entry : unmatchedAccount.getEntries()) {
			AmazonEntry amazonEntry = entry.getExtension(AmazonEntryInfo.getPropertySet(), false);
			if (amazonEntry != null) {
				if (amazonEntry.getOrderId().equals(orderId)
						&& amazonEntry.getTrackingNumber().equals(trackingNumber)
						&& amazonEntry.getShipmentDate().equals(shipmentDate)) {
					matchingEntry = amazonEntry;
				}
			}
		}
		return matchingEntry;
	}

	public static IncomeExpenseAccount findUnmatchedAccount(Session session, Currency currency)
			throws ImportException {
		/*
		 * Look for a category account that has a name that starts with "Amazon unmatched"
		 * and a currency that matches the currency of the charge account.
		 */
		IncomeExpenseAccount unmatchedAccount = null;
		for (Iterator<IncomeExpenseAccount> iter = session.getIncomeExpenseAccountIterator(); iter.hasNext(); ) {
			IncomeExpenseAccount eachAccount = iter.next();
			if (eachAccount.getName().startsWith("Amazon unmatched")
					&& eachAccount.getCurrency() == currency) {
				unmatchedAccount = eachAccount;
				break;
			}
		}
		if (unmatchedAccount == null) {
			throw new ImportException("No account exists with a name that begins 'Amazon unmatched' and a currency of " + currency.getName() + ".");
		}
		return unmatchedAccount;
	}

	public static BankAccount findChargeAccount(Shell shell, Session session, String lastFourDigits)
			throws ImportException {
		BankAccount chargedAccount = null;
		for (Iterator<CapitalAccount> iter = session.getCapitalAccountIterator(); iter.hasNext(); ) {
			CapitalAccount eachAccount = iter.next();
			if (eachAccount instanceof BankAccount) {
				BankAccount eachBankAccount = (BankAccount)eachAccount;
				String accountNumber = eachBankAccount.getAccountNumber();
				if (accountNumber != null && accountNumber.endsWith(lastFourDigits)) {
					chargedAccount = eachBankAccount;
					break;
				}
			}
		}
		if (chargedAccount == null) {
			boolean result = MessageDialog.openQuestion(shell, "Account not Found",
					"No account exists with an account number ending with " + lastFourDigits + "."
							+ "  Do you want to skip this one and continue importing the rest?  Press 'No' to cancel the entire import.");
			if (!result) {
				throw new ImportException("Import cancelled due to missing account.");
			}
		}
		return chargedAccount;
	}

	public abstract class ItemsShippedTransaction implements MultiRowTransaction {
		protected IncomeExpenseAccount unmatchedAccount;
		protected IncomeExpenseAccount unknownAmazonPurchaseAccount;

		protected Date orderDate;
		protected String trackingNumber;
		protected Date shipmentDate;
		protected String orderId;
		protected List<ItemRow> rowItems = new ArrayList<ItemRow>();

		protected boolean done = false;

		/**
		 * Initial constructor called when first item in a shipment found.
		 * 
		 * @param trackingNumber
		 * @param shipmentDate
		 * @param quantity
		 * @param stock
		 * @throws ImportException 
		 */
		public ItemsShippedTransaction(IncomeExpenseAccount unmatchedAccount, IncomeExpenseAccount unknownAmazonPurchaseAccount) throws ImportException {
			this.unmatchedAccount = unmatchedAccount;
			this.unknownAmazonPurchaseAccount = unknownAmazonPurchaseAccount;
			this.orderDate = column_orderDate.getDate();
			this.trackingNumber = column_trackingNumber.getText();
			this.shipmentDate = column_shipmentDate.getDate();
			this.orderId = column_orderId.getText();
		}

		@Override
		public boolean processCurrentRow(Session session) throws ImportException {
			if (orderId.equals(column_orderId.getText())
					&& trackingNumber.equals(column_trackingNumber.getText())
					&& shipmentDate.equals(column_shipmentDate.getDate())) {
				ItemRow item = new ItemRow();

				
				StringBuffer memo = new StringBuffer();
				if (!column_category.getText().isEmpty()) {
					memo.append(column_category.getText().toLowerCase()).append(" - ");
				}
				if (!column_title.getText().isEmpty()) {
					memo.append(column_title.getText());
				} else {
					memo.append(column_seller.getText());
				}
				if (column_quantity.getAmount() != 1) {
					memo.append(" x" + column_quantity.getAmount());
				}
				item.memo = memo.toString();

				item.id = column_id.getText();
				item.quantity = column_quantity.getAmount();
				item.unitPrice = column_unitPrice.getAmount();
				item.subtotal = column_subtotal.getAmount();

				rowItems.add(item);
				return true;
			} else {
				/*
				 * Shipment dates can be out of order and rows with the same shipment
				 * date separated.  However rows with the same order ids are kept together,
				 * so we can't be sure we are done with a processor until we see a different
				 * order id.
				 */
				done = !orderId.equals(column_orderId.getText());
				return false;
			}
		}

		protected void addItemsToTransaction(Transaction trans) {
			for (ItemRow rowItem2 : rowItems) {
				AmazonEntry itemEntry = trans.createEntry().getExtension(AmazonEntryInfo.getPropertySet(), true);
				itemEntry.setAccount(unknownAmazonPurchaseAccount);
				itemEntry.setAmount(rowItem2.unitPrice * rowItem2.quantity);
				itemEntry.setMemo(rowItem2.memo);
				itemEntry.setOrderId(orderId);
				itemEntry.setTrackingNumber(trackingNumber);
				itemEntry.setShipmentDate(shipmentDate);
				itemEntry.setAsinOrIsbn(rowItem2.id);
			
				/*
				 * See http://superuser.com/questions/123911/how-to-get-the-full-image-when-the-shopping-site-like-amazon-shows-you-only-a-pa
				 */
				String urlString = MessageFormat.format(
						"http://z2-ec2.images-amazon.com/images/P/{0}.01._SX_SCRMZZZZZZZ_V217104471.jpg",
						rowItem2.id);
//				try {
//					URL picture = new URL(urlString);
//					itemEntry.setPicture(new UrlBlob(picture));
//				} catch (MalformedURLException e) {
//					// Should not happen so convert to an unchecked exception
//					throw new RuntimeException(e);
//				}
			}
		}

		@Override
		public boolean isDone() {
			return done;
		}
	}

	/**
	 * This class handles the import of items from the 'items' file when the matching record from the
	 * 'orders' file has already been imported.
	 * 
	 *  In this case the list of items replaces the entry in the unmatched entries account.
	 */
	public class ItemsShippedTransactionMatched extends ItemsShippedTransaction {

		private AmazonEntry matchingEntry;

		public ItemsShippedTransactionMatched(IncomeExpenseAccount unmatchedAccount, IncomeExpenseAccount unknownAmazonPurchaseAccount, AmazonEntry matchingEntry) throws ImportException {
			super(unmatchedAccount, unknownAmazonPurchaseAccount);
			this.matchingEntry = matchingEntry;
		}

		@Override
		public void createTransaction(Session session) throws ImportException {
			// Modify the existing transaction
			Transaction trans = matchingEntry.getTransaction();

			// TODO worry about shipping and handling and stuff like that.

			// Distribute the shipping and handling amount
			//			distribute(shippingAndHandlingAmount, rowItems);

			addItemsToTransaction(trans);

			// Remove the unmatched entry as it is being replaced by
			// the items.
			trans.deleteEntry(matchingEntry.getBaseObject());

			assertValid(trans);
		}

	}

	/**
	 * This class handles the import of items from the 'items' file when no matching record from the
	 * 'orders' file has yet been imported.
	 * 
	 *  In this case the list of items is created.  However we don't know the total charged because details
	 *  such as shipping costs are in the 'orders' file.  We therefore put an entry in the 'unmatched' account
	 *  with the total cost of all the items.
	 */
	public class ItemsShippedTransactionUnmatched extends ItemsShippedTransaction {

		public ItemsShippedTransactionUnmatched(IncomeExpenseAccount unmatchedAccount, IncomeExpenseAccount unknownAmazonPurchaseAccount, Date date, String orderId) throws ImportException {
			super(unmatchedAccount, unknownAmazonPurchaseAccount);
		}

		@Override
		public void createTransaction(Session session) throws ImportException {
			// Start a new transaction
			Transaction trans = session.createTransaction();
			trans.setDate(orderDate);

			long total = 0;
			for (ItemRow rowItem2 : rowItems) {
				total += rowItem2.unitPrice * rowItem2.quantity;
			}

			// Create a single entry in the "unmatched entries" account
			AmazonEntry mainEntry = trans.createEntry().getExtension(AmazonEntryInfo.getPropertySet(), true);
			mainEntry.setAccount(unmatchedAccount);
			mainEntry.setAmount(-total);
			mainEntry.setTrackingNumber(trackingNumber);
			mainEntry.setShipmentDate(shipmentDate);
			mainEntry.setOrderId(orderId);

			addItemsToTransaction(trans);

			assertValid(trans);
		}

	}

	private void assertValid(Transaction trans) {
		long total = 0;
		for (Entry entry : trans.getEntryCollection()) {
			total += entry.getAmount();
		}
		if (total != 0) {
			throw new RuntimeException("unbalanced");
		}
	}

	public class ItemRow {
		public String memo;
		public String id;
		public long quantity;
		public long unitPrice; 
		public long subtotal;
	}
}
