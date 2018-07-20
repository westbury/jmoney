/*
 *
 *  JMoney - A Personal Finance Manager
 *  Copyright (c) 2009 Nigel Westbury <westbury@users.sourceforge.net>
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

package net.sf.jmoney.stocks.pages;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.commands.IHandler;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.IValueChangeListener;
import org.eclipse.core.databinding.observable.value.ValueChangeEvent;
import org.eclipse.core.databinding.property.value.IValueProperty;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.SectionPart;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.part.EditorPart;

import net.sf.jmoney.entrytable.BalanceColumn;
import net.sf.jmoney.entrytable.Block;
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
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.EntryInfo;
import net.sf.jmoney.model2.IDataManagerForAccounts;
import net.sf.jmoney.model2.Transaction;
import net.sf.jmoney.model2.TransactionInfo;
import net.sf.jmoney.stocks.model.Security;
import net.sf.jmoney.stocks.model.StockAccount;
import net.sf.jmoney.stocks.pages.StockEntryRowControl.TransactionType;
import net.sf.jmoney.views.AccountEditorInput;

public class StockDetailsEditor extends EditorPart {

	

	//	static public final String ID = "net.sf.jmoney.stocks.stockDetailsEditor";

	/**
	 * The account being shown in this page.
	 */
	private StockAccount account;

	/**
	 * The stock being shown in this page.
	 */
	private final Security stock;

	private EntriesTable<StockEntryRowControl> fEntriesControl;

	public StockDetailsEditor(Security stock) {
		this.stock = stock;
	}

	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {

		setSite(site);
		setInput(input);

		// Set the account that this page is viewing and editing.
		AccountEditorInput input2 = (AccountEditorInput)input;
		IDataManagerForAccounts sessionManager = (IDataManagerForAccounts)site.getPage().getInput();
		account = (StockAccount)sessionManager.getSession().getAccountByFullName(input2.getFullAccountName());
	}

	@Override
	public boolean isDirty() {
		// Page is never dirty
		return false;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		// Will never be called because editor is never dirty.
	}

	@Override
	public void doSaveAs() {
		// Will never be called because editor is never dirty and 'save as' is not allowed anyway.
	}

	@Override
	public void createPartControl(Composite parent) {
		FormToolkit toolkit = new FormToolkit(parent.getDisplay());
		ScrolledForm form = toolkit.createScrolledForm(parent);
		form.getBody().setLayout(new GridLayout());

		SectionPart section = new SectionPart(form.getBody(), toolkit, ExpandableComposite.TITLE_BAR);
		section.getSection().setText("All Entries");
		section.getSection().setLayoutData(new GridData(GridData.FILL_BOTH));

		Composite contents = createContents(section.getSection());

		// Activate the handlers
		//		IHandler handler = new NewTransactionHandler(rowTracker, fEntriesControl);
		//		handlerService.activateHandler("net.sf.jmoney.newTransaction", handler);

		section.getSection().setClient(contents);
		toolkit.paintBordersFor(contents);
		section.refresh();  // ?????



		StringBuffer formTitle = new StringBuffer();
		formTitle.append("Activity for ").append(stock.getName());
		if (stock.getSymbol() != null) {
			formTitle.append(" (").append(stock.getSymbol()).append(")");
		}
		form.setText(formTitle.toString());
	}

	private Composite createContents(Composite parent) {
		Composite composite = new Composite(parent, SWT.None);
		composite.setLayout(new GridLayout());

		/*
		 * Setup the layout structure of the header and rows.
		 */
		IndividualBlock<IObservableValue<? extends EntryFacade>> transactionDateColumn = PropertyBlock.createTransactionColumn(TransactionInfo.getDateAccessor());

		IndividualBlock<IObservableValue<StockEntryFacade>> actionColumn = new TransactionTypeBlock();

		IndividualBlock<IObservableValue<StockEntryFacade>> shareNameColumn = new SecurityBlock();

		IndividualBlock<IObservableValue<StockEntryFacade>> priceColumn = new StockPriceBlock(account);

		IndividualBlock<IObservableValue<StockEntryFacade>> shareQuantityColumn = new ShareQuantityBlock(account);

		IValueProperty<StockEntryFacade, Long> withholdingTaxProperty = new PropertyOnObservable<StockEntryFacade, Long>(Long.class) {
			@Override
			protected IObservableValue<Long> getObservable(StockEntryFacade source) {
				return source.withholdingTax();
			}
		};
		
		// Null does not work as a child of StackBlock so create an empty one.
		final Block<IObservableValue<StockEntryFacade>> withholdingTaxColumn =
//				account.getWithholdingTaxAccount() == null 
//				? new BlankBlock()
//				: 
					new EntryAmountBlock("Withholding Tax", withholdingTaxProperty, account.getWithholdingTaxAccount().getCurrency());

		List<Block<? super IObservableValue<StockEntryFacade>>> expenseColumns = new ArrayList<Block<? super IObservableValue<StockEntryFacade>>>();

		if (account.getCommissionAccount() != null) {
			IValueProperty<StockEntryFacade, Long> commissionProperty = new PropertyOnObservable<StockEntryFacade, Long>(Long.class) {
				@Override
				protected IObservableValue<Long> getObservable(StockEntryFacade source) {
					return source.commission();
				}
			};
			
			final Block<IObservableValue<StockEntryFacade>> commissionColumn =
					new EntryAmountBlock("Commission", commissionProperty, account.getCommissionAccount().getCurrency());
			
			expenseColumns.add(commissionColumn);
		}

		if (account.getTax1Name() != null && account.getTax1Account() != null) {
			IValueProperty<StockEntryFacade, Long> tax1Property = new PropertyOnObservable<StockEntryFacade, Long>(Long.class) {
				@Override
				protected IObservableValue<Long> getObservable(StockEntryFacade source) {
					return source.tax1();
				}
			};
			
			final Block<IObservableValue<StockEntryFacade>> tax1Column =
					new EntryAmountBlock(account.getTax1Name(), tax1Property, account.getTax1Account().getCurrency());

			expenseColumns.add(tax1Column);
		}

		if (account.getTax2Name() != null && account.getTax2Account() != null) {
			IValueProperty<StockEntryFacade, Long> tax2Property = new PropertyOnObservable<StockEntryFacade, Long>(Long.class) {
				@Override
				protected IObservableValue<Long> getObservable(StockEntryFacade source) {
					return source.tax2();
				}
			};
			
			final Block<IObservableValue<StockEntryFacade>> tax2Column =
					new EntryAmountBlock(account.getTax2Name(), tax2Property, account.getTax2Account().getCurrency());

			expenseColumns.add(tax2Column);
		}

		final Block<IObservableValue<StockEntryFacade>> purchaseOrSaleInfoColumn = new VerticalBlock<IObservableValue<StockEntryFacade>>(
				priceColumn,
				shareQuantityColumn,
				new HorizontalBlock<IObservableValue<StockEntryFacade>>(
						expenseColumns
				)
		);

		final IndividualBlock<IObservableValue<StockEntryFacade>> transferAccountColumn = new PropertyBlock<IObservableValue<StockEntryFacade>, Entry>(EntryInfo.getAccountAccessor(), "transferAccount", "Transfer Account") {
			@Override
			public Entry getObjectContainingProperty(IObservableValue<StockEntryFacade> data) {
				return data.getValue().getTransferEntry();
			}
		};

		final Block<IObservableValue<StockEntryFacade>> customTransactionColumn = new OtherEntriesBlock<StockEntryFacade>(
				new HorizontalBlock<IObservableValue<Entry>>(
						new SingleOtherEntryDetailPropertyBlock(EntryInfo.getAccountAccessor()),
						new SingleOtherEntryDetailPropertyBlock(EntryInfo.getMemoAccessor(), net.sf.jmoney.resources.Messages.EntriesSection_EntryDescription),
						new SingleOtherEntryDetailPropertyBlock(EntryInfo.getAmountAccessor())
						)
				);

    	Block<StockEntryRowControl> debitAndCreditColumnsManager = new DelegateBlock<StockEntryRowControl, IObservableValue<Entry>>(
    			DebitAndCreditColumns.createDebitAndCreditColumns(stock)
			) {
			@Override
			protected IObservableValue<Entry> convert(StockEntryRowControl blockInput) {
				return blockInput.observeMainEntry();
			}
		};

		Block<StockEntryRowControl> balanceColumnManager = new DelegateBlock<StockEntryRowControl, IObservableValue<EntryData>>(new BalanceColumn(stock)) {
			@Override
			protected IObservableValue<EntryData> convert(StockEntryRowControl blockInput) {
				return blockInput.getRowInput();
			}
		};
		
		Block<IObservableValue<StockEntryFacade>> part1SubBlock = new HorizontalBlock<IObservableValue<StockEntryFacade>>(
				transactionDateColumn,
				new VerticalBlock<IObservableValue<StockEntryFacade>>(
						new HorizontalBlock<IObservableValue<StockEntryFacade>>(
								actionColumn,
								shareNameColumn
								),
								new PropertyBlock<IObservableValue<StockEntryFacade>, Entry>(EntryInfo.getMemoAccessor(), "entry") { //$NON-NLS-1$
							@Override
							public Entry getObjectContainingProperty(IObservableValue<StockEntryFacade> data) {
								return data.getValue() == null ? null : data.getValue().getMainEntry();
							}
						}
				),
				new StackBlock<IObservableValue<StockEntryFacade>>(
						withholdingTaxColumn,
						purchaseOrSaleInfoColumn,
						transferAccountColumn,
						customTransactionColumn
						) {

					@Override
					protected Block<IObservableValue<StockEntryFacade>> getTopBlock(IObservableValue<StockEntryFacade> data) {
						if (data.getValue().getTransactionType() == null) {
							return null;
						} else {
							switch (data.getValue().getTransactionType()) {
							case Buy:
							case Sell:
								return purchaseOrSaleInfoColumn;
							case Dividend:
								// Note that this will be null if there is no withholding tax account
								return withholdingTaxColumn;
							case Transfer:
								return transferAccountColumn;
							case Other:
								return customTransactionColumn;
							default:
								throw new RuntimeException("bad case");
							}
						}
					}

					@Override
					public Control createCellControl(Composite parent, final IObservableValue<StockEntryFacade> blockInput, final RowControl rowControl) {
						final StackControl<IObservableValue<StockEntryFacade>> control = new StackControl<IObservableValue<StockEntryFacade>>(parent, rowControl, blockInput, this);

						IValueProperty<StockEntryFacade, TransactionType> transactionProperty = new PropertyOnObservable<StockEntryFacade, TransactionType>(TransactionType.class) {
							@Override
							protected IObservableValue<TransactionType> getObservable(
									StockEntryFacade source) {
								return source.transactionType();
							}
						};

						transactionProperty.observeDetail(blockInput).addValueChangeListener(new IValueChangeListener<TransactionType>() {

							@Override
							public void handleValueChange(ValueChangeEvent<? extends TransactionType> event) {
								//								todo: queue this code and run asynchronously.  So it runs once only
								//								when lots of changes are made by the same code???

								Block<IObservableValue<StockEntryFacade>> topBlock = getTopBlock(blockInput);

								// Set this block in the control
								control.setTopBlock(topBlock);

								/*
								 * This stack layout has a size this is the
								 * preferred size of the top control, ignoring
								 * all the other controls. Therefore changing
								 * the top control may change the size of the
								 * row.
								 */
								// TODO: It is a bit funny using the coordinator here
								// This needs to be cleaned up.
								fEntriesControl.table.refreshSize(rowControl);

								/*
								 * The above method will re-size the height of the row
								 * to its preferred height, but it won't layout the child
								 * controls if the preferred height did not change.
								 * We therefore force a layout in order to bring the new
								 * top control to the top and layout its child controls.
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
				}
		);

    	Block<StockEntryRowControl> part1Block = new DelegateBlock<StockEntryRowControl, IObservableValue<StockEntryFacade>>(part1SubBlock) {
			@Override
			protected IObservableValue<StockEntryFacade> convert(StockEntryRowControl blockInput) {
				return blockInput.observeEntryFacade();
			}
		};

		Block<StockEntryRowControl> rootBlock = new HorizontalBlock<StockEntryRowControl>(
				part1Block,
				debitAndCreditColumnsManager,
				balanceColumnManager
		);

		RowSelectionTracker<StockEntryRowControl> rowTracker = new RowSelectionTracker<StockEntryRowControl>();

		IEntriesContent entriesProvider = new IEntriesContent() {

			@Override
			public Collection<Entry> getEntries() {
				/*
				 * We want only cash entries, not stock entries.  This is providing
				 * content for a table of entries that show the running balance.
				 * A stock entry or an entry in a currency other than the currency
				 * of the account should not be returned.
				 */
				Collection<Entry> entries = new ArrayList<Entry>();
				for (Entry entry : account.getEntries()) {
					if (entry.getCommodityInternal() == stock) {
						entries.add(entry);
					}
				}

				return entries;
			}

			@Override
			public boolean isEntryInTable(Entry entry) {
				/*
				 * Entry must be in right account AND be an entry that affects or is related
				 * to the given stock.
				 */
				return account == entry.getAccount()
						&& entry.getCommodityInternal() == stock;
			}

			/* (non-Javadoc)
			 * @see net.sf.jmoney.pages.entries.IEntriesContent#filterEntry(net.sf.jmoney.pages.entries.EntriesTable.DisplayableTransaction)
			 */
			@Override
			public boolean filterEntry(EntryData data) {
				return true;
			}

			/* (non-Javadoc)
			 * @see net.sf.jmoney.pages.entries.IEntriesContent#getStartBalance()
			 */
			@Override
			public long getStartBalance() {
				return 0;
			}

			@Override
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
		};

		// Create the table control.
		IRowProvider<EntryData, StockEntryRowControl> rowProvider = new StockRowProvider(rootBlock);
		fEntriesControl = new EntriesTable<StockEntryRowControl>(composite, rootBlock, entriesProvider, rowProvider, account.getSession(), transactionDateColumn, rowTracker) {
			@Override
			protected EntryData createEntryRowInput(Entry entry) {
				return new EntryData(entry, session.getDataManager());
			}

			@Override
			protected EntryData createNewEntryRowInput() {
				return new EntryData(null, session.getDataManager());
			}
		};

		fEntriesControl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		// Activate the handlers
		IHandlerService handlerService = getSite().getService(IHandlerService.class);

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

		return composite;
	}

	@Override
	public void setFocus() {
		// Don't bother to do anything.  User can select as required.
	}

	/*
	 * Method used when searching editors in the multi-part editor to see
	 * if an editor page already exists for a stock.  There may be another
	 * way of doing this, perhaps by setting the stock through an editor input.
	 */
	public Security getStock() {
		return stock;
	}
}
