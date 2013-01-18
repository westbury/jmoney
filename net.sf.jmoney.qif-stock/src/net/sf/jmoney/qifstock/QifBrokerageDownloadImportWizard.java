/*
 * 
 *  JMoney - A Personal Finance Manager
 *  Copyright (c) 2004 Nigel Westbury <westbury@users.sourceforge.net>
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

package net.sf.jmoney.qifstock;

import java.io.File;
import java.io.IOException;

import net.sf.jmoney.isolation.TransactionManager;
import net.sf.jmoney.model2.DatastoreManager;
import net.sf.jmoney.model2.Session;
import net.sf.jmoney.qif.QIFPlugin;
import net.sf.jmoney.qif.parser.AmbiguousDateException;
import net.sf.jmoney.qif.parser.InvalidQifFileException;
import net.sf.jmoney.qif.parser.QifDateFormat;
import net.sf.jmoney.qif.parser.QifFile;
import net.sf.jmoney.qif.parser.QifImportException;
import net.sf.jmoney.stocks.model.StockAccount;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;

/**
 * A wizard to import data from a QIF file that has been down-loaded from a stock
 * brokerage web site.  This class is not intended to import QIF files that have
 * been exported from another accounting program.
 * <P>
 * Currently this wizard is a single page wizard that asks only for the file.
 * This feature is implemented as a wizard because the Eclipse workbench import
 * action requires all import implementations to be wizards.
 */
public class QifBrokerageDownloadImportWizard extends Wizard implements IImportWizard {
	private IWorkbenchWindow window;

	private QifImportWizardPage mainPage;

	private Session session;

	/**
	 * The account selected when the import wizard was started, or null if either no
	 * account was selected or multiple objects were selected
	 */
	private StockAccount account;

	public QifBrokerageDownloadImportWizard() {
		IDialogSettings workbenchSettings = QIFPlugin.getDefault().getDialogSettings();
		IDialogSettings section = workbenchSettings.getSection("QifImportWizard");//$NON-NLS-1$
		if (section == null) {
			section = workbenchSettings.addNewSection("QifImportWizard");//$NON-NLS-1$
		}
		setDialogSettings(section);
	}

	/**
	 * We will cache window object in order to be able to provide parent shell
	 * for the message dialog.
	 */
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		this.window = workbench.getActiveWorkbenchWindow();

		/*
		 * If the selection is a single account, save the account.  This may be needed
		 * later because if the QIF file has no account information then the data are
		 * imported into the selected account.
		 */
		account = null;
		if (selection.size() == 1) {
			Object selectedElement = selection.getFirstElement();
			if (selectedElement instanceof StockAccount) {
				account = (StockAccount)selectedElement;
			}
		}
		if (account == null) {
			MessageDialog.openError(getShell(), "Destiation Account not Selected", "You must first select the destination account.  "
					+ "The destination account must be a stock account.");
			return;
		}
		
		// Original JMoney disabled the import menu items when no
		// session was open. I don't know how to do that in Eclipse,
		// so we display a message instead.
		DatastoreManager sessionManager = (DatastoreManager)window.getActivePage().getInput();
		if (sessionManager == null) {
			MessageDialog.openError(
					window.getShell(),
					"Disabled Action Selected",
					"You cannot import data into an accounting session unless you have a session open.  You must first open a session or create a new session.");
			return;
		}

		session = sessionManager.getSession();
		
		mainPage = new QifImportWizardPage(window, account);
		addPage(mainPage);
	}

	@Override
	public boolean performFinish() {
		String fileName = mainPage.getFileName();
		if (fileName != null) {
			File qifFile = new File(fileName);
			importFile(qifFile);
		}

		return true;
	}


	public void importFile(File file) {
		try {
			QifFile qifFile = new QifFile(file, QifDateFormat.DetermineFromFileAndSystem);

			/*
			 * Create a transaction to be used to import the entries.  This allows the entries to
			 * be more efficiently written to the back-end datastore and it also groups
			 * the entire import as a single change for undo/redo purposes.
			 */
			TransactionManager transactionManager = new TransactionManager(session.getDataManager());
			Session sessionInTransaction = transactionManager.getSession();
			StockAccount accountInTransaction = transactionManager.getCopyInTransaction(account);
			
			/*
			 * Check that this QIF file is of a form that has entries only, no account information.  This is the
			 * form that is down-loaded from a banking site.
			 */
			if (!qifFile.accountList.isEmpty()) {
				throw new QifImportException("QIF file contains information not typical of a bank download.  It looks like the file was exported from an accounting program.");
			}

			if (!qifFile.transactions.isEmpty()) {
				throw new QifImportException("This QIF file has transactions that are not investement transactions.  This is not supported for brokerage imports though there is no reason why they could not be.");
			}

			/*
			 * Import transactions that have no account information.
			 */
			if (!qifFile.invstTransactions.isEmpty()) {
				// TODO: This does not work well.  The importData method is designed to import QIF files
				// that are exported from another accounting program.  There may need to be a different form,
				// though most of the code will be the same, to import down-loads from brokerages.
				InvestmentImporter importer = new InvestmentImporter();
				int transactionCount = 0;
				importer.importData(qifFile, sessionInTransaction, accountInTransaction);

				/*
				 * All entries have been imported and all the properties
				 * have been set and should be in a valid state, so we
				 * can now commit the imported entries to the datastore.
				 */
				String transactionDescription = String.format("Import {0}", file.getName());
				transactionManager.commit(transactionDescription);									

				if (transactionCount != 0) {
					StringBuffer combined = new StringBuffer();
					combined.append(file.getName());
					combined.append(" was successfully imported. ");
//					combined.append(transactionCount).append(" transactions were imported.");
					MessageDialog.openInformation(window.getShell(), "QIF file imported", combined.toString());
				} else {
					throw new QifImportException("No transactions were found within the given date range.");
				}
			}
		} catch (IOException e) {
			MessageDialog.openError(window.getShell(), "Unable to read QIF file", e.getLocalizedMessage());
		} catch (InvalidQifFileException e) {
			MessageDialog.openError(window.getShell(), "Unable to import QIF file", e.getLocalizedMessage());
		} catch (QifImportException e) {
			MessageDialog.openError(window.getShell(), "Unable to import QIF file", e.getLocalizedMessage());
		} catch (AmbiguousDateException e) {
			MessageDialog.openError(window.getShell(), "QIF file has an ambiguous date format that cannot be guessed from your locale.", e.getLocalizedMessage());
		}
	}
}