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
package net.sf.jmoney.property.pages;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import net.sf.jmoney.entrytable.BalanceColumn;
import net.sf.jmoney.entrytable.BaseEntryRowControl;
import net.sf.jmoney.entrytable.Block;
import net.sf.jmoney.entrytable.CellBlock;
import net.sf.jmoney.entrytable.CutTransactionHandler;
import net.sf.jmoney.entrytable.DebitAndCreditColumns;
import net.sf.jmoney.entrytable.DeleteTransactionHandler;
import net.sf.jmoney.entrytable.DuplicateTransactionHandler;
import net.sf.jmoney.entrytable.EntriesTable;
import net.sf.jmoney.entrytable.EntryData;
import net.sf.jmoney.entrytable.EntryRowControl;
import net.sf.jmoney.entrytable.HorizontalBlock;
import net.sf.jmoney.entrytable.IEntriesContent;
import net.sf.jmoney.entrytable.IRowProvider;
import net.sf.jmoney.entrytable.ISplitEntryContainer;
import net.sf.jmoney.entrytable.IndividualBlock;
import net.sf.jmoney.entrytable.NewTransactionHandler;
import net.sf.jmoney.entrytable.OpenTransactionDialogHandler;
import net.sf.jmoney.entrytable.OtherEntriesBlock;
import net.sf.jmoney.entrytable.PasteCombineTransactionHandler;
import net.sf.jmoney.entrytable.PropertyBlock;
import net.sf.jmoney.entrytable.ReusableRowProvider;
import net.sf.jmoney.entrytable.RowControl;
import net.sf.jmoney.entrytable.RowSelectionTracker;
import net.sf.jmoney.entrytable.SingleOtherEntryPropertyBlock;
import net.sf.jmoney.entrytable.VerticalBlock;
import net.sf.jmoney.isolation.TransactionManager;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.EntryInfo;
import net.sf.jmoney.model2.Transaction;
import net.sf.jmoney.model2.TransactionInfo;
import net.sf.jmoney.property.model.RealPropertyAccount;
import net.sf.jmoney.resources.Messages;

import org.eclipse.core.commands.IHandler;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.SectionPart;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.handlers.IHandlerService;

/**
 * TODO
 * 
 * @author Johann Gyger
 */
public class EntriesSection extends SectionPart implements IEntriesContent {

	private RealPropertyAccount account;

    private EntriesTable<EntryData> fEntriesControl;
    
    private Block<EntryData, EntryRowControl> rootBlock;
    
    public EntriesSection(Composite parent, final RealPropertyAccount account, FormToolkit toolkit, IHandlerService handlerService) {
        super(parent, toolkit, ExpandableComposite.TITLE_BAR);
        getSection().setText("All Entries");
        this.account = account;
    	
		/*
		 * Setup the layout structure of the header and rows.
		 */
		IndividualBlock<EntryData, RowControl> transactionDateColumn = PropertyBlock.createTransactionColumn(TransactionInfo.getDateAccessor());

		List<Block<? super EntryData, ? super StockEntryRowControl>> expenseColumns = new ArrayList<Block<? super EntryData, ? super StockEntryRowControl>>();
		
		CellBlock<EntryData, BaseEntryRowControl> debitColumnManager = DebitAndCreditColumns.createDebitColumn(account.getCurrency());
		CellBlock<EntryData, BaseEntryRowControl> creditColumnManager = DebitAndCreditColumns.createCreditColumn(account.getCurrency());
    	CellBlock<EntryData, BaseEntryRowControl> balanceColumnManager = new BalanceColumn(account.getCurrency());
		
		RowSelectionTracker<EntryRowControl> rowTracker = new RowSelectionTracker<EntryRowControl>();

		rootBlock = new HorizontalBlock<EntryData, EntryRowControl>(
				transactionDateColumn,
				new VerticalBlock<EntryData, EntryRowControl>(
						new OtherEntriesBlock(
								new HorizontalBlock<Entry, ISplitEntryContainer>(
										new SingleOtherEntryPropertyBlock(EntryInfo.getAccountAccessor()),
										new SingleOtherEntryPropertyBlock(EntryInfo.getMemoAccessor(), Messages.EntriesSection_EntryDescription),
										new SingleOtherEntryPropertyBlock(EntryInfo.getAmountAccessor())
								)
						),
						PropertyBlock.createEntryColumn(EntryInfo.getMemoAccessor())
				),
				debitColumnManager,
				creditColumnManager,
				balanceColumnManager
		);

		// Create the table control.
	    IRowProvider<EntryData> rowProvider = new ReusableRowProvider(rootBlock);
		fEntriesControl = new EntriesTable<EntryData>(getSection(), rootBlock, this, rowProvider, account.getSession(), transactionDateColumn, rowTracker) {
			@Override
			protected EntryData createEntryRowInput(Entry entry) {
				return new EntryData(entry, session.getDataManager());
			}

			@Override
			protected EntryData createNewEntryRowInput() {
				return new EntryData(null, session.getDataManager());
			}
		}; 
			
		// Activate the handlers
		IHandler handler = new NewTransactionHandler(rowTracker, fEntriesControl);
		handlerService.activateHandler("net.sf.jmoney.newTransaction", handler);		

		handler = new DeleteTransactionHandler(rowTracker);
		handlerService.activateHandler("net.sf.jmoney.deleteTransaction", handler);		

		handler = new DuplicateTransactionHandler(rowTracker, fEntriesControl);
		handlerService.activateHandler("net.sf.jmoney.duplicateTransaction", handler);		

		handler = new OpenTransactionDialogHandler(rowTracker);
		handlerService.activateHandler("net.sf.jmoney.transactionDetails", handler);		

		handler = new CutTransactionHandler(rowTracker);
		handlerService.activateHandler("net.sf.jmoney.cutTransaction", handler);		

		handler = new PasteCombineTransactionHandler(rowTracker);
		handlerService.activateHandler("net.sf.jmoney.pasteCombineTransaction", handler);		

        getSection().setClient(fEntriesControl);
        toolkit.paintBordersFor(fEntriesControl);
        refresh();
    }

    public void refreshEntryList() {
    	fEntriesControl.refreshEntryList();
    }

    /**
     * @return the entries to be shown in the table, unsorted
     */
	public Collection<Entry> getEntries() {
		/*
		 * We want only cash entries, not stock entries.  This is providing
		 * content for a table of entries that show the running balance.
		 * A stock entry or an entry in a currency other than the currency
		 * of the account should not be returned.
		 */
		Collection<Entry> entries = new ArrayList<Entry>();
		for (Entry entry : account.getEntries()) {
			if (entry.getCommodityInternal() == account.getCurrency()) {
				entries.add(entry);
			}
		}
		
		return entries;
	}

	public boolean isEntryInTable(Entry entry) {
		/*
		 * Entry must be in right account AND be in the currency of the account.
		 * The account will contain stock entries and these should not appear
		 * as top level entries in the table.
		 */
		return account == entry.getAccount()
			&& entry.getCommodityInternal() == account.getCurrency();
	}

	/* (non-Javadoc)
	 * @see net.sf.jmoney.pages.entries.IEntriesContent#filterEntry(net.sf.jmoney.pages.entries.EntriesTable.DisplayableTransaction)
	 */
	public boolean filterEntry(EntryData data) {
		return true;
	}

	/* (non-Javadoc)
	 * @see net.sf.jmoney.pages.entries.IEntriesContent#getStartBalance()
	 */
	public long getStartBalance() {
        return 0; 
        // ???? account.getStartBalance();
	}

	public Entry createNewEntry(Transaction newTransaction) {
		/*
		 * For stock entries, we create a single entry only.
		 * The other entries are created as appropriate when a
		 * transaction type is selected.
		 */
		Entry entryInTransaction = newTransaction.createEntry();

		// It is assumed that the entry is in a data manager that is a direct
		// child of the data manager that contains the account.
		TransactionManager tm = (TransactionManager)entryInTransaction.getDataManager();
		entryInTransaction.setAccount(tm.getCopyInTransaction(account));

		return entryInTransaction;
	}
}
