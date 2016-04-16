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

package net.sf.jmoney.importer.wizards;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.sf.jmoney.associations.model.AccountAssociation;
import net.sf.jmoney.associations.model.AccountAssociationsExtension;
import net.sf.jmoney.associations.model.AccountAssociationsInfo;
import net.sf.jmoney.importer.Activator;
import net.sf.jmoney.importer.matcher.EntryData;
import net.sf.jmoney.importer.matcher.IPatternMatcher;
import net.sf.jmoney.importer.matcher.ImportEntryProperty;
import net.sf.jmoney.importer.matcher.ImportMatcher;
import net.sf.jmoney.importer.matcher.PatternMatchingDialog;
import net.sf.jmoney.importer.model.PatternMatcherAccount;
import net.sf.jmoney.importer.model.PatternMatcherAccountInfo;
import net.sf.jmoney.importer.model.ReconciliationEntryInfo;
import net.sf.jmoney.importer.model.TransactionType;
import net.sf.jmoney.importer.model.TransactionTypeBasic;
import net.sf.jmoney.model2.Account;
import net.sf.jmoney.model2.CapitalAccount;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.IDataManagerForAccounts;
import net.sf.jmoney.model2.Session;
import net.sf.jmoney.model2.TransactionManagerForAccounts;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.IWorkbenchWindow;

/**
 * A wizard to import data from a comma-separated file that has been down-loaded
 * into a file on the local machine.
 * <P>
 * This wizard is a single page wizard that asks only for the file.
 */
public abstract class CsvImportToAccountWizard extends CsvImportWizard implements IAccountImportWizard {

	private Account accountOutsideTransaction;

	/**
	 * Set when <code>importFile</code> is called.
	 */
	protected Account accountInsideTransaction;

	protected Collection<EntryData> importedEntries = new ArrayList<EntryData>();


	public CsvImportToAccountWizard() {
		IDialogSettings workbenchSettings = Activator.getDefault().getDialogSettings();
		IDialogSettings section = workbenchSettings.getSection("CsvImportToAccountWizard");//$NON-NLS-1$
		if (section == null) {
			section = workbenchSettings.addNewSection("CsvImportToAccountWizard");//$NON-NLS-1$
		}
		setDialogSettings(section);
	}

	/**
	 * This form of this method is called when the wizard is initiated from JMoney
	 * code and a Paypal account is available from the context.
	 * <P>
	 * We will cache window object in order to be able to provide parent shell
	 * for the message dialog.
	 */
	@Override
	public void init(IWorkbenchWindow window, Account account) {
		this.window = window;
		this.accountOutsideTransaction = account;

		mainPage = new CsvImportWizardPage(window, getDescription());
		addPage(mainPage);
	}

	/**
	 * Given an id for an account association, returns the account that is associated with the
	 * account into which we are importing.
	 * <P>
	 * This account is inside the transaction.
	 * 
	 * @param id
	 * @return
	 */
	protected Account getAssociatedAccount(String id) {
		AccountAssociationsExtension a = accountInsideTransaction.getExtension(AccountAssociationsInfo.getPropertySet(), false);
		if (a != null) {
			for (AccountAssociation aa : a.getAssociationCollection()) {
				if (aa.getId().equals(id)) {
					return aa.getAccount();
				}
			}
		}
		return null;
	}

	@Override
	protected void startImport(TransactionManagerForAccounts transactionManager) throws ImportException {
		accountInsideTransaction = transactionManager.getCopyInTransaction(accountOutsideTransaction);
		setAccount(accountInsideTransaction);
	}

	protected abstract void setAccount(Account accountInsideTransaction) throws ImportException;

	protected void addEntryToBeProcessed(EntryData entryData) {
		importedEntries.add(entryData);
	}
	
	@Override
	protected boolean processRows(Session session) throws IOException, ImportException {
		if (!super.processRows(session)) {
			return false;
		}
		
		/*
		 * Import the entries using the matcher dialog
		 */

		PatternMatcherAccount matcherAccount = accountInsideTransaction.getExtension(PatternMatcherAccountInfo.getPropertySet(), true);

		// Ameritrade has no default account or pattern matching at all,
		// so we need this test.
		if (matcherAccount.isReconcilable()) {
		/**
		 * We can't import if there is no default category into which
		 * entries can be put.
		 */
		// TODO actually this is not entirely true.  Some importers (i.e. Paypal)
		// set a default which is overwritten by any pattern matches but is not
		// overwritten by the default.  A default should not be required in these
		// cases.  We really need to consolidate the way we handle default accounts
		// (set here somehow based on the currency or set by the importer)
		if (matcherAccount.getDefaultCategory() == null) {
			MessageDialog.openError(window.getShell(), "Import Error", MessageFormat.format("No default category is set for {0}.", accountInsideTransaction.getName()));
			return false;
		}

		/*
		 * If any entries need match processing, do that now.
		 */
		if (!importedEntries.isEmpty()) {
//			// HACK - convert Entry to EntryData
//			Collection<EntryData> importedEntries2 = new ArrayList<EntryData>();
//
//			for (Entry entry : importedEntries) {
//				EntryData entryData = new EntryData();
//				entryData.amount = entry.getAmount();
//				entryData.check = entry.getCheck();
//				entryData.valueDate = entry.getTransaction().getDate();
//				entryData.clearedDate = entry.getValuta();
//				entryData.setMemo(entry.getMemo());
//				entryData.setPayee(entry.getMemo());
//				importedEntries2.add(entryData);
//			}

			/*
			 * All changes within this dialog are made within a transaction, so canceling
			 * is trivial (the transaction is simply not committed).
			 */
			// TODO simplify these three lines...
			TransactionManagerForAccounts transactionManager = new TransactionManagerForAccounts(accountOutsideTransaction.getDataManager());
			CapitalAccount accountInTransaction = transactionManager.getCopyInTransaction(matcherAccount.getBaseObject());
			IPatternMatcher patternMatcher = accountInTransaction.getExtension(PatternMatcherAccountInfo.getPropertySet(), true);

			Dialog dialog = new PatternMatchingDialog(window.getShell(), patternMatcher, importedEntries, Arrays.asList(getImportEntryProperties()), getApplicableTransactionTypes());
			int returnCode = dialog.open();
			
			if (returnCode == Dialog.OK || returnCode == PatternMatchingDialog.SAVE_PATTERNS_ONLY) {
				// All edits are transferred to the model as they are made,
				// so we just need to commit them.
				transactionManager.commit("Change Import Options");
			}
			
			if (returnCode == Dialog.OK) {
				ImportMatcher matcher = new ImportMatcher(patternMatcher, Arrays.asList(getImportEntryProperties()), getApplicableTransactionTypes());

				Set<Entry> ourEntries = new HashSet<Entry>();
				for (EntryData entryData: importedEntries) {
					Entry entry = matcher.process(entryData, accountInsideTransaction.getSession(), ourEntries);
					//				ReconciliationEntryInfo.getUniqueIdAccessor().setValue(entry, entryData.uniqueId);
					ourEntries.add(entry);
				}

				return true;
			} else {
				return false;
			}
		}
		}
		
		return true;
	}

	public ImportEntryProperty[] getImportEntryProperties() {
		return new ImportEntryProperty [] {
				new ImportEntryProperty("memo", "Memo") {
					protected String getCurrentValue(EntryData importEntry) {
						return importEntry.getMemo();
					}
				},
		};
	}

	/**
	 * This method returns a label that describes the source and is suitable for use
	 * in labels and messages shown to the user.  This will typically be the name of the
	 * bank or brokerage firm.
	 */
	protected abstract String getSourceLabel();

	/**
	 * This is mostly a copy of the method from the base class.  However it is different
	 * because it collects all the import entries into an array first.  This allows it to
	 * present all the entries to the user in a dialog before any are committed.
	 */
	@Override
	public boolean importFile(File file) {
		if (newWay()) {
			return importFile2(file);
		} else {
			return super.importFile(file);
		}
	}

	protected boolean newWay() {
		return false;
	}

	protected void importLine(CsvTransactionReader reader, Collection<EntryData> entryDataList) throws ImportException {
		throw new RuntimeException("but we are not doing this the new way...");
	}

	/**
	 * This is mostly a copy of the method from the base class.  However it is different
	 * because it collects all the import entries into an array first.  This allows it to
	 * present all the entries to the user in a dialog before any are committed.
	 */
	public boolean importFile2(File file) {

		IDataManagerForAccounts datastoreManager = (IDataManagerForAccounts)window.getActivePage().getInput();
		if (datastoreManager == null) {
			MessageDialog.openError(window.getShell(), "Unavailable", "You must open an accounting session before you can create an account.");
			return false;
		}
		
		try {
			/*
			 * Create a transaction to be used to import the entries.  This allows the entries to
			 * be more efficiently written to the back-end datastore and it also groups
			 * the entire import as a single change for undo/redo purposes.
			 */
			TransactionManagerForAccounts transactionManager = new TransactionManagerForAccounts(datastoreManager);
			Session session = transactionManager.getSession();

			startImport(transactionManager);
			
			reader = getCsvTransactionReader(file);
        	
			/*
			 * Read the data
			 */
			while (!reader.isEndOfFile()) {
				importLine(reader, importedEntries);
				reader.readNext();
		    }
			
			for (MultiRowTransaction currentMultiRowProcessor : currentMultiRowProcessors) {
				currentMultiRowProcessor.createTransaction(session);
			}
			
			/*
			 * Import the entries using the matcher dialog
			 */
			return doImport(transactionManager, file);
					
		} catch (FileNotFoundException e) {
			// This should not happen because the file dialog only allows selection of existing files.
			throw new RuntimeException(e);
		} catch (IOException e) {
			// This is probably not likely to happen so the default error handling is adequate.
			throw new RuntimeException(e);
		} catch (ImportException e) {
			// There are data in the import file that we are unable to process
			e.printStackTrace();
			MessageDialog.openError(window.getShell(), "Errors in the downloaded file", e.getMessage());
			return false;
		} catch (Exception e) {
			// There are data in the import file that we are unable to process
			e.printStackTrace();
			MessageDialog.openError(window.getShell(), "Errors in the downloaded file", e.getMessage());
			return false;
		}
	}

	/**
	 * This method was split out so that this code can be replaced in the new and improved stock account
	 * importers.
	 * 
	 * @param transactionManager
	 * @param file
	 * @return
	 */
	protected boolean doImport(TransactionManagerForAccounts transactionManager, File file) {
		PatternMatcherAccount matcherAccount = accountInsideTransaction.getExtension(PatternMatcherAccountInfo.getPropertySet(), true);
		
		/**
		 * We can't import if there is no default category into which
		 * entries can be put.
		 */
		if (matcherAccount.getDefaultCategory() == null) {
			MessageDialog.openError(window.getShell(), "Import Error", MessageFormat.format("No default category is set for {0}.", accountInsideTransaction.getName()));
			return false;
		}

		/*
		 * All changes within this dialog are made within a transaction, so canceling
		 * is trivial (the transaction is simply not committed).
		 */
		// TODO simplify these three lines...
		TransactionManagerForAccounts transactionManager2 = new TransactionManagerForAccounts(accountOutsideTransaction.getDataManager());
		CapitalAccount accountInTransaction2 = transactionManager2.getCopyInTransaction(matcherAccount.getBaseObject());
		IPatternMatcher patternMatcher = accountInTransaction2.getExtension(PatternMatcherAccountInfo.getPropertySet(), true);
		
		List<TransactionType> applicableTransactionTypes = getApplicableTransactionTypes();
		Dialog dialog = new PatternMatchingDialog(window.getShell(), patternMatcher, importedEntries, Arrays.asList(getImportEntryProperties()), applicableTransactionTypes);
		int returnCode = dialog.open();
		
		if (returnCode == Dialog.OK || returnCode == PatternMatchingDialog.SAVE_PATTERNS_ONLY) {
			// All edits are transferred to the model as they are made,
			// so we just need to commit them.
			transactionManager2.commit("Change Import Options");
		}
		
		if (returnCode == Dialog.OK) {
			ImportMatcher matcher = new ImportMatcher(matcherAccount, Arrays.asList(getImportEntryProperties()), getApplicableTransactionTypes());

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
			String transactionDescription = MessageFormat.format("Import {0}", file.getName());
			transactionManager.commit(transactionDescription);									

			return true;
		} else {
			return false;
		}
	}

	/**
	 * Note that this list is not cached, meaning new instances will be created
	 * for each call to this method.
	 * 
	 * @param account
	 * @return
	 */
	public List<TransactionType> getApplicableTransactionTypes() {
			List<TransactionType> result = new ArrayList<TransactionType>();

			result.add(new TransactionTypeBasic());

			return result;
	}

}