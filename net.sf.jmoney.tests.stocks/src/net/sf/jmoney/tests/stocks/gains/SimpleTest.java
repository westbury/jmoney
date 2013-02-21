package net.sf.jmoney.tests.stocks.gains;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

import net.sf.jmoney.JMoneyPlugin;
import net.sf.jmoney.model2.Currency;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.IDatastoreManager;
import net.sf.jmoney.model2.Session;
import net.sf.jmoney.model2.Transaction;
import net.sf.jmoney.serializeddatastore.SessionManager;
import net.sf.jmoney.serializeddatastore.SimpleObjectKey;
import net.sf.jmoney.stocks.gains.CapitalGainsCalculator;
import net.sf.jmoney.stocks.gains.StockPurchaseAndSale;
import net.sf.jmoney.stocks.model.Security;
import net.sf.jmoney.stocks.model.Stock;
import net.sf.jmoney.stocks.model.StockAccount;
import net.sf.jmoney.stocks.model.StockAccountInfo;
import net.sf.jmoney.stocks.model.StockInfo;

import org.eclipse.core.runtime.IStatus;
import org.junit.Before;
import org.junit.Test;

public class SimpleTest {

	DateFormat df = new SimpleDateFormat("yyyy-M-d");

	private Currency cash;

	private Session session;

	private Stock companyA;

	private Stock companyB;

	@Test
	public void test() {
        setUp();

		StockAccount account = session.getAccountCollection().createNewElement(StockAccountInfo.getPropertySet());

		buyTransaction(account, "2012-1-1", companyA, 50, 1000);
		buyTransaction(account, "2012-1-2", companyA, 100, 1100);
		sellTransaction(account, "2012-1-3", companyA, 100, 1200);

		Collection<StockPurchaseAndSale> matchedPurchaseAndSales = new ArrayList<StockPurchaseAndSale>();
		try {
			IStatus status = CapitalGainsCalculator.exportCapitalGains(account, df.parse("2012-1-1"), df.parse("2012-1-3"), matchedPurchaseAndSales);

			assertEquals(2, matchedPurchaseAndSales.size());
			assertContains(55000, 60000, 50000, "2012-1-2", "2012-1-3", matchedPurchaseAndSales);
			assertContains(100000, 60000, 50000, "2012-1-1", "2012-1-3", matchedPurchaseAndSales);

		} catch (IOException | ParseException e) {
			fail("exception");
		}
	}

	@Before
	private void setUp() {
		IDatastoreManager sessionManager = createSession();

		session = sessionManager.getSession();

		JMoneyPlugin.initSystemCurrency(session);

		cash = session.getDefaultCurrency();

		companyA = session.getCommodityCollection().createNewElement(StockInfo.getPropertySet());
		companyA.setName("Company A");

		companyB = session.getCommodityCollection().createNewElement(StockInfo.getPropertySet());
		companyB.setName("Company B");
	}

	@Test
	public void shortSale() throws IOException, ParseException {
        setUp();

        StockAccount account = session.getAccountCollection().createNewElement(StockAccountInfo.getPropertySet());

		sellTransaction(account, "2012-1-1", companyA, 50, 1000);
		sellTransaction(account, "2012-1-2", companyA, 100, 1100);
		buyTransaction(account, "2012-1-3", companyA, 100, 1200);

		Collection<StockPurchaseAndSale> matchedPurchaseAndSales = new ArrayList<StockPurchaseAndSale>();

		IStatus status = CapitalGainsCalculator.exportCapitalGains(account, df.parse("2012-1-1"), df.parse("2012-1-3"), matchedPurchaseAndSales);

		assertEquals(2, matchedPurchaseAndSales.size());
		assertContains(55000, 60000, 50000, "2012-1-2", "2012-1-3", matchedPurchaseAndSales);
		assertContains(100000, 60000, 50000, "2012-1-1", "2012-1-3", matchedPurchaseAndSales);
	}

	/**
	 * This test covers the case where stock is transferred from
	 * one brokerage account to another.  No taxable gain or loss
	 * occurs on the transfer.  The
	 * @throws IOException
	 * @throws ParseException
	 */
	@Test
	public void transferBetweenAccounts() throws IOException, ParseException {
        setUp();

		StockAccount account1 = session.getAccountCollection().createNewElement(StockAccountInfo.getPropertySet());
		StockAccount account2 = session.getAccountCollection().createNewElement(StockAccountInfo.getPropertySet());

		buyTransaction(account1, "2012-1-1", companyA, 50, 1000);
		transferTransaction(account1, account2, "2012-1-10", companyA, 100);
		sellTransaction(account2, "2012-1-3", companyA, 100, 1200);

		Collection<StockPurchaseAndSale> matchedPurchaseAndSales = new ArrayList<StockPurchaseAndSale>();

		IStatus status = CapitalGainsCalculator.exportCapitalGains(account1, df.parse("2012-1-1"), df.parse("2012-1-20"), matchedPurchaseAndSales);

		assertEquals(2, matchedPurchaseAndSales.size());
		assertContains(55000, 60000, 50000, "2012-1-2", "2012-1-3", matchedPurchaseAndSales);
		assertContains(100000, 60000, 50000, "2012-1-1", "2012-1-3", matchedPurchaseAndSales);
	}

	private void assertContains(long basis, long proceeds, long quantity, String buyDateString,
			String sellDateString,
			Collection<StockPurchaseAndSale> matchedPurchaseAndSales) {
		try {
			Date buyDate = df.parse(buyDateString);
			Date sellDate = df.parse(sellDateString);

			for (StockPurchaseAndSale matchedPurchaseAndSale : matchedPurchaseAndSales) {
				if (matchedPurchaseAndSale.getBasis() == basis
						&& matchedPurchaseAndSale.getProceeds() == proceeds
						&& matchedPurchaseAndSale.getQuantity() == quantity
						&& matchedPurchaseAndSale.getBuyDate().equals(buyDate)
						&& matchedPurchaseAndSale.getSellDate().equals(sellDate)) {
					return;
				}
			}

			for (StockPurchaseAndSale matchedPurchaseAndSale : matchedPurchaseAndSales) {
				System.out.println("Basis " + matchedPurchaseAndSale.getBasis()
						+ ", proceeds " + matchedPurchaseAndSale.getProceeds()
						+ ", quantity " + matchedPurchaseAndSale.getQuantity()
						+ ", buy " + df.format(matchedPurchaseAndSale.getBuyDate())
						+ ", sell " + df.format(matchedPurchaseAndSale.getSellDate()));
			}
			fail("no match");
		} catch (ParseException e) {
			fail("bad date in test");
		}

	}

	private void buyTransaction(StockAccount account, String date, Security security, int quantity, long cost) {
		try {
			Transaction transaction = account.getSession().createTransaction();
			transaction.setDate(df.parse(date));

			Entry mainEntry = transaction.createEntry();
			mainEntry.setAccount(account);
			mainEntry.setCommodity(security);
			mainEntry.setAmount(quantity * 1000);

			Entry costEntry = transaction.createEntry();
			costEntry.setAccount(account);
			costEntry.setCommodity(cash);
			costEntry.setAmount(-cost * 100);
		} catch (ParseException e) {
			fail("bad date format");
		}
	}

	private void sellTransaction(StockAccount account, String date, Security security, int quantity, long proceeds) {
		try {
			Transaction transaction = account.getSession().createTransaction();
			transaction.setDate(df.parse(date));

			Entry mainEntry = transaction.createEntry();
			mainEntry.setAccount(account);
			mainEntry.setCommodity(security);
			mainEntry.setAmount(-quantity * 1000);

			Entry proceedsEntry = transaction.createEntry();
			proceedsEntry.setAccount(account);
			proceedsEntry.setCommodity(cash);
			proceedsEntry.setAmount(proceeds * 100);
		} catch (ParseException e) {
			fail("bad date format");
		}
	}

	private void transferTransaction(StockAccount sourceAccount,
			StockAccount destinationAccount, String date, Stock security, int quantity) {
		try {
			Transaction transaction = session.createTransaction();
			transaction.setDate(df.parse(date));

			Entry fromEntry = transaction.createEntry();
			fromEntry.setAccount(sourceAccount);
			fromEntry.setCommodity(security);
			fromEntry.setAmount(-quantity * 1000);

			Entry toEntry = transaction.createEntry();
			toEntry.setAccount(destinationAccount);
			toEntry.setCommodity(security);
			toEntry.setAmount(quantity * 1000);
		} catch (ParseException e) {
			fail("bad date format");
		}
	}

	public SessionManager createSession() {
		// Create a session manager that has no file (and even
		// no file format) associated with it.
		SessionManager sessionManager = new SessionManager(null, null, null);

		SimpleObjectKey sessionKey = new SimpleObjectKey(sessionManager);

		Session newSession = new Session(sessionKey, null);

		sessionKey.setObject(newSession);

		sessionManager.setSession(newSession);

		return sessionManager;
	}

}
