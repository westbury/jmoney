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

package net.sf.jmoney.property.pages;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import net.sf.jmoney.entrytable.BalanceColumn;
import net.sf.jmoney.entrytable.BaseEntryRowControl;
import net.sf.jmoney.entrytable.Block;
import net.sf.jmoney.entrytable.CellBlock;
import net.sf.jmoney.entrytable.CellFocusListener;
import net.sf.jmoney.entrytable.CutTransactionHandler;
import net.sf.jmoney.entrytable.DebitAndCreditColumns;
import net.sf.jmoney.entrytable.DeleteTransactionHandler;
import net.sf.jmoney.entrytable.DuplicateTransactionHandler;
import net.sf.jmoney.entrytable.EntriesTable;
import net.sf.jmoney.entrytable.EntryData;
import net.sf.jmoney.entrytable.EntryRowControl;
import net.sf.jmoney.entrytable.HorizontalBlock;
import net.sf.jmoney.entrytable.ICellControl2;
import net.sf.jmoney.entrytable.IEntriesContent;
import net.sf.jmoney.entrytable.IRowProvider;
import net.sf.jmoney.entrytable.ISplitEntryContainer;
import net.sf.jmoney.entrytable.IndividualBlock;
import net.sf.jmoney.entrytable.NewTransactionHandler;
import net.sf.jmoney.entrytable.OpenTransactionDialogHandler;
import net.sf.jmoney.entrytable.OtherEntriesBlock;
import net.sf.jmoney.entrytable.PasteCombineTransactionHandler;
import net.sf.jmoney.entrytable.PropertyBlock;
import net.sf.jmoney.entrytable.RowControl;
import net.sf.jmoney.entrytable.RowSelectionTracker;
import net.sf.jmoney.entrytable.SingleOtherEntryPropertyBlock;
import net.sf.jmoney.entrytable.StackBlock;
import net.sf.jmoney.entrytable.StackControl;
import net.sf.jmoney.entrytable.VerticalBlock;
import net.sf.jmoney.fields.IAmountFormatter;
import net.sf.jmoney.isolation.IDataManager;
import net.sf.jmoney.isolation.TransactionManager;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.EntryInfo;
import net.sf.jmoney.model2.IDataManagerForAccounts;
import net.sf.jmoney.model2.Transaction;
import net.sf.jmoney.model2.TransactionInfo;
import net.sf.jmoney.property.model.RealProperty;
import net.sf.jmoney.property.model.RealPropertyAccount;
import net.sf.jmoney.property.model.RealPropertyControl;
import net.sf.jmoney.property.model.RealPropertyEntry;
import net.sf.jmoney.property.model.RealPropertyEntryInfo;
import net.sf.jmoney.property.pages.StockEntryRowControl.TransactionType;
import net.sf.jmoney.resources.Messages;
import net.sf.jmoney.views.AccountEditorInput;

import org.eclipse.core.commands.IHandler;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.SectionPart;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.statushandlers.StatusManager;

public class AssetDetailsEditor extends EditorPart {

//	static public final String ID = "net.sf.jmoney.property.stockDetailsEditor";

	/**
	 * The account being shown in this page.
	 */
	private RealPropertyAccount account;

	/**
	 * The asset being shown in this page.
	 */
	private RealProperty asset;

    private EntriesTable<StockEntryData> fEntriesControl;

	public AssetDetailsEditor(RealProperty stock) {
		this.asset = stock;
	}

	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {

		setSite(site);
		setInput(input);

    	// Set the account that this page is viewing and editing.
		AccountEditorInput input2 = (AccountEditorInput)input;
		IDataManagerForAccounts sessionManager = (IDataManagerForAccounts)site.getPage().getInput();
        account = (RealPropertyAccount)sessionManager.getSession().getAccountByFullName(input2.getFullAccountName());
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
        formTitle.append("Activity for ").append(asset.getName());
        form.setText(formTitle.toString());
	}

	private Composite createContents(Composite parent) {
		Composite composite = new Composite(parent, SWT.None);
		composite.setLayout(new GridLayout());

		/*
		 * Setup the layout structure of the header and rows.
		 */
		IndividualBlock<EntryData, RowControl> transactionDateColumn = PropertyBlock.createTransactionColumn(TransactionInfo.getDateAccessor());

		IndividualBlock<StockEntryData, StockEntryRowControl> actionColumn = new IndividualBlock<StockEntryData, StockEntryRowControl>("Action", 50, 1) {

			@Override
			public Control createCellControl(Composite parent, IObservableValue<? extends StockEntryData> master, RowControl rowControl, final StockEntryRowControl coordinator) {
				final CCombo control = new CCombo(parent, SWT.NONE);
				control.add("buy");
				control.add("sell");
				control.add("custom");

				control.addSelectionListener(new SelectionAdapter(){
					@Override
					public void widgetSelected(SelectionEvent e) {
						int index = control.getSelectionIndex();
						switch (index) {
						case 0:
							coordinator.getUncommittedEntryData().forceTransactionToBuy();
							break;
						case 1:
							coordinator.getUncommittedEntryData().forceTransactionToSell();
							break;
						case 2:
							coordinator.getUncommittedEntryData().forceTransactionToCustom();
							break;
						}

						coordinator.fireTransactionTypeChange();
					}
				});

				ICellControl2<StockEntryData> cellControl = new ICellControl2<StockEntryData>() {

					public Control getControl() {
						return control;
					}

					public void load(StockEntryData data) {
						if (data.getTransactionType() == null) {
							control.deselectAll();
							control.setText("");
						} else {
							switch (data.getTransactionType()) {
							case Buy:
								control.select(0);
								break;
							case Sell:
								control.select(1);
								break;
							case Other:
								control.select(2);
								break;
							default:
								throw new RuntimeException("bad case");
							}
						}
					}

					public void save() {
						// TODO Auto-generated method stub

					}

					public void setSelected() {
						control.setBackground(RowControl.selectedCellColor);
					}

					public void setUnselected() {
						control.setBackground(null);
					}
				};

				FocusListener controlFocusListener = new CellFocusListener<RowControl>(rowControl, cellControl);

				/*
				 * The control may in fact be a composite control, in which case the
				 * composite control itself will never get the focus. Only the child
				 * controls will get the focus, so we add the listener recursively
				 * to all child controls.
				 */
				control.addFocusListener(controlFocusListener);

				return cellControl.getControl();
			}
		};

		IndividualBlock<StockEntryData, StockEntryRowControl> shareNameColumn = new IndividualBlock<StockEntryData, StockEntryRowControl>("Stock", 50, 1) {

			@Override
			public Control createCellControl(Composite parent, IObservableValue<? extends StockEntryData> master, RowControl rowControl, final StockEntryRowControl coordinator) {
				final RealPropertyControl<RealProperty> control = new RealPropertyControl<RealProperty>(parent, null, RealProperty.class);

				ICellControl2<StockEntryData> cellControl = new ICellControl2<StockEntryData>() {
					private StockEntryData data;

					public Control getControl() {
						return control;
					}

					public void load(StockEntryData data) {
						this.data = data;

						/*
						 * We have to find the appropriate entry in the transaction that contains
						 * the stock.
						 *
						 * - If this is a purchase or sale, then the stock will be set as the commodity
						 * for one of the entries.  We find this entry.
						 * - If this is a dividend payment then the stock will be set as an additional
						 * field in the dividend category.
						 */
						RealProperty realProperty;
						if (data.isPurchaseOrSale()) {
							Entry entry = data.getPurchaseOrSaleEntry();
							realProperty = (RealProperty)entry.getCommodityInternal();
						} else {
							realProperty = null;
							control.setEnabled(false);
						}

				        control.setSession(data.getEntry().getSession(), RealProperty.class);

						control.setSecurity(realProperty);
					}

					public void save() {
						RealProperty realProperty = control.getSecurity();

						if (data.isPurchaseOrSale()) {
							Entry entry = data.getPurchaseOrSaleEntry();
							RealPropertyEntry stockEntry = entry.getExtension(RealPropertyEntryInfo.getPropertySet(), true);
							stockEntry.setSecurity(realProperty);
						}
					}

					public void setSelected() {
						control.setBackground(RowControl.selectedCellColor);
					}

					public void setUnselected() {
						control.setBackground(null);
					}
				};

				FocusListener controlFocusListener = new CellFocusListener<RowControl>(rowControl, cellControl);

				/*
				 * The control may in fact be a composite control, in which case the
				 * composite control itself will never get the focus. Only the child
				 * controls will get the focus, so we add the listener recursively
				 * to all child controls.
				 */
				addFocusListenerRecursively(cellControl.getControl(), controlFocusListener);


				coordinator.addTransactionTypeChangeListener(new ITransactionTypeChangeListener() {

					public void transactionTypeChanged() {
						/*
						 * If the user changes the transaction type, the stock control remains
						 * the same as it was in the previous transaction type.
						 *
						 * For example, suppose an entry is a purchase of stock in Foo company.
						 * The user changes the entry to a dividend.  The entry will then
						 * be a dividend from stock in Foo company.  The user changes the stock
						 * to Bar company.  Then the user changes the transaction type back
						 * to a purchase.  The entry will now show a purchase of stock in Bar
						 * company.
						 */
						RealProperty realProperty = control.getSecurity();
						if (coordinator.getUncommittedEntryData().isPurchaseOrSale()) {
							Entry entry = coordinator.getUncommittedEntryData().getPurchaseOrSaleEntry();
							RealPropertyEntryInfo.getSecurityAccessor().setValue(entry, realProperty);
							control.setEnabled(true);
						} else {
							realProperty = null;
							control.setEnabled(false);
						}
					}
				});

				return cellControl.getControl();
			}

			private void addFocusListenerRecursively(Control control, FocusListener listener) {
				control.addFocusListener(listener);
				if (control instanceof Composite) {
					for (Control child: ((Composite)control).getChildren()) {
						addFocusListenerRecursively(child, listener);
					}
				}
			}
		};

		IndividualBlock<StockEntryData, StockEntryRowControl> priceColumn = new IndividualBlock<StockEntryData, StockEntryRowControl>("Price", 60, 1) {

			@Override
			public Control createCellControl(Composite parent, IObservableValue<? extends StockEntryData> master, RowControl rowControl, final StockEntryRowControl coordinator) {
				final Text control = new Text(parent, SWT.RIGHT);

				ICellControl2<StockEntryData> cellControl = new ICellControl2<StockEntryData>() {

					public Control getControl() {
						return control;
					}

					public void load(StockEntryData data) {
						assert(data.isPurchaseOrSale());
						// TODO:This is a bit funny.  We are passed the data object but
						// the co-ordinator is keeping track of the share price?
						setControlValue(coordinator.getAgreedPrice());

						// Listen for changes in the stock price
						coordinator.addStockPriceChangeListener(new IPropertyChangeListener<BigDecimal>() {
							public void propertyChanged(BigDecimal newValue) {
								setControlValue(newValue);
							}
						});
					}

					private void setControlValue(BigDecimal sharePrice) {
						if (sharePrice != null) {
							long lPrice = sharePrice.movePointRight(4).longValue();
							control.setText(account.getCurrency().format(lPrice));
						} else {
							control.setText("");
						}
					}

					public void save() {
						try {
							long amount = account.getCurrency().parse(control.getText());
						} catch (CoreException e) {
							StatusManager.getManager().handle(e.getStatus());
							return;
						}
					}

					public void setSelected() {
						control.setBackground(RowControl.selectedCellColor);
					}

					public void setUnselected() {
						control.setBackground(null);
					}
				};

				FocusListener controlFocusListener = new CellFocusListener<RowControl>(rowControl, cellControl);
				control.addFocusListener(controlFocusListener);

				return cellControl.getControl();

			}
		};

		IndividualBlock<StockEntryData, StockEntryRowControl> shareNumberColumn = new IndividualBlock<StockEntryData, StockEntryRowControl>("Quantity", EntryInfo.getAmountAccessor().getMinimumWidth(), EntryInfo.getAmountAccessor().getWeight()) {

			@Override
			public Control createCellControl(Composite parent, IObservableValue<? extends StockEntryData> master, RowControl rowControl, final StockEntryRowControl coordinator) {
				final Text control = new Text(parent, SWT.RIGHT);

				ICellControl2<StockEntryData> cellControl = new ICellControl2<StockEntryData>() {

					private StockEntryData data;

					public Control getControl() {
						return control;
					}

					public void load(StockEntryData data) {
						this.data = data;

						IAmountFormatter formatter = getFormatter();

						long quantity = data.getPurchaseOrSaleEntry().getAmount();
						if (data.getTransactionType() == TransactionType.Sell) {
							quantity = -quantity;
						}
						control.setText(formatter.format(quantity));
					}

					private IAmountFormatter getFormatter() {
						IAmountFormatter formatter = data.getPurchaseOrSaleEntry().getCommodityInternal();
						if (formatter == null) {
							/*
							 * The user has not yet selected the stock. As the
							 * way the quantity of a stock is formatted may
							 * potentially depend on the stock, we do not know
							 * exactly how to format and parse the quantity.
							 * However in practice it is unlikely to differ
							 * between different stock in the same account so we
							 * use a default formatter from the account.
							 */
							formatter = account.getQuantityFormatter();
						}
						return formatter;
					}

					public void save() {
						try {
							IAmountFormatter formatter = getFormatter();
							long quantity = formatter.parse(control.getText());
							if (data.getTransactionType() == TransactionType.Sell) {
								quantity = -quantity;
							}

							Entry entry = data.getPurchaseOrSaleEntry();
							entry.setAmount(quantity);
						} catch (CoreException e) {
							StatusManager.getManager().handle(e.getStatus());
							return;
						}
					}

					public void setSelected() {
						control.setBackground(RowControl.selectedCellColor);
					}

					public void setUnselected() {
						control.setBackground(null);
					}
				};

				FocusListener controlFocusListener = new CellFocusListener<RowControl>(rowControl, cellControl);
				control.addFocusListener(controlFocusListener);

				return cellControl.getControl();
			}
		};

		List<Block<? super StockEntryData, ? super StockEntryRowControl>> expenseColumns = new ArrayList<Block<? super StockEntryData, ? super StockEntryRowControl>>();

		final Block<StockEntryData, StockEntryRowControl> purchaseOrSaleInfoColumn = new VerticalBlock<StockEntryData, StockEntryRowControl>(
				// TEMP
				new VerticalBlock<StockEntryData, StockEntryRowControl>(
						priceColumn,
						shareNumberColumn
				),
				new HorizontalBlock<StockEntryData, StockEntryRowControl>(
						expenseColumns
				)
		);

		final Block<EntryData, BaseEntryRowControl> customTransactionColumn = new OtherEntriesBlock(
				new HorizontalBlock<Entry, ISplitEntryContainer>(
						new SingleOtherEntryPropertyBlock(EntryInfo.getAccountAccessor()),
						new SingleOtherEntryPropertyBlock(EntryInfo.getMemoAccessor(), Messages.EntriesSection_EntryDescription),
						new SingleOtherEntryPropertyBlock(EntryInfo.getAmountAccessor())
				)
		);

		CellBlock<EntryData, BaseEntryRowControl> debitColumnManager = DebitAndCreditColumns.createDebitColumn(account.getCurrency());
		CellBlock<EntryData, BaseEntryRowControl> creditColumnManager = DebitAndCreditColumns.createCreditColumn(account.getCurrency());
    	CellBlock<EntryData, BaseEntryRowControl> balanceColumnManager = new BalanceColumn(account.getCurrency());

		RowSelectionTracker<EntryRowControl> rowTracker = new RowSelectionTracker<EntryRowControl>();

		Block<StockEntryData, StockEntryRowControl> rootBlock = new HorizontalBlock<StockEntryData, StockEntryRowControl>(
				transactionDateColumn,
				new VerticalBlock<StockEntryData, StockEntryRowControl>(
						new HorizontalBlock<StockEntryData, StockEntryRowControl>(
								actionColumn,
								shareNameColumn
						),
						PropertyBlock.createEntryColumn(EntryInfo.getMemoAccessor())
				),
				new StackBlock<StockEntryData, StockEntryRowControl>(
						purchaseOrSaleInfoColumn,
						customTransactionColumn
				) {

					@Override
					protected Block<? super StockEntryData, ? super StockEntryRowControl> getTopBlock(StockEntryData data) {
						if (data.getTransactionType() == null) {
							return null;
						} else {
							switch (data.getTransactionType()) {
							case Buy:
							case Sell:
								return purchaseOrSaleInfoColumn;
							case Other:
								return customTransactionColumn;
							default:
								throw new RuntimeException("bad case");
							}
						}
					}

				    @Override
					public Control createCellControl(Composite parent, IObservableValue<? extends StockEntryData> master, final RowControl rowControl, final StockEntryRowControl coordinator) {
						final StackControl<StockEntryData, StockEntryRowControl> control = new StackControl<StockEntryData, StockEntryRowControl>(parent, rowControl, coordinator, this, master);

						coordinator.addTransactionTypeChangeListener(new ITransactionTypeChangeListener() {

							public void transactionTypeChanged() {
								Block<? super StockEntryData, ? super StockEntryRowControl> topBlock = getTopBlock(coordinator.getUncommittedEntryData());

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
								fEntriesControl.table.refreshSize(coordinator);

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
					protected IDataManager getDataManager(StockEntryData data) {
						return data.getEntry().getDataManager();
					}
				},
				debitColumnManager,
				creditColumnManager,
				balanceColumnManager
		);

		IEntriesContent entriesProvider = new IEntriesContent() {

			public Collection<Entry> getEntries() {
				/*
				 * We want only cash entries, not stock entries.  This is providing
				 * content for a table of entries that show the running balance.
				 * A stock entry or an entry in a currency other than the currency
				 * of the account should not be returned.
				 */
				Collection<Entry> entries = new ArrayList<Entry>();
				for (Entry entry : account.getEntries()) {
					if (entry.getCommodityInternal() == asset) {
						entries.add(entry);
					}
				}

				return entries;
			}

			public boolean isEntryInTable(Entry entry) {
				/*
				 * Entry must be in right account AND be an entry that affects or is related
				 * to the given stock.
				 */
				return account == entry.getAccount()
					&& entry.getCommodityInternal() == asset;
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
		};

		// Create the table control.
	    IRowProvider<StockEntryData> rowProvider = new StockRowProvider(rootBlock);
		fEntriesControl = new EntriesTable<StockEntryData>(composite, rootBlock, entriesProvider, rowProvider, account.getSession(), transactionDateColumn, rowTracker) {
			@Override
			protected StockEntryData createEntryRowInput(Entry entry) {
				return new StockEntryData(entry, session.getDataManager());
			}

			@Override
			protected StockEntryData createNewEntryRowInput() {
				return new StockEntryData(null, session.getDataManager());
			}
		};

		fEntriesControl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		// Activate the handlers
		IHandlerService handlerService = (IHandlerService) getSite().getService(IHandlerService.class);

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
	 * if an editor page already exists for an asset.  There may be another
	 * way of doing this, perhaps by setting the asset through an editor input.
	 */
	public RealProperty getAsset() {
		return asset;
	}
}
