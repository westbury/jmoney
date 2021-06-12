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
package net.sf.jmoney.stocks.pages;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.core.commands.IHandler;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.IValueChangeListener;
import org.eclipse.core.databinding.observable.value.ValueChangeEvent;
import org.eclipse.core.databinding.property.value.IValueProperty;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.SectionPart;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.handlers.IHandlerService;

import net.sf.jmoney.entrytable.BalanceColumn;
import net.sf.jmoney.entrytable.Block;
import net.sf.jmoney.entrytable.CellBlock;
import net.sf.jmoney.entrytable.CutTransactionHandler;
import net.sf.jmoney.entrytable.DebitAndCreditColumns;
import net.sf.jmoney.entrytable.DelegateBlock;
import net.sf.jmoney.entrytable.DeleteTransactionHandler;
import net.sf.jmoney.entrytable.DuplicateTransactionHandler;
import net.sf.jmoney.entrytable.EntriesTable;
import net.sf.jmoney.entrytable.EntryData;
import net.sf.jmoney.entrytable.EntryFacade;
import net.sf.jmoney.entrytable.HorizontalBlock;
import net.sf.jmoney.entrytable.IEntriesContent;
import net.sf.jmoney.entrytable.IRowProvider;
import net.sf.jmoney.entrytable.IndividualBlock;
import net.sf.jmoney.entrytable.NewTransactionHandler;
import net.sf.jmoney.entrytable.OpenTransactionDialogHandler;
import net.sf.jmoney.entrytable.OtherEntriesBlock;
import net.sf.jmoney.entrytable.PasteCombineTransactionHandler;
import net.sf.jmoney.entrytable.PropertyBlock;
import net.sf.jmoney.entrytable.RowControl;
import net.sf.jmoney.entrytable.RowSelectionTracker;
import net.sf.jmoney.entrytable.SingleOtherEntryDetailPropertyBlock;
import net.sf.jmoney.entrytable.StackBlock;
import net.sf.jmoney.entrytable.StackControl;
import net.sf.jmoney.entrytable.VerticalBlock;
import net.sf.jmoney.isolation.TransactionManager;
import net.sf.jmoney.model2.Account;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.EntryInfo;
import net.sf.jmoney.model2.IDataManagerForAccounts;
import net.sf.jmoney.model2.Transaction;
import net.sf.jmoney.model2.TransactionInfo;
import net.sf.jmoney.stocks.model.StockAccount;
import net.sf.jmoney.stocks.types.TransactionType;

/**
 * TODO: This code is duplicated in StockDetailsEditor. Remove the duplication
 *
 * @author Johann Gyger
 */
public class StockEntriesSection extends SectionPart implements IEntriesContent {

	public static class BlankBlock<F> extends CellBlock<IObservableValue<F>> {
		BlankBlock() {
			super(0, 0);
		}

		@Override
		public Control createCellControl(Composite parent, IObservableValue<F> master,
				RowControl rowControl) {
			return new Label(parent, SWT.NONE);
		}

		@Override
		public void createHeaderControls(Composite parent) {
			/*
			 * We need to create something here because the layout expects the correct
			 * number of controls. Create an empty label.
			 */
			new Label(parent, SWT.NONE);
		}
	}

	private Account thisAccount;
	private StockAccount stockAccount;

	private EntriesTable<StockEntryRowControl> fEntriesControl;

	private Block<StockEntryRowControl> rootBlock;

	public StockEntriesSection(Composite parent, Account thisAccount, StockAccount stockAccount, FormToolkit toolkit,
			IHandlerService handlerService) {
		super(parent, toolkit, ExpandableComposite.TITLE_BAR);
		getSection().setText("All Entries");
		this.thisAccount = thisAccount;
		this.stockAccount = stockAccount;

		/*
		 * Setup the layout structure of the header and rows.
		 */
		IndividualBlock<IObservableValue<? extends EntryFacade>> transactionDateColumn = PropertyBlock
				.createTransactionColumn(TransactionInfo.getDateAccessor());

		IndividualBlock<IObservableValue<StockEntryFacade>> actionColumn = new TransactionTypeBlock();

		IndividualBlock<IObservableValue<StockEntryFacade>> shareNameColumn = new SecurityBlock();

//		ValueProperty<StockEntryFacade, Long> withholdingTaxProperty2 = new ValueProperty<StockEntryFacade, Long>() {
//
//			@Override
//			public Object getValueType() {
//				return withholdingTaxProperty.getValueType();
//			}
//
//			@Override
//			public IObservableValue<Long> observe(Realm realm, StockEntryFacade source) {
//				return new ComputedValue<Long>() {
//					@Override
//					protected Long calculate() {
//						StockDividendFacade facade = source.dividendFacade().getValue();
//						return withholdingTaxProperty.getValue(facade);
//					}
//				};
//			}
//		};
		
		Block<IObservableValue<StockEntryFacade>> withholdingTaxColumn 
			= new DividendInfoColumn(stockAccount);
				

		Block<IObservableValue<StockEntryFacade>> purchaseOrSaleInfoColumn 
			= new TradeInfoColumn(stockAccount);


		
		
		
		final Block<IObservableValue<StockEntryFacade>> customTransactionColumn = new OtherEntriesBlock<StockEntryFacade>(
				new HorizontalBlock<IObservableValue<Entry>>(
						new SingleOtherEntryDetailPropertyBlock(EntryInfo.getAccountAccessor()),
						new SingleOtherEntryDetailPropertyBlock(EntryInfo.getMemoAccessor(),
								net.sf.jmoney.resources.Messages.EntriesSection_EntryDescription),
						new SingleOtherEntryDetailPropertyBlock(EntryInfo.getAmountAccessor())));

		Block<StockEntryRowControl> debitAndCreditColumnsManager = new DelegateBlock<StockEntryRowControl, IObservableValue<Entry>>(
				DebitAndCreditColumns.createDebitAndCreditColumns(stockAccount.getCurrency())) {
			@Override
			protected IObservableValue<Entry> convert(StockEntryRowControl blockInput) {
				return blockInput.observeMainEntry();
			}
		};

		Block<StockEntryRowControl> balanceColumnManager = new DelegateBlock<StockEntryRowControl, IObservableValue<EntryData>>(
				new BalanceColumn(stockAccount.getCurrency())) {
			@Override
			protected IObservableValue<EntryData> convert(StockEntryRowControl blockInput) {
				return blockInput.getRowInput();
			}
		};

		RowSelectionTracker<StockEntryRowControl> rowTracker = new RowSelectionTracker<StockEntryRowControl>();

		
		Block<IObservableValue<StockEntryFacade>> part1SubBlock = new HorizontalBlock<IObservableValue<StockEntryFacade>>(
				transactionDateColumn,
				new VerticalBlock<IObservableValue<StockEntryFacade>>(
						new HorizontalBlock<IObservableValue<StockEntryFacade>>(actionColumn, shareNameColumn),
						new PropertyBlock<IObservableValue<StockEntryFacade>, Entry>(EntryInfo.getMemoAccessor(),
								"entry") { //$NON-NLS-1$
							@Override
							public Entry getObjectContainingProperty(IObservableValue<StockEntryFacade> data) {
								return data.getValue() == null ? null : data.getValue().getMainEntry();
							}
						}),
				new StackBlock<IObservableValue<StockEntryFacade>>(withholdingTaxColumn, purchaseOrSaleInfoColumn,
						customTransactionColumn) {

					@Override
					protected Block<IObservableValue<StockEntryFacade>> getTopBlock(IObservableValue<StockEntryFacade> data) {
						// TODO this is wrong. If we get an observable, we must take into account that
						// it may change.
						// If in fact it never changes then the value should be passed in, not the
						// observable.

						TransactionType transactionType = data.getValue().getTransactionType();
						if (transactionType == null) {
							return null;
						} else {
							switch (transactionType) {
							case Buy:
							case Sell:
//								purchaseOrSaleInfoColumn.setFacade(data.getValue().getBuyOrSellFacade());
								return purchaseOrSaleInfoColumn;
							case Dividend:
								// Note that this will be null if there is no withholding tax account
//								if (withholdingTaxColumn != null) {
//									withholdingTaxColumn.setFacade(data.getValue().getDividendFacade());
//								}
								return withholdingTaxColumn;
							case Other:
								return customTransactionColumn;
							default:
								throw new RuntimeException("bad case");
							}
						}
					}

					@Override
					public Control createCellControl(Composite parent,
							final IObservableValue<StockEntryFacade> blockInput, final RowControl rowControl) {
						final StackControl<IObservableValue<StockEntryFacade>> control = new StackControl<IObservableValue<StockEntryFacade>>(
								parent, rowControl, blockInput, this);

						IValueProperty<StockEntryFacade, TransactionType> transactionProperty = new PropertyOnObservable<StockEntryFacade, TransactionType>(
								TransactionType.class) {
							@Override
							protected IObservableValue<TransactionType> getObservable(StockEntryFacade source) {
								return source.transactionType();
							}
						};

						transactionProperty.observeDetail(blockInput)
								.addValueChangeListener(new IValueChangeListener<TransactionType>() {

									@Override
									public void handleValueChange(ValueChangeEvent<? extends TransactionType> event) {
										// todo: queue this code and run asynchronously. So it runs once only
										// when lots of changes are made by the same code???

										Block<IObservableValue<StockEntryFacade>> topBlock = getTopBlock(blockInput);

										// Set this block in the control
										control.setTopBlock(topBlock);

										/*
										 * This stack layout has a size this is the preferred size of the top control,
										 * ignoring all the other controls. Therefore changing the top control may
										 * change the size of the row.
										 */
										// TODO: It is a bit funny using the coordinator here
										// This needs to be cleaned up.
										fEntriesControl.table.refreshSize(rowControl);

										/*
										 * The above method will re-size the height of the row to its preferred height,
										 * but it won't layout the child controls if the preferred height did not
										 * change. We therefore force a layout in order to bring the new top control to
										 * the top and layout its child controls.
										 */
										rowControl.layout(true);
									}
								});

						return control;
					}

					@Override
					protected IDataManagerForAccounts getDataManager(IObservableValue<StockEntryFacade> data) {
						return data.getValue().getMainEntry().getDataManager();
					}
				});

		Block<StockEntryRowControl> part1Block = new DelegateBlock<StockEntryRowControl, IObservableValue<StockEntryFacade>>(
				part1SubBlock) {
			@Override
			protected IObservableValue<StockEntryFacade> convert(StockEntryRowControl blockInput) {
				return blockInput.observeEntryFacade();
			}
		};

		rootBlock = new HorizontalBlock<StockEntryRowControl>(part1Block, debitAndCreditColumnsManager,
				balanceColumnManager);

		// Create the table control.
		IRowProvider<EntryData, StockEntryRowControl> rowProvider = new StockRowProvider(rootBlock);
		fEntriesControl = new EntriesTable<StockEntryRowControl>(getSection(), rootBlock, this, rowProvider,
				thisAccount.getSession(), transactionDateColumn, rowTracker) {
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
	@Override
	public Collection<Entry> getEntries() {
		/*
		 * We want only cash entries, not stock entries. This is providing content for a
		 * table of entries that show the running balance. A stock entry or an entry in
		 * a currency other than the currency of the account should not be returned.
		 */
		Collection<Entry> entries = new ArrayList<Entry>();
		for (Entry entry : thisAccount.getEntries()) {
			if (entry.getCommodityInternal() == stockAccount.getCurrency()) {
				entries.add(entry);
			}
		}

		return entries;
	}

	@Override
	public boolean isEntryInTable(Entry entry) {
		/*
		 * Entry must be in right account AND be in the currency of the account. The
		 * account will contain stock entries and these should not appear as top level
		 * entries in the table.
		 */
		return entry.getAccount() == thisAccount && entry.getCommodityInternal() == stockAccount.getCurrency();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * net.sf.jmoney.pages.entries.IEntriesContent#filterEntry(net.sf.jmoney.pages.
	 * entries.EntriesTable.DisplayableTransaction)
	 */
	@Override
	public boolean filterEntry(EntryData data) {
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.sf.jmoney.pages.entries.IEntriesContent#getStartBalance()
	 */
	@Override
	public long getStartBalance() {
		return 0;
		// ???? account.getStartBalance();
	}

	@Override
	public Entry createNewEntry(Transaction newTransaction) {
		/*
		 * For stock entries, we create a single entry only. The other entries are
		 * created as appropriate when a transaction type is selected.
		 */
		Entry entryInTransaction = newTransaction.createEntry();

		// It is assumed that the entry is in a data manager that is a direct
		// child of the data manager that contains the account.
		TransactionManager tm = (TransactionManager) entryInTransaction.getDataManager();
		entryInTransaction.setAccount(tm.getCopyInTransaction(thisAccount));

		return entryInTransaction;
	}
}
