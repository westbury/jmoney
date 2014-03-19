package net.sf.jmoney.shoebox;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import net.sf.jmoney.ITransactionTemplate;
import net.sf.jmoney.entrytable.BaseEntryRowControl;
import net.sf.jmoney.entrytable.Block;
import net.sf.jmoney.entrytable.CellBlock;
import net.sf.jmoney.entrytable.DebitAndCreditColumns;
import net.sf.jmoney.entrytable.DelegateBlock;
import net.sf.jmoney.entrytable.EntriesTable;
import net.sf.jmoney.entrytable.EntryData;
import net.sf.jmoney.entrytable.EntryFacade;
import net.sf.jmoney.entrytable.EntryRowControl;
import net.sf.jmoney.entrytable.HorizontalBlock;
import net.sf.jmoney.entrytable.IEntriesContent;
import net.sf.jmoney.entrytable.IRowProvider;
import net.sf.jmoney.entrytable.IndividualBlock;
import net.sf.jmoney.entrytable.OtherEntriesBlock;
import net.sf.jmoney.entrytable.PropertyBlock;
import net.sf.jmoney.entrytable.ReusableRowProvider;
import net.sf.jmoney.entrytable.RowSelectionTracker;
import net.sf.jmoney.entrytable.SingleOtherEntryDetailPropertyBlock;
import net.sf.jmoney.entrytable.VerticalBlock;
import net.sf.jmoney.isolation.IObjectKey;
import net.sf.jmoney.isolation.UncommittedObjectKey;
import net.sf.jmoney.model2.Currency;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.EntryInfo;
import net.sf.jmoney.model2.IDataManagerForAccounts;
import net.sf.jmoney.model2.Session;
import net.sf.jmoney.model2.Transaction;
import net.sf.jmoney.model2.TransactionInfo;
import net.sf.jmoney.shoebox.resources.Messages;

import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;

public class ShoeboxEditor extends EditorPart {

	static public final String ID = "net.sf.jmoney.shoebox.editor";
	
	private Session session;
	
    private EntriesTable<EntryRowControl> recentlyAddedEntriesControl;
    private IEntriesContent recentEntriesTableContents = null;
    
	Collection<IObjectKey> ourEntryList = new Vector<IObjectKey>();
	
	public Map<String, ITransactionTemplate> transactionTypes = new HashMap<String, ITransactionTemplate>();

	/**
	 * The transaction manager used for all changes made by
	 * this page.  It is created by the page is created and
	 * remains usable for the rest of the time that this page
	 * exists.
	 */
//	private TransactionManager transactionManager = null;

    private Block<EntryRowControl> rootBlock;
    
	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		
		setSite(site);
		setInput(input);
		
        // Create our own transaction manager.
        // This ensures that uncommitted changes
    	// made by this page are isolated from datastore usage outside
    	// of this page.
		IDataManagerForAccounts sessionManager = (IDataManagerForAccounts)getSite().getPage().getInput();
		session = sessionManager.getSession();

//        transactionManager = new TransactionManager(sessionManager);
	}

	@Override
	public boolean isDirty() {
		// Editor is never dirty
		return false;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		// Will never be called because editor is never dirty.
		System.out.println("here");
	}

	@Override
	public void doSaveAs() {
		// Will never be called because editor is never dirty and 'save as' is not allowed anyway.
	}

	@SuppressWarnings("unchecked")
	@Override
	public void createPartControl(Composite parent) {
		
        recentEntriesTableContents = new IEntriesContent() {

			public Collection<Entry> getEntries() {
				Collection<Entry> committedEntries = new Vector<Entry>();
				for (IObjectKey objectKey: ourEntryList) {
					Entry committedEntry = (Entry)((UncommittedObjectKey)objectKey).getCommittedObjectKey().getObject();
					committedEntries.add(committedEntry);
				}
				return committedEntries;
			}

			public boolean isEntryInTable(Entry entry) {
				/*
				 * This entry is to be shown if the entry was entered using
				 * this editor. We keep a list of entries that were entered
				 * through this editor.
				 */
				for (IObjectKey objectKey: ourEntryList) {
					IObjectKey committedKey = ((UncommittedObjectKey)objectKey).getCommittedObjectKey();
					if (committedKey.equals(entry.getObjectKey())) {
						return true;
					}
				}
				return false;
			}

			public boolean filterEntry(EntryData data) {
				// No filter here, so entries always match
				return true;
			}

			public long getStartBalance() {
				// No balance in this table
				return 0;
			}

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
				/*
				 * There are no properties we must set when an entry is
				 * added to this table.
				 */
			}
        };

		//Create an outer composite for spacing
        ScrolledComposite scrolled = new ScrolledComposite(parent, SWT.V_SCROLL | SWT.H_SCROLL);

		scrolled.setExpandHorizontal(true);
		scrolled.setExpandVertical(true);
		
		GridData scrolledData = new GridData(SWT.FILL, SWT.FILL, true, true);
				
		scrolled.setLayoutData(scrolledData);
		
		Composite topLevelControl = new Composite(scrolled, SWT.NONE);

		GridData resultData = new GridData(SWT.FILL, SWT.FILL, true, true);
				
		topLevelControl.setLayoutData(resultData);
		
		scrolled.setContent(topLevelControl);
        
		topLevelControl.setLayout(new GridLayout(1, false));
		parent.setBackground(this.getSite().getShell().getDisplay().getSystemColor(SWT.COLOR_RED));
		topLevelControl.setBackground(getSite().getShell().getDisplay().getSystemColor(SWT.COLOR_CYAN));
		
		/*
		 * Setup the layout structure of the header and rows.
		 */
		IndividualBlock<IObservableValue<? extends EntryFacade>> transactionDateColumn = PropertyBlock.createTransactionColumn(TransactionInfo.getDateAccessor());

		Block<IObservableValue<EntryFacade>> part1SubBlock = new HorizontalBlock<IObservableValue<EntryFacade>>(
				transactionDateColumn,
				new VerticalBlock<IObservableValue<EntryFacade>>(
						new HorizontalBlock<IObservableValue<EntryFacade>>(
								PropertyBlock.createEntryColumn(EntryInfo.getAccountAccessor()),
								PropertyBlock.createEntryColumn(EntryInfo.getCheckAccessor())
						),
						PropertyBlock.createEntryColumn(EntryInfo.getMemoAccessor())
				),
				new OtherEntriesBlock<EntryFacade>(
						new HorizontalBlock<IObservableValue<Entry>>(
								new SingleOtherEntryDetailPropertyBlock(EntryInfo.getAccountAccessor()),
								new SingleOtherEntryDetailPropertyBlock(EntryInfo.getMemoAccessor(), net.sf.jmoney.resources.Messages.EntriesSection_EntryDescription),
								new SingleOtherEntryDetailPropertyBlock(EntryInfo.getAmountAccessor())
						)
				)
			);

		Block<EntryRowControl> part1Block = new DelegateBlock<EntryRowControl, IObservableValue<EntryFacade>>(part1SubBlock) {
			@Override
			protected IObservableValue<EntryFacade> convert(
					EntryRowControl blockInput) {
				return blockInput.observeEntryFacade();
			}
		};

    	Block<EntryRowControl> debitAndCreditColumnsManager = new DelegateBlock<EntryRowControl, IObservableValue<Entry>>(
    			DebitAndCreditColumns.createDebitAndCreditColumns(session.getDefaultCurrency())
			) {
			@Override
			protected IObservableValue<Entry> convert(EntryRowControl blockInput) {
				return blockInput.observeMainEntry();
			}
		};

		rootBlock = new HorizontalBlock<EntryRowControl>(
				part1Block,
				debitAndCreditColumnsManager
		);

        // Create the table control.
	    IRowProvider<EntryData, EntryRowControl> rowProvider = new ReusableRowProvider(rootBlock);
        recentlyAddedEntriesControl = new EntriesTable<EntryRowControl>(topLevelControl, rootBlock, recentEntriesTableContents, rowProvider, this.session, transactionDateColumn, new RowSelectionTracker()) {
			@Override
			protected EntryData createEntryRowInput(Entry entry) {
				return new EntryData(entry, session.getDataManager());
			}

			@Override
			protected EntryData createNewEntryRowInput() {
				return new EntryData(null, session.getDataManager());
			}
        }; 
		
		recentlyAddedEntriesControl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		// The tab control
		GridData gdTabControl = new GridData(SWT.FILL, SWT.FILL, true, false);
		createTabbedArea(topLevelControl).setLayoutData(gdTabControl);

		Point s = topLevelControl.computeSize(SWT.DEFAULT, SWT.DEFAULT);
		scrolled.setMinSize(s);
	}
	
	private Control createTabbedArea(Composite parent) {
		TabFolder tabFolder = new TabFolder(parent, SWT.NONE);

		
		// Load the extensions
		IExtensionRegistry registry = Platform.getExtensionRegistry();
		for (IConfigurationElement element: registry.getConfigurationElementsFor("net.sf.jmoney.shoebox.templates")) {
			if (element.getName().equals("template")) {

//				String label = element.getAttribute("label");
				String id = element.getAttribute("id");
//				String position = element.getAttribute("position");
				String fullId = element.getNamespaceIdentifier() + "." + id;

				try {
					ITransactionTemplate transactionType = (ITransactionTemplate)element.createExecutableExtension("class");

					TabItem tabItem = new TabItem(tabFolder, SWT.NULL);
					tabItem.setText(transactionType.getDescription());
					tabItem.setControl(transactionType.createControl(tabFolder, session, true, null, ourEntryList));

//					int positionNumber = 800;
//					if (position != null) {
//						positionNumber = Integer.parseInt(position);
//					}

					transactionTypes.put(fullId, transactionType);

				} catch (CoreException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
		final IDialogSettings section = getSettings();
		
		initState(section);
		
		tabFolder.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				saveState(section);
			}
		});
		
        return tabFolder;
	}

	private IDialogSettings getSettings() {
		IDialogSettings workbenchSettings = ShoeboxPlugin.getDefault().getDialogSettings();
		IDialogSettings section = workbenchSettings.getSection("ShoeboxEditor");//$NON-NLS-1$
		if (section == null) {
			section = workbenchSettings.addNewSection("ShoeboxEditor");//$NON-NLS-1$
		}
		return section;
	}

	private void initState(IDialogSettings section) {
		for (String id: transactionTypes.keySet()) {
			ITransactionTemplate transactionType = transactionTypes.get(id);
			transactionType.init(section.getSection(id));
		}
	}

	private void saveState(IDialogSettings section) {
		for (String id: transactionTypes.keySet()) {
			ITransactionTemplate transactionType = transactionTypes.get(id);
			transactionType.saveState(section.addNewSection(id));
		}
	}
	
	@Override
	public void setFocus() {
		// Don't bother to do anything.  User can select as required.
	}
}
