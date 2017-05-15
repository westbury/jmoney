package net.sf.jmoney.amazon.copytext;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;

import net.sf.jmoney.amazon.AccountFinder;
import net.sf.jmoney.amazon.AmazonEntry;
import net.sf.jmoney.amazon.AmazonEntryInfo;
import net.sf.jmoney.importer.wizards.ImportException;
import net.sf.jmoney.model2.Account;
import net.sf.jmoney.model2.CapitalAccount;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.IncomeExpenseAccount;
import net.sf.jmoney.model2.Session;
import net.sf.jmoney.model2.Transaction;

public class AmazonOrder {

	private String orderNumber;

	private List<AmazonOrderItem> items = new ArrayList<>();

	private Date orderDate;
	
	private long orderTotal;

	private String expectedDate;

	private Date deliveryDate;

	/** never null */
	private Transaction transaction;
	
	/** never null */
	private Entry chargeEntry;
	
	/** 
	 * null only if no p&p entry, never null if the transaction
	 * has a p&p entry.
	 */
	private Entry postageAndPackagingEntry = null;

	/**
	 * This form is used when no transaction exists in the session for this
	 * order.  A new transaction is created.
	 * 
	 * @param orderNumber
	 * @param transaction
	 */
	public AmazonOrder(String orderNumber, Session session) {
		this.orderNumber = orderNumber;
		
		// Create a new transaction to hold these entries.
		transaction = session.createTransaction();
		
		this.chargeEntry = transaction.createEntry();
		IncomeExpenseAccount unmatchedAccount;
		try {
			unmatchedAccount = AccountFinder.findUnmatchedAccount(transaction.getSession(), transaction.getSession().getCurrencyForCode("GBP"));
		} catch (ImportException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		chargeEntry.setAccount(unmatchedAccount);
	}
	
	/**
	 * This form is used when a transaction already exists for this order.
	 * 
	 * @param orderNumber
	 * @param transaction
	 */
	public AmazonOrder(String orderNumber, Transaction transaction) {
		this.orderNumber = orderNumber;
		this.transaction = transaction;
		
		for (Entry entry : transaction.getEntryCollection()) {
			if (isChargeEntry(entry)) {
				chargeEntry = entry;
			} else if (isPostageAndPackagingEntry(entry)) {
				postageAndPackagingEntry = entry;
			} else {
				items.add(new AmazonOrderItem(entry.getExtension(AmazonEntryInfo.getPropertySet(), true)));
			}
		}
	}

	private boolean isChargeEntry(Entry entry) {
		return entry.getAccount() instanceof CapitalAccount;
	}

	private boolean isPostageAndPackagingEntry(Entry entry) {
		return entry.getAccount().getName().contains("postage");
//		Stream<Entry> matches = items.filter(item -> item.getAmount() == postageAndPackaging);
//		if (matches.count() > 1) {
//			matches = matches.filter(item -> item.getMemo().equals("postage & packaging"));
//		}
	}

	public void addItem(AmazonOrderItem item) {
		items.add(item);
	}
	
	public String getOrderNumber() {
		return orderNumber;
	}

	public List<AmazonOrderItem> getItems() {
		return items;
	}

	public void setOrderDate(Date orderDate) {
		// TODO check same if already set
		this.orderDate = orderDate;
		transaction.setDate(orderDate);
	}

	public Date getOrderDate() {
		return orderDate;
	}

	public void setOrderTotal(String orderTotalAsString) {
		this.orderTotal = new BigDecimal(orderTotalAsString).scaleByPowerOfTen(2).longValueExact();
		
		chargeEntry.setAmount(-orderTotal);
	}

	public long getOrderTotal() {
		return orderTotal;
	}

	public void setExpectedDate(String expectedDate) {
		this.expectedDate = expectedDate;
	}

	public String getExpectedDate() {
		return expectedDate;
	}

	public void setDeliveryDate(Date deliveryDate) {
		this.deliveryDate = deliveryDate;
	}

	public Date getDeliveryDate() {
		return deliveryDate;
	}

	public Entry getChargeEntry() {
		return chargeEntry;
	}

	public AmazonOrderItem createNewItem(String description, long itemAmount) {
		IncomeExpenseAccount unmatchedAccount;
		try {
			unmatchedAccount = AccountFinder.findDefaultPurchaseAccount(transaction.getSession(), transaction.getSession().getCurrencyForCode("GBP"));
		} catch (ImportException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		
		Entry entry = transaction.createEntry();
		entry.setAmount(itemAmount);
		entry.setAccount(unmatchedAccount);
		AmazonEntry amazonEntry = entry.getExtension(AmazonEntryInfo.getPropertySet(), true);
		AmazonOrderItem item = new AmazonOrderItem(amazonEntry);
		item.getEntry().setMemo(description);
		item.getEntry().setOrderId(orderNumber);
		items.add(item);
		return item;
	}

	public void setPostageAndPackaging(long postageAndPackagingAmount) {
		IncomeExpenseAccount postageAndPackagingAccount;
		try {
			postageAndPackagingAccount = AccountFinder.findPostageAndPackagingAccount(transaction.getSession(), transaction.getSession().getCurrencyForCode("GBP"));
		} catch (ImportException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		
		if (postageAndPackagingEntry != null) {
			if (postageAndPackagingEntry.getAmount() != postageAndPackagingAmount) {
				throw new RuntimeException("p&p amounts mismatch");
			}
		} else {
			postageAndPackagingEntry = transaction.createEntry();
			postageAndPackagingEntry.setAccount(postageAndPackagingAccount);
			postageAndPackagingEntry.setMemo("Amazon");
			postageAndPackagingEntry.setAmount(postageAndPackagingAmount);
		}
	}

	public void setChargeAccount(CapitalAccount chargeAccount) {
		IncomeExpenseAccount unmatchedAccount;
		try {
			unmatchedAccount = AccountFinder.findUnmatchedAccount(transaction.getSession(), transaction.getSession().getCurrencyForCode("GBP"));
		} catch (ImportException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		
		if (chargeEntry.getAccount() != unmatchedAccount && chargeEntry.getAccount() != chargeAccount) {
			throw new RuntimeException("charge accounts mismatch");
		}
		chargeEntry.setAccount(chargeAccount);
	}

}
