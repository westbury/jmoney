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
import java.io.FileReader;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;

import net.sf.jmoney.importer.Activator;
import net.sf.jmoney.importer.matcher.EntryData;
import net.sf.jmoney.importer.matcher.ImportMatcher;
import net.sf.jmoney.importer.matcher.PatternMatchingDialog;
import net.sf.jmoney.importer.model.AccountAssociation;
import net.sf.jmoney.importer.model.ImportAccount;
import net.sf.jmoney.importer.model.ImportAccountInfo;
import net.sf.jmoney.importer.model.PatternMatcherAccount;
import net.sf.jmoney.importer.model.PatternMatcherAccountInfo;
import net.sf.jmoney.importer.model.ReconciliationEntryInfo;
import net.sf.jmoney.model2.Account;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.IDataManagerForAccounts;
import net.sf.jmoney.model2.Session;
import net.sf.jmoney.model2.TransactionManagerForAccounts;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.IWorkbenchWindow;

import au.com.bytecode.opencsv.CSVReader;

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
	private Account accountInsideTransaction;


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
		ImportAccount a = accountInsideTransaction.getExtension(ImportAccountInfo.getPropertySet(), false);
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

	protected void importLine(String[] line, Collection<EntryData> entryDataList) throws ImportException {
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
			
        	reader = new CSVReader(new FileReader(file));
        	rowNumber = 0;
        	
    		/*
    		 * Get the list of expected columns, validate the header row, and set the column indexes
    		 * into the column objects.  It would be possible to allow the columns to be in any order or
    		 * to allow columns to be optional, setting the column indexes here based on the column in
    		 * which the matching header was found.
    		 * 
    		 * At this time, however, there is no known requirement for that, so we simply validate that
    		 * the first row contains exactly these columns in this order and set the indexes sequentially.
    		 * 
    		 * We trim the text in the header.  This is helpful because some banks add spaces.  For example
    		 * Paypal puts a space before the text in each header cell.
    		 */
			String headerRow[] = readHeaderRow();
			
    		ImportedColumn[] expectedColumns = getExpectedColumns();
    		for (int columnIndex = 0; columnIndex < expectedColumns.length; columnIndex++) {
    			if (expectedColumns[columnIndex] != null) {
    				if (!headerRow[columnIndex].trim().equals(expectedColumns[columnIndex].getName())) {
    					MessageDialog.openError(getShell(), "Unexpected Data", "Expected '" + expectedColumns[columnIndex].getName()
    							+ "' in row 1, column " + (columnIndex+1) + " but found '" + headerRow[columnIndex] + "'.");
    					return false;
    				}
    				expectedColumns[columnIndex].setColumnIndex(columnIndex);
    			}
    		}

			/*
			 * Read the data
			 */
			
    		Collection<EntryData> importedEntries = new ArrayList<EntryData>();

			currentLine = readNext();
			while (currentLine != null) {
				
				/*
				 * If it contains a single empty string then we ignore this line but we don't terminate.
				 * Nationwide Building Society puts such a line after the header.
				 */
				if (currentLine.length == 1 && currentLine[0].isEmpty()) {
					currentLine = readNext();
					continue;
				}
				
				/*
				 * There may be extra columns in the file that we ignore, but if there are
				 * fewer columns than expected then we can't import the row.
				 */
				if (currentLine.length < expectedColumns.length) {
					break;
				}
				
				importLine(currentLine, importedEntries);
		        
		        currentLine = readNext();
		    }
			
			if (currentLine != null) {
				// Ameritrade contains this.
				assert (currentLine.length == 1);
				assert (currentLine[0].equals("***END OF FILE***"));
			}

			for (MultiRowTransaction currentMultiRowProcessor : currentMultiRowProcessors) {
				currentMultiRowProcessor.createTransaction(session);
			}
			
			/*
			 * Import the entries using the matcher dialog
			 */
			
			PatternMatcherAccount matcherAccount = accountInsideTransaction.getExtension(PatternMatcherAccountInfo.getPropertySet(), true);
			
			/**
			 * We can't import if there is no default category into which
			 * entries can be put.
			 */
			if (matcherAccount.getDefaultCategory() == null) {
				MessageDialog.openError(window.getShell(), "Import Error", MessageFormat.format("No default category is set for {0}.", accountInsideTransaction.getName()));
				return false;
			}

			Dialog dialog = new PatternMatchingDialog(window.getShell(), matcherAccount, importedEntries);
			if (dialog.open() == Dialog.OK) {
				ImportMatcher matcher = new ImportMatcher(matcherAccount);

				for (EntryData entryData: importedEntries) {
					Entry entry = matcher.process(entryData, transactionManager.getSession());
					ReconciliationEntryInfo.getUniqueIdAccessor().setValue(entry, entryData.uniqueId);
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
}