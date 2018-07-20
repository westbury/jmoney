package net.sf.jmoney.search.views;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.IHandler;
import org.eclipse.core.databinding.observable.value.AbstractObservableValue;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.IValueChangeListener;
import org.eclipse.core.databinding.observable.value.ValueChangeEvent;
import org.eclipse.core.databinding.observable.value.ValueDiff;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.part.ViewPart;

import net.sf.jmoney.entrytable.Block;
import net.sf.jmoney.entrytable.DebitAndCreditColumns;
import net.sf.jmoney.entrytable.DelegateBlock;
import net.sf.jmoney.entrytable.DeleteTransactionHandler;
import net.sf.jmoney.entrytable.EntryData;
import net.sf.jmoney.entrytable.EntryFacade;
import net.sf.jmoney.entrytable.EntryRowControl;
import net.sf.jmoney.entrytable.FocusCellTracker;
import net.sf.jmoney.entrytable.Header;
import net.sf.jmoney.entrytable.HorizontalBlock;
import net.sf.jmoney.entrytable.ICompositeTable;
import net.sf.jmoney.entrytable.IndividualBlock;
import net.sf.jmoney.entrytable.OpenTransactionDialogHandler;
import net.sf.jmoney.entrytable.OtherEntriesBlock;
import net.sf.jmoney.entrytable.PropertyBlock;
import net.sf.jmoney.entrytable.RowSelectionTracker;
import net.sf.jmoney.entrytable.SingleOtherEntryDetailPropertyBlock;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.EntryInfo;
import net.sf.jmoney.model2.IDataManagerForAccounts;
import net.sf.jmoney.model2.Session;
import net.sf.jmoney.model2.TransactionInfo;
import net.sf.jmoney.resources.Messages;
import net.sf.jmoney.search.IEntrySearch;

public class SearchView extends ViewPart {
	public static String ID = "net.sf.jmoney.search.EntrySearchView";

	private Composite tableComposite;

	private RowSelectionTracker<EntryRowControl> rowTracker;

	private FocusCellTracker cellTracker;

	private List<Entry> entries = new ArrayList<Entry>();

	private Block<EntryRowControl> rootBlock;

	private ScrolledComposite sc;

	private SearchAgainAction fSearchAgainAction;
	private SearchHistoryDropDownAction fSearchHistoryDropDownAction;

	private List<IEntrySearch> searchHistory = new ArrayList<IEntrySearch>();

	private StackLayout stackLayout;

	private Composite parent;

	private Label noSearchLabel;

	public SearchView() {
		fSearchAgainAction= new SearchAgainAction(this);
		fSearchAgainAction.setEnabled(false);

		fSearchHistoryDropDownAction= new SearchHistoryDropDownAction(this);
		fSearchHistoryDropDownAction.setEnabled(false);
	}

	private void contributeToActionBars() {
		IActionBars bars = getViewSite().getActionBars();
		fillLocalPullDown(bars.getMenuManager());
		fillLocalToolBar(bars.getToolBarManager());
	}

	private void fillLocalPullDown(IMenuManager manager) {
		// No items are currently in the pull down menu.
	}

	private void fillLocalToolBar(IToolBarManager manager) {
		manager.add(fSearchAgainAction);
		manager.add(fSearchHistoryDropDownAction);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void createPartControl(Composite parent) {
		IDataManagerForAccounts manager = (IDataManagerForAccounts)getSite().getPage().getInput();
		if (manager == null) {
			// TODO put up some message, or can we stop the user from opening this
			// view when there is no session open?
		} else {
			Session session = manager.getSession();

			/*
			 * Setup the layout structure of the header and rows.
			 */
			IndividualBlock<IObservableValue<? extends EntryFacade>> transactionDateColumn = PropertyBlock.createTransactionColumn(TransactionInfo.getDateAccessor());

			// TODO: Formatting according to the default currency is not necessarily correct
	    	Block<IObservableValue<EntryFacade>> debitAndCreditColumnsManager = new DelegateBlock<IObservableValue<EntryFacade>, IObservableValue<Entry>>(
	    			DebitAndCreditColumns.createDebitAndCreditColumns(session.getDefaultCurrency())
				) {
				@Override
				protected IObservableValue<Entry> convert(final IObservableValue<EntryFacade> blockInput) {

					return new AbstractObservableValue<Entry>() {

						private IValueChangeListener<EntryFacade> delegateListener = new IValueChangeListener<EntryFacade>() {

							@Override
							public void handleValueChange(
									final ValueChangeEvent<? extends EntryFacade> event) {
								fireValueChange(new ValueDiff<Entry>() {
									@Override
									public Entry getOldValue() {
										return event.diff.getOldValue().getMainEntry();
									}

									@Override
									public Entry getNewValue() {
										return event.diff.getNewValue().getMainEntry();
									}
								});
							}
						};

						@Override
						public Object getValueType() {
							return Entry.class;
						}

						@Override
						protected Entry doGetValue() {
							return blockInput.getValue() == null ? null : blockInput.getValue().getMainEntry();
						}
						
						@Override
						public synchronized void addValueChangeListener(
								IValueChangeListener<? super Entry> listener) {
							if (!hasListeners()) {
								blockInput.addValueChangeListener(delegateListener);
							}
							super.addValueChangeListener(listener);
						}

						@Override
						public synchronized void removeValueChangeListener(
								IValueChangeListener<? super Entry> listener) {
							super.removeValueChangeListener(listener);
							if (!hasListeners()) {
								blockInput.removeValueChangeListener(delegateListener);
							}
						}
					};
				}
			};

			rootBlock = new DelegateBlock<EntryRowControl, IObservableValue<EntryFacade>>(new HorizontalBlock<IObservableValue<EntryFacade>>(
					transactionDateColumn,
					PropertyBlock.createEntryColumn(EntryInfo.getAccountAccessor()),
					PropertyBlock.createEntryColumn(EntryInfo.getMemoAccessor()),
					new OtherEntriesBlock(
							new HorizontalBlock<IObservableValue<Entry>>(
									new SingleOtherEntryDetailPropertyBlock(EntryInfo.getAccountAccessor()),
									new SingleOtherEntryDetailPropertyBlock(EntryInfo.getMemoAccessor(), Messages.EntriesSection_EntryDescription),
									new SingleOtherEntryDetailPropertyBlock(EntryInfo.getAmountAccessor())
							)
					),
					debitAndCreditColumnsManager
			)) {
				@Override
				protected IObservableValue<EntryFacade> convert(
						EntryRowControl blockInput) {
					return blockInput.observeEntryFacade();
				}
			}; 
					
			/*
			 * Ensure indexes are set.
			 */
			rootBlock.initIndexes(0);

			stackLayout = new StackLayout();
			parent.setLayout(stackLayout);
			this.parent = parent;

			noSearchLabel = new Label(parent, SWT.WRAP);
			noSearchLabel.setText("No search results available. Start a search from the search dialog...");

			stackLayout.topControl = noSearchLabel;

			sc = new ScrolledComposite(parent, SWT.V_SCROLL | SWT.H_SCROLL);

			rowTracker = new RowSelectionTracker<EntryRowControl>();
			cellTracker = new FocusCellTracker();
			tableComposite = new Composite(sc, SWT.NONE);
			GridLayout layout = new GridLayout(1, false);
			layout.verticalSpacing = 0;
			layout.marginWidth = 0;
			layout.marginHeight = 0;
			tableComposite.setLayout(layout);

			createTableControls(rootBlock);

			sc.setContent(tableComposite);	

			sc.setExpandHorizontal(true);
			sc.setExpandVertical(true);
			sc.setMinSize(tableComposite.computeSize(SWT.DEFAULT, SWT.DEFAULT));

			GridData resultData = new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL
					| GridData.GRAB_VERTICAL);
			tableComposite.setLayoutData(resultData);

			contributeToActionBars();

			// Activate the handlers
			IHandlerService handlerService = getSite().getService(IHandlerService.class);

			IHandler handler = new DeleteTransactionHandler(rowTracker);
			handlerService.activateHandler("net.sf.jmoney.deleteTransaction", handler);		

			//		handler = new DuplicateTransactionHandler(rowTracker, entriesControl);
			//		handlerService.activateHandler("net.sf.jmoney.duplicateTransaction", handler);		

			handler = new OpenTransactionDialogHandler(rowTracker);
			handlerService.activateHandler("net.sf.jmoney.transactionDetails", handler);		
		}
	}

	private void createTableControls(Block<EntryRowControl> rootBlock) {
		new Header<EntryRowControl>(tableComposite, SWT.NONE, rootBlock).setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

		ICompositeTable<EntryData, EntryRowControl> tableWrapper = new ICompositeTable<EntryData, EntryRowControl>() {

			@Override
			public void rowDeselected(EntryRowControl rowControl) {
				// TODO Auto-generated method stub

			}

			@Override
			public void scrollToShowRow(EntryRowControl rowControl) {
				// TODO Auto-generated method stub

			}

			@Override
			public void setCurrentRow(EntryData input,
					EntryRowControl entryRowControl) {
				// TODO Auto-generated method stub

			}
		};

		for (Entry entry: entries) {
			EntryData entryData = new EntryData(entry, entry.getDataManager());
			EntryRowControl row = new EntryRowControl(tableComposite, tableWrapper, rootBlock, rowTracker, cellTracker);
			row.setRowInput(entryData);
			row.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		}
	}

	@Override
	public void setFocus() {
		// TODO Auto-generated method stub

	}

	private void setEntries(List<Entry> entries) {
		this.entries = entries;
		if (tableComposite != null) {

			/*
			 * there may be a selected row in the previous search that has
			 * unsaved changes. the following call will clear the selection only
			 * if there are no unsaved changes in it. if there are unsaved
			 * changes, a message will be shown to the user and we should simply
			 * leave the old search.
			 * 
			 * it would be better if this check were done before the user
			 * entered the new search, but never mind.
			 */
			if (rowTracker.setSelection(null, null)) {
				for (Control child : tableComposite.getChildren()) {
					child.dispose();
				}
				createTableControls(rootBlock);
				sc.setMinSize(tableComposite.computeSize(SWT.DEFAULT, SWT.DEFAULT));
				tableComposite.layout(true);  // Seems to be needed - following line does not seem to layout the table for us.  Probably because of new child controls.
				sc.layout(true);
			}
		}
	}

	public void showSearch(IEntrySearch search) {
		/*
		 * Add to the end of the past search list, removing
		 * it first from the list if it is already somewhere
		 * in the list.
		 */
		searchHistory.remove(search);
		searchHistory.add(0, search);

		setEntries(search.getEntries());

		fSearchAgainAction.setEnabled(true);
		fSearchHistoryDropDownAction.setEnabled(true);

		this.setContentDescription(search.getTooltip());

		stackLayout.topControl = sc;
		parent.layout(false);
	}

	public boolean hasQueries() {
		return !searchHistory.isEmpty();
	}

	public List<IEntrySearch> getQueries() {
		return searchHistory;
	}

	public IEntrySearch getCurrentSearch() {
		return searchHistory.get(0);
	}

	public void removeAllSearches() {
		searchHistory.clear();

		/*
		 * Despite what the javadoc says, passing the empty string seems
		 * to cause the content description to be removed from the view.
		 */
		setContentDescription("");

		// TODO: Is this correct?
		setEntries(new ArrayList<Entry>());

		stackLayout.topControl = noSearchLabel;
		parent.layout(false);

		fSearchAgainAction.setEnabled(false);
		fSearchHistoryDropDownAction.setEnabled(false);
	}

	public void removeSearches(List<IEntrySearch> removedEntries) {
		searchHistory.removeAll(removedEntries);		
	}

}
