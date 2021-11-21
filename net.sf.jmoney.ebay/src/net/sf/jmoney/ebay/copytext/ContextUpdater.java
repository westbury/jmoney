package net.sf.jmoney.ebay.copytext;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.WritableValue;

import analyzer.EbayOrder;
import analyzer.EbayOrderItem;
import ebayscraper.IContextUpdater;
import ebayscraper.IItemUpdater;
import ebayscraper.IOrderUpdater;
import net.sf.jmoney.ebay.AccountFinder;
import net.sf.jmoney.ebay.EbayEntry;
import net.sf.jmoney.ebay.EbayEntryInfo;
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
	 * Creates a new EbayOrder object for an order that is not already in our view.
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
	public EbayOrder createEbayOrder(String orderNumber, Date orderDate) {
		Set<EbayEntry> entriesInOrder = lookupEntriesInOrder(orderNumber, orderDate);

		Transaction transaction;

		if (entriesInOrder.isEmpty()) {
			transaction = session.createTransaction();
		} else {
			Transaction[] transactions = entriesInOrder.stream().map(entry -> entry.getTransaction()).distinct().toArray(Transaction[]::new);
			if (transactions.length > 1) {
				throw new RuntimeException("There are multiple transactions with entries with an order id of " + orderNumber + ".  Please sort that out before importing.");
			}
			transaction = transactions[0];
		}
		
		IOrderUpdater orderUpdater = new OrderUpdater(transaction, accountFinder, defaultChargeAccount.getValue());
		EbayOrder order = new EbayOrder(orderNumber, orderUpdater);

		// Is this correct?  It must be here for new transtion, but what about an existing one?
		order.setOrderDate(orderDate);

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
	private Set<EbayEntry> lookupEntriesInOrder(String orderNumber, Date orderDate) {
		/*
		 * The getEntries method is not supported in an uncommitted data manager.  Therefore we do
		 * the search on the committed data manager and copy the results into the uncommitted data manager.
		 * This is ok because we know there will not be any changes to this order in the uncommitted data manager,
		 * nor will this order be in an uncommitted state.
		 */

		List<Entry> entriesInDateRange = sessionManager.getEntries(orderDate, orderDate, null, null);

		Set<EbayEntry> entriesInOrder = new HashSet<>();
		for (Entry entry : entriesInDateRange) {
			String thisOrderNumber = EbayEntryInfo.getOrderNumberAccessor().getValue(entry);
			if (orderNumber.equals(thisOrderNumber)) {
				Entry entryInTransaction = uncommittedSessionManager.getCopyInTransaction(entry);
				EbayEntry ebayEntry = entryInTransaction.getExtension(EbayEntryInfo.getPropertySet(), false);
				entriesInOrder.add(ebayEntry);
			}
		}
		return entriesInOrder;
	}

}
