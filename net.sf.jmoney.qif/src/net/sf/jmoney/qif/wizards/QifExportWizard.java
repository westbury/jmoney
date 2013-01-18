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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;

import net.sf.jmoney.model2.Account;
import net.sf.jmoney.model2.CapitalAccount;
import net.sf.jmoney.model2.CurrencyAccount;
import net.sf.jmoney.model2.DatastoreManager;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.Session;
import net.sf.jmoney.model2.TransactionInfo;
import net.sf.jmoney.qif.AccountChooser;
import net.sf.jmoney.qif.QIFEntry;
import net.sf.jmoney.qif.QIFEntryInfo;
import net.sf.jmoney.qif.QIFPlugin;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;

/**
 * A wizard to export data to a QIF file.
 * <P>
 * Currently this wizard is a single page wizard that asks only for the file.
 * This feature is implemented as a wizard because the Eclipse workbench import
 * action requires all export implementations to be wizards.
 */
public class QifExportWizard extends Wizard implements IExportWizard {
	private static SimpleDateFormat df = (SimpleDateFormat)DateFormat.getDateInstance();

	private Calendar calendar = Calendar.getInstance();

	private NumberFormat number = NumberFormat.getInstance(Locale.US);

	private IWorkbenchWindow window;

	private QifExportWizardPage mainPage;

	private Session session;

	public QifExportWizard() {
		IDialogSettings workbenchSettings = QIFPlugin.getDefault().getDialogSettings();
		IDialogSettings section = workbenchSettings.getSection("QifexportWizard");//$NON-NLS-1$
		if (section == null) {
			section = workbenchSettings.addNewSection("QifExportWizard");//$NON-NLS-1$
		}
		setDialogSettings(section);

		number.setMinimumFractionDigits(2);
		number.setMaximumFractionDigits(2);
	}

	/**
	 * We will cache window object in order to be able to provide parent shell
	 * for the message dialog.
	 */
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		this.window = workbench.getActiveWorkbenchWindow();
		
		// Original JMoney disabled the export menu items when no
		// session was open.  I don't know how to do that in Eclipse,
		// so we display a message instead.
		DatastoreManager sessionManager = (DatastoreManager)window.getActivePage().getInput();
		if (sessionManager == null) {
	        MessageDialog waitDialog =
				new MessageDialog(
						window.getShell(), 
						"Disabled Action Selected", 
						null, // accept the default window icon
						"You cannot export data unless you have a session open.  You must first open a session.", 
						MessageDialog.ERROR, 
						new String[] { IDialogConstants.OK_LABEL }, 0);
	        waitDialog.open();
			return;
		}

		mainPage = new QifExportWizardPage(window);
		addPage(mainPage);
	}

	@Override
	public boolean performFinish() {
		String fileName = mainPage.getFileName();
		if (fileName != null) {
			File qifFile = new File(fileName);
			exportFile(qifFile);
		}

		return true;
	}


	private void exportFile(File qifFile) {
			if (dontOverwrite(qifFile))
				return;
			
		    AccountChooser accountChooser = new AccountChooser(window.getShell(), QIFPlugin.getResourceString("MainFrame.chooseAccountToExport"));
			accountChooser.open();
/* TODO: get this working
 * need to decide if we should get the account from the selection in the navigation view.
 * Should we allow multiple selection in the nav. view?			
			int result =
				accountChooser.showDialog(
						session.getAccountCollection().iterator(),
						"hello",
						false);
			if (result == Dialog.OK) {
		        exportAccount(
						accountChooser.getSelectedAccount(),
						qifFile);
			}
*/			
	}

	private boolean dontOverwrite(File file) {
	    if (file.exists()) {
	    	String question = QIFPlugin.getResourceString("MainFrame.OverwriteExistingFile")
	        	+ " "
				+ file.getPath()
				+ "?";
	    	String title = QIFPlugin.getResourceString("MainFrame.FileExists");
	    	
	    	boolean answer = MessageDialog.openQuestion(
					window.getShell(),
					title,
					question);
	        return !answer;
	    } else {
	        return false;
	    }
	}

	@SuppressWarnings("unused")
	private void exportAccount(CapitalAccount capitalAccount, File file) {
		if (!(capitalAccount instanceof CurrencyAccount)) {
			// TODO: process other account types
			return;
		}
		CurrencyAccount account = (CurrencyAccount) capitalAccount;

		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(file));

			// Get the entries in date order.
			// The entries must be in date order because the date of the
			// first entry is used as the date of the opening balance record.
			Collection entries = account.getSortedEntries(TransactionInfo
					.getDateAccessor(), false);

			// write header
			writeln(writer, "!Type:Bank");

			// write first entry (containing the start balance)
			if (!entries.isEmpty()) {
				Entry entry = (Entry) entries.iterator().next();
				String dateString = formatDate(entry.getTransaction().getDate());
				if (dateString != null)
					writeln(writer, dateString);
			}
			writeln(writer, "T"
					+ formatAmount(account.getStartBalance(), account));
			writeln(writer, "CX");
			writeln(writer, "POpening Balance");
			writeln(writer, "L[" + account.getName() + "]");
			writeln(writer, "^");

			// write entries
			for (Iterator entryIter = entries.iterator(); entryIter.hasNext();) {
				Entry entry = (Entry) entryIter.next();
				// date
				String dateString = formatDate(entry.getTransaction().getDate());
				if (dateString != null)
					writeln(writer, dateString);
				// memo
				if (entry.getMemo() != null)
					writeln(writer, "M" + entry.getMemo());

				// status
				QIFEntry ourEntry = entry.getExtension(QIFEntryInfo.getPropertySet(), false);
				if (ourEntry == null) {
					writeln(writer, "C ");
				} else if (ourEntry.getReconcilingState() == ' ') {
					writeln(writer, "C ");
				} else if (ourEntry.getReconcilingState() == '*') {
					writeln(writer, "C*");
				} else if (ourEntry.getReconcilingState() == 'X') {
					writeln(writer, "CX");
				}

				// amount
				writeln(writer, "T" + formatAmount(entry.getAmount(), account));
				// check
				if (entry.getCheck() != null)
					writeln(writer, "N" + entry.getCheck());
				// description
				if (entry.getMemo() != null)
					writeln(writer, "P" + entry.getMemo());
				// category
				Account category = entry.getAccount();
				if (category != null) {
					if (category instanceof CapitalAccount)
						writeln(writer, "L[" + category.getName() + "]");
					else {
						writeln(writer, "L" + category.getFullAccountName());
					}
					// TODO: Split Entries
				}
				// end of entry
				writeln(writer, "^");
			}
			writer.close();
		} catch (IOException e) {
		}
	}

	private String formatDate(Date date) {
		if (date == null)
			return null;
		calendar.setTime(date);
		df.applyPattern("dd/mm/yyyy");
		return df.format(date);
	}

	private String formatAmount(long amount, CurrencyAccount account) {
		return number.format(((double) amount)
				/ account.getCurrency().getScaleFactor());
	}

	/**
	 * Writes a line and jumps to a new one.
	 */
	private void writeln(BufferedWriter writer, String line) throws IOException {
		writer.write(line);
		writer.newLine();
	}
}