package net.sf.jmoney.ebay;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import net.sf.jmoney.associations.AssociationMetadata;
import net.sf.jmoney.fields.IAmountFormatter;
import net.sf.jmoney.importer.MatchingEntryFinder;
import net.sf.jmoney.importer.matcher.EntryData;
import net.sf.jmoney.importer.matcher.ImportEntryProperty;
import net.sf.jmoney.importer.matcher.ImportMatcher;
import net.sf.jmoney.importer.matcher.PatternMatchingDialog;
import net.sf.jmoney.importer.model.PatternMatcherAccount;
import net.sf.jmoney.importer.model.PatternMatcherAccountInfo;
import net.sf.jmoney.importer.model.ReconciliationEntryInfo;
import net.sf.jmoney.importer.model.TransactionType;
import net.sf.jmoney.importer.model.TransactionTypeBasic;
import net.sf.jmoney.importer.wizards.IAccountImportWizard;
import net.sf.jmoney.importer.wizards.ImportException;
import net.sf.jmoney.importer.wizards.TxrMismatchException;
import net.sf.jmoney.isolation.TransactionManager;
import net.sf.jmoney.model2.Account;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.IDataManagerForAccounts;
import net.sf.jmoney.model2.TransactionManagerForAccounts;

import txr.matchers.DocumentMatcher;
import txr.matchers.MatchResults;
import txr.parser.TxrErrorInDocumentException;

public class EbayCopytextImportWizard extends Wizard implements IAccountImportWizard<EntryData> {

	private IWorkbenchWindow window;

	private Account accountOutsideTransaction;
	
	/**
	 * Set when <code>performFinish</code> is called.
	 */
	protected Account accountInsideTransaction;

	private EbayImportWizardPage mainPage;


	@Override
	public void init(IWorkbenchWindow window, Account account) {
		this.window = window;
		this.accountOutsideTransaction = account;

		mainPage = new EbayImportWizardPage(window);
		addPage(mainPage);
	}

	@Override
	public AssociationMetadata[] getAssociationMetadata() {
		return new AssociationMetadata[] {
		};
	}

	@Override
	public List<TransactionType<EntryData>> getApplicableTransactionTypes() {
		return Collections.singletonList(new TransactionTypeBasic());
	}

	@Override
	public boolean performFinish() {
		IDataManagerForAccounts sessionManager = (IDataManagerForAccounts)window.getActivePage().getInput();
		if (sessionManager == null) {
			MessageDialog waitDialog = new MessageDialog(
					window.getShell(),
					"Disabled Action Selected",
					null, // accept the default window icon
					"You cannot import data into an accounting session unless you have a session open.  You must first open a session or create a new session.",
					MessageDialog.ERROR,
					new String[] { IDialogConstants.OK_LABEL }, 0);
			waitDialog.open();
			return false;
		}

		try {
			/*
			 * Create a transaction to be used to import the entries.  This allows the entries to
			 * be more efficiently written to the back-end datastore and it also groups
			 * the entire import as a single change for undo/redo purposes.
			 */
			TransactionManagerForAccounts transactionManager = new TransactionManagerForAccounts(sessionManager);

			accountInsideTransaction = transactionManager.getCopyInTransaction(accountOutsideTransaction);
			Collection<EntryData> importedEntries = new ArrayList<EntryData>();
			
			processFromClipboard(accountOutsideTransaction, transactionManager, importedEntries);
			
			/*
			 * Import the entries using the matcher dialog
			 */
			
			PatternMatcherAccount matcherAccount = accountInsideTransaction.getExtension(PatternMatcherAccountInfo.getPropertySet(), true);
			
			Dialog dialog = new PatternMatchingDialog<EntryData>(window.getShell(), matcherAccount, importedEntries, getImportEntryProperties(), getApplicableTransactionTypes());
			int returnCode = dialog.open();
			
			if (returnCode == Dialog.OK || returnCode == PatternMatchingDialog.SAVE_PATTERNS_ONLY) {
				// All edits are transferred to the model as they are made,
				// so we just need to commit them.
				transactionManager.commit("Change Import Options");
			}
			
			if (returnCode != Dialog.OK) {
				return false;
			}

			ImportMatcher<EntryData> matcher = new ImportMatcher<>(matcherAccount, getImportEntryProperties(), getApplicableTransactionTypes(), null);

			Set<Entry> ourEntries = new HashSet<Entry>();
			for (EntryData entryData: importedEntries) {
				Entry entry = matcher.process(entryData, transactionManager.getSession(), ourEntries);

				ReconciliationEntryInfo.getUniqueIdAccessor().setValue(entry, entryData.uniqueId);

				ourEntries.add(entry);
			}

			/*
			 * All entries have been imported and all the properties
			 * have been set and should be in a valid state, so we
			 * can now commit the imported entries to the datastore.
			 */
			if (transactionManager.hasChanges()) {
				transactionManager.commit("Import");
				MessageDialog.openInformation(window.getShell(), "Clipboard imported", "The Ebay order history in the clipboard was successfully imported.");
			} else {
				MessageDialog.openWarning(window.getShell(), "Ebay orders not imported",
					"The Ebay order history in the clipboard was not imported because all the data in it had already been imported."
				);
			}
		} catch (ImportException e) {
			MessageDialog.openError(window.getShell(), "Unable to import Ebay orders", e.getLocalizedMessage());
			return false;
		} catch (TxrMismatchException e) {
			e.showInDebugView(window);
		}

		return true;
	}

	private MatchResults doMatchingFromClipboard(DocumentMatcher matcher, String resourceName) throws TxrMismatchException {
		Display display = Display.getCurrent();
		Clipboard clipboard = new Clipboard(display);
		String plainText = (String)clipboard.getContents(TextTransfer.getInstance());
		clipboard.dispose();        

		MatchResults bindings = matcher.process(plainText);
		if (bindings == null || bindings.getCollections(0).isEmpty()) {
			ClassLoader classLoader = getClass().getClassLoader();
			URL resource = classLoader.getResource(resourceName);
			throw new TxrMismatchException(resource, plainText, "EBay page");
		}

		return bindings;
	}

	private DocumentMatcher createMatcherFromResource(String resourceName) {
		ClassLoader classLoader = getClass().getClassLoader();
		URL resource = classLoader.getResource(resourceName);
		try (InputStream txrInputStream = resource.openStream()) {
			return new DocumentMatcher(txrInputStream, "UTF-8");
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		} catch (TxrErrorInDocumentException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	/**
	 * 
	 * @param accountOutsideTransaction
	 * @param transactionManager
	 * @param importedEntries
	 * @return the date of the imported statement
	 * @throws ImportException
	 * @throws TxrMismatchException 
	 */
	private void processFromClipboard(Account accountOutsideTransaction, TransactionManager transactionManager, Collection<EntryData> importedEntries) throws ImportException, TxrMismatchException {
		DocumentMatcher statementMatcher = createMatcherFromResource("ebay-orders.txr");

		MatchResults bindings = doMatchingFromClipboard(statementMatcher, "ebay-orders.txr");

		// TODO lookup the ebay account from the username?
		DateFormat dateFormat = new SimpleDateFormat("dd MMM, yyyy");

		String userName = bindings.getVariable("username").text;
		
		for (MatchResults transactionBindings : bindings.getCollections(0)) {
			String dateAsString = transactionBindings.getVariable("orderdate").text;
			String description = transactionBindings.getVariable("description").text;
			String amountAsStringWithCurrency = transactionBindings.getVariable("amount").text;
			String orderNumber = transactionBindings.getVariable("ordernumber").text;
			String seller = transactionBindings.getVariable("seller").text;

			String amountAsString = amountAsStringWithCurrency.replace("�", "");

			Date transactionDate;
			try {
				transactionDate = dateFormat.parse(dateAsString);
			} catch (ParseException e) {
				// TODO Return as error to TXR when that is supported???
				e.printStackTrace();
				throw new RuntimeException("bad date", e);
			}

			BigDecimal amountAsDecimal = new BigDecimal(amountAsString.replaceAll(",", ""));
			long amount = amountAsDecimal.scaleByPowerOfTen(2).longValueExact();

			/*
			 * See if an entry already exists with this order number.
			 * 
			 * Note that we check entries outside the transaction.  This ensures
			 * that if multiple entries in this import have the same order number then they
			 * all get imported.
			 */
			boolean matchFound = false;
			for (Entry entry : accountOutsideTransaction.getEntries()) {
				if (orderNumber.equals(ReconciliationEntryInfo.getUniqueIdAccessor().getValue(entry))) {
					// This row has already been imported so ignore it.
					matchFound = true;
					break;
				}
			}
			if (matchFound) {
				continue;
			}
			
			/**
			 * A 'matcher' that will match if an entry has not already been matched
			 * to an imported entry and if the entry appears to match based on date,
			 * amount, and check number.
			 */
			MatchingEntryFinder matchFinder = new MatchingEntryFinder() {
				@Override
				protected boolean doNotConsiderEntryForMatch(Entry entry) {
					return ReconciliationEntryInfo.getUniqueIdAccessor().getValue(entry) != null;
				}

				@Override
				protected boolean nearEnoughMatches(Date dateOfExistingTransaction, Date dateInImport, Entry entry) {
						return isDateInRange(dateInImport, dateOfExistingTransaction, 3);
				}
			};

			/*
			 * First we try auto-matching.
			 *
			 * If we have an auto-match then we don't have to create a new
			 * transaction at all. We just update a few properties in the
			 * existing entry.
			 */
			Entry match = matchFinder.findMatch(accountOutsideTransaction, -amount, transactionDate);
			if (match != null) {
				Entry entryInTrans = transactionManager.getCopyInTransaction(match);
				entryInTrans.setValuta(transactionDate);
				ReconciliationEntryInfo.getUniqueIdAccessor().setValue(entryInTrans, orderNumber);
			} else {
				/*
				 * No existing entry matches, either on FITID or by matching dates and amounts,
				 * so we need to create a new transaction.
				 */
				EntryData entryData = new EntryData();
				entryData.amount = -amount;
				entryData.valueDate = transactionDate;
				entryData.clearedDate = transactionDate;
				entryData.setMemo(description);
				entryData.setPayee(seller);
				entryData.uniqueId = orderNumber;

				importedEntries.add(entryData);
			}
		}
	}

	public List<ImportEntryProperty<EntryData>> getImportEntryProperties() {
		return new ArrayList<ImportEntryProperty<EntryData>>() {
			private static final long serialVersionUID = 1L;

			{
				add(new ImportEntryProperty<EntryData>("memo", "Memo") {
					@Override
					protected String getCurrentValue(EntryData importEntry) {
						return importEntry.getMemo();
					}
				});
				add(new ImportEntryProperty<EntryData>("amount", "Amount") {
					@Override
					protected String getCurrentValue(EntryData importEntry) {
						// As we don't have an account accessible that would give us a currency, just format using GBP
						// TODO There must be a better way...
						IDataManagerForAccounts sessionManager = (IDataManagerForAccounts)window.getActivePage().getInput();
						IAmountFormatter formatter = sessionManager.getSession().getCurrencyForCode("GBP");
						return formatter.format(importEntry.amount);
					}
				});
			}
		};
	}

}
