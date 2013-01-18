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

package net.sf.jmoney.qif.wizards;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import net.sf.jmoney.isolation.TransactionManager;
import net.sf.jmoney.model2.DatastoreManager;
import net.sf.jmoney.model2.MalformedPluginException;
import net.sf.jmoney.model2.Session;
import net.sf.jmoney.qif.IQifImporter;
import net.sf.jmoney.qif.QIFPlugin;
import net.sf.jmoney.qif.parser.AmbiguousDateException;
import net.sf.jmoney.qif.parser.InvalidQifFileException;
import net.sf.jmoney.qif.parser.QifDateFormat;
import net.sf.jmoney.qif.parser.QifFile;
import net.sf.jmoney.qif.parser.QifImportException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;

/**
 * A wizard to import data from a QIF file.
 * <P>
 * Currently this wizard is a single page wizard that asks only for the file.
 * This feature is implemented as a wizard because the Eclipse workbench import
 * action requires all import implementations to be wizards.
 */
public class QifFullImportWizard extends Wizard implements IImportWizard {
	private IWorkbenchWindow window;

	private QifImportWizardPage mainPage;

	private Session session;

	public QifFullImportWizard() {
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
		
		mainPage = new QifImportWizardPage(window, null);
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

			List<String> results = new ArrayList<String>();

			/*
			 * Create a transaction to be used to import the entries.  This allows the entries to
			 * be more efficiently written to the back-end datastore and it also groups
			 * the entire import as a single change for undo/redo purposes.
			 */
			TransactionManager transactionManager = new TransactionManager(session.getDataManager());
			Session sessionInTransaction = transactionManager.getSession();
			
			IExtensionRegistry registry = Platform.getExtensionRegistry();
			for (IConfigurationElement element: registry.getConfigurationElementsFor("net.sf.jmoney.qif.importers")) {
				if (element.getName().equals("importer")) {
					try {
						Object listener = element.createExecutableExtension("class");
						if (!(listener instanceof IQifImporter)) {
							throw new MalformedPluginException(
									"Plug-in " + element.getContributor().getName()
									+ " extends the net.sf.jmoney.qif.importers extension point. "
									+ "However, the class specified by the class attribute "
									+ "(" + listener.getClass().getName() + ") "
									+ "does not implement the IQifImporter interface. "
									+ "This interface must be implemented by all classes referenced "
									+ "by the class attribute.");
						}

						IQifImporter importer = (IQifImporter)listener;

						String result = importer.importData(qifFile, sessionInTransaction, null);
						if (result != null) {
							results.add(result);
						}
					} catch (CoreException e) {
						if (e.getStatus().getException() instanceof ClassNotFoundException) {
							ClassNotFoundException e2 = (ClassNotFoundException)e.getStatus().getException();
							throw new MalformedPluginException(
									"Plug-in " + element.getContributor().getName()
									+ " extends the net.sf.jmoney.qif.importers extension point. "
									+ "However, the class specified by the class attribute "
									+ "(" + e2.getMessage() + ") "
									+ "could not be found. "
									+ "The class attribute must specify a class that implements the "
									+ "IQifImporter interface.");
						}
					}

				}
			}

			/*
			 * All entries have been imported and all the properties
			 * have been set and should be in a valid state, so we
			 * can now commit the imported entries to the datastore.
			 */
			String transactionDescription = MessageFormat.format("Import {0}", file.getName());
			transactionManager.commit(transactionDescription);									

			if (!results.isEmpty()) {
				StringBuffer combined = new StringBuffer();
				combined.append(file.getName());
				combined.append(" was successfully imported. ");
				combined.append("The following were imported: \n\n");
				
				String separator = "";
				for (String result : results) {
					combined.append(separator);
					combined.append(result);
					separator = ", ";
				}
				MessageDialog.openInformation(window.getShell(), "QIF file imported", combined.toString());
			} else {
				MessageDialog.openError(window.getShell(), "Unable to import QIF file", "No data was found that could be imported by any of the installed plug-ins.");
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