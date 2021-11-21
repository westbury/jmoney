package net.sf.jmoney.ebay.copytext;

import ebayscraper.IOrderUpdater;

import java.util.HashSet;
import java.util.Set;

import ebayscraper.IItemUpdater;
import net.sf.jmoney.ebay.AccountFinder;
import net.sf.jmoney.ebay.EbayEntry;
import net.sf.jmoney.ebay.EbayEntryInfo;
import net.sf.jmoney.importer.wizards.ImportException;
import net.sf.jmoney.model2.Account;
import net.sf.jmoney.model2.BankAccount;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.Session;
import net.sf.jmoney.model2.Transaction;

public class OrderUpdater implements IOrderUpdater {

	/** never null */
	private Transaction transaction;
	
	private AccountFinder accountFinder;
	
	/**
	 * The default charge account, which may be changed from time to time, but the
	 * value when the order is first created is the value used.
	 */
	private BankAccount defaultChargeAccount;
	
	private Set<IItemUpdater> items = new HashSet<>();

	public OrderUpdater(Transaction transaction, AccountFinder accountFinder, BankAccount defaultChargeAccount) {
		this.transaction = transaction;
		this.accountFinder = accountFinder;
		this.defaultChargeAccount = defaultChargeAccount;
	}

//	@Override
//	public IItemUpdater createNewItemUpdater() {
//		Transaction transaction = session.createTransaction();
//		return new ItemUpdater(transaction, accountFinder, defaultChargeAccount);
//	}

	@Override
	public ItemUpdater createNewItemUpdater(long itemAmount) {
		Entry entry = transaction.createEntry();
		entry.setAmount(itemAmount);
		try {
			Account defaultPurchaseAccount = accountFinder.findDefaultPurchaseAccount();
			entry.setAccount(defaultPurchaseAccount);
		} catch (ImportException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		EbayEntry ebayEntry = entry.getExtension(EbayEntryInfo.getPropertySet(), true);
		ItemUpdater itemUpdater = new ItemUpdater(ebayEntry, accountFinder);
		return itemUpdater;
	}

	@Override
	public Set<IItemUpdater> getItemUpdaters() {
		return items;
	}

	@Override
	public long getPostageAndPackaging() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setPostageAndPackaging(long postageAndPackagingAmount) {
		// TODO Auto-generated method stub
		
	}

}
