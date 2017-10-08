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

import java.io.IOException;
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;

import net.sf.jmoney.importer.Activator;
import net.sf.jmoney.importer.matcher.BaseEntryData;
import net.sf.jmoney.importer.matcher.ImportEntryProperty;
import net.sf.jmoney.importer.matcher.ImportMatcher;
import net.sf.jmoney.importer.matcher.PatternMatchingDialog;
import net.sf.jmoney.importer.model.PatternMatcherAccount;
import net.sf.jmoney.importer.model.PatternMatcherAccountInfo;
import net.sf.jmoney.importer.model.TransactionType;
import net.sf.jmoney.model2.BankAccount;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.IDataManagerForAccounts;
import net.sf.jmoney.model2.Session;
import net.sf.jmoney.model2.TransactionManagerForAccounts;

/**
 * A wizard to import data from text that has been copied to the clipboard.
 * Such text is typically copied from a page in a browser, so just the visible
 * text is available, no HTML markup.
 * <P>
 * This wizard is a single page wizard.
 */
public abstract class TxrImportWizard<T extends BaseEntryData> extends Wizard {

	protected IWorkbenchWindow window;

	protected String importDescription;
	
	protected List<T> entryDataList;

	protected BankAccount cashAccount;

	protected Session sessionOutsideTransaction;

	/**
	 * This form of the constructor is used when being called from
	 * the Eclipse 'import' menu.
	 */
	public TxrImportWizard() {
		IDialogSettings workbenchSettings = Activator.getDefault().getDialogSettings();
		IDialogSettings section = workbenchSettings.getSection("TxrImportWizard");//$NON-NLS-1$
		if (section == null) {
			section = workbenchSettings.addNewSection("TxrImportWizard");//$NON-NLS-1$
		}
		setDialogSettings(section);
	}

	/**
	 * This form of this method is called when the wizard is initiated from the
	 * 'import' menu.
	 */
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		this.window = workbench.getActiveWorkbenchWindow();

		performFinish();
	}

	@Override
	public boolean performFinish() {
		String text = getTextFromClipboard();
		
		if (text != null) {
			IDataManagerForAccounts datastoreManager = (IDataManagerForAccounts)window.getActivePage().getInput();
			if (datastoreManager == null) {
				MessageDialog.openError(window.getShell(), "Unavailable", "You cannot import data into an accounting session unless you have a session open.  You must first open a session or create a new session.");
				return false;
			}

			sessionOutsideTransaction = datastoreManager.getSession();
			
			try {
				/*
				 * Create a transaction to be used to import the entries.  This allows the entries to
				 * be more efficiently written to the back-end datastore and it also groups
				 * the entire import as a single change for undo/redo purposes.
				 */
				TransactionManagerForAccounts transactionManager = new TransactionManagerForAccounts(datastoreManager);
				Session session = transactionManager.getSession();

	        	if (processRows(session, text)) {
	        		/*
	        		 * All entries have been imported so we
	        		 * can now commit the imported entries to the datastore.
	        		 */
	    			if (transactionManager.hasChanges()) {
	    				String transactionDescription = MessageFormat.format("Import {0}", getDescription());
	    				transactionManager.commit(transactionDescription);

	    				StringBuffer combined = new StringBuffer()
	    						.append(getDescription())
	    						.append(" was successfully imported.");
	    				MessageDialog.openInformation(window.getShell(), "Data imported", combined.toString());
	    			} else {
	    				MessageDialog.openWarning(window.getShell(), "Data not imported",
	    						MessageFormat.format(
	    								"{0} was not imported because all the data in it had already been imported.",
	    								getDescription()));
	    			}
	        		
	        		return true;
	        	} else {
	        		return false;
	        	}
			} catch (IOException e) {
				// This is probably not likely to happen so the default error handling is adequate.
				throw new RuntimeException(e);
//			} catch (ImportException e) {
//				// There are data in the import file that we are unable to process
//				e.printStackTrace();
//				MessageDialog.openError(window.getShell(), "Error in row " + e.getRowNumber(), e.getMessage());
//				return false;
			} catch (Exception e) {
				// There are data in the import file that we are unable to process
				e.printStackTrace();
				MessageDialog.openError(window.getShell(), "Errors in the downloaded file", e.getMessage());
				return false;
			}
		}

		return true;
	}

	private String getTextFromClipboard() {
		Display display = Display.getCurrent();
		Clipboard clipboard = new Clipboard(display);
		String plainText = (String)clipboard.getContents(TextTransfer.getInstance());
		clipboard.dispose();        

		return plainText;
	}

	/**
	 * 
	 * @param session
	 * @return true if processing completed, false if processing did not complete
	 * 			because the user cancelled or some other reason
	 * @throws IOException
	 * @throws ImportException
	 */
	protected boolean processRows(Session session, String inputText) throws IOException, ImportException {
		
		// Set from abstract method, but should be set in import because that
		// allows the description to depend on the data.  importDescription may be
		// overwritten in buildEntryDataList.
		importDescription = this.getDescription();
		
		buildEntryDataList(session, inputText);

		if (!entryDataList.isEmpty()) {
			/*
			 * All changes within this dialog are made within a transaction, so canceling
			 * is trivial (the transaction is simply not committed).
			 */

			/*
			 * Import the entries using the matcher dialog
			 */

			PatternMatcherAccount matcherAccount = cashAccount.getExtension(PatternMatcherAccountInfo.getPropertySet(), true);

			Dialog dialog = new PatternMatchingDialog<T>(window.getShell(), matcherAccount, entryDataList, getImportEntryProperties(), getApplicableTransactionTypes());
			int returnCode = dialog.open();

			if (returnCode == PatternMatchingDialog.SAVE_PATTERNS_ONLY) {
				// All edits are transferred to the model as they are made,
				// so we just need to commit them.
				return true;
//				session.getDataManager().commitTransaction("Change Import Options");
			}

			if (returnCode != Dialog.OK) {
				return false;
			}

			ImportMatcher<T> matcher = new ImportMatcher<>(matcherAccount, getImportEntryProperties(), getApplicableTransactionTypes());

			Set<Entry> ourEntries = new HashSet<Entry>();
			for (T entryData: entryDataList) {
				Entry entry = matcher.process(entryData, session, ourEntries);

				ourEntries.add(entry);
			}
		} else {
			// There is nothing new to import that has not already been imported.
			MessageDialog.openInformation(getShell(), "Nothing Imported", MessageFormat.format("Nothing was imported from {0} because there was nothing to import or everything in the import has already been imported", importDescription));
		}

		/*
		 * All entries have been imported and all the properties
		 * have been set and should be in a valid state, so we
		 * can now commit the imported entries to the datastore.
		 */
		
		return true;
	}

	protected abstract List<ImportEntryProperty<T>> getImportEntryProperties();

	protected abstract List<TransactionType<T>> getApplicableTransactionTypes();

	/**
	 * The implementations of this abstract method must do the following:
	 * <UL>
	 * <LI>set importDescription
	 * <LI>set entryDataList
	 * <LI>set cashAccount to the account that contains the entries,
	 * 			being the entries that are searched for matches.
	 * </UL>
	 * 
	 * @param session
	 * @param inputText
	 * @throws ImportException
	 */
	protected abstract void buildEntryDataList(Session session, String inputText) throws ImportException;

	protected abstract String getDescription();

}