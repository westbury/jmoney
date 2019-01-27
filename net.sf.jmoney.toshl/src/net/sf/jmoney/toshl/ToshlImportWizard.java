/*
 *
 *  JMoney - A Personal Finance Manager
 *  Copyright (c) 2004,2009, 2016 Nigel Westbury <westbury@users.sourceforge.net>
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

package net.sf.jmoney.toshl;

import java.io.IOException;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWizard;

import net.sf.jmoney.fields.IAmountFormatter;
import net.sf.jmoney.importer.matcher.EntryData;
import net.sf.jmoney.importer.matcher.ImportEntryProperty;
import net.sf.jmoney.importer.matcher.ImportMatcher;
import net.sf.jmoney.importer.matcher.PatternMatchingDialog;
import net.sf.jmoney.importer.model.TransactionType;
import net.sf.jmoney.importer.model.TransactionTypeBasic;
import net.sf.jmoney.importer.wizards.CsvImportWizard;
import net.sf.jmoney.importer.wizards.CsvImportWizardPage;
import net.sf.jmoney.importer.wizards.CsvTransactionReader;
import net.sf.jmoney.importer.wizards.ImportException;
import net.sf.jmoney.model2.Account;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.IDataManagerForAccounts;
import net.sf.jmoney.model2.IDatastoreManager;
import net.sf.jmoney.model2.IncomeExpenseAccount;
import net.sf.jmoney.model2.Session;
import net.sf.jmoney.model2.Session.NoAccountFoundException;
import net.sf.jmoney.model2.Session.SeveralAccountsFoundException;
import net.sf.jmoney.model2.Transaction;
import net.sf.jmoney.model2.TransactionManagerForAccounts;
import net.sf.jmoney.reconciliation.ReconciliationEntryInfo;

/**
 * A wizard to import data from a comma-separated file that has been downloaded
 * from Toshl.
 */
public class ToshlImportWizard extends CsvImportWizard implements IWorkbenchWizard {

	private static DateFormat df = new SimpleDateFormat("ddMMyy");

	private ImportedDateColumn   column_date           = new ImportedDateColumn("Date", new SimpleDateFormat("dd-MMM-yyyy"));
	private ImportedTextColumn   column_account        = new ImportedTextColumn("Account");
	private ImportedTextColumn   column_category       = new ImportedTextColumn("Category");
	private ImportedTextColumn   column_tags           = new ImportedTextColumn("Tags");
	private ImportedAmountColumn column_expenseAmount  = new ImportedAmountColumn("Expense amount");
	private ImportedAmountColumn column_incomeAmount   = new ImportedAmountColumn("Income amount");
	private ImportedTextColumn   column_currency       = new ImportedTextColumn("Currency");
	private ImportedAmountColumn column_inMainCurrency = new ImportedAmountColumn("In main currency");
	private ImportedTextColumn   column_mainCurrency   = new ImportedTextColumn("Main currency");
	private ImportedTextColumn   column_description    = new ImportedTextColumn("Description");

	/**
	 * Currency of the Toshl account (do we need this?)
	 */
//	Currency currency;

	/**
	 * Session outside transaction
	 */
	private Session session;

	private Session sessionInsideTransaction;

	private Collection<EntryData> entryDataList;
	
	public ToshlImportWizard() {
		// TODO check these dialog settings are used by the base class
		// so the default filename location is separate for each import type.
		IDialogSettings workbenchSettings = Activator.getDefault().getDialogSettings();
		IDialogSettings section = workbenchSettings.getSection("ToshlImportWizard");//$NON-NLS-1$
		if (section == null) {
			section = workbenchSettings.addNewSection("ToshlImportWizard");//$NON-NLS-1$
		}
		setDialogSettings(section);
	}

	/**
	 * This form of this method is called when the wizard is initiated from the
	 * 'import' menu.  This is the only case supported by the Toshl import because
	 * the import does not act on a target account.  The target accounts are
	 * determined from the import file and the Toshl import options.
	 */
	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();

		IDatastoreManager sessionManager = (IDatastoreManager)window.getActivePage().getInput();

		session = sessionManager.getSession();

		this.window = window;

		mainPage = new CsvImportWizardPage(window, getDescription());
		addPage(mainPage);
	}

	@Override
	protected void startImport(TransactionManagerForAccounts transactionManager) throws ImportException {
		sessionInsideTransaction = transactionManager.getSession();
	}

	@Override
	protected boolean processRows(Session session) throws IOException, ImportException {
		entryDataList = new ArrayList<>();
		
		if (!super.processRows(session)) {
			return false;
		}
		
		/*
		 * Import the entries using the matcher dialog
		 */

		/*
		 * If any entries need match processing, do that now.
		 */
		if (!entryDataList.isEmpty()) {
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

			ToshlAccount toshlAccount = lookupToshlAccount("Cash");
			
			/*
			 * All changes within this dialog are made within a transaction, so canceling
			 * is trivial (the transaction is simply not committed).
			 */
			TransactionManagerForAccounts transactionManager = new TransactionManagerForAccounts(session.getDataManager());
			ToshlAccount accountInTransaction = transactionManager.getCopyInTransaction(toshlAccount);
			
			Dialog dialog = new PatternMatchingDialog(window.getShell(), accountInTransaction, entryDataList, Arrays.asList(getImportEntryProperties()), getApplicableTransactionTypes());
			int returnCode = dialog.open();
			
			if (returnCode == Dialog.OK || returnCode == PatternMatchingDialog.SAVE_PATTERNS_ONLY) {
				// All edits are transferred to the model as they are made,
				// so we just need to commit them.
				transactionManager.commit("Change Import Options");
			}
			
			if (returnCode == Dialog.OK) {
				ImportMatcher matcher = new ImportMatcher(accountInTransaction, getImportEntryProperties(), getApplicableTransactionTypes(), null);

				Set<Entry> ourEntries = new HashSet<Entry>();
				for (EntryData entryData: entryDataList) {
					Entry entry = matcher.process(entryData, sessionInsideTransaction, ourEntries);
					//				ReconciliationEntryInfo.getUniqueIdAccessor().setValue(entry, entryData.uniqueId);
					ourEntries.add(entry);
				}

				return true;
			} else {
				return false;
			}
		}
		
		return true;
	}

	@Override
	public void importLine(CsvTransactionReader reader) throws ImportException {
		Date date = column_date.getDate();

		long amount;
		Long expenseAmount = column_expenseAmount.getAmount();
		Long incomeAmount = column_incomeAmount.getAmount();
		if (expenseAmount != 0 && incomeAmount == 0) {
			amount = -expenseAmount;
		} else if (expenseAmount == 0 && incomeAmount != 0) {
			amount = incomeAmount;
		} else {
			throw new ImportException("One of income and expense amount must be specified.");
		}

		if (!column_account.getText().equals("Cash") && !column_account.getText().equals("sale")) {
			throw new ImportException("Don't know how to import Toshl account '" + column_account.getText() + "'.  Configured Toshl accounts are: 'Cash', 'sale'.");
		}
		if (!column_currency.getText().equals("GBP")) {
			throw new ImportException("Don't know how to import because 'Currency' not GBP");
		}
		if (!column_mainCurrency.getText().equals("GBP")) {
			throw new ImportException("Don't know how to import because 'Main currency' not GBP");
		}

		ToshlAccount toshlAccount = lookupToshlAccount(column_account.getText());

		String[] tags = column_tags.getText().split(",");

		EntryData entryData = new EntryData();


		/**
		 * Unique id, used to avoid duplicates if overlapping exports are imported.
		 */
		String uniqueId = df.format(date) + "~" + column_category.getText() + "~" + column_tags.getText() + "~" + Long.toString(amount);

		/*
		 * See if an entry already exists with this uniqueId.
		 */
		for (Entry entry : toshlAccount.getAccount().getEntries()) {
			if (uniqueId.equals(ReconciliationEntryInfo.getUniqueIdAccessor().getValue(entry))) {
				// This row has already been imported so ignore it.
				return;
			}
		}

		/*
		 * No existing entry matches, either on FITID or by matching dates and amounts,
		 * so we need to create a new transaction.
		 */
		entryData.valueDate = date;
		entryData.setMemo(column_description.getText());
		entryData.setAmount(amount);
		entryData.setUniqueId(uniqueId);
		entryDataList.add(entryData);
	}

	private ToshlAccount lookupToshlAccount(String toshlAccountName) throws ImportException {
		// Lookup the Toshl account
		ToshlAccount toshlAccount = null;
		ToshlSession toshlSession = sessionInsideTransaction.getExtension(ToshlSessionInfo.getPropertySet(), true);
		for (ToshlAccount eachToshlAccount : toshlSession.getToshlAccountCollection()) {
			if (eachToshlAccount.getToshlAccountName().equals(toshlAccountName)) {
					toshlAccount = eachToshlAccount;
					break;
			}
		}

		if (toshlAccount == null) {
			toshlAccount = toshlSession.getToshlAccountCollection().createNewElement(ToshlAccountInfo.getPropertySet());
			toshlAccount.setToshlAccountName(toshlAccountName);
			
			try {
				Account account = sessionInsideTransaction.getAccountByShortName("Misc UK");

				if (!(account instanceof IncomeExpenseAccount)) {
					throw new ImportException("The 'Misc UK' account must be an income/expense account.");
				}

				toshlAccount.setAccount((IncomeExpenseAccount)account);
				toshlAccount.setDefaultCategory((IncomeExpenseAccount)account);
			} catch (NoAccountFoundException e) {
				throw new ImportException("No account exists called 'Misc UK'");
			} catch (SeveralAccountsFoundException e) {
				throw new ImportException("Multiple accounts exists called 'Misc UK'");
			}
		}
		
		/**
		 * We can't import if there is no default category into which
		 * entries can be put.
		 */
		// Right place to put this???
		if (toshlAccount.getDefaultCategory() == null) {
			throw new ImportException(MessageFormat.format("No default category is set for {0}.", toshlAccount.getToshlAccountName()));
		}


		return toshlAccount;
	}

	private void assertValid(Transaction trans) {
		long total = 0;
		for (Entry entry : trans.getEntryCollection()) {
			total += entry.getAmount();
		}
		if (total != 0) {
			System.out.println("unbalanced");
		}
		assert(total == 0);
	}

	@Override
	protected ImportedColumn[] getExpectedColumns() {
		return new ImportedColumn [] {
				column_date,
				column_account,
				column_category,
				column_tags,
				column_expenseAmount,
				column_incomeAmount,
				column_currency,
				column_inMainCurrency,
				column_mainCurrency,
				column_description,
		};
	}


	public void checkNull(Object object, String message) throws ImportException {
		if (object != null) {
			throw new ImportException(message);
		}

	}

	@Override
	protected String getDescription() {
		return "The selected CSV file will be imported. " +
				"This file must have been downloaded from Toshl for this import to work.  To download from Toshl, go to 'Download ???' on the '???' menu, choose '???'." +
				"You should also check the box '???' at the bottom to get itemized entries. " +
				"If entries have already been imported then this import will not create duplicates.";
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
