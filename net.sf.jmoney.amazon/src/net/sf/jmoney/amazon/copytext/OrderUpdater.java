package net.sf.jmoney.amazon.copytext;

import amazonscraper.IOrderUpdater;
import amazonscraper.IShipmentUpdater;
import net.sf.jmoney.amazon.AccountFinder;
import net.sf.jmoney.model2.BankAccount;
import net.sf.jmoney.model2.Session;
import net.sf.jmoney.model2.Transaction;

public class OrderUpdater implements IOrderUpdater {

	private Session session;
	
	private AccountFinder accountFinder;
	
	/**
	 * The default charge account, which may be changed from time to time, but the
	 * value when the order is first created is the value used.
	 */
	private BankAccount defaultChargeAccount;
	
	public OrderUpdater(Session session, AccountFinder accountFinder, BankAccount defaultChargeAccount) {
		this.session = session;
		this.accountFinder = accountFinder;
		this.defaultChargeAccount = defaultChargeAccount;
	}

	@Override
	public IShipmentUpdater createNewShipmentUpdater() {
		Transaction transaction = session.createTransaction();
		return new ShipmentUpdater(transaction, accountFinder, defaultChargeAccount);
	}

}
