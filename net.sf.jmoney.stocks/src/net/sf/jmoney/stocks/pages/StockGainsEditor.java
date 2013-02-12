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

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

import net.sf.jmoney.fields.DateControl;
import net.sf.jmoney.isolation.IListPropertyAccessor;
import net.sf.jmoney.isolation.IModelObject;
import net.sf.jmoney.isolation.IScalarPropertyAccessor;
import net.sf.jmoney.isolation.SessionChangeAdapter;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.EntryInfo;
import net.sf.jmoney.model2.IDataManagerForAccounts;
import net.sf.jmoney.model2.IDatastoreManager;
import net.sf.jmoney.stocks.gains.CapitalGainsCalculator;
import net.sf.jmoney.stocks.gains.StockPurchaseAndSale;
import net.sf.jmoney.stocks.model.Stock;
import net.sf.jmoney.stocks.model.StockAccount;
import net.sf.jmoney.views.AccountEditor;
import net.sf.jmoney.views.AccountEditorInput;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
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
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
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

/**
 * This page shows the capital gains and losses for the account.
 */
public class StockGainsEditor extends EditorPart {
	static public final String ID = "net.sf.jmoney.stocks.stockGainsEditor";

	static private DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");

	private AccountEditor accountEditor;

	/**
	 * The account being shown in this page.
	 */
	private StockAccount account;

	private TableViewer gainsViewer;

	private DateControl startDateControl;

	private DateControl endDateControl;

	private SessionChangeAdapter sessionListener = new SessionChangeAdapter() {
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
	public StockGainsEditor(AccountEditor accountEditor) {
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

		startDateControl.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				Date balanceDate = startDateControl.getDate();
				if (balanceDate != null) {
					calculateTotals();
				}
			}
		});

		endDateControl.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				Date balanceDate = endDateControl.getDate();
				if (balanceDate != null) {
					calculateTotals();
				}
			}
		});

        startDateControl.setDate(new Date());
        endDateControl.setDate(new Date());

		calculateTotals();

		// Activate the handlers
//		IHandler handler = new NewTransactionHandler(rowTracker, fEntriesControl);
//		handlerService.activateHandler("net.sf.jmoney.newTransaction", handler);

        toolkit.paintBordersFor(contents);

        form.setText("Investment Account Gains and Losses");
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
		Date startDate = startDateControl.getDate();
		Date endDate = endDateControl.getDate();

		Collection<StockPurchaseAndSale> matchedPurchasesAndSales = new ArrayList<StockPurchaseAndSale>();

				IStatus status;
				try {
					status = CapitalGainsCalculator.exportCapitalGains(account, startDate, endDate, matchedPurchasesAndSales);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					throw new RuntimeException(e);
				}
				if (!status.isOK()) {
					throw new RuntimeException(new CoreException(status));
				}

		gainsViewer.setInput(matchedPurchasesAndSales);
	}

	private Control createConfigurationControls(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(2, false));

		createStartDateControl(composite);
		createEndDateControl(composite);

		return composite;
	}

	private Control createStartDateControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(2, false));

		new Label(composite, SWT.NONE).setText("Start Date:");

		startDateControl = new DateControl(composite);

		return composite;
	}

	private Control createEndDateControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(2, false));

		new Label(composite, SWT.NONE).setText("End Date:");

		endDateControl = new DateControl(composite);

		return composite;
	}

	private Control createTable(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout());

		gainsViewer = new TableViewer(composite, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL | SWT.FULL_SELECTION);

		GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
		gridData.widthHint = 300;
		gridData.heightHint = 100;
		gainsViewer.getTable().setLayoutData(gridData);

		gainsViewer.getTable().setHeaderVisible(true);
		gainsViewer.getTable().setLinesVisible(true);

		gainsViewer.setContentProvider(ArrayContentProvider.getInstance());

		// Sort by sell date
		gainsViewer.setComparator(new ViewerComparator() {
			@Override
		    public int compare(Viewer viewer, Object element1, Object element2) {
				StockPurchaseAndSale stockWrapper1 = (StockPurchaseAndSale)element1;
				StockPurchaseAndSale stockWrapper2 = (StockPurchaseAndSale)element2;

				if (stockWrapper1.getSellDate().before(stockWrapper2.getBuyDate())) {
					return -1;
				} else if (stockWrapper1.getSellDate().after(stockWrapper2.getBuyDate())) {
					return 1;
				} else {
					// Both sold on same date so sort on stock name
					return stockWrapper1.getStock().getName().compareTo(stockWrapper2.getStock().getName());
				}
			}
		});

		TableViewerColumn stockNameColumn = new TableViewerColumn(gainsViewer, SWT.LEFT);
		stockNameColumn.getColumn().setText("Stock");
		stockNameColumn.getColumn().setWidth(200);

		stockNameColumn.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				StockPurchaseAndSale stockWrapper = (StockPurchaseAndSale)cell.getElement();
				cell.setText(stockWrapper.getStock().getName());
			}
		});

		TableViewerColumn buyDateColumn = new TableViewerColumn(gainsViewer, SWT.LEFT);
		buyDateColumn.getColumn().setText("Buy Date");
		buyDateColumn.getColumn().setWidth(100);

		buyDateColumn.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				StockPurchaseAndSale stockWrapper = (StockPurchaseAndSale)cell.getElement();
				cell.setText(dateFormat.format(stockWrapper.getBuyDate()));
			}
		});

		TableViewerColumn sellDateColumn = new TableViewerColumn(gainsViewer, SWT.LEFT);
		sellDateColumn.getColumn().setText("Sell Date");
		sellDateColumn.getColumn().setWidth(100);

		sellDateColumn.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				StockPurchaseAndSale stockWrapper = (StockPurchaseAndSale)cell.getElement();
				cell.setText(dateFormat.format(stockWrapper.getSellDate()));
			}
		});

		TableViewerColumn quantityColumn = new TableViewerColumn(gainsViewer, SWT.LEFT);
		quantityColumn.getColumn().setText("Number of Shares");
		quantityColumn.getColumn().setWidth(100);

		quantityColumn.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				StockPurchaseAndSale stockWrapper = (StockPurchaseAndSale)cell.getElement();
				cell.setText(stockWrapper.getStock().format(stockWrapper.getQuantity()));
			}
		});

		TableViewerColumn basisColumn = new TableViewerColumn(gainsViewer, SWT.LEFT);
		basisColumn.getColumn().setText("Basis");
		basisColumn.getColumn().setWidth(100);

		basisColumn.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				StockPurchaseAndSale stockWrapper = (StockPurchaseAndSale)cell.getElement();
				cell.setText(account.getCurrency().format(stockWrapper.getBasis()));
			}
		});

		TableViewerColumn proceedsColumn = new TableViewerColumn(gainsViewer, SWT.LEFT);
		proceedsColumn.getColumn().setText("Proceeds");
		proceedsColumn.getColumn().setWidth(100);

		proceedsColumn.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				StockPurchaseAndSale stockWrapper = (StockPurchaseAndSale)cell.getElement();
				cell.setText(account.getCurrency().format(stockWrapper.getProceeds()));
			}
		});

		TableViewerColumn gainColumn = new TableViewerColumn(gainsViewer, SWT.LEFT);
		gainColumn.getColumn().setText("Gain/Loss");
		gainColumn.getColumn().setWidth(100);

		gainColumn.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				StockPurchaseAndSale stockWrapper = (StockPurchaseAndSale)cell.getElement();
				cell.setText(account.getCurrency().format(stockWrapper.getProceeds() - stockWrapper.getBasis()));
			}
		});

		// Create the pop-up menu
		MenuManager menuMgr = new MenuManager();
		// TODO We are making assumptions about where this editor is placed when
		// we make the following cast to AccountEditor.  Can this be cleaned up?
		menuMgr.add(new ShowDetailsAction(gainsViewer));
		menuMgr.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
		getSite().registerContextMenu(menuMgr, gainsViewer);

		Control control = gainsViewer.getControl();
		Menu menu = menuMgr.createContextMenu(control);
		control.setMenu(menu);

		return composite;
	}

	@Override
	public void setFocus() {
		// Don't bother to do anything.  User can select as required.
	}

	public class ShowDetailsAction extends Action {

		private ISelectionProvider selectionProvider;

		public ShowDetailsAction(ISelectionProvider selectionProvider) {
			super("Show Stock Gain/Loss Details");
			this.selectionProvider = selectionProvider;
		}

		@Override
		public void run() {
			IStructuredSelection selection = (IStructuredSelection)selectionProvider.getSelection();
			Object element = selection.getFirstElement();
			if (element instanceof StockPurchaseAndSale) {
				Stock stock = ((StockPurchaseAndSale)element).getStock();
				// TODO
			}
		}
	}
}
