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
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

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
public abstract class TxrImportWizard extends Wizard {

	protected IWorkbenchWindow window;

	protected CsvImportWizardPage mainPage;

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

	public void init(IWorkbench workbench, IStructuredSelection selection) {
		this.window = workbench.getActiveWorkbenchWindow();

		mainPage = new CsvImportWizardPage(window, getDescription());

		addPage(mainPage);
	}

	@Override
	public boolean performFinish() {
		String text = getTextFromClipboard();
		
		if (text != null) {
			boolean allImported = importFile(text);
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


	public boolean importFile(String text) {

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

        	if (processRows(session, text)) {
        		/*
        		 * All entries have been imported so we
        		 * can now commit the imported entries to the datastore.
        		 */
        		String transactionDescription = MessageFormat.format("Import {0}", "text from clipboard");
        		transactionManager.commit(transactionDescription);									
        		return true;
        	} else {
        		return false;
        	}
		} catch (IOException e) {
			// This is probably not likely to happen so the default error handling is adequate.
			throw new RuntimeException(e);
//		} catch (ImportException e) {
//			// There are data in the import file that we are unable to process
//			e.printStackTrace();
//			MessageDialog.openError(window.getShell(), "Error in row " + e.getRowNumber(), e.getMessage());
//			return false;
		} catch (Exception e) {
			// There are data in the import file that we are unable to process
			e.printStackTrace();
			MessageDialog.openError(window.getShell(), "Errors in the downloaded file", e.getMessage());
			return false;
		}
	}

	/**
	 * 
	 * @param session
	 * @return true if processing completed, false if processing did not complete
	 * 			because the user cancelled or some other reason
	 * @throws IOException
	 * @throws ImportException
	 */
	protected abstract boolean processRows(Session session, String text)
			throws IOException, ImportException;

	protected abstract String getDescription();

	protected abstract void startImport(TransactionManagerForAccounts transactionManager) throws ImportException;
}