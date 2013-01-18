package net.sf.jmoney.currencypage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.Vector;

import net.sf.jmoney.JMoneyPlugin;
import net.sf.jmoney.isolation.IModelObject;
import net.sf.jmoney.isolation.IScalarPropertyAccessor;
import net.sf.jmoney.isolation.ReferenceViolationException;
import net.sf.jmoney.isolation.SessionChangeAdapter;
import net.sf.jmoney.isolation.SessionChangeListener;
import net.sf.jmoney.model2.Account;
import net.sf.jmoney.model2.CapitalAccount;
import net.sf.jmoney.model2.Commodity;
import net.sf.jmoney.model2.Currency;
import net.sf.jmoney.model2.CurrencyAccount;
import net.sf.jmoney.model2.CurrencyInfo;
import net.sf.jmoney.model2.DatastoreManager;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.IncomeExpenseAccount;
import net.sf.jmoney.model2.Session;
import net.sf.jmoney.model2.SessionInfo;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.part.EditorPart;

public class CurrencyEditor extends EditorPart {

	static public final String ID = "net.sf.jmoney.currencypage.editor";
	
	class SelectedContentProvider implements IStructuredContentProvider {
        @Override
		public void dispose() {
        }
        @Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
        }
        /** 
         * Returns the currencies that are available in this accounting datastore.
         */
        @Override
		public Object[] getElements(Object parent) {
            Vector<ISOCurrencyData> currencies = new Vector<ISOCurrencyData>();

            for (Iterator<ISOCurrencyData> iter = allIsoCurrencies.iterator(); iter.hasNext(); ) {
            	ISOCurrencyData isoCurrency = iter.next();
            	if (isoCurrency.currency != null) {
            		currencies.add(isoCurrency);
            	}
    		}
    			
            return currencies.toArray();
        }
    }

	class ISOCurrencyData {
		String name;
		String code;
		int decimals;
		
		/** null if this currency is not selected into the session */
		Currency currency;
		
		@Override
		public String toString() {
			return name + " (" + code + ")";
		}
	}
	
	class AvailableContentProvider implements IStructuredContentProvider {
        @Override
		public void dispose() {
        }
        @Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
        }
        /** 
         * Returns the currencies that are available in this accounting datastore.
         */
        @Override
		public Object[] getElements(Object parent) {
            Vector<ISOCurrencyData> currencies = new Vector<ISOCurrencyData>();

            for (ISOCurrencyData isoCurrency: allIsoCurrencies) {
            	if (isoCurrency.currency == null) {
            		currencies.add(isoCurrency);
            	}
    		}
    			
            return currencies.toArray();
        }
    }

    class CurrencyLabelProvider extends LabelProvider implements ITableLabelProvider {
        protected NumberFormat nf = NumberFormat.getCurrencyInstance();

        @Override
		public String getColumnText(Object obj, int index) {
        	switch (index) {
        	case 0: // name
        		return obj.toString();
        	case 1: // 'used', 'unused', or 'default'
	        	ISOCurrencyData currencyData = (ISOCurrencyData)obj;
	        	Currency currency = currencyData.currency;
        		if (currency.equals(session.getDefaultCurrency())) {
        			return "default";
        		} else {
        			return usedCurrencies.contains(currency) 
					? "in use"
							: "unused";
        		}
        	}
        	
        	return ""; //$NON-NLS-1$
        }

        @Override
		public Image getColumnImage(Object obj, int index) {
            return null;
        }
    }


	
	class CurrencySorter extends ViewerSorter {
		@Override
		public boolean isSorterProperty(Object element, String property) {
			return property.equals("name");
		}
	}
	
	private TableViewer availableListViewer;

	private Label countLabel;

	TableViewer selectedListViewer;

	Session session;

	/**
	 * Set of currencies that are used in some way in the current session.
	 * These currencies cannot be removed from the session.
	 */
	Set<Currency> usedCurrencies = new HashSet<Currency>();
	
	Vector<ISOCurrencyData> allIsoCurrencies = new Vector<ISOCurrencyData>();
	
	private SessionChangeListener listener =
		new SessionChangeAdapter() {
		@Override
		public void objectChanged(IModelObject changedObject, IScalarPropertyAccessor changedProperty, Object oldValue, Object newValue) {
			if (changedObject.equals(session)
					&& changedProperty == SessionInfo.getDefaultCurrencyAccessor()) {
				TableItem[] items = selectedListViewer.getTable().getItems();
				for (int i = 0; i < items.length; i++) {
					ISOCurrencyData currencyData = (ISOCurrencyData)items[i].getData();
					if (currencyData.currency.equals(oldValue)
							|| currencyData.currency.equals(newValue)) {
						selectedListViewer.update(currencyData, null);
					}
				}
			}
		}
		
		@Override
		public void objectInserted(IModelObject newObject) {
			// TODO: currency added
		}

		@Override
		public void objectRemoved(IModelObject deletedObject) {
			// TODO: currency removed
		}

	};
    
	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		
		setSite(site);
		setInput(input);
		
//        // Create our own transaction manager.
//        // This ensures that uncommitted changes
//    	// made by this page are isolated from datastore usage outside
//    	// of this page.
//		DatastoreManager sessionManager = JMoneyPlugin.getDefault().getSessionManager();
//		session = sessionManager.getSession();
//
//        transactionManager = new TransactionManager(sessionManager);
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
		FormToolkit toolkit = new FormToolkit(Display.getCurrent());
		
		/*
		 * Get the session for this editor from the workbench page input.
		 * It should not be possible to open this editor on a page with null
		 * input (no session set) because the command is not enabled if no
		 * session is set. 
		 */
		DatastoreManager manager = (DatastoreManager)getSite().getPage().getInput();
		session = manager.getSession();

		Composite container = toolkit.createComposite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		layout.makeColumnsEqualWidth = false;
		layout.horizontalSpacing = 5;
		layout.verticalSpacing = 10;
		container.setLayout(layout);
				
		// Calculate the list of used currencies.
		findUsedCurrencies(session.getAccountCollection(), usedCurrencies);

		// Build the list of available ISO 4217 currencies.
	    ResourceBundle NAME =
	    	ResourceBundle.getBundle("net.sf.jmoney.resources.Currency");

        InputStream in = JMoneyPlugin.class.getResourceAsStream("Currencies.txt");
		BufferedReader buffer = new BufferedReader(new InputStreamReader(in));
		try {
			String line = buffer.readLine();
			while (line != null) {
				String code = line.substring(0, 3);
				
				int decimals;
				try {
					decimals = Byte.parseByte(line.substring(4, 5));
				} catch (Exception ex) {
					decimals = 2;
				}
				
				ISOCurrencyData isoCurrency = new ISOCurrencyData();
				isoCurrency.name = NAME.getString(code);
				isoCurrency.code = code;
				isoCurrency.decimals = decimals;
				isoCurrency.currency = session.getCurrencyForCode(code);
				allIsoCurrencies.add(isoCurrency);
				
				line = buffer.readLine();
			}
		} catch (IOException e) {
			
		}
		
		
		createAvailableList(toolkit, container).setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		createButtonArea(toolkit, container);
		createSelectedList(toolkit, container).setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		updateCount();
		
		// Listen for changes to the session data.
		session.getDataManager().addChangeListener(listener, parent);
		
		/*
		 * Listen for events on the tables. The user may double click
		 * currencies from the available list to add them or may click
		 * double click on currencies in the selected list (provided the
		 * currency is not in use) to de-selected the currency.
		 */
		availableListViewer.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				handleAdd();
			}
		});
				
		selectedListViewer.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				handleRemove();
			}
		});
		
		Dialog.applyDialogFont(container);
		
		// Set up the context menus.
		hookContextMenu(getSite());
	}
	
	public void saveState(IMemento memento) {
		// This editor has no state to save.
	}
	
	@Override
	public void setFocus() {
		// Don't bother to do anything.  User can select as required.
	}

	/**
	 * Scan through an iteration of accounts and find all the
	 * currencies that are used.  Sub accounts are searched too.
	 * 
	 * @param accountIterator
	 * @param usedCurrencies
	 */
	private void findUsedCurrencies(Collection<? extends Account> accounts, Set<Currency> usedCurrencies) {
		for (Account account: accounts) {
			if (account instanceof IncomeExpenseAccount) {
				IncomeExpenseAccount a = (IncomeExpenseAccount)account;
				if (a.isMultiCurrency()) {
					// TODO: The ability to iterate over entries is currently only
					// available for capital accounts.  We need it for income and
					// expense accounts too.
/*						
					for (Iterator iter2 = a.getEntries.iterator(); iter2.hasNext(); ) {
						Entry entry = (Entry)iter2.next();
						Commodity commodity = entry.getCommodity();
						if (commodity instanceof Currency) {
							usedCurrencies.add(commodity);
						}
					}
*/
				} else {
					usedCurrencies.add(a.getCurrency());
				}
			} else if (account instanceof CurrencyAccount) {
				usedCurrencies.add(((CurrencyAccount)account).getCurrency());
			} else {
				CapitalAccount a = (CapitalAccount)account;
				for (Iterator<Entry> iter2 = a.getEntries().iterator(); iter2.hasNext(); ) {
					Entry entry = iter2.next();
					Commodity commodity = entry.getCommodityInternal();
					if (commodity instanceof Currency) {
						usedCurrencies.add((Currency)commodity);
					}
				}
			}
			
			// Search the sub-accounts
			findUsedCurrencies(account.getSubAccountCollection(), usedCurrencies);
		}
	}

	private Composite createAvailableList(FormToolkit toolkit, Composite parent) {
		Composite container = toolkit.createComposite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		container.setLayout(layout);
		container.setLayoutData(new GridData());

		toolkit.createLabel(container, CurrencyPagePlugin
                .getResourceString("CurrencyPage.isoCurrencyList"), //$NON-NLS-1$ 
                SWT.NONE);

		Table table = toolkit.createTable(container, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.widthHint = 225;
		gd.heightHint = 200;
		table.setLayoutData(gd);

		availableListViewer = new TableViewer(table);
		availableListViewer.setLabelProvider(new LabelProvider());
		availableListViewer.setContentProvider(new AvailableContentProvider());
		availableListViewer.setInput(session);
		availableListViewer.setSorter(new CurrencySorter());

		return container;
	}
	
	
	private Composite createButtonArea(FormToolkit toolkit, Composite parent) {
		Composite comp = toolkit.createComposite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginWidth = layout.marginHeight = 0;
		comp.setLayout(layout);
		comp.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, false, true));
		
		Composite container = toolkit.createComposite(comp, SWT.NONE);
		layout = new GridLayout();
		layout.marginWidth = 0;
		layout.marginHeight = 30;
		container.setLayout(layout);
		container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Button button;

		button = toolkit.createButton(container, CurrencyPagePlugin.getResourceString("CurrencyPage.add"), SWT.PUSH); //$NON-NLS-1$
		button.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		button.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				handleAdd();
			}
		});
		setButtonDimensionHint(button);
		
		button = toolkit.createButton(container, CurrencyPagePlugin.getResourceString("CurrencyPage.addAll"), SWT.PUSH); //$NON-NLS-1$
		button.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		button.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				handleAddAll();
			}
		});
		setButtonDimensionHint(button);
		
		button = toolkit.createButton(container, CurrencyPagePlugin.getResourceString("CurrencyPage.remove"), SWT.PUSH); //$NON-NLS-1$
		button.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		button.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				handleRemove();
			}
		});
		setButtonDimensionHint(button);
		
		button = toolkit.createButton(container, CurrencyPagePlugin.getResourceString("CurrencyPage.removeUnused"), SWT.PUSH); //$NON-NLS-1$
		button.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		button.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				handleRemoveUnused();
			}
		});
		setButtonDimensionHint(button);
		
		// Create some extra space between groups of buttons
		toolkit.createLabel(container, null, SWT.NONE);
		
		button = toolkit.createButton(container, CurrencyPagePlugin.getResourceString("CurrencyPage.setDefault"), SWT.PUSH); //$NON-NLS-1$
		button.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		button.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				handleSetDefault();
			}
		});
		setButtonDimensionHint(button);
		
		countLabel = toolkit.createLabel(comp, null, SWT.NONE);
		countLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_CENTER));		
		return container;
	}
	
	/**
	 * Sets width and height hint for the button control.
	 * <b>Note:</b> This is a NOP if the button's layout data is not
	 * an instance of <code>GridData</code>.
	 * 
	 * @param	the button for which to set the dimension hint
	 */
	public void setButtonDimensionHint(Button button) {
		Dialog.applyDialogFont(button);
		Object gd = button.getLayoutData();
		if (gd instanceof GridData) {
			((GridData) gd).widthHint = getButtonWidthHint(button);
		}
	}

	/**
	 * Returns a width hint for a button control.
	 */
	public int getButtonWidthHint(Button button) {
		if (button.getFont().equals(JFaceResources.getDefaultFont()))
			button.setFont(JFaceResources.getDialogFont());
		PixelConverter converter= new PixelConverter(button);
		int widthHint= converter.convertHorizontalDLUsToPixels(IDialogConstants.BUTTON_WIDTH);
		return Math.max(widthHint, button.computeSize(SWT.DEFAULT, SWT.DEFAULT, true).x);
	}

	public class PixelConverter {
		
		private org.eclipse.swt.graphics.FontMetrics fFontMetrics;
		
		public PixelConverter(Control control) {
			GC gc = new GC(control);
			gc.setFont(control.getFont());
			fFontMetrics= gc.getFontMetrics();
			gc.dispose();
		}
		
			
		/**
		 * @see DialogPage#convertHeightInCharsToPixels
		 */
		public int convertHeightInCharsToPixels(int chars) {
			return Dialog.convertHeightInCharsToPixels(fFontMetrics, chars);
		}

		/**
		 * @see DialogPage#convertHorizontalDLUsToPixels
		 */
		public int convertHorizontalDLUsToPixels(int dlus) {
			return Dialog.convertHorizontalDLUsToPixels(fFontMetrics, dlus);
		}

		/**
		 * @see DialogPage#convertVerticalDLUsToPixels
		 */
		public int convertVerticalDLUsToPixels(int dlus) {
			return Dialog.convertVerticalDLUsToPixels(fFontMetrics, dlus);
		}
		
		/**
		 * @see DialogPage#convertWidthInCharsToPixels
		 */
		public int convertWidthInCharsToPixels(int chars) {
			return Dialog.convertWidthInCharsToPixels(fFontMetrics, chars);
		}	
	}
	
	protected Composite createSelectedList(FormToolkit toolkit, Composite parent) {
		Composite container = toolkit.createComposite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		container.setLayout(layout);
		container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		toolkit.createLabel(container, CurrencyPagePlugin
                .getResourceString("CurrencyPage.selectedList"), SWT.NONE); //$NON-NLS-1$ 

		Table table = toolkit.createTable(container, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.widthHint = 225;
		gd.heightHint = 200;
		table.setLayoutData(gd);

        TableLayout tlayout = new TableLayout();
        
        new TableColumn(table, SWT.LEFT);
        tlayout.addColumnData(new ColumnWeightData(0, 100));

        new TableColumn(table, SWT.LEFT);
        tlayout.addColumnData(new ColumnWeightData(0, 70));

        table.setLayout(tlayout);

        selectedListViewer = new TableViewer(table);
		selectedListViewer.setLabelProvider(new CurrencyLabelProvider());
		selectedListViewer.setContentProvider(new SelectedContentProvider());
		selectedListViewer.setInput(session);
		selectedListViewer.setSorter(new CurrencySorter());
		return container;
	}
	
	protected void pageChanged() {
		updateCount();
	}

	private void updateCount() {
		countLabel.setText(
			CurrencyPagePlugin.getFormattedMessage(
				"CurrencyPage.count", //$NON-NLS-1$
				new String[] {
					new Integer(selectedListViewer.getTable().getItemCount()).toString(),
					new Integer(allIsoCurrencies.size()).toString()}));
		countLabel.getParent().layout();
	}
	
	void handleAdd() {
		IStructuredSelection ssel = (IStructuredSelection)availableListViewer.getSelection();
		if (ssel.size() > 0) {
			Table table = availableListViewer.getTable();
			int index = table.getSelectionIndices()[0];
			for (Object selection: ssel.toList()) {
				ISOCurrencyData currencyData = (ISOCurrencyData)selection;
				Currency newCurrency = session.createCommodity(CurrencyInfo.getPropertySet());
				newCurrency.setName(currencyData.name);
				newCurrency.setCode(currencyData.code);
				newCurrency.setDecimals(currencyData.decimals);
				currencyData.currency = newCurrency;
			}

			selectedListViewer.add(ssel.toArray());
			availableListViewer.remove(ssel.toArray());

			table.setSelection(index < table.getItemCount() ? index : table.getItemCount() -1);
			pageChanged();
		}		
	}

	void handleAddAll() {
		TableItem[] items = availableListViewer.getTable().getItems();

		ArrayList<ISOCurrencyData> data = new ArrayList<ISOCurrencyData>();
		for (TableItem item: items) {
			ISOCurrencyData currencyData = (ISOCurrencyData) item.getData();

			data.add(currencyData);

			Currency newCurrency = session.createCommodity(CurrencyInfo.getPropertySet());
			newCurrency.setName(currencyData.name);
			newCurrency.setCode(currencyData.code);
			newCurrency.setDecimals(currencyData.decimals);
			currencyData.currency = newCurrency;
		}
		
		if (data.size() > 0) {
			selectedListViewer.add(data.toArray());
			availableListViewer.remove(data.toArray());
			pageChanged();
		}
	}
	
	void handleRemove() {
		IStructuredSelection ssel = (IStructuredSelection)selectedListViewer.getSelection();
		if (ssel.size() > 0) {
			Table table = selectedListViewer.getTable();
			int index = table.getSelectionIndices()[0];
			
			Object [] isoCurrencies = ssel.toArray();
			ArrayList<ISOCurrencyData> data = new ArrayList<ISOCurrencyData>();
			for (int i = 0; i < ssel.size(); i++) {
				ISOCurrencyData currencyData = (ISOCurrencyData)isoCurrencies[i];
				if (!usedCurrencies.contains(currencyData.currency)
						&& !currencyData.currency.equals(session.getDefaultCurrency())) {
					data.add(currencyData);
					try {
						session.deleteCommodity(currencyData.currency);
					} catch (ReferenceViolationException e) {
						/*
						 * Shouldn't happen because we have checked for use of
						 * this currency. This exception could potentially
						 * happen if third-party plug-ins have extended the
						 * model, in which case we probably will need to think
						 * about how we can be more user-friendly.
						 */
						throw new RuntimeException("This is an unlikely error and should not happen unless plug-ins are doing something complicated.", e);
					}
					currencyData.currency = null;
				} else {
					MessageDialog dialog = new MessageDialog(
							this.getSite().getShell(),
							"Disallowed Action",
							null, // accept the default window icon
							"You cannot remove the default currency or any currencies that are in use.",
							MessageDialog.ERROR,
							new String[] { IDialogConstants.OK_LABEL }, 0);
					dialog.open();
				}
			}
			
			if (data.size() > 0) {
				availableListViewer.add(data.toArray());
				selectedListViewer.remove(data.toArray());
				table.setSelection(index < table.getItemCount() ? index : table.getItemCount() -1);
				pageChanged();
			}		
		}		
	}
	
	void handleRemoveUnused() {
		TableItem[] items = selectedListViewer.getTable().getItems();
		
		ArrayList<ISOCurrencyData> data = new ArrayList<ISOCurrencyData>();
		for (int i = 0; i < items.length; i++) {
			ISOCurrencyData currencyData = (ISOCurrencyData)items[i].getData();
			if (!usedCurrencies.contains(currencyData.currency)
					&& !currencyData.currency.equals(session.getDefaultCurrency())) {
				data.add(currencyData);
				try {
					session.deleteCommodity(currencyData.currency);
				} catch (ReferenceViolationException e) {
					/*
					 * Shouldn't happen because we have checked for use of
					 * this currency. This exception could potentially
					 * happen if third-party plug-ins have extended the
					 * model, in which case we probably will need to think
					 * about how we can be more user-friendly.
					 */
					throw new RuntimeException("This is an unlikely error and should not happen unless plug-ins are doing something complicated.", e);
				}
				currencyData.currency = null;
			}
		}
		if (data.size() > 0) {
			availableListViewer.add(data.toArray());
			selectedListViewer.remove(data.toArray());
			pageChanged();
		}		
	}
	
	void handleSetDefault() {
		// If more than one currency is selected, set the
		// first currency in the selection as the default.
		IStructuredSelection ssel = (IStructuredSelection)selectedListViewer.getSelection();
		if (ssel.size() > 0) {
			Table table = selectedListViewer.getTable();
			TableItem items [] = table.getSelection();
			Currency newDefaultCurrency = ((ISOCurrencyData)items[0].getData()).currency;
			session.setDefaultCurrency(newDefaultCurrency);
			
			pageChanged();
		}		
	}
	
	private void hookContextMenu(IWorkbenchPartSite site) {
		MenuManager menuMgr = new MenuManager("#PopupMenu");
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			@Override
			public void menuAboutToShow(IMenuManager manager) {
				fillContextMenu(manager);
			}
		});
		Menu menu = menuMgr.createContextMenu(selectedListViewer.getControl());
		selectedListViewer.getControl().setMenu(menu);
		
		site.registerContextMenu(menuMgr, selectedListViewer);
	}
	
	protected void fillContextMenu(IMenuManager manager) {
		// Other plug-ins can contribute their actions here
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
	}
}
