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

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import net.sf.jmoney.JMoneyPlugin;
import net.sf.jmoney.fields.DateControl;
import net.sf.jmoney.isolation.IListPropertyAccessor;
import net.sf.jmoney.isolation.IModelObject;
import net.sf.jmoney.isolation.IScalarPropertyAccessor;
import net.sf.jmoney.isolation.SessionChangeAdapter;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.EntryInfo;
import net.sf.jmoney.model2.IDataManagerForAccounts;
import net.sf.jmoney.model2.IDatastoreManager;
import net.sf.jmoney.stocks.ShowStockDetailsHandler;
import net.sf.jmoney.stocks.model.Security;
import net.sf.jmoney.stocks.model.Stock;
import net.sf.jmoney.stocks.model.StockAccount;
import net.sf.jmoney.views.AccountEditor;
import net.sf.jmoney.views.AccountEditorInput;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.part.EditorPart;

public class StockBalancesEditor extends EditorPart {
	static public final String ID = "net.sf.jmoney.stocks.stockBalancesEditor";

	private final AccountEditor accountEditor;

	/**
	 * The account being shown in this page.
	 */
	private StockAccount account;

	private class StockWrapper {
		private final Security stock;
		public long total = 0;

		public StockWrapper(Security stock) {
			this.stock = stock;
		}

	}

	private Map<Security, StockWrapper> totals;

	private TableViewer balancesViewer;

	private Button zeroBalanceCheckbox;

	private DateControl balanceDateControl;

	private final SessionChangeAdapter sessionListener = new SessionChangeAdapter() {
		@Override
		public void objectChanged(IModelObject changedObject,
				IScalarPropertyAccessor changedProperty, Object oldValue,
				Object newValue) {
			if (changedObject instanceof Entry) {
				Entry changedEntry = (Entry)changedObject;
				if (changedEntry.getAccount() == account
						&& changedEntry.getCommodityInternal() instanceof Stock
						&& changedProperty == EntryInfo.getAmountAccessor()) {

				}
			}

		}

		@Override
		public void objectCreated(IModelObject newObject) {
			// TODO Auto-generated method stub

		}

		@Override
		public void objectDestroyed(IModelObject deletedObject) {
			// TODO Auto-generated method stub

		}

		@Override
		public void objectInserted(IModelObject newObject) {
			// TODO Auto-generated method stub
			System.out.println("here");
		}

		@Override
		public void objectMoved(IModelObject movedObject,
				IModelObject originalParent, IModelObject newParent,
				IListPropertyAccessor originalParentListProperty,
				IListPropertyAccessor newParentListProperty) {
			// TODO Auto-generated method stub

		}

		@Override
		public void objectRemoved(IModelObject deletedObject) {
			// TODO Auto-generated method stub

		}

		@Override
		public void performRefresh() {
			// TODO Auto-generated method stub
			calculateTotals();
		}
	};

	/**
	 * Currently this editor can be created only when given an AccountEditor.
	 * This editor is a child editor of the AccountEditor (a multi-page editor),
	 * though this class does not make that assumption.  The AccountEditor is used
	 * for certain actions such as opening stock details pages.
	 *
	 * @param editor
	 */
	public StockBalancesEditor(AccountEditor accountEditor) {
		this.accountEditor = accountEditor;
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

        sessionManager.addChangeListener(sessionListener);
	}

	@Override
	public void dispose() {
        IDatastoreManager sessionManager = (IDatastoreManager)getSite().getPage().getInput();
        sessionManager.removeChangeListener(sessionListener);
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
        form.getBody().setLayout(new FillLayout());

		// Get the handler service and pass it on so that handlers can be activated as appropriate
		IHandlerService handlerService = (IHandlerService) getSite().getService(IHandlerService.class);

        Composite contents = createContents(form.getBody());

        final ViewerFilter hideZeroBalancesFilter = new ViewerFilter() {
			@Override
			public boolean select(Viewer viewer, Object parentElement, Object element) {
				StockWrapper wrapper = (StockWrapper)element;
				return wrapper.total != 0;
			}
		};

		balanceDateControl.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				Date balanceDate = balanceDateControl.getDate();
				if (balanceDate != null) {
					calculateTotals();
				}
			}
		});

        zeroBalanceCheckbox.addSelectionListener(new SelectionAdapter() {
        	@Override
			public void widgetSelected(SelectionEvent e) {
        		if (zeroBalanceCheckbox.getSelection()) {
        			balancesViewer.removeFilter(hideZeroBalancesFilter);
        		} else {
        			balancesViewer.addFilter(hideZeroBalancesFilter);
        		}
			}
		});

        balanceDateControl.setDate(new Date());
		calculateTotals();

		// Activate the handlers
//		IHandler handler = new NewTransactionHandler(rowTracker, fEntriesControl);
//		handlerService.activateHandler("net.sf.jmoney.newTransaction", handler);

        toolkit.paintBordersFor(contents);

        form.setText("Investment Account Balances");
	}

	private Composite createContents(Composite parent) {
		Composite composite = new Composite(parent, SWT.None);
		composite.setLayout(new GridLayout());

		createConfigurationControls(composite);

		Control table = createTable(composite);
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		return composite;
	}

	private void calculateTotals() {
		Date asOf = balanceDateControl.getDate();

		totals = new HashMap<Security, StockWrapper>();

		for (Entry entry : account.getEntries()) {
			if (!entry.getTransaction().getDate().after(asOf)
					 && entry.getCommodity() instanceof Security) {
				Security security = (Security)entry.getCommodity();
				StockWrapper securityWrapper = totals.get(security);
				if (securityWrapper == null) {
					securityWrapper = new StockWrapper(security);
					totals.put(security, securityWrapper);
				}

				securityWrapper.total += entry.getAmount();
			}
		}

		balancesViewer.setInput(totals.values());
	}

	private Control createConfigurationControls(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(5, false));

		createDateControl(composite);
		createShowZeroBalanceCheckbox(composite);

		return composite;
	}

	private Control createDateControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(5, false));

		new Label(composite, SWT.NONE).setText("Balance as of:");

		balanceDateControl = new DateControl(composite);

		return composite;
	}

	private Control createShowZeroBalanceCheckbox(Composite parent) {
		zeroBalanceCheckbox = new Button(parent, SWT.CHECK);
		zeroBalanceCheckbox.setText("Show Stock with Zero Balance");

		return zeroBalanceCheckbox;
	}

	private Control createTable(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout());

		balancesViewer = new TableViewer(composite, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL | SWT.FULL_SELECTION);

		GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
		gridData.widthHint = 300;
		gridData.heightHint = 100;
		balancesViewer.getTable().setLayoutData(gridData);

		balancesViewer.getTable().setHeaderVisible(true);
		balancesViewer.getTable().setLinesVisible(true);

		balancesViewer.setContentProvider(ArrayContentProvider.getInstance());

		// Sort by stock name
		balancesViewer.setComparator(new ViewerComparator() {
			@Override
		    public int compare(Viewer viewer, Object element1, Object element2) {
				StockWrapper stockWrapper1 = (StockWrapper)element1;
				StockWrapper stockWrapper2 = (StockWrapper)element2;
				return stockWrapper1.stock.getName().compareTo(stockWrapper2.stock.getName());
			}
		});

		TableViewerColumn stockNameColumn = new TableViewerColumn(balancesViewer, SWT.LEFT);
		stockNameColumn.getColumn().setText("Stock");
		stockNameColumn.getColumn().setWidth(300);

		stockNameColumn.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				StockWrapper stockWrapper = (StockWrapper)cell.getElement();
				cell.setText(stockWrapper.stock.getName());
			}
		});

		TableViewerColumn balanceColumn = new TableViewerColumn(balancesViewer, SWT.LEFT);
		balanceColumn.getColumn().setText("Number of Shares");
		balanceColumn.getColumn().setWidth(100);

		balanceColumn.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				StockWrapper stockWrapper = (StockWrapper)cell.getElement();
				cell.setText(stockWrapper.stock.format(stockWrapper.total));
			}
		});

		// Create the pop-up menu
		MenuManager menuMgr = new MenuManager();
		// TODO We are making assumptions about where this editor is placed when
		// we make the following cast to AccountEditor.  Can this be cleaned up?
		menuMgr.add(new ShowDetailsAction(balancesViewer));
		menuMgr.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
		getSite().registerContextMenu(menuMgr, balancesViewer);

		Control control = balancesViewer.getControl();
		Menu menu = menuMgr.createContextMenu(control);
		control.setMenu(menu);

		return composite;
	}

	@Override
	public void setFocus() {
		// Don't bother to do anything.  User can select as required.
	}

	public class ShowDetailsAction extends Action {

		private final ISelectionProvider selectionProvider;

		public ShowDetailsAction(ISelectionProvider selectionProvider) {
			super("Show Stock Activity");
			this.selectionProvider = selectionProvider;
		}

		@Override
		public void run() {
			IStructuredSelection selection = (IStructuredSelection)selectionProvider.getSelection();
			Object element = selection.getFirstElement();
			if (element instanceof StockWrapper) {
				Security stock = ((StockWrapper)element).stock;
				try {
					ShowStockDetailsHandler.showStockDetails(accountEditor, stock);
				} catch (PartInitException e) {
					JMoneyPlugin.log(e);
				}
			}
		}
	}
}
