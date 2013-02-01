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
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;

import net.sf.jmoney.importer.MatchingEntryFinder;
import net.sf.jmoney.importer.matcher.EntryData;
import net.sf.jmoney.importer.matcher.ImportMatcher;
import net.sf.jmoney.importer.matcher.PatternMatchingDialog;
import net.sf.jmoney.importer.model.PatternMatcherAccount;
import net.sf.jmoney.importer.model.PatternMatcherAccountInfo;
import net.sf.jmoney.importer.model.ReconciliationEntryInfo;
import net.sf.jmoney.model2.CapitalAccount;
import net.sf.jmoney.model2.Currency;
import net.sf.jmoney.model2.CurrencyAccount;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.IDataManagerForAccounts;
import net.sf.jmoney.model2.Session;
import net.sf.jmoney.model2.TransactionManagerForAccounts;
import net.sf.jmoney.qif.QIFPlugin;
import net.sf.jmoney.qif.parser.AmbiguousDateException;
import net.sf.jmoney.qif.parser.InvalidQifFileException;
import net.sf.jmoney.qif.parser.QifDate;
import net.sf.jmoney.qif.parser.QifDateFormat;
import net.sf.jmoney.qif.parser.QifFile;
import net.sf.jmoney.qif.parser.QifImportException;
import net.sf.jmoney.qif.parser.QifTransaction;

import org.eclipse.jface.dialogs.Dialog;
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
public class QifBankDownloadImportWizard extends Wizard implements IImportWizard {
	private IWorkbenchWindow window;

	private QifImportWizardPage mainPage;

	private Session session;

	/**
	 * The account selected when the import wizard was started, or null if either no
	 * account was selected or multiple objects were selected
	 */
	private CapitalAccount account;

	public QifBankDownloadImportWizard() {
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
			if (selectedElement instanceof CapitalAccount) {
				account = (CapitalAccount)selectedElement;
			}
		}
		if (account == null) {
			MessageDialog.openError(getShell(), "Destiation Account not Selected", "You must first select the destination account.  "
					+ "The destination account must not be an income or expense account.");
			return;
		}
		
		// Original JMoney disabled the import menu items when no
		// session was open. I don't know how to do that in Eclipse,
		// so we display a message instead.
		IDataManagerForAccounts sessionManager = (IDataManagerForAccounts)window.getActivePage().getInput();
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

			if (!(account instanceof CurrencyAccount)) {
				// TODO: process error properly
				if (QIFPlugin.DEBUG) System.out.println("account is not a currency account");
				throw new QifImportException("selected account is not a currency account");
			}

			CurrencyAccount currencyAccount = (CurrencyAccount)account;


			/*
			 * Create a transaction to be used to import the entries.  This allows the entries to
			 * be more efficiently written to the back-end datastore and it also groups
			 * the entire import as a single change for undo/redo purposes.
			 */
			TransactionManagerForAccounts transactionManager = new TransactionManagerForAccounts(session.getDataManager());
			Session sessionInTransaction = transactionManager.getSession();
			CurrencyAccount accountInTransaction = transactionManager.getCopyInTransaction(currencyAccount);
			
			/*
			 * Check that this QIF file is of a form that has entries only, no account information.  This is the
			 * form that is down-loaded from a banking site.
			 */
			if (!qifFile.accountList.isEmpty()) {
				throw new QifImportException("QIF file contains information not typical of a bank download.  It looks like the file was exported from an accounting program.");
			}

			if (!qifFile.invstTransactions.isEmpty()) {
				throw new QifImportException("This QIF file has investement transactions.  This is not supported for bank imports.");
			}

			/*
			 * Import transactions that have no account information.
			 */
			if (!qifFile.transactions.isEmpty()) {
				// TODO: This should come from the account????
						Currency currency = sessionInTransaction.getDefaultCurrency();
				
						int transactionCount = 0;
						
						MatchingEntryFinder matchFinder = new MatchingEntryFinder() {
							@Override
							protected boolean alreadyMatched(Entry entry) {
								return ReconciliationEntryInfo.getUniqueIdAccessor().getValue(entry) != null;
							}
						};
						
//						ImportMatcher matcher = new ImportMatcher(accountInTransaction.getExtension(PatternMatcherAccountInfo.getPropertySet(), true));

						Collection<EntryData> importedEntries = new ArrayList<EntryData>();

						for (QifTransaction qifTransaction : qifFile.transactions) {
							
							if (!qifTransaction.getSplits().isEmpty()) {
								throw new RuntimeException("QIF file contains information not typical of a bank download.  It looks like the file was exported from an accounting program.");
							}
				
							if (qifTransaction.getCategory() != null) {
								throw new RuntimeException("When transactions are listed in the QIF file with no account information (downloaded from bank), there must not be any category information.");
							}
				
							Date date = convertDate(qifTransaction.getDate());
							long amount = adjustAmount(qifTransaction.getAmount(), currency);

							String uniqueId = Long.toString(date.getTime())
									+ '~' + amount;
							
							Entry match = matchFinder.findMatch(currencyAccount, amount, date, qifTransaction.getCheckNumber());
							if (match != null) {
								Entry entryInTrans = transactionManager.getCopyInTransaction(match);
								entryInTrans.setValuta(date);
								entryInTrans.setCheck(qifTransaction.getCheckNumber());
								ReconciliationEntryInfo.getUniqueIdAccessor().setValue(entryInTrans, uniqueId);
							} else {
								EntryData entryData = new EntryData();
								entryData.amount = amount;
								entryData.check = qifTransaction.getCheckNumber();
								//							entryData.valueDate = date;
								entryData.clearedDate = date;
								entryData.setMemo(qifTransaction.getMemo());
								entryData.setPayee(qifTransaction.getPayee());
								
//								Entry entry = matcher.process(entryData, sessionInTransaction);
//								entry.setPropertyValue(ReconciliationEntryInfo.getUniqueIdAccessor(), uniqueId);
								
								importedEntries.add(entryData);
							}							

							
							
							
							/*
do just the above.  The following is obsolete.
		Then remove the duplicate autoMatch method.					
							
							
							// Create a new transaction
							Transaction transaction = sessionInTransaction.createTransaction();
				
							// Add the first entry for this transaction and set the account
							QIFEntry firstEntry = transaction.createEntry().getExtension(QIFEntryInfo.getPropertySet(), true);
							firstEntry.setAccount(accountInTransaction);
				
							transaction.setDate(date);
							firstEntry.setAmount(amount);
							firstEntry.setReconcilingState(qifTransaction.getStatus());
							firstEntry.setCheck(qifTransaction.getCheckNumber());
							firstEntry.setMemo(qifTransaction.getPayee());
				
							// Add the second entry for this transaction
							Entry secondEntry = transaction.createEntry();
				
							secondEntry.setAmount(-amount);
				
							firstEntry.setMemo(qifTransaction.getMemo());
				
							String address = null;
							for (String line : qifTransaction.getAddressLines()) {
								if (address == null) {
									address = line;
								} else {
									address = address + '\n' + line; 
								}
							}
							firstEntry.setAddress(address);
				
							if (secondEntry.getAccount() instanceof IncomeExpenseAccount) {
								// If this entry is for a multi-currency account,
								// set the currency to be the same as the currency for this
								// bank account.
								if (((IncomeExpenseAccount)secondEntry.getAccount()).isMultiCurrency()) {
									secondEntry.setIncomeExpenseCurrency(currency);
								}
							}
							*/
							
							transactionCount++;
						}

						
						
						PatternMatcherAccount matcherAccount = account.getExtension(PatternMatcherAccountInfo.getPropertySet(), true);
						
						Dialog dialog = new PatternMatchingDialog(window.getShell(), matcherAccount, importedEntries);
						if (dialog.open() == Dialog.OK) {
							ImportMatcher matcher = new ImportMatcher(accountInTransaction.getExtension(PatternMatcherAccountInfo.getPropertySet(), true));

							for (net.sf.jmoney.importer.matcher.EntryData entryData: importedEntries) {
								Entry entry = matcher.process(entryData, sessionInTransaction);
//								entry.setPropertyValue(ReconciliationEntryInfo.getUniqueIdAccessor(), uniqueId);
							}
						} else {
							return;
						}

						
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
					combined.append(transactionCount).append(" transactions were imported.");
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
	
	private Date convertDate(QifDate date) {
		Calendar calendar = Calendar.getInstance();
		calendar.clear();
		calendar.set(Calendar.YEAR, date.getYear());
		calendar.set(Calendar.MONTH, date.getMonth()-1);
		calendar.set(Calendar.DAY_OF_MONTH, date.getDay());
		return calendar.getTime();
	}

	private long adjustAmount(BigDecimal amount, Currency currency) {
		// TODO: revisit this method.
		return amount.movePointRight(currency.getDecimals()).longValue();
	}

}