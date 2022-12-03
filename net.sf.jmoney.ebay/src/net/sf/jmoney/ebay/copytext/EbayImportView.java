/*
 *
 *  JMoney - A Personal Finance Manager
 *  Copyright (c) 2010 Nigel Westbury <westbury@users.sourceforge.net>
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

package net.sf.jmoney.ebay.copytext;

import java.awt.Toolkit;
import java.awt.datatransfer.FlavorEvent;
import java.awt.datatransfer.FlavorListener;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.IValueChangeListener;
import org.eclipse.core.databinding.observable.value.ValueChangeEvent;
import org.eclipse.core.databinding.observable.value.WritableValue;
import org.eclipse.core.internal.databinding.provisional.bind.Bind;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.databinding.viewers.IViewerObservableValue;
import org.eclipse.jface.databinding.viewers.ViewersObservables;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.ImageTransfer;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Sash;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import analyzer.EbayOrder;
import analyzer.EbayOrderItem;
import analyzer.UnsupportedImportDataException;
import ebayscraper.EbayScraperContext;
import ebayscraper.IContextUpdater;
import net.sf.jmoney.ebay.AccountFinder;
import net.sf.jmoney.ebay.EbayEntry;
import net.sf.jmoney.ebay.EbayEntryInfo;
import net.sf.jmoney.ebay.EbayTransactionInfo;
import net.sf.jmoney.ebay.UrlBlob;
import net.sf.jmoney.fields.AccountControl;
import net.sf.jmoney.fields.DateControl;
import net.sf.jmoney.fields.IAmountFormatter;
import net.sf.jmoney.fields.IBlob;
import net.sf.jmoney.importer.Activator;
import net.sf.jmoney.importer.wizards.ImportException;
import net.sf.jmoney.importer.wizards.TxrMismatchException;
import net.sf.jmoney.model2.BankAccount;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.EntryInfo;
import net.sf.jmoney.model2.ExtendableObject;
import net.sf.jmoney.model2.IDatastoreManager;
import net.sf.jmoney.model2.ScalarPropertyAccessor;
import net.sf.jmoney.model2.Session;
import net.sf.jmoney.model2.Transaction;
import net.sf.jmoney.model2.TransactionManagerForAccounts;

public class EbayImportView extends ViewPart {

	public EbayScraperContext scraperContext;
	
	protected static Pattern urlToImagePattern;
	static {
		urlToImagePattern = Pattern.compile("https://i.ebayimg.com/images/g/((\\d|\\w|-|~)+)/s-l\\d\\d\\d\\d?\\.(jpg|webp)");
	}

	protected static Pattern urlToProductPattern;
	static {
		urlToProductPattern = Pattern.compile("https://www.ebay.com/itm/(\\d+)(\\?.+)?");
	}

	protected static Pattern urlFromItemPageToImageCodePattern;
	static {
		// Sometimes 1500, sometimes 1300
		// https://i.ebayimg.com/images/g/LGQAAOSw7kdgRpaG/s-l300.jpg
		urlFromItemPageToImageCodePattern = Pattern.compile("https://i.ebayimg.com/images/g/((\\d|\\w|-|~)+)/s-l?1?\\d00\\.jpg");
	}
	
	public class PasteOrdersAction extends Action {
		public PasteOrdersAction() {
			super("Paste Orders");
			setToolTipText("Paste from Ebay Orders Page");
			setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_TOOL_PASTE));
		}

		@Override
		public void run() {
			try {
				pasteOrders();
			} catch (Exception e) {
				e.printStackTrace();
				MessageDialog.openError(getViewSite().getShell(), "Paste Failed", e.getMessage());
			}
		}
	}

	public class PasteDetailsAction extends Action {
		public PasteDetailsAction() {
			super("Paste Details");
			setToolTipText("Paste from Ebay Details Page");
			setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_TOOL_PASTE));
		}

		@Override
		public void run() {
			try {
				pasteDetails();
			} catch (Exception e) {
				e.printStackTrace();
				MessageDialog.openError(getViewSite().getShell(), "Paste Failed", e.getMessage());
			}
		}
	}

	public static String ID = "net.sf.jmoney.ebay.EbayImportView";

	private TreeViewer viewer;

	private IViewerObservableValue<Object> selObs;

	private PasteOrdersAction pasteOrdersAction;

	private PasteDetailsAction pasteDetailsAction;

	private IDatastoreManager committedSessionManager;
	
	private TransactionManagerForAccounts uncommittedSessionManager;

	// Session, inside transaction
	private Session session;

	private IAmountFormatter currencyFormatter;
	
	private Image errorImage;

	/** outside transaction */
	private IObservableValue<BankAccount> defaultChargeAccount = new WritableValue<>();

	public EbayImportView() {
		pasteOrdersAction = new PasteOrdersAction();
		pasteDetailsAction = new PasteDetailsAction();

		// Load the error indicator
		URL installURL = Activator.getDefault().getBundle().getEntry("/icons/error.gif");
		errorImage = ImageDescriptor.createFromURL(installURL).createImage();
	}

	@Override
	public void init(IViewSite viewSite, IMemento memento) throws PartInitException {
		super.init(viewSite, memento);

		//		if (memento != null) {
		//			filter.init(memento.getChild("filter"));
		//		}

	}

	@Override
	public void saveState(IMemento memento) {	
		super.saveState(memento);
		//		filter.saveState(memento.createChild("filter"));
	}

	private void contributeToActionBars() {
		IActionBars bars = getViewSite().getActionBars();
		fillLocalPullDown(bars.getMenuManager());
		fillLocalToolBar(bars.getToolBarManager());
	}

	private void fillLocalPullDown(IMenuManager manager) {
		// No items are currently in the pull down menu.
		Toolkit.getDefaultToolkit().getSystemClipboard().addFlavorListener(new FlavorListener() { 
			@Override 
			public void flavorsChanged(FlavorEvent e) {

				System.out.println("ClipBoard UPDATED: " + e.getSource() + " " + e.toString());
			} 
		}); ;
	}

	private void fillLocalToolBar(IToolBarManager manager) {
		manager.add(pasteOrdersAction);
		manager.add(pasteDetailsAction);
	}

	@Override
	public void createPartControl(Composite parent) {
		Composite stackComposite = new Composite(parent, SWT.NONE);

		StackLayout stackLayout = new StackLayout();
		stackComposite.setLayout(stackLayout);

		Label noSessionLabel = new Label(stackComposite, SWT.WRAP);
		noSessionLabel.setText("No accounting session is open.");

		Control dataComposite = createMainArea(stackComposite);

		final IWorkbenchPage activePage = getSite().getWorkbenchWindow().getActivePage();
		committedSessionManager = (IDatastoreManager)activePage.getInput();
		if (committedSessionManager == null) {
			stackLayout.topControl = noSessionLabel;
		} else {
			stackLayout.topControl = dataComposite;

			uncommittedSessionManager = new TransactionManagerForAccounts(committedSessionManager);
			session = uncommittedSessionManager.getSession();
		
			currencyFormatter = session.getCurrencyForCode("GBP");
			
			BankAccount defaultDefaultChargeAccount = (BankAccount)committedSessionManager.getSession().getAccountByFullName("Aqua (8795)");
			defaultChargeAccount.setValue(defaultDefaultChargeAccount);
			
			IContextUpdater contextUpdater = new ContextUpdater(committedSessionManager, uncommittedSessionManager, defaultChargeAccount);
			scraperContext = new EbayScraperContext(contextUpdater);
		}
		activePage.addPartListener(new IPartListener2() {

			@Override
			public void partActivated(IWorkbenchPartReference partRef) {
				// TODO Auto-generated method stub

			}

			@Override
			public void partBroughtToTop(IWorkbenchPartReference partRef) {
				// TODO Auto-generated method stub

			}

			@Override
			public void partClosed(IWorkbenchPartReference partRef) {
				// TODO Auto-generated method stub

			}

			@Override
			public void partDeactivated(IWorkbenchPartReference partRef) {
				// TODO Auto-generated method stub

			}

			@Override
			public void partOpened(IWorkbenchPartReference partRef) {
				// TODO Auto-generated method stub

			}

			@Override
			public void partHidden(IWorkbenchPartReference partRef) {
				// TODO Auto-generated method stub

			}

			@Override
			public void partVisible(IWorkbenchPartReference partRef) {
				// TODO Auto-generated method stub

			}

			@Override
			public void partInputChanged(IWorkbenchPartReference partRef) {
				// TODO deal with uncommitted changes to any previous session???

				IDatastoreManager committedSessionManager = (IDatastoreManager)activePage.getInput();
				if (committedSessionManager == null) {
					stackLayout.topControl = noSessionLabel;

					uncommittedSessionManager = null;
					session = null;
					scraperContext = null;
				} else {
					stackLayout.topControl = dataComposite;

					uncommittedSessionManager = new TransactionManagerForAccounts(committedSessionManager);
					session = uncommittedSessionManager.getSession();
					
					BankAccount defaultDefaultChargeAccount = (BankAccount)committedSessionManager.getSession().getAccountByFullName("Aqua (8795)");
					defaultChargeAccount.setValue(defaultDefaultChargeAccount);
					
					IContextUpdater contextUpdater = new ContextUpdater(committedSessionManager, uncommittedSessionManager, defaultChargeAccount);
					scraperContext = new EbayScraperContext(contextUpdater);
				}
				stackComposite.layout();
			}
		});

		contributeToActionBars();
	}

	private Control createMainArea(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(1, false));

		createDefaultAccountsArea(composite);
		createVerticallySplitArea(composite);
		createButtonComposite(composite);

		return composite;
	}

	private Control createButtonComposite(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new RowLayout(SWT.HORIZONTAL));

		Button commitButton = new Button(composite, SWT.PUSH);
		commitButton.setText("Commit");
		commitButton.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				commitChanges();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				commitChanges();
			}

			private void commitChanges() {
				try {
					validateTransactions();
				} catch (RuntimeException e) {
					MessageDialog.openError(getViewSite().getShell(), "Cannot Commit", e.getLocalizedMessage());
					return;
				}
				uncommittedSessionManager.commit("Ebay Import from Clipboard");

				IDatastoreManager committedSessionManager = (IDatastoreManager)getSite().getWorkbenchWindow().getActivePage().getInput();
				uncommittedSessionManager = new TransactionManagerForAccounts(committedSessionManager);
				session = uncommittedSessionManager.getSession();
				
				IContextUpdater contextUpdater = new ContextUpdater(committedSessionManager, uncommittedSessionManager, defaultChargeAccount);
				scraperContext = new EbayScraperContext(contextUpdater);

				viewer.setInput(scraperContext.orders.toArray(new EbayOrder[0]));
			}

			private void validateTransactions() {
				for (EbayOrder order : scraperContext.orders) {
					checkOrderValid(order);
				}
			}
		});

		Button abortButton = new Button(composite, SWT.PUSH);
		abortButton.setText("Abort");
		abortButton.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				abortChanges();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				abortChanges();
			}

			private void abortChanges() {
				IDatastoreManager committedSessionManager = (IDatastoreManager)getSite().getWorkbenchWindow().getActivePage().getInput();
				uncommittedSessionManager = new TransactionManagerForAccounts(committedSessionManager);
				session = uncommittedSessionManager.getSession();
				
				IContextUpdater contextUpdater = new ContextUpdater(committedSessionManager, uncommittedSessionManager, defaultChargeAccount);
				scraperContext = new EbayScraperContext(contextUpdater);

				viewer.setInput(scraperContext.orders.toArray(new EbayOrder[0]));
			}
		});

		return composite;
	}

	private void checkOrderValid(EbayOrder order) {
		long totalForOrder = 0;
		for (EbayOrderItem item : order.getItems()) {
			totalForOrder += item.getNetCost();
		}
		totalForOrder += order.getPostageAndPackaging();
		totalForOrder -= order.getDiscount();
		
		if (totalForOrder != order.getOrderTotal()) {
			throw new RuntimeException(MessageFormat.format("Order shipments for {2} add up to {0} but the order total is given as {1}.  These are expected to match so information from the details page is needed to resolve this.", 
					currencyFormatter.format(totalForOrder), 
					currencyFormatter.format(order.getOrderTotal()), 
					order.getOrderNumber()));
		}
	}

	private void pasteOrders() throws ImportException {
		try {
			String text = getTextFromClipboard();
			scraperContext.importOrders(text);
	
			viewer.setInput(scraperContext.orders.toArray(new EbayOrder[0]));
		} catch (TxrMismatchException e) {
			IWorkbenchWindow window = this.getViewSite().getWorkbenchWindow();
			e.showInDebugView(window);
		}
	}

	private void pasteDetails() throws ImportException {
		try {
			String text = getTextFromClipboard();
			scraperContext.importDetails(text);
			viewer.setInput(scraperContext.orders.toArray(new EbayOrder[0]));
		} catch (UnsupportedImportDataException e) {
			throw new ImportException("Import of details failed.", e);
		} catch (TxrMismatchException e) {
			IWorkbenchWindow window = this.getViewSite().getWorkbenchWindow();
			e.showInDebugView(window);
		}
	}

	private String getTextFromClipboard() {
		Display display = Display.getCurrent();
		Clipboard clipboard = new Clipboard(display);
		String plainText = (String)clipboard.getContents(TextTransfer.getInstance());
		clipboard.dispose();        

		return plainText;
	}

	private Control createDefaultAccountsArea(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(2, false));
 
		AccountControl<BankAccount> chargeAccountControl = new AccountControl<BankAccount>(composite, BankAccount.class) {
			@Override
			protected Session getSession() {
				return committedSessionManager.getSession();
			}
		};
		chargeAccountControl.setLayoutData(new GridData(200, SWT.DEFAULT));

		Bind.twoWay(chargeAccountControl.account).to(defaultChargeAccount);
		
		return composite;
	}
	
	private Control createVerticallySplitArea(Composite parent) {
		Composite containerOfSash = new Composite(parent, SWT.NONE);
		containerOfSash.setLayout(new FormLayout());

		// Create the sash first, so the other controls
		// can be attached to it.
		final Sash sash = new Sash(containerOfSash, SWT.BORDER | SWT.HORIZONTAL);
		FormData formData = new FormData();
		formData.left = new FormAttachment(0, 0); // Attach to left
		formData.right = new FormAttachment(100, 0); // Attach to right
		formData.top = new FormAttachment(50, 0); // Attach halfway down
		sash.setLayoutData(formData);

		sash.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				final int mimimumHeight = 61;  // In Windows, allows 3 lines minimum.  TODO: Calculate this for other OS's
				int y = event.y;
				if (y < mimimumHeight) {
					y = mimimumHeight;
				}
				if (y + sash.getSize().y > sash.getParent().getSize().y - mimimumHeight) {
					y = sash.getParent().getSize().y - mimimumHeight - sash.getSize().y;
				}

				// We re-attach to the top edge, and we use the y value of the event to
				// determine the offset from the top
				((FormData) sash.getLayoutData()).top = new FormAttachment(0, y);

				// Until the parent window does a layout, the sash will not be redrawn in
				// its new location.
				sash.getParent().layout();
			}
		});

		GridData gridData1 = new GridData(SWT.FILL, SWT.FILL, true, true);
		gridData1.heightHint = 200;   // TODO: tidy up???
		gridData1.widthHint = 200;   // TODO: tidy up???
		containerOfSash.setLayoutData(gridData1);

		Control fStatementSection = createTreeControl(containerOfSash);

		formData = new FormData();
		formData.top = new FormAttachment(0, 0);
		formData.bottom = new FormAttachment(sash, 0);
		formData.left = new FormAttachment(0, 0);
		formData.right = new FormAttachment(100, 0);
		fStatementSection.setLayoutData(formData);

		Control fUnreconciledSection = createFieldArea(containerOfSash);

		formData = new FormData();
		formData.top = new FormAttachment(sash, 0);
		formData.bottom = new FormAttachment(100, 0);
		formData.left = new FormAttachment(0, 0);
		formData.right = new FormAttachment(100, 0);
		fUnreconciledSection.setLayoutData(formData);

		return containerOfSash;
	}	


	
	
	private Control createTreeControl(Composite parent) {
		viewer = new TreeViewer(parent, SWT.H_SCROLL
				| SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.HIDE_SELECTION);
		viewer.setContentProvider(new EbayOrderContentProvider());
		viewer.setAutoExpandLevel(3);
		
		selObs = ViewersObservables.observeSingleSelection(viewer);

		Tree tree = viewer.getTree();

		tree.setHeaderVisible(true);
		tree.setLinesVisible(true);

		ColumnViewerToolTipSupport.enableFor(viewer); 
		
		TreeViewerColumn nameColumn = new TreeViewerColumn(viewer, SWT.LEFT);
		nameColumn.getColumn().setText("Order/Item");
		nameColumn.getColumn().setWidth(300);

		  nameColumn.setLabelProvider(new ColumnLabelProvider() {

				@Override 
				public String getToolTipText(Object element) {
					if (element instanceof EbayOrder) {
						EbayOrder order = (EbayOrder)element;
					try {	
						checkOrderValid(order);
						return null;
					} catch (Exception e) {
						return e.getLocalizedMessage();
					}
					} else {
						return null;
					}
				}

				@Override
				public Image getImage(Object element) {
					if (element instanceof EbayOrder) {
						EbayOrder order = (EbayOrder)element;
						try {	
							checkOrderValid(order);
							return null;
						} catch (Exception e) {
							return errorImage;
						}
					} else {
						return null;
					}
				}

				@Override
				public String getText(Object element) {
					if (element instanceof EbayOrder) {
						EbayOrder order = (EbayOrder)element;
						return order.getOrderNumber();
					} else if (element instanceof EbayOrderItem) {
						EbayOrderItem item = (EbayOrderItem)element;
						return item.getEbayDescription();
					} else {
						return null;
					}
				}
		  }); 

		TreeViewerColumn itemNumberColumn = new TreeViewerColumn(viewer, SWT.LEFT);
		itemNumberColumn.getColumn().setText("Item Number");
		itemNumberColumn.getColumn().setWidth(200);

		itemNumberColumn.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				if (cell.getElement() instanceof EbayOrderItem) {
					EbayOrderItem item = (EbayOrderItem)cell.getElement();
					String text = item.getUnderlyingItem().getItemNumber();
					cell.setText(text);
				}
			}
		});

		TreeViewerColumn imageCodeColumn = new TreeViewerColumn(viewer, SWT.LEFT);
		imageCodeColumn.getColumn().setText("Image Code");
		imageCodeColumn.getColumn().setWidth(200);

		imageCodeColumn.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				if (cell.getElement() instanceof EbayOrderItem) {
					EbayOrderItem item = (EbayOrderItem)cell.getElement();
					String text = item.getUnderlyingItem().getImageCode();
					cell.setText(text);
				}
			}
		});
		
		viewer.setSorter(new ViewerSorter() {
		    @Override
			public int compare(Viewer viewer, Object e1, Object e2) {
		    	if (e1 instanceof EbayOrder && e2 instanceof EbayOrder) {
		    		EbayOrder order1 = (EbayOrder)e1;
		    		EbayOrder order2 = (EbayOrder)e2;
		    		// Reverse, so most recent is first
		    		return -order1.getOrderDate().compareTo(order2.getOrderDate());
		    	}
		    	return super.compare(viewer, e1, e2);
		    }

		});
		
		// Create the pop-up menu
		MenuManager menuMgr = new MenuManager();
		menuMgr.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
		getSite().registerContextMenu(menuMgr, viewer);

		Control control = viewer.getControl();
		Menu menu = menuMgr.createContextMenu(control);
		control.setMenu(menu);

		return control;
	}

	private Control createFieldArea(Composite parent) {
		Composite stackComposite = new Composite(parent, SWT.NONE);
		StackLayout stackLayout = new StackLayout();
		stackComposite.setLayout(stackLayout);

		IObservableValue<Entry> ebayEntry = new WritableValue<Entry>();
		IObservableValue<Transaction> ebayTransaction = new WritableValue<Transaction>();

		Control orderControls = createOrderControls(stackComposite, ebayTransaction);
		Control itemControls = createItemControls(stackComposite, ebayEntry);

		selObs.addValueChangeListener(new IValueChangeListener<Object>() {
			@Override
			public void handleValueChange(ValueChangeEvent<? extends Object> event) {
//				if (event.diff.getOldValue() instanceof EbayOrderItem) {
//					EbayOrderItem item = (EbayOrderItem)selObs.getValue();
//					item.getUnderlyingItem().
//				}
				
				if (selObs.getValue() instanceof EbayOrder) {
					EbayOrder order = (EbayOrder)selObs.getValue();
					stackLayout.topControl= orderControls;
					
					OrderUpdater order2 = (OrderUpdater)order.getUnderlyingOrder();
					ebayTransaction.setValue(order2.getTransaction().getBaseObject());

				} else if (selObs.getValue() instanceof EbayOrderItem) {
					EbayOrderItem item = (EbayOrderItem)selObs.getValue();
					ItemUpdater item2 = (ItemUpdater)item.getUnderlyingItem();
					
					stackLayout.topControl = itemControls;

					ebayEntry.setValue(item2.getEntry().getBaseObject());
					
//					IObservableValue<String> o = EbayEntryInfo.getImageCodeAccessor().observeDetail(ebayEntry);
//					IValueChangeListener<String> imageCodeListener = new IValueChangeListener<String>() {
//						@Override
//						public void handleValueChange(ValueChangeEvent<? extends String> event) {
//							EbayEntry entry = ebayEntry.getValue().getExtension(EbayEntryInfo.getPropertySet(), true);
//							String imageCode = event.diff.getNewValue();
//							if (!imageCode.equals(entry.getImageCode()) {
//								entry.
//							}
//							System.out.println("update image - TODO");
//						}
//					};
//					// This is probably not needed????
//					o.addValueChangeListener(imageCodeListener);
//					parent.addDisposeListener(new DisposeListener() {
//						@Override
//						public void widgetDisposed(DisposeEvent e) {
//							o.removeValueChangeListener(imageCodeListener);
//						}
//					});
					

				} else {
					stackLayout.topControl = null;
				}

				stackComposite.layout();
			}
		});

		return stackComposite;
	}

	private Control createOrderControls(Composite parent, IObservableValue<Transaction> ebayTransaction) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(2, false));

		Label orderDateLabel = new Label(composite, 0);
		orderDateLabel.setText("Order Date:");
		DateControl orderDateControl = new DateControl(composite);
		orderDateControl.setLayoutData(new GridData(200, SWT.DEFAULT));

		Label orderNumberLabel = new Label(composite, 0);
		orderNumberLabel.setText("Order Number:");
		Text orderControl = new Text(composite, SWT.NONE);
		orderControl.setLayoutData(new GridData(200, SWT.DEFAULT));

		Label sellerLabel = new Label(composite, 0);
		sellerLabel.setText("Sold By:");
		Text sellerControl = new Text(composite, SWT.NONE);
		sellerControl.setLayoutData(new GridData(200, SWT.DEFAULT));

		Label orderAmountLabel = new Label(composite, 0);
		orderAmountLabel.setText("Order Total:");
		Text orderAmountControl = new Text(composite, SWT.TRAIL);
		orderAmountControl.setLayoutData(new GridData(80, SWT.DEFAULT));

		Label paymentDateLabel = new Label(composite, 0);
		paymentDateLabel.setText("Payment Date:");
		DateControl paymentDateControl = new DateControl(composite);
		paymentDateControl.setLayoutData(new GridData(200, SWT.DEFAULT));

		Label shippingDateLabel = new Label(composite, 0);
		shippingDateLabel.setText("Ship Date:");
		DateControl shippingDateControl = new DateControl(composite);
		shippingDateControl.setLayoutData(new GridData(200, SWT.DEFAULT));

//		createLabelAndControl(composite, EbayTransactionInfo.getShippingCostAccessor(), ebayTransaction);
//		createLabelAndControl(composite, EbayTransactionInfo.getDiscountAccessor(), ebayTransaction);

		Label postageAndPackagingLabel = new Label(composite, 0);
		postageAndPackagingLabel.setText("Shipping:");
		Text postageAndPackagingControl = new Text(composite, SWT.TRAIL);
		postageAndPackagingControl.setLayoutData(new GridData(80, SWT.DEFAULT));

		Label discountLabel = new Label(composite, 0);
		discountLabel.setText("Discount:");
		Text discountControl = new Text(composite, SWT.TRAIL);
		discountControl.setLayoutData(new GridData(80, SWT.DEFAULT));

		selObs.addValueChangeListener(new IValueChangeListener<Object>() {
			@Override
			public void handleValueChange(ValueChangeEvent<? extends Object> event) {
				if (selObs.getValue() instanceof EbayOrder) {
					EbayOrder order = (EbayOrder)selObs.getValue();

					orderDateControl.setDate(order.getOrderDate());
					orderControl.setText(order.getOrderNumber());
					sellerControl.setText(order.getSeller() == null ? "" : order.getSeller()); // Why would seller be null?
					orderAmountControl.setText(currencyFormatter.format(order.getOrderTotal()));

					paymentDateControl.setDate(order.getPaidDate());
					shippingDateControl.setDate(order.getShippingDate());

					if (order.getPostageAndPackaging() != 0) {
						postageAndPackagingControl.setText(currencyFormatter.format(order.getPostageAndPackaging()));
					} else {
						postageAndPackagingControl.setText("");
					}

					if (order.getDiscount() != 0) {
						discountControl.setText(currencyFormatter.format(order.getDiscount()));
					} else {
						discountControl.setText("");
					}
				}
			}
		});

		postageAndPackagingControl.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent event) {
				Object selection = selObs.getValue();
				if (!(selection instanceof EbayOrder)) {
					throw new RuntimeException("Control should not be visible");
				}
				EbayOrder order = (EbayOrder)selection;
				String shippingAsString = postageAndPackagingControl.getText();
				if (shippingAsString.isBlank()) {
					order.setPostageAndPackaging(0);
				} else {
					try {
						long shipping = new BigDecimal(shippingAsString).scaleByPowerOfTen(2).longValueExact();
						order.setPostageAndPackaging(shipping);
					} catch (Exception ex) {

					}
				}

				orderAmountControl.setText(currencyFormatter.format(order.getOrderTotal()));
			}
		});
		
		discountControl.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent event) {
				Object selection = selObs.getValue();
				if (!(selection instanceof EbayOrder)) {
					throw new RuntimeException("Control should not be visible");
				}
				EbayOrder order = (EbayOrder)selection;
				String discountAsString = discountControl.getText();
				if (discountAsString.isBlank()) {
					order.setPostageAndPackaging(0);
				} else {
					try {
						long discount = new BigDecimal(discountAsString).scaleByPowerOfTen(2).longValueExact();
						order.setDiscount(discount);
					} catch (Exception ex) {

					}
				}
				
				orderAmountControl.setText(currencyFormatter.format(order.getOrderTotal()));
			}
		});
		
		return composite;
	}

	private Control createItemControls(Composite parent, IObservableValue<Entry> ebayEntry) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(2, false));

		createPicture(composite, ebayEntry);
		createItemFields(composite, ebayEntry);

		return composite;
	}

	private Control createItemFields(Composite parent, IObservableValue<Entry> ebayEntry) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(2, false));

		createLabelAndControl(composite, EbayEntryInfo.getItemNumberAccessor(), ebayEntry);
		createLabelAndControl(composite, EntryInfo.getMemoAccessor(), ebayEntry);
		createLabelAndControl(composite, EntryInfo.getAmountAccessor(), ebayEntry);
		createLabelAndControl(composite, EbayEntryInfo.getDeliveryDateAccessor(), ebayEntry);
		createLabelAndControl(composite, EbayEntryInfo.getImageCodeAccessor(), ebayEntry);

		Label netAmountLabel = new Label(composite, 0);
		netAmountLabel.setText("Net Amount:");
		Text netAmountControl = new Text(composite, SWT.TRAIL);
		netAmountControl.setLayoutData(new GridData(80, SWT.DEFAULT));

		Label quantityLabel = new Label(composite, 0);
		quantityLabel.setText("Quantity:");
		Text quantityControl = new Text(composite, SWT.NONE);
		quantityControl.setLayoutData(new GridData(200, SWT.DEFAULT));

		Label detailLabel = new Label(composite, 0);
		detailLabel.setText("Detail:");
		Text detailControl = new Text(composite, SWT.NONE);
		detailControl.setLayoutData(new GridData(200, SWT.DEFAULT));

		selObs.addValueChangeListener(new IValueChangeListener<Object>() {
			@Override
			public void handleValueChange(ValueChangeEvent<? extends Object> event) {
				if (selObs.getValue() instanceof EbayOrderItem) {
					EbayOrderItem item = (EbayOrderItem)selObs.getValue();
					
					netAmountControl.setText(new BigDecimal(item.getNetCost()).movePointLeft(2).toPlainString());
					quantityControl.setText(item.getQuantity() == 1 ? "" : Integer.toString(item.getQuantity()));
					detailControl.setText(item.getDetail() == null ? "" : item.getDetail());
				}
			}
		});

		Button pasteImageUrl = new Button(composite, SWT.PUSH);
		pasteImageUrl.setText("Paste Image URL");
		pasteImageUrl.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				String text = getTextFromClipboard();
				Matcher m = urlToImagePattern.matcher(text);
				if (m.matches()) {
					String imageCode = m.group(1);

					setImageCode(imageCode, null); // TODO remove param
				} else {
					throw new RuntimeException("bad image url");
				}				
			}
		});
		
		return composite;
	}

	private Control createPicture(Composite parent, IObservableValue<Entry> ebayEntry) {
		// Create the picture
		Canvas canvas = new Canvas(parent, SWT.NONE);
		canvas.setLayoutData(new GridData(100,100));
		canvas.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_CYAN));

		selObs.addValueChangeListener(new IValueChangeListener<Object>() {
			@Override
			public void handleValueChange(ValueChangeEvent<? extends Object> event) {
				canvas.redraw();
			}
		});

		// Monitor the image code. If this is set, either in the above control or by someone else,
		// we update the picture.
		
		IObservableValue<String> observable = EbayEntryInfo.getImageCodeAccessor().observeDetail(ebayEntry);
		observable.addValueChangeListener(new IValueChangeListener<String>() {
			@Override
			public void handleValueChange(ValueChangeEvent<? extends String> event) {
				//image may have changed, or entry selection changed
				String imageCode = event.diff.getNewValue();
				setImageCode(imageCode, canvas);

				canvas.redraw();

			}
		});

		try {

			canvas.addPaintListener(new PaintListener() {
				@Override
				public void paintControl(PaintEvent e) {
					IStructuredSelection selection = (IStructuredSelection)viewer.getSelection();
					Object selectedElement = selection.getFirstElement();
					if (selectedElement instanceof EbayOrderItem) {
						EbayOrderItem selectedItem = (EbayOrderItem)selectedElement;
						ItemUpdater selectedItem2 = (ItemUpdater)selectedItem.getUnderlyingItem();
						Image image = selectedItem2.getImage(getViewSite().getShell().getDisplay());
						if (image != null) {
							int width = 90;
							int height = 90;
							Image resizedImage = resize(image, width, height);

							e.gc.drawImage(resizedImage, 5, 5);
						} else {
							e.gc.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
						}
					} else {
						e.gc.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
					}		    	
				}

				private Image resize(Image image, int width, int height) {
					Image scaled = new Image(Display.getDefault(), width, height);
					GC gc = new GC(scaled);
					gc.setAntialias(SWT.ON);
					gc.setInterpolation(SWT.HIGH);
					gc.drawImage(image, 0, 0, 
							image.getBounds().width, image.getBounds().height, 
							0, 0, width, height);
					gc.dispose();
					image.dispose(); // don't forget about me!
					return scaled;
				}		    	
			});

		} catch (Exception e1) {
			e1.printStackTrace();
		}

		final DropTarget dropTarget = new DropTarget(canvas, DND.DROP_COPY | DND.DROP_MOVE | DND.DROP_LINK);

		// Data is provided using a local reference only (can only drag and drop
		// within the Java VM)
		Transfer[] types = new Transfer[] { TextTransfer.getInstance(), ImageTransfer.getInstance() };
		dropTarget.setTransfer(types);

		dropTarget.addDropListener(new DropTargetAdapter() {

			@Override
			public void dragEnter(DropTargetEvent event) {
				/*
				 * We want to check what is being dragged, in case it is not an
				 * EntryData object.  Unfortunately this is not available on all platforms,
				 * only on Windows.  The following call to the nativeToJava method will
				 * return the ISelection object on Windows but null on other platforms.
				 * If we get null back, we assume the drop is valid.
				 */
				ImageData selection = (ImageData)ImageTransfer.getInstance().nativeToJava(event.currentDataType);
				if (selection != null) {
					// The selection cannot be determined on this platform - accept the drop
					return;
				} else {

					String selection2 = (String)TextTransfer.getInstance().nativeToJava(event.currentDataType);
					if (selection2 != null) {
						Matcher m = urlToProductPattern.matcher(selection2);
						if (m.matches()) {
							// We can get the item number from this URL, so accept the drop
							System.out.println("drop allowed");
							return;
						}
						Matcher m2 = urlToImagePattern.matcher(selection2);
						if (m2.matches()) {
							// We can get the image from this URL, so accept the drop
							System.out.println("drop allowed");
							return;
						}
					}
				}

				// we don't want to accept drop
				event.detail = DND.DROP_NONE;
			}

			@Override
			public void dragLeave(DropTargetEvent event) {
				// TODO Auto-generated method stub

			}

			@Override
			public void dragOperationChanged(DropTargetEvent event) {
				// TODO Auto-generated method stub

			}

			@Override
			public void dragOver(DropTargetEvent event) {
				// TODO Auto-generated method stub

			}


			@Override
			public void drop(DropTargetEvent event) {
				if (TextTransfer.getInstance().isSupportedType(event.currentDataType)) {
					String selection2 = (String)TextTransfer.getInstance().nativeToJava(event.currentDataType);
					assert selection2 != null;

					Matcher m = urlToProductPattern.matcher(selection2);
					if (m.matches()) {
						String itemNumber = m.group(1);

						String imageCode = getImageCodeFromItemNumber(itemNumber);
						setImageCode(imageCode, canvas);

//						try {
//							URL url = new URL(selection2);
//							URLConnection x = url.openConnection();
//							InputStream is = x.getInputStream();
//
//							StringBuilder inputStringBuilder = new StringBuilder();
//							BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
//							String line = bufferedReader.readLine();
//							while(line != null){
//								System.out.println(line);
//								if (line.contains(".jpg")) {
//									System.out.println("");
//									inputStringBuilder.append(line);inputStringBuilder.append('\n');
//								}
//								line = bufferedReader.readLine();
//							}
//							String xx = inputStringBuilder.toString();
//							System.out.println(inputStringBuilder.toString());
//						} catch (IOException e){
//							System.out.println(e);
//						}

						//javarevisited.blogspot.com/2012/08/convert-inputstream-to-string-java-example-tutorial.html#ixzz4h2YXpNH3

						IStructuredSelection selection = (IStructuredSelection)viewer.getSelection();
						Object selectedElement = selection.getFirstElement();
						if (selectedElement instanceof EbayOrderItem) {
							EbayOrderItem selectedItem = (EbayOrderItem)selectedElement;

							ItemUpdater selectedItem2 = (ItemUpdater)selectedItem.getUnderlyingItem();
							selectedItem2.getEntry().setImageCode(itemNumber); // totally wrong
						}
					}

					Matcher m2 = urlToImagePattern.matcher(selection2);
					if (m2.matches()) {
						String imageCode = m2.group(1);

						setImageCode(imageCode, canvas);
					} else {
						event.detail = DND.DROP_NONE;
					}
				}
			}

			@Override
			public void dropAccept(DropTargetEvent event) {
				// TODO Auto-generated method stub
			}
		});

		canvas.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				dropTarget.dispose();
			}
		});

		return canvas;
	}

	private <T extends ExtendableObject> void createLabelAndControl(Composite parent, ScalarPropertyAccessor<?, T> propertyAccessor,
			IObservableValue<T> ebayEntry) {
		Label propertyLabel = new Label(parent, 0);
		propertyLabel.setText(propertyAccessor.getDisplayName() + ':');
		Control propertyControl = propertyAccessor.createPropertyControl2(parent, ebayEntry);
	}

	@Override
	public void setFocus() {
		// TODO Auto-generated method stub

	}

	private void setImageCode(String imageCode, Canvas canvas) {
		IStructuredSelection selection = (IStructuredSelection)viewer.getSelection();
		Object selectedElement = selection.getFirstElement();
		if (selectedElement instanceof EbayOrderItem) {
			EbayOrderItem selectedItem = (EbayOrderItem)selectedElement;

			ItemUpdater selectedItem2 = (ItemUpdater)selectedItem.getUnderlyingItem();

			setImageIntoItem(imageCode, selectedItem2);

//			canvas.redraw();
			viewer.update(selectedItem, null);
		}
	}

	public static void setImageIntoItem(String imageCode, ItemUpdater itemUpdater) {
		itemUpdater.getEntry().setImageCode(imageCode);

		if (imageCode != null) {
		/*
		 * See http://superuser.com/questions/123911/how-to-get-the-full-
		 * image-when-the-shopping-site-like-amazon-shows-you-only-a-pa
		 * and also http://aaugh.com/imageabuse.html
		 */
		String urlString = MessageFormat
				.format("https://i.ebayimg.com/images/g/{0}/s-l1600.jpg",
						imageCode);

		try {
			URL picture = new URL(urlString);

			IBlob blob = new UrlBlob(picture);

			// We must go thru the wrapper item, do not set
			// directly on the entry, because the wrapper caches
			// the image.
			itemUpdater.setImage(blob);

		} catch (Exception e) {
			// s-l140 also works, but that is more thumbnail size
			urlString = MessageFormat
					.format("https://i.ebayimg.com/images/g/{0}/s-l500.jpg",
							imageCode);

			try {
				URL picture = new URL(urlString);

				IBlob blob = new UrlBlob(picture);

				// We must go thru the wrapper item, do not set
				// directly on the entry, because the wrapper caches
				// the image.
				itemUpdater.setImage(blob);

			} catch (MalformedURLException e2) {
				// Should not happen so convert to an unchecked exception
				throw new RuntimeException(e2);
			}
		}
		} else {
			itemUpdater.setImage(null);
		}
	}

	// Is this the right place for this function?
	public static String getImageCodeFromItemNumber(String itemNumber) {

		String urlString = MessageFormat
				.format("https://www.ebay.com/itm/{0}",
						itemNumber);
		Document doc;
		try {
			doc = Jsoup.connect(urlString).get();

			Elements elements = doc.getElementsByClass("ux-image-carousel-item");			

			if (elements.isEmpty()) {
				throw new RuntimeException("Got back content for item but could not find expected elements.");
			}
			Element firstElementInCarousel = elements.first();
			
			Element element = firstElementInCarousel.getElementsByTag("img").first();
			String srcAttr = element.attr("src");

			Matcher m = urlFromItemPageToImageCodePattern.matcher(srcAttr);
			if (m.matches()) {
				String imageCode = m.group(1);
				return imageCode;
			} else {
				throw new RuntimeException("Could not extract image code from " + srcAttr);
			}

		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			// Item no longer exists. User needs to copy URL from image on order page.
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public void dispose() {
		super.dispose();
		errorImage.dispose();
	}

}
