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

package net.sf.jmoney.amazon.copytext;

import java.awt.Toolkit;
import java.awt.datatransfer.FlavorEvent;
import java.awt.datatransfer.FlavorListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.eclipse.core.databinding.observable.list.IObservableList;
import org.eclipse.core.databinding.observable.list.WritableList;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.IValueChangeListener;
import org.eclipse.core.databinding.observable.value.ValueChangeEvent;
import org.eclipse.core.databinding.observable.value.WritableValue;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.databinding.viewers.IViewerObservableValue;
import org.eclipse.jface.databinding.viewers.ViewersObservables;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
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
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import net.sf.jmoney.amazon.AmazonEntry;
import net.sf.jmoney.amazon.AmazonEntryInfo;
import net.sf.jmoney.amazon.UrlBlob;
import net.sf.jmoney.fields.DateControl;
import net.sf.jmoney.fields.IBlob;
import net.sf.jmoney.model2.CapitalAccount;
import net.sf.jmoney.model2.Currency;
import net.sf.jmoney.model2.CurrencyAccount;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.EntryInfo;
import net.sf.jmoney.model2.IDatastoreManager;
import net.sf.jmoney.model2.ScalarPropertyAccessor;
import net.sf.jmoney.model2.Session;
import net.sf.jmoney.model2.Transaction;
import net.sf.jmoney.model2.TransactionManagerForAccounts;
import txr.matchers.DocumentMatcher;
import txr.matchers.MatchResults;
import txr.parser.AST;
import txr.parser.Parser;

public class AmazonImportView extends ViewPart {

	public class ItemBuilder {

		AmazonOrder order;

		Set<AmazonOrderItem> preexistingItems;

		public ItemBuilder(AmazonOrder order, List<AmazonOrderItem> items) {
			this.order = order;
			preexistingItems = new HashSet<>(items);
		}

		public AmazonOrderItem get(String asin, String description, long itemAmount, ShipmentObject shipmentObject, Session session) {
			// Find the matching entry
			if (preexistingItems.isEmpty()) {
				AmazonOrderItem item = order.createNewItem(description, itemAmount, shipmentObject, session);
				if (asin != null) {
					item.getEntry().setAsinOrIsbn(asin);
				}
				return item;
			} else {
				AmazonOrderItem[] matches = preexistingItems.stream().filter(item -> item.getEntry().getAmount() == itemAmount).toArray(AmazonOrderItem[]::new);
				if (matches.length > 1) {
					matches = Stream.of(matches).filter(item -> item.getEntry().getAmazonDescription().equals(description)).toArray(AmazonOrderItem[]::new);
				}
				if (matches.length != 1) {
					throw new RuntimeException("Existing transaction for order does not match up.");
				}
				AmazonOrderItem matchingItem = matches[0];

				/*
				 * Check the shipment splitting is consistent (i.e. has not changed).
				 * 
				 */
				final Transaction transactionOfThisItem = matchingItem.getEntry().getTransaction();
				if (shipmentObject.shipment != null) {
					if (shipmentObject.shipment.getTransaction() != transactionOfThisItem) {
						throw new RuntimeException("Inconsistent shipment");
					}
				} else {
					AmazonShipment[] matchingShipments = order.getShipments().stream().filter(shipment -> shipment.getTransaction() == transactionOfThisItem).toArray(AmazonShipment[]::new);
					assert(matchingShipments.length == 1);
					AmazonShipment shipment = matchingShipments[0];

					shipmentObject.shipment = shipment;
				}



				preexistingItems.remove(matchingItem);
				return matchingItem;
			}
		}

		/**
		 * This method is used only to assert that all the items matched,
		 * none were left unmatched.
		 * 
		 * @return
		 */
		public boolean isEmpty() {
			return preexistingItems.isEmpty();
		}
	}

	protected static Pattern urlToProductPattern;
	static {
		urlToProductPattern = Pattern.compile("https://www.amazon.co.uk/gp/product/((\\d|\\w)*)/.*");
	}

	protected static Pattern urlToImagePattern;
	static {
		urlToImagePattern = Pattern.compile("https://images-na.ssl-images-amazon.com/images/I/((\\d|\\w)*).jpg");
	}

	protected static Pattern urlToImageCodePattern;
	static {
		urlToImageCodePattern = Pattern.compile("https://images-eu.ssl-images-amazon.com/images/I/((\\d|\\w)*)\\._SY300_QL70_\\.jpg");
	}

	public class PasteOrdersAction extends Action {
		public PasteOrdersAction() {
			super("Paste Orders");
			setToolTipText("Paste from Amazon Orders Page");
			setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_TOOL_PASTE));
		}

		@Override
		public void run() {
			try {
				pasteOrders();
			} catch (Exception e) {
				MessageDialog.openError(getViewSite().getShell(), "Paste Failed", e.getMessage());
			}
		}
	}

	public class PasteDetailsAction extends Action {
		public PasteDetailsAction() {
			super("Paste Details");
			setToolTipText("Paste from Amazon Details Page");
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

	public static String ID = "net.sf.jmoney.amazon.AmazonImportView";

	//	private TableSorter sorter = new TableSorter(VISIBLE_FIELDS);

	private TreeViewer viewer;

	private IViewerObservableValue<Viewer> selObs;

	private PasteOrdersAction pasteOrdersAction;

	private PasteDetailsAction pasteDetailsAction;

	// Lazily created
	private DocumentMatcher ordersMatcher = null;

	// Lazily created
	private DocumentMatcher detailsMatcher = null;

	private TransactionManagerForAccounts uncommittedSessionManager;

	// Session, inside transaction
	private Session session;

	private IObservableList<AmazonOrder> orders = new WritableList<>();

	public AmazonImportView() {
		pasteOrdersAction = new PasteOrdersAction();
		pasteDetailsAction = new PasteDetailsAction();
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
		IDatastoreManager sessionManager = (IDatastoreManager)activePage.getInput();
		if (sessionManager == null) {
			stackLayout.topControl = noSessionLabel;
		} else {
			stackLayout.topControl = dataComposite;

			uncommittedSessionManager = new TransactionManagerForAccounts(sessionManager);
			session = uncommittedSessionManager.getSession();
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
				} else {
					stackLayout.topControl = dataComposite;

					uncommittedSessionManager = new TransactionManagerForAccounts(committedSessionManager);
					session = uncommittedSessionManager.getSession();
				}
				stackComposite.layout();
			}
		});

		contributeToActionBars();
	}

	private Control createMainArea(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(1, false));

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
				uncommittedSessionManager.commit("Amazon Import from Clipboard");

				orders.clear();
				viewer.setInput(orders.toArray(new AmazonOrder[0]));

				IDatastoreManager committedSessionManager = (IDatastoreManager)getSite().getWorkbenchWindow().getActivePage().getInput();
				uncommittedSessionManager = new TransactionManagerForAccounts(committedSessionManager);
				session = uncommittedSessionManager.getSession();
			}

			private void validateTransactions() {
				for (AmazonOrder order : orders) {
					long totalForOrder = 0;
					for (AmazonShipment shipment : order.getShipments()) {
						Transaction transaction = shipment.getChargeEntry().getTransaction();

						// If charge amount is zero, add up the transaction to set it.
						if (shipment.getChargeEntry().getAmount() == 0) {
							long total = 0;
							for (Entry entry : transaction.getEntryCollection()) {
								total += entry.getAmount();
							}
							shipment.getChargeEntry().setAmount(-total);
						}

						totalForOrder -= shipment.getChargeEntry().getAmount();

						if (transaction.getDate() == null) {
							throw new RuntimeException("No date set on order " + order.getOrderNumber());
						}
						long total = 0;
						for (Entry entry : transaction.getEntryCollection()) {
							if (entry.getAmount() == 0) {
								throw new RuntimeException("Zero amount set on order " + order.getOrderNumber());
							}
							if (entry.getAccount() == null) {
								throw new RuntimeException("No account set on order " + order.getOrderNumber());
							}
							total += entry.getAmount();
						}
						if (total != 0) {
							throw new RuntimeException("Unbalanced transaction on order " + order.getOrderNumber());
						}
					}

					if (totalForOrder != order.getOrderTotal()) {
						throw new RuntimeException("Order does not add up to order total for order " + order.getOrderNumber());
					}
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
				orders.clear();
				viewer.setInput(orders.toArray(new AmazonOrder[0]));

				IDatastoreManager committedSessionManager = (IDatastoreManager)getSite().getWorkbenchWindow().getActivePage().getInput();
				uncommittedSessionManager = new TransactionManagerForAccounts(committedSessionManager);
				session = uncommittedSessionManager.getSession();
			}
		});

		return composite;
	}

	private void pasteOrders() {
		if (ordersMatcher == null) {
			ordersMatcher = createMatcherFromResource("amazon-orders.txr");
		}

		MatchResults bindings = doMatchingFromClipboard(ordersMatcher);

		if (bindings == null || bindings.getCollections(0).isEmpty()) {
			throw new RuntimeException("Data in clipboard does not appear to be copied from the orders page.");
		}

		DateFormat dateFormat = new SimpleDateFormat("d MMM yyyy");

		for (MatchResults orderBindings : bindings.getCollections(0)) {
			String orderDateAsString = orderBindings.getVariable("date").text;
			String orderNumber = orderBindings.getVariable("ordernumber").text;
			String orderTotal = orderBindings.getVariable("totalamount").text;

			Date orderDate;
			try {
				orderDate = dateFormat.parse(orderDateAsString);
			} catch (ParseException e) {
				// TODO Return as error to TXR when that is supported???
				e.printStackTrace();
				throw new RuntimeException("bad date");
			}

			/*
			 * If no AmazonOrder exists yet in this view then create one.
			 * This will be either initially empty if the order does not yet
			 * exist in the session or it will be a wrapper for the existing order
			 * found in the session.
			 */
			AmazonOrder order = getAmazonOrderWrapper(orderNumber, orderDate, session);

			order.setOrderDate(orderDate);  // Done here and in above....
			order.setOrderTotal(orderTotal);

			ItemBuilder itemBuilder = new ItemBuilder(order, order.getItems());

			for (MatchResults shipmentBindings : orderBindings.getCollections(0)) {
				String expectedDateAsString = shipmentBindings.getVariable("expecteddate").text;
				String deliveryDateAsString = shipmentBindings.getVariable("deliverydate").text;

				Date deliveryDate;
				try {
					deliveryDate = parsePastDate(deliveryDateAsString);
				} catch (ParseException e) {
					// TODO Return as error to TXR when that is supported???
					e.printStackTrace();
					throw new RuntimeException("bad date");
				}

				/*
				 * If no AmazonShipment exists yet in this view then create one.
				 * 
				 * AmazonItem object created from datastore initially have no shipment.
				 * Only when matched here is a shipment assigned to the item.
				 * 
				 * Two types of shipment creators.  One creates a new shipment each time.
				 * 
				 * 
				 * This will be either initially empty if the order does not yet
				 * exist in the session or it will be a wrapper for the existing order
				 * found in the session.
				 */
				ShipmentObject shipmentObject = new ShipmentObject();

				for (MatchResults itemBindings : shipmentBindings.getCollections(0)) {
					String description = itemBindings.getVariable("description").text;
					String itemAmountAsString = itemBindings.getVariable("itemamount").text;
					String soldBy = itemBindings.getVariable("soldby").text;
					String author = itemBindings.getVariable("author").text;
					String returnDeadline = itemBindings.getVariable("returndeadline").text;

					long itemAmount = new BigDecimal(itemAmountAsString).scaleByPowerOfTen(2).longValueExact();

					String asin = null;

					AmazonOrderItem item = itemBuilder.get(asin, description, itemAmount, shipmentObject, session);

					item.setSoldBy(soldBy);
					item.setAuthor(author);
					item.setReturnDeadline(returnDeadline);
				}

				if (!itemBuilder.isEmpty()) {
					throw new RuntimeException("gone wrong");
				}

				// Now we have the items in this shipment,
				// we can access the actual shipment.
				AmazonShipment shipment = shipmentObject.shipment;

				shipment.setExpectedDate(expectedDateAsString);
				shipment.setDeliveryDate(deliveryDate);
			}
		}

		viewer.setInput(orders.toArray(new AmazonOrder[0]));
	}

	/**
	 * Looks to see if this order is already in this view.  If not,
	 * creates the AmazonOrder object for this order.
	 * <P>
	 * Note that when an AmazonOrder object is created, the order may or may
	 * not already exist in the accounting datastore.  If the order did not
	 * already exist in the datastore then a new transaction is created.
	 * 
	 * @param orderNumber
	 * @param orderDate
	 * @param session
	 * @return
	 */
	private AmazonOrder getAmazonOrderWrapper(String orderNumber, Date orderDate, Session session) {
		// Look to see if this order is already in the view.
		Optional<AmazonOrder> order = orders.stream().filter(x -> x.getOrderNumber().equals(orderNumber))
				.findFirst();

		if (order.isPresent()) {
			return order.get();
		}

		AmazonOrder newOrder = createAmazonOrderWrapper(orderNumber, orderDate, session);
		orders.add(newOrder);
		return newOrder;
	}

	/**
	 * Creates a new AmazonOrder object for an order that is not already in our view.
	 * The order may or may
	 * not already exist in the accounting datastore.  If the order did not
	 * already exist in the datastore then a new transaction is created.
	 * 
	 * @param orderNumber
	 * @param orderDate this is used when looking for the given order number because
	 * 			this is indexed (or at least should be)
	 * @param session
	 * @return
	 */
	private AmazonOrder createAmazonOrderWrapper(String orderNumber, Date orderDate, Session session) {
		Set<AmazonEntry> entriesInOrder = lookupEntriesInOrder(orderNumber, orderDate);

		if (entriesInOrder.isEmpty()) {
			AmazonOrder order = new AmazonOrder(orderNumber, session);
			order.setOrderDate(orderDate);
			return order;
		} else {
			Transaction[] transactions = entriesInOrder.stream().map(entry -> entry.getTransaction()).distinct().toArray(Transaction[]::new);
			AmazonOrder order = new AmazonOrder(orderNumber, transactions);
			return order;
		}
	}

	private Date parsePastDate(String dateAsString) throws ParseException {
		if (dateAsString == null) {
			return null;
		}

		DayOfWeek givenDayOfWeek = null;

		switch (dateAsString) {
		case "today":
			return new Date();
		case "yesterday":
		{
			Date currentDate = new Date();
			LocalDateTime localDateTime = currentDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
			localDateTime = localDateTime.minusDays(1);
			Date resultDate = Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
			return resultDate;
		}
		case "Monday":
			givenDayOfWeek = DayOfWeek.MONDAY;
			break;
		case "Tuesday":
			givenDayOfWeek = DayOfWeek.TUESDAY;
			break;
		case "Wednesday":
			givenDayOfWeek = DayOfWeek.WEDNESDAY;
			break;
		case "Thursday":
			givenDayOfWeek = DayOfWeek.THURSDAY;
			break;
		case "Friday":
			givenDayOfWeek = DayOfWeek.FRIDAY;
			break;
		case "Saturday":
			givenDayOfWeek = DayOfWeek.SATURDAY;
			break;
		case "Sunday":
			givenDayOfWeek = DayOfWeek.SUNDAY;
			break;
		}

		if (givenDayOfWeek != null) {
			// Or should we always use London time?  Is that what amazon.co.uk uses?
			Date currentDate = new Date();
			LocalDateTime localDateTime = currentDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();

			while (localDateTime.getDayOfWeek().compareTo(givenDayOfWeek) != 0) {
				localDateTime = localDateTime.minusDays(1);
			}

			// convert LocalDateTime to date
			Date resultDate = Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
			return resultDate;
		}

		DateFormat dateFormat = new SimpleDateFormat("d MMM yyyy");
		try {
			return dateFormat.parse(dateAsString);
		} catch (ParseException e) {
			// TODO Return as error to TXR when that is supported???
			e.printStackTrace();
			throw new RuntimeException("bad date");
		}

	}

	/**
	 * Given an order number, return all entries that have the given
	 * order number set.
	 * 
	 * @param orderNumber
	 * @param orderDate the date of the order, being the transaction date,
	 * 			this parameter being used for performance as the entries are
	 * 			indexed by date
	 * @return
	 */
	private Set<AmazonEntry> lookupEntriesInOrder(String orderNumber, Date orderDate) {
		/*
		 * The getEntries method is not supported in an uncommitted data manager.  Therefore we do
		 * the search on the committed data manager and copy the results into the uncommitted data manager.
		 * This is ok because we know there will not be any changes to this order in the uncommitted data manager,
		 * nor will this order be in an uncommitted state.
		 */
		IDatastoreManager sessionManager = (IDatastoreManager)getSite().getWorkbenchWindow().getActivePage().getInput();

		List<Entry> entriesInDateRange = sessionManager.getEntries(orderDate, orderDate, null, null);

		Set<AmazonEntry> entriesInOrder = new HashSet<>();
		for (Entry entry : entriesInDateRange) {
			String thisOrderId = AmazonEntryInfo.getOrderIdAccessor().getValue(entry);
			if (orderNumber.equals(thisOrderId)) {
				Entry entryInTransaction = uncommittedSessionManager.getCopyInTransaction(entry);
				AmazonEntry amazonEntry = entryInTransaction.getExtension(AmazonEntryInfo.getPropertySet(), false);
				entriesInOrder.add(amazonEntry);
			}
		}
		return entriesInOrder;
	}

	private void pasteDetails() {
		if (detailsMatcher == null) {
			detailsMatcher = createMatcherFromResource("amazon-details.txr");
		}

		MatchResults orderBindings = doMatchingFromClipboard(detailsMatcher);

		if (orderBindings == null) {
			throw new RuntimeException("Data in clipboard does not appear to be a details page.");
		}

		String orderDateAsString = orderBindings.getVariable("orderdate").text;
		String orderNumber = orderBindings.getVariable("ordernumber").text;
		String subTotalAsString = orderBindings.getVariable("subtotal").text;
		String orderTotalAsString = orderBindings.getVariable("total").text;
		String grandTotalAsString = orderBindings.getVariable("grandtotal").text;
		String lastFourDigits = orderBindings.getVariable("lastfourdigits").text;
		String postageAndPackagingAsString = orderBindings.getVariable("postageandpackaging").text;

		Currency thisCurrency = session.getCurrencyForCode("GBP");

		DateFormat dateFormat = new SimpleDateFormat("d MMM yyyy");

		Date orderDate;
		try {
			orderDate = dateFormat.parse(orderDateAsString);
		} catch (ParseException e) {
			// TODO Return as error to TXR when that is supported???
			e.printStackTrace();
			throw new RuntimeException("bad date");
		}

		long orderTotal = new BigDecimal(orderTotalAsString).scaleByPowerOfTen(2).longValueExact();
		long postageAndPackaging = new BigDecimal(postageAndPackagingAsString).scaleByPowerOfTen(2).longValueExact();

		/*
		 * If no AmazonOrder exists yet in this view then create one.
		 * This will be either initially empty if the order does not yet
		 * exist in the session or it will be a wrapper for the existing order
		 * found in the session.
		 */
		AmazonOrder order = getAmazonOrderWrapper(orderNumber, orderDate, session);

		ItemBuilder itemBuilder = new ItemBuilder(order, order.getItems());

		for (MatchResults shipmentBindings : orderBindings.getCollections(0)) {
			String expectedDateAsString = shipmentBindings.getVariable("expecteddate").text;
			String deliveryDateAsString = shipmentBindings.getVariable("deliverydate").text;

			Date deliveryDate;
			try {
				deliveryDate = parsePastDate(deliveryDateAsString);
			} catch (ParseException e) {
				// TODO Return as error to TXR when that is supported???
				e.printStackTrace();
				throw new RuntimeException("bad date");
			}

			/*
			 * Find the account which is charged for the purchases.
			 */
			CapitalAccount chargeAccount = null;
			if (lastFourDigits != null) {
				for (Iterator<CapitalAccount> iter = session.getCapitalAccountIterator(); iter.hasNext(); ) {
					CapitalAccount eachAccount = iter.next();
					if (eachAccount.getName().endsWith(lastFourDigits)
							&& eachAccount instanceof CurrencyAccount
							&& ((CurrencyAccount)eachAccount).getCurrency() == thisCurrency) {
						chargeAccount = (CurrencyAccount)eachAccount;
						break;
					}
				}
				if (chargeAccount == null) {
					throw new RuntimeException("No account exists with the given last four digits and a currency of " + thisCurrency.getName() + ".");
				}
			}

			order.setOrderDate(orderDate);
			order.setOrderTotal(orderTotalAsString);

			ShipmentObject shipmentObject = new ShipmentObject();

			for (MatchResults itemBindings : shipmentBindings.getCollections(0)) {
				String description = itemBindings.getVariable("description").text;
				String soldBy = itemBindings.getVariable("soldby").text;
				String author = itemBindings.getVariable("author").text;
				String itemAmountAsString = itemBindings.getVariable("itemamount").text;

				String asin = null;

				long itemAmount = new BigDecimal(itemAmountAsString).scaleByPowerOfTen(2).longValueExact();

				AmazonOrderItem item = itemBuilder.get(asin, description, itemAmount, shipmentObject, session);

				// TODO any item data to merge here???

				item.setAuthor(author);
				item.setSoldBy(soldBy);
			}

			if (!itemBuilder.isEmpty()) {
				throw new RuntimeException("gone wrong");
			}

			// Now we have the items in this shipment,
			// we can access the actual shipment.
			AmazonShipment shipment = shipmentObject.shipment;

			shipment.setExpectedDate(expectedDateAsString);
			shipment.setDeliveryDate(deliveryDate);
			shipment.setChargeAccount(chargeAccount);
		}


		if (postageAndPackaging != 0) {
			if (order.getShipments().size() == 1) {
				AmazonShipment shipment = order.getShipments().get(0); 
				shipment.setPostageAndPackaging(postageAndPackaging);
			} else {
				throw new RuntimeException("p&p but multiple shipments.  We need to see an example of this to decide how to handle this.");
			}
		}

		viewer.setInput(orders.toArray(new AmazonOrder[0]));
	}

	private MatchResults doMatchingFromClipboard(DocumentMatcher matcher) {
		Display display = Display.getCurrent();
		Clipboard clipboard = new Clipboard(display);

		String plainText = (String)clipboard.getContents(TextTransfer.getInstance());
		System.out.println("PLAIN: " + plainText);
		clipboard.dispose();        

		List<String> result2 = new ArrayList<>();

		try (Scanner scanner = new Scanner(plainText)) {
			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();
				result2.add(line);
			}

		}

		String [] inputText = result2.toArray(new String[0]);
		MatchResults bindings = matcher.process(inputText);

		return bindings;
	}

	private DocumentMatcher createMatcherFromResource(String resourceName) {
		Parser p = new Parser();
		ClassLoader classLoader = getClass().getClassLoader();
		URL resource = classLoader.getResource(resourceName);
		InputStream txrInputStream;
		try {
			txrInputStream = resource.openStream();
			StringBuilder result = new StringBuilder("");

			try (Scanner scanner = new Scanner(txrInputStream, "UTF-8")) {
				while (scanner.hasNextLine()) {
					String line = scanner.nextLine();
					result.append(line).append("\n");
				}
			}

			AST ast = p.parse(result.toString());

			DocumentMatcher matcher = new DocumentMatcher(ast);

			return matcher;
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			throw new RuntimeException(e1);
		}
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
		viewer.setContentProvider(new AmazonOrderContentProvider());

		selObs = ViewersObservables.observeSingleSelection(viewer);

		Tree tree = viewer.getTree();

		tree.setHeaderVisible(true);
		tree.setLinesVisible(true);

		TreeViewerColumn statusColumn = new TreeViewerColumn(viewer, SWT.LEFT);
		statusColumn.getColumn().setText("Order/Item");
		statusColumn.getColumn().setWidth(300);

		statusColumn.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				if (cell.getElement() instanceof AmazonOrder) {
					AmazonOrder order = (AmazonOrder)cell.getElement();
					cell.setText(order.getOrderNumber());
				} else if (cell.getElement() instanceof AmazonShipment) {
					AmazonShipment shipment = (AmazonShipment)cell.getElement();
					if (shipment.getExpectedDate() != null) {
						cell.setText("Expected: " + shipment.getExpectedDate());
					} else if (shipment.getDeliveryDate() != null) {
						DateFormat df = new SimpleDateFormat("dd/MM/yyyy");
						String deliveryDateAsString = df.format(shipment.getDeliveryDate());
						cell.setText("Delivered: " + deliveryDateAsString);
					}
				} else if (cell.getElement() instanceof AmazonOrderItem) {
					AmazonOrderItem item = (AmazonOrderItem)cell.getElement();
					cell.setText(item.getDescription());
				}
			}
		});

		TreeViewerColumn asinColumn = new TreeViewerColumn(viewer, SWT.LEFT);
		asinColumn.getColumn().setText("ASIN/ISBN");
		asinColumn.getColumn().setWidth(200);

		asinColumn.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				if (cell.getElement() instanceof AmazonOrderItem) {
					AmazonOrderItem item = (AmazonOrderItem)cell.getElement();
					String text = item.getEntry().getAsinOrIsbn();
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
				if (cell.getElement() instanceof AmazonOrderItem) {
					AmazonOrderItem item = (AmazonOrderItem)cell.getElement();
					String text = item.getEntry().getImageCode();
					cell.setText(text);
				}
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

		IObservableValue<Entry> amazonEntry = new WritableValue<Entry>();

		Control orderControls = createOrderControls(stackComposite);
		Control shipmentControls = createShipmentControls(stackComposite);
		Control combinedControls = createCombinedOrderAndShipmentControls(stackComposite);
		Control itemControls = createItemControls(stackComposite, amazonEntry);


		selObs.addValueChangeListener(new IValueChangeListener<Object>() {
			@Override
			public void handleValueChange(ValueChangeEvent<Object> event) {
				if (selObs.getValue() instanceof AmazonOrder) {
					AmazonOrder order = (AmazonOrder)selObs.getValue();
					if (order.getShipments().size() == 1) {
						stackLayout.topControl= combinedControls;
					} else {
						stackLayout.topControl= orderControls;
					}
				} else if (selObs.getValue() instanceof AmazonShipment) {
					stackLayout.topControl = shipmentControls;
				} else if (selObs.getValue() instanceof AmazonOrderItem) {
					AmazonOrderItem item = (AmazonOrderItem)selObs.getValue();

					stackLayout.topControl = itemControls;

					amazonEntry.setValue(item.getEntry().getBaseObject());
				} else {
					stackLayout.topControl = null;
				}

				stackComposite.layout();
			}
		});

		return stackComposite;
	}

	private Control createOrderControls(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(2, false));

		Label orderNumberLabel = new Label(composite, 0);
		orderNumberLabel.setText("Order Number:");
		Text orderControl = new Text(composite, SWT.NONE);
		orderControl.setLayoutData(new GridData(200, SWT.DEFAULT));

		Label orderDateLabel = new Label(composite, 0);
		orderDateLabel.setText("Order Date:");
		DateControl orderDateControl = new DateControl(composite);
		orderDateControl.setLayoutData(new GridData(200, SWT.DEFAULT));

		Label orderAmountLabel = new Label(composite, 0);
		orderAmountLabel.setText("Order Total:");
		Text orderAmountControl = new Text(composite, SWT.NONE);
		orderAmountControl.setLayoutData(new GridData(200, SWT.DEFAULT));

		selObs.addValueChangeListener(new IValueChangeListener<Object>() {
			@Override
			public void handleValueChange(ValueChangeEvent<Object> event) {
				if (selObs.getValue() instanceof AmazonOrder) {
					AmazonOrder order = (AmazonOrder)selObs.getValue();

					orderControl.setText(order.getOrderNumber());
					orderDateControl.setDate(order.getOrderDate());

					Currency currency = session.getCurrencyForCode("GBP");
					orderAmountControl.setText(currency.format(order.getOrderTotal()));
				}
			}
		});

		return composite;
	}

	private Control createShipmentControls(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(2, false));

		Label expectedDateLabel = new Label(composite, 0);
		expectedDateLabel.setText("Expected Date:");
		Text expectedDateControl = new Text(composite, SWT.NONE);
		expectedDateControl.setLayoutData(new GridData(200, SWT.DEFAULT));

		Label deliveryDateLabel = new Label(composite, 0);
		deliveryDateLabel.setText("Delivery Date:");
		DateControl deliveryDateControl = new DateControl(composite);
		deliveryDateControl.setLayoutData(new GridData(200, SWT.DEFAULT));

		Label postageAndPackagingLabel = new Label(composite, 0);
		postageAndPackagingLabel.setText("Postage and Packaging:");
		Text postageAndPackagingControl = new Text(composite, SWT.NONE);
		postageAndPackagingControl.setLayoutData(new GridData(200, SWT.DEFAULT));

		selObs.addValueChangeListener(new IValueChangeListener<Object>() {
			@Override
			public void handleValueChange(ValueChangeEvent<Object> event) {
				if (selObs.getValue() instanceof AmazonShipment) {
					AmazonShipment shipment = (AmazonShipment)selObs.getValue();

					expectedDateControl.setText(shipment.getExpectedDate() == null ? "" : shipment.getExpectedDate());
					deliveryDateControl.setDate(shipment.getDeliveryDate());

					if (shipment.postageAndPackagingEntry != null) {
						Currency currency = session.getCurrencyForCode("GBP");
						postageAndPackagingControl.setText(currency.format(shipment.postageAndPackagingEntry.getAmount()));
					} else {
						postageAndPackagingControl.setText("");
					}
				}
			}
		});

		return composite;
	}

	private Control createCombinedOrderAndShipmentControls(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(2, false));

		Label orderNumberLabel = new Label(composite, 0);
		orderNumberLabel.setText("Order Number:");
		Text orderControl = new Text(composite, SWT.NONE);
		orderControl.setLayoutData(new GridData(200, SWT.DEFAULT));

		Label orderDateLabel = new Label(composite, 0);
		orderDateLabel.setText("Order Date:");
		DateControl orderDateControl = new DateControl(composite);
		orderDateControl.setLayoutData(new GridData(200, SWT.DEFAULT));

		Label orderAmountLabel = new Label(composite, 0);
		orderAmountLabel.setText("Order Total:");
		Text orderAmountControl = new Text(composite, SWT.NONE);
		orderAmountControl.setLayoutData(new GridData(200, SWT.DEFAULT));

		Label expectedDateLabel = new Label(composite, 0);
		expectedDateLabel.setText("Expected Date:");
		Text expectedDateControl = new Text(composite, SWT.NONE);
		expectedDateControl.setLayoutData(new GridData(200, SWT.DEFAULT));

		Label deliveryDateLabel = new Label(composite, 0);
		deliveryDateLabel.setText("Delivery Date:");
		DateControl deliveryDateControl = new DateControl(composite);
		deliveryDateControl.setLayoutData(new GridData(200, SWT.DEFAULT));

		Label postageAndPackagingLabel = new Label(composite, 0);
		postageAndPackagingLabel.setText("Postage and Packaging:");
		Text postageAndPackagingControl = new Text(composite, SWT.NONE);
		postageAndPackagingControl.setLayoutData(new GridData(200, SWT.DEFAULT));

		selObs.addValueChangeListener(new IValueChangeListener<Object>() {
			@Override
			public void handleValueChange(ValueChangeEvent<Object> event) {
				if (selObs.getValue() instanceof AmazonOrder) {
					AmazonOrder order = (AmazonOrder)selObs.getValue();

					if (order.getShipments().size() == 1) {
						orderControl.setText(order.getOrderNumber());
						orderDateControl.setDate(order.getOrderDate());

						Currency currency = session.getCurrencyForCode("GBP");
						orderAmountControl.setText(currency.format(order.getOrderTotal()));

						AmazonShipment shipment = order.getShipments().get(0);

						expectedDateControl.setText(shipment.getExpectedDate() == null ? "" : shipment.getExpectedDate());
						deliveryDateControl.setDate(shipment.getDeliveryDate());

						if (shipment.postageAndPackagingEntry != null) {
							postageAndPackagingControl.setText(currency.format(shipment.postageAndPackagingEntry.getAmount()));
						} else {
							postageAndPackagingControl.setText("");
						}
					}
				}
			}
		});

		return composite;
	}

	private Control createItemControls(Composite parent, IObservableValue<Entry> amazonEntry) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(2, false));

		createPicture(composite, amazonEntry);
		createItemFields(composite, amazonEntry);

		return composite;
	}

	private Control createItemFields(Composite parent, IObservableValue<Entry> amazonEntry) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(2, false));

		createLabelAndControl(composite, EntryInfo.getMemoAccessor(), amazonEntry);
		createLabelAndControl(composite, EntryInfo.getAmountAccessor(), amazonEntry);
		createLabelAndControl(composite, AmazonEntryInfo.getAsinOrIsbnAccessor(), amazonEntry);
		createLabelAndControl(composite, AmazonEntryInfo.getImageCodeAccessor(), amazonEntry);

		Label soldByLabel = new Label(composite, 0);
		soldByLabel.setText("Sold By:");
		Text soldByControl = new Text(composite, SWT.NONE);
		soldByControl.setLayoutData(new GridData(200, SWT.DEFAULT));

		Label authorLabel = new Label(composite, 0);
		authorLabel.setText("Author:");
		Text authorControl = new Text(composite, SWT.NONE);
		authorControl.setLayoutData(new GridData(200, SWT.DEFAULT));

		selObs.addValueChangeListener(new IValueChangeListener<Object>() {
			@Override
			public void handleValueChange(ValueChangeEvent<Object> event) {
				if (selObs.getValue() instanceof AmazonOrderItem) {
					AmazonOrderItem item = (AmazonOrderItem)selObs.getValue();

					soldByControl.setText(item.getSoldBy() == null ? "" : item.getSoldBy());
					authorControl.setText(item.getAuthor() == null ? "" : item.getAuthor());
				}
			}
		});

		return composite;
	}

	private Control createPicture(Composite parent, IObservableValue<Entry> amazonEntry) {
		// Create the picture
		Canvas canvas = new Canvas(parent, SWT.NONE);
		canvas.setLayoutData(new GridData(100,100));
		canvas.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_CYAN));

		selObs.addValueChangeListener(new IValueChangeListener<Object>() {
			@Override
			public void handleValueChange(ValueChangeEvent<Object> event) {
				canvas.redraw();
			}
		});

		try {

			canvas.addPaintListener(new PaintListener() {
				@Override
				public void paintControl(PaintEvent e) {
					IStructuredSelection selection = (IStructuredSelection)viewer.getSelection();
					Object selectedElement = selection.getFirstElement();
					if (selectedElement instanceof AmazonOrderItem) {
						AmazonOrderItem selectedItem = (AmazonOrderItem)selectedElement;
						Image image = selectedItem.getImage(getViewSite().getShell().getDisplay());
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
							// We can get the ASIN from this URL, so accept the drop
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
						String asin = m.group(1);

						parseItemHtml(asin, canvas);

						try {
							URL url = new URL(selection2);
							URLConnection x = url.openConnection();
							InputStream is = x.getInputStream();

							StringBuilder inputStringBuilder = new StringBuilder();
							BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
							String line = bufferedReader.readLine();
							while(line != null){
								System.out.println(line);
								if (line.contains(".jpg")) {
									System.out.println("");
									inputStringBuilder.append(line);inputStringBuilder.append('\n');
								}
								line = bufferedReader.readLine();
							}
							String xx = inputStringBuilder.toString();
							System.out.println(inputStringBuilder.toString());
						} catch (IOException e){
							System.out.println(e);
						}

						//javarevisited.blogspot.com/2012/08/convert-inputstream-to-string-java-example-tutorial.html#ixzz4h2YXpNH3

						IStructuredSelection selection = (IStructuredSelection)viewer.getSelection();
						Object selectedElement = selection.getFirstElement();
						if (selectedElement instanceof AmazonOrderItem) {
							AmazonOrderItem selectedItem = (AmazonOrderItem)selectedElement;

							selectedItem.getEntry().setAsinOrIsbn(asin);
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

	private void createLabelAndControl(Composite parent, ScalarPropertyAccessor<?, Entry> propertyAccessor,
			IObservableValue<Entry> amazonEntry) {
		Label propertyLabel = new Label(parent, 0);
		propertyLabel.setText(propertyAccessor.getDisplayName() + ':');
		Control propertyControl = propertyAccessor.createPropertyControl2(parent, amazonEntry);
	}

	@Override
	public void setFocus() {
		// TODO Auto-generated method stub

	}

	private void setImageCode(String imageCode, Canvas canvas) {
		IStructuredSelection selection = (IStructuredSelection)viewer.getSelection();
		Object selectedElement = selection.getFirstElement();
		if (selectedElement instanceof AmazonOrderItem) {
			AmazonOrderItem selectedItem = (AmazonOrderItem)selectedElement;

			selectedItem.getEntry().setImageCode(imageCode);

			/*
			 * See http://superuser.com/questions/123911/how-to-get-the-full-
			 * image-when-the-shopping-site-like-amazon-shows-you-only-a-pa
			 * and also http://aaugh.com/imageabuse.html
			 */
			String urlString = MessageFormat
					.format("https://images-na.ssl-images-amazon.com/images/I/{0}.jpg",
							imageCode);

			String urlStringThumb = MessageFormat
					.format("https://images-eu.ssl-images-amazon.com/images/I/{0}._US40_.jpg",
							imageCode);

			try {
				URL picture = new URL(urlString);

				IBlob blob = new UrlBlob(picture);

				// We must go thru the wrapper item, do not set
				// directly on the entry, because the wrapper caches
				// the image.
				selectedItem.setImage(blob);

				canvas.redraw();

			} catch (MalformedURLException e) {
				// Should not happen so convert to an unchecked exception
				throw new RuntimeException(e);
			}

		}
	}

	private void parseItemHtml(String asin, Canvas canvas) {

		String urlString = MessageFormat
				.format("https://www.amazon.co.uk/gp/product/{0}/ref=oh_aui_detailpage_o00_s00?ie=UTF8&psc=1",
						asin);
		Document doc;
		try {
			doc = Jsoup.connect(urlString).get();

			Element element = doc.getElementById("imgTagWrapperId");			

			Elements orderLevelElements = element.getElementsByTag("img");
			assert (orderLevelElements.size() == 1);
			Element orderLevelElement = orderLevelElements.get(0);
			String srcAttr = orderLevelElement.attr("src");

			Matcher m = urlToImageCodePattern.matcher(srcAttr);
			if (m.matches()) {
				String imageCode = m.group(1);
				setImageCode(imageCode, canvas);
			}

		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
