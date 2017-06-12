package net.sf.jmoney.amazon.csv;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.eclipse.ui.IImportWizard;

import net.sf.jmoney.amazon.AccountFinder;
import net.sf.jmoney.amazon.AmazonEntry;
import net.sf.jmoney.amazon.AmazonEntryInfo;
import net.sf.jmoney.amazon.UrlBlob;
import net.sf.jmoney.importer.wizards.CsvImportWizard;
import net.sf.jmoney.importer.wizards.CsvTransactionReader;
import net.sf.jmoney.importer.wizards.ImportException;
import net.sf.jmoney.importer.wizards.MultiRowTransaction;
import net.sf.jmoney.model2.Account;
import net.sf.jmoney.model2.BankAccount;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.IncomeExpenseAccount;
import net.sf.jmoney.model2.Session;
import net.sf.jmoney.model2.Transaction;
import net.sf.jmoney.model2.TransactionManagerForAccounts;

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

	Pattern imageUrlPattern;

	@Override
	protected void startImport(TransactionManagerForAccounts transactionManager) throws ImportException {
		this.session = transactionManager.getSession();

		try {
			// src="(http:\/\/ecx\.images\-amazon\.com\/images\/[A-Z]\/[A-Z,a-z,0-9]*\._AA160_\.jpg)" class="productImage" alt="Product Details"
			imageUrlPattern = Pattern.compile("src=\"(http:\\/\\/ecx\\.images\\-amazon\\.com\\/images\\/[A-Z]\\/[A-Z,a-z,0-9]*\\._AA160_\\.jpg)\" class=\"productImage\" alt=\"Product Details\"");
		} catch (PatternSyntaxException e) {
			throw new RuntimeException("pattern failed", e);
		}
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
	public void importLine(CsvTransactionReader reader) throws ImportException {
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
		 * Find the account to which this entry has been charged.
		 */
		String lastFourDigits = column_paymentCard.getText();
		if (lastFourDigits == null) {
			throw new ImportException("Last four digits of payment card cannot be blank.");
		}

		Account chargedAccount;
		AccountFinder accountFinder;
		if (lastFourDigits.equals("Gift Certificate/Card")) {

			// TODO figure out actual currency of gift certificate
			accountFinder = new AccountFinder(session, "USD");

			/*
			 * Look for an income and expense account that can be used by default for items where
			 * the 'Payment - Last 4 Digits' column contains 'Gift Certificate/Card'.
			 */
			BankAccount giftCardAccount = accountFinder.findGiftcardAccount();

			chargedAccount = giftCardAccount;
		} else {
			if (lastFourDigits.length() != 4) {
				throw new ImportException("Last four digits of payment card must be 4 digits or indicate a gift certificate.");
			}

			BankAccount chargedBankAccount = AccountFinder.findChargeAccount(getShell(), session, lastFourDigits);
			accountFinder = new AccountFinder(session, chargedBankAccount.getCurrency());
			chargedAccount = chargedBankAccount;
		}

		IncomeExpenseAccount unmatchedAccount = accountFinder.findUnmatchedAccount();

		/*
		 * Look in the unmatched entries account for an entry that matches on order id and shipment date.
		 */
		AmazonEntry matchingEntry = findMatchingEntry(orderId,	trackingNumber, shipmentDate, unmatchedAccount);

		/*
		 * Look for an income and expense account that can be used by default for the purchases.
		 * The currency of this account must match the currency of the charge account.
		 */
		IncomeExpenseAccount unknownAmazonPurchaseAccount = accountFinder.findDefaultPurchaseAccount();

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

//				/**
//				 * Submit a search for the ASIN to Amazon, then we look at the html
//				 * that comes back and find in it the URL to the picture.
//				 */
//				String urlString2 = MessageFormat.format(
//						"http://www.amazon.com/s/ref=nb_sb_noss?url=search-alias%3Daps&field-keywords={0}",
//						rowItem2.id);
//				try {
//					URL searchResultsPage = new URL(urlString2);
//					InputStream is = searchResultsPage.openStream();
//					String sourceHtml = convertStreamToString(is);
//					Matcher m = imageUrlPattern.matcher(sourceHtml);
//					if (!m.find()) {
//						throw new RuntimeException("no image found");
//					}
//					String imageUrlString = m.group(1);
//					if (m.find()) {
//						throw new RuntimeException("more than one image found");
//					}
//					System.out.println(imageUrlString);
//					URL picture = new URL(imageUrlString);
//					itemEntry.setPicture(new UrlBlob(picture));
//				} catch (MalformedURLException e) {
//					// Should not happen so convert to an unchecked exception
//					throw new RuntimeException(e);
//				} catch (IOException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}

				/*
				 * See http://superuser.com/questions/123911/how-to-get-the-full-
				 * image-when-the-shopping-site-like-amazon-shows-you-only-a-pa
				 * and also http://aaugh.com/imageabuse.html
				 */
				String urlString = MessageFormat
						.format("http://z2-ec2.images-amazon.com/images/P/{0}.01._SX_SCRMZZZZZZZ_V217104471.jpg",
								rowItem2.id);
				try {
					URL picture = new URL(urlString);
					itemEntry.setPicture(new UrlBlob(picture));
				} catch (MalformedURLException e) {
					// Should not happen so convert to an unchecked exception
					throw new RuntimeException(e);
				}
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

			addItemsToTransaction(trans);

			// Remove the unmatched entry as it is being replaced by
			// the items.
			trans.deleteEntry(matchingEntry.getBaseObject());

			AccountFinder.assertValid(trans);
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

			AccountFinder.assertValid(trans);
		}
	}

	public class ItemRow {
		public String memo;
		public String id;
		public long quantity;
		public long unitPrice;
		public long subtotal;
	}

	private String convertStreamToString(InputStream is) throws IOException {
		/*
		 * To convert the InputStream to String we use the Reader.read(char[]
		 * buffer) method. We iterate until the Reader return -1 which means
		 * there's no more data to read. We use the StringWriter class to
		 * produce the string.
		 */
		Writer writer = new StringWriter();
		char[] buffer = new char[1024];
		try {
			Reader reader = new BufferedReader(new InputStreamReader(is,
					"UTF-8"));
			int n;
			while ((n = reader.read(buffer)) != -1) {
				writer.write(buffer, 0, n);
			}
		} finally {
			is.close();
		}
		return writer.toString();
	}

	@Override
	protected String getDescription() {
		return "The selected CSV file will be imported.  The CSV file must have been exported from Amazon " +
				"as an 'item' export.  This can be done if orders are made through amazon.com.  If you ordered through amazon.co.uk then you will not be able to get an item report.  You must instead use screen scraping (Amazon HTML import) which is quite a pain. " +
		"You must import both orders and items into JMoney because not all the information is in either export.  JMoney will match the data in the imports to obtain a single transaction for each order.";
	}
}
