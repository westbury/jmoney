package net.sf.jmoney.amazon.copytext;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.WritableValue;

import amazonscraper.AmazonOrder;
import amazonscraper.AmazonShipment;
import amazonscraper.IContextUpdater;
import amazonscraper.IOrderUpdater;
import amazonscraper.IShipmentUpdater;
import net.sf.jmoney.amazon.AccountFinder;
import net.sf.jmoney.amazon.AmazonEntry;
import net.sf.jmoney.amazon.AmazonEntryInfo;
import net.sf.jmoney.importer.wizards.ImportException;
import net.sf.jmoney.model2.BankAccount;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.IDatastoreManager;
import net.sf.jmoney.model2.Session;
import net.sf.jmoney.model2.Transaction;
import net.sf.jmoney.model2.TransactionManagerForAccounts;

public class ContextUpdater implements IContextUpdater {

	private TransactionManagerForAccounts uncommittedSessionManager;

	// Session, inside transaction
	private Session session;

	private IObservableValue<BankAccount> defaultChargeAccount = new WritableValue<>();

	private AccountFinder accountFinder;

	private IDatastoreManager sessionManager;

	public ContextUpdater(IDatastoreManager committedSessionManager, TransactionManagerForAccounts uncommittedSessionManager, AccountFinder accountFinder,
			IObservableValue<BankAccount> defaultChargeAccount) {
		this.sessionManager = committedSessionManager;
		this.uncommittedSessionManager = uncommittedSessionManager;
		this.session = uncommittedSessionManager.getSession();
		this.accountFinder = accountFinder;
		this.defaultChargeAccount = defaultChargeAccount;
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
	 * @throws ImportException 
	 */
	@Override
	public AmazonOrder createAmazonOrder(String orderNumber, Date orderDate) {
		Set<AmazonEntry> entriesInOrder = lookupEntriesInOrder(orderNumber, orderDate);

		IOrderUpdater orderUpdater = new OrderUpdater(session, accountFinder, defaultChargeAccount.getValue());
		AmazonOrder order = new AmazonOrder(orderNumber, orderUpdater);
		if (entriesInOrder.isEmpty()) {
			order.setOrderDate(orderDate);
		} else {
			Transaction[] transactions = entriesInOrder.stream().map(entry -> entry.getTransaction()).distinct().toArray(Transaction[]::new);
			for (Transaction transaction : transactions) {
				IShipmentUpdater shipmentUpdater = new ShipmentUpdater(transaction, accountFinder, defaultChargeAccount.getValue());
				final AmazonShipment shipment = new AmazonShipment(order, shipmentUpdater);
				order.addShipment(shipment);
			}
		}
		return order;
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

}
