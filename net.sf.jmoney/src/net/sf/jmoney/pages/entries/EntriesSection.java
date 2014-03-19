/*
 *  JMoney - A Personal Finance Manager
 *  Copyright (c) 2004 Johann Gyger <jgyger@users.sf.net>
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
 */
package net.sf.jmoney.pages.entries;

import java.util.Collection;

import net.sf.jmoney.entrytable.BalanceColumn;
import net.sf.jmoney.entrytable.Block;
import net.sf.jmoney.entrytable.DebitAndCreditColumns;
import net.sf.jmoney.entrytable.DelegateBlock;
import net.sf.jmoney.entrytable.DeleteTransactionHandler;
import net.sf.jmoney.entrytable.DuplicateTransactionHandler;
import net.sf.jmoney.entrytable.EntriesTable;
import net.sf.jmoney.entrytable.EntryData;
import net.sf.jmoney.entrytable.EntryFacade;
import net.sf.jmoney.entrytable.EntryRowControl;
import net.sf.jmoney.entrytable.HorizontalBlock;
import net.sf.jmoney.entrytable.IEntriesContent;
import net.sf.jmoney.entrytable.IRowProvider;
import net.sf.jmoney.entrytable.IndividualBlock;
import net.sf.jmoney.entrytable.NewTransactionHandler;
import net.sf.jmoney.entrytable.OpenTransactionDialogHandler;
import net.sf.jmoney.entrytable.OtherEntriesBlock;
import net.sf.jmoney.entrytable.PropertyBlock;
import net.sf.jmoney.entrytable.ReusableRowProvider;
import net.sf.jmoney.entrytable.RowSelectionTracker;
import net.sf.jmoney.entrytable.SingleOtherEntryDetailPropertyBlock;
import net.sf.jmoney.entrytable.VerticalBlock;
import net.sf.jmoney.isolation.TransactionManager;
import net.sf.jmoney.model2.Account;
import net.sf.jmoney.model2.Currency;
import net.sf.jmoney.model2.CurrencyAccount;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.EntryInfo;
import net.sf.jmoney.model2.IncomeExpenseAccount;
import net.sf.jmoney.model2.Transaction;
import net.sf.jmoney.model2.TransactionInfo;
import net.sf.jmoney.resources.Messages;

import org.eclipse.core.commands.IHandler;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.runtime.Assert;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.SectionPart;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.handlers.IHandlerService;

/**
 * TODO
 * 
 * @author Johann Gyger
 */
public class EntriesSection extends SectionPart implements IEntriesContent {

	private Account account;
	private EntriesFilter filter;
	private IHandlerService handlerService;

    private EntriesTable fEntriesControl;
    
    private EntryRowSelectionListener tableSelectionListener = null;
    
    private Block<EntryRowControl> rootBlock;
    
    public EntriesSection(Composite parent, Account account, EntriesFilter filter, FormToolkit toolkit, IHandlerService handlerService) {
        super(parent, toolkit, Section.TITLE_BAR);
        getSection().setText(Messages.EntriesSection_Text);
        this.account = account;
        this.filter = filter;
        this.handlerService = handlerService;
        createClient(toolkit);
    }

    public void refreshEntryList() {
    	fEntriesControl.refreshEntryList();
    }

    protected void createClient(FormToolkit toolkit) {
    	
    	tableSelectionListener = new EntryRowSelectionAdapter() {
			@Override
    		public void widgetSelected(EntryData selectedObject) {
    			Assert.isNotNull(selectedObject);
    			
    			// Do we want to keep the entry section at the bottom?
    			// Do we need this listener at all?
			}
        };

        

		// By default, do not include the column for the currency
		// of the entry in the category (which applies only when
		// the category is a multi-currency income/expense category)
		// and the column for the amount (which applies only when
		// the currency is different from the entry in the capital 
		// account)
//		if (!entriesSectionProperty.getId().equals("common2.net.sf.jmoney.entry.incomeExpenseCurrency")
//				&& !entriesSectionProperty.getId().equals("other.net.sf.jmoney.entry.amount")) {
  
        
		/*
		 * Setup the layout structure of the header and rows.
		 */
		IndividualBlock<IObservableValue<? extends EntryFacade>> transactionDateColumn = PropertyBlock.createTransactionColumn(TransactionInfo.getDateAccessor());

		Currency currencyOfAccount;
		Block<IObservableValue<EntryFacade>> part1SubBlock;
		
		if (account instanceof CurrencyAccount) {
			CurrencyAccount currencyAccount = (CurrencyAccount)account;
			currencyOfAccount = currencyAccount.getCurrency();

			part1SubBlock = new HorizontalBlock<IObservableValue<EntryFacade>>(
				transactionDateColumn,
				new VerticalBlock<IObservableValue<EntryFacade>>(
						PropertyBlock.createEntryColumn(EntryInfo.getMemoAccessor()),
						new HorizontalBlock<IObservableValue<EntryFacade>>(
								PropertyBlock.createEntryColumn(EntryInfo.getCheckAccessor()),
								PropertyBlock.createEntryColumn(EntryInfo.getValutaAccessor())
						)
				),
				new OtherEntriesBlock<EntryFacade>(
						new HorizontalBlock<IObservableValue<Entry>>(
								new SingleOtherEntryDetailPropertyBlock(EntryInfo.getAccountAccessor()),
								new SingleOtherEntryDetailPropertyBlock(EntryInfo.getMemoAccessor(), Messages.EntriesSection_EntryDescription),
								new SingleOtherEntryDetailPropertyBlock(EntryInfo.getAmountAccessor())
						)
				)
			);
		} else {
			IncomeExpenseAccount categoryAccount = (IncomeExpenseAccount)account;
			currencyOfAccount = categoryAccount.getCurrency();
			
			part1SubBlock = new HorizontalBlock<IObservableValue<EntryFacade>>(
					transactionDateColumn,
					PropertyBlock.createEntryColumn(EntryInfo.getMemoAccessor(), Messages.CategoryEntriesSection_EntryDescription),
					new OtherEntriesBlock<EntryFacade>(
							new HorizontalBlock<IObservableValue<Entry>>(
									new SingleOtherEntryDetailPropertyBlock(EntryInfo.getAccountAccessor()),
									new SingleOtherEntryDetailPropertyBlock(EntryInfo.getMemoAccessor()),
									new SingleOtherEntryDetailPropertyBlock(EntryInfo.getAmountAccessor())
							)
					)
			);
		}
		
		Block<EntryRowControl> part1Block = new DelegateBlock<EntryRowControl, IObservableValue<EntryFacade>>(part1SubBlock) {
			@Override
			protected IObservableValue<EntryFacade> convert(
					EntryRowControl blockInput) {
				return blockInput.observeEntryFacade();
			}
		};

    	Block<EntryRowControl> debitAndCreditColumnsManager = new DelegateBlock<EntryRowControl, IObservableValue<Entry>>(
    			DebitAndCreditColumns.createDebitAndCreditColumns(currencyOfAccount)
			) {
			@Override
			protected IObservableValue<Entry> convert(EntryRowControl blockInput) {
				return blockInput.observeMainEntry();
			}
		};

		Block<EntryRowControl> balanceColumnManager = new DelegateBlock<EntryRowControl, IObservableValue<EntryData>>(new BalanceColumn(currencyOfAccount)) {
			@Override
			protected IObservableValue<EntryData> convert(EntryRowControl blockInput) {
				return blockInput.getRowInput();
			}
		};

		rootBlock = new HorizontalBlock<EntryRowControl>(
				part1Block,
				debitAndCreditColumnsManager,
				balanceColumnManager
		);

		// Create the table control.
	    IRowProvider<EntryData, EntryRowControl> rowProvider = new ReusableRowProvider(rootBlock);
		RowSelectionTracker<EntryRowControl> rowTracker = new RowSelectionTracker<EntryRowControl>();
		fEntriesControl = new EntriesTable<EntryRowControl>(getSection(), rootBlock, this, rowProvider, account.getSession(), transactionDateColumn, rowTracker) {
			@Override
			protected EntryData createEntryRowInput(Entry entry) {
				return new EntryData(entry, session.getDataManager());
			}

			@Override
			protected EntryData createNewEntryRowInput() {
				return new EntryData(null, session.getDataManager());
			}
		}; 
		fEntriesControl.addSelectionListener(tableSelectionListener);
			
		// Activate the handlers
		IHandler handler = new NewTransactionHandler(rowTracker, fEntriesControl);
		handlerService.activateHandler("net.sf.jmoney.newTransaction", handler);		 //$NON-NLS-1$

		handler = new DeleteTransactionHandler(rowTracker);
		handlerService.activateHandler("net.sf.jmoney.deleteTransaction", handler);		 //$NON-NLS-1$

		handler = new DuplicateTransactionHandler(rowTracker, fEntriesControl);
		handlerService.activateHandler("net.sf.jmoney.duplicateTransaction", handler);		 //$NON-NLS-1$

		handler = new OpenTransactionDialogHandler(rowTracker);
		handlerService.activateHandler("net.sf.jmoney.transactionDetails", handler);		 //$NON-NLS-1$

		getSection().setClient(fEntriesControl);
        toolkit.paintBordersFor(fEntriesControl);
        refresh();
    }

	@Override
	public Collection<Entry> getEntries() {
/* The caller always sorts, so there is no point in us returning
 * sorted results.  It may be at some point we decide it is more
 * efficient to get the database to sort for us, but that would
 * only help the first time the results are fetched, it would not
 * help on a re-sort.  It also only helps if the database indexes
 * on the date.		
        CurrencyAccount account = fPage.getAccount();
        Collection accountEntries = 
        	account
				.getSortedEntries(TransactionInfo.getDateAccessor(), false);
        return accountEntries;
*/
		return account.getEntries();
	}

	@Override
	public boolean isEntryInTable(Entry entry) {
		return account.equals(entry.getAccount());
	}

	/* (non-Javadoc)
	 * @see net.sf.jmoney.pages.entries.IEntriesContent#filterEntry(net.sf.jmoney.pages.entries.EntriesTable.DisplayableTransaction)
	 */
	@Override
	public boolean filterEntry(EntryData data) {
		return filter.filterEntry(data);
	}

	/* (non-Javadoc)
	 * @see net.sf.jmoney.pages.entries.IEntriesContent#getStartBalance()
	 */
	@Override
	public long getStartBalance() {
		if (account instanceof CurrencyAccount) {
			CurrencyAccount currencyAccount = (CurrencyAccount)account;
			return currencyAccount.getStartBalance();
		} else {
			return 0;
		}
	}

	@Override
	public Entry createNewEntry(Transaction newTransaction) {
		Entry entryInTransaction = newTransaction.createEntry();
		Entry otherEntry = newTransaction.createEntry();

		setNewEntryProperties(entryInTransaction);

		// TODO: See if this code has any effect, and
		// should this be here at all?
		/*
		 * We set the currency by default to be the currency of the
		 * top-level entry.
		 * 
		 * The currency of an entry is not applicable if the entry is an
		 * entry in a currency account or an income and expense account
		 * that is restricted to a single currency.
		 * However, we set it anyway so the value is there if the entry
		 * is set to an account which allows entries in multiple currencies.
		 * 
		 * It may be that the currency of the top-level entry is not
		 * known. This is not possible if entries in a currency account
		 * are being listed, but may be possible if this entries list
		 * control is used for more general purposes. In this case, the
		 * currency is not set and so the user must enter it.
		 */
		if (entryInTransaction.getCommodityInternal() instanceof Currency) {
			otherEntry.setIncomeExpenseCurrency((Currency)entryInTransaction.getCommodityInternal());
		}
		
		return entryInTransaction;
	}
	
	private void setNewEntryProperties(Entry newEntry) {
		// It is assumed that the entry is in a data manager that is a direct
		// child of the data manager that contains the account.
		TransactionManager tm = (TransactionManager)newEntry.getDataManager();
		newEntry.setAccount(tm.getCopyInTransaction(account));
	}
}
