package net.sf.jmoney.amazon.copytext;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import net.sf.jmoney.amazon.AccountFinder;
import net.sf.jmoney.amazon.AmazonEntryInfo;
import net.sf.jmoney.importer.wizards.ImportException;
import net.sf.jmoney.model2.Account;
import net.sf.jmoney.model2.BankAccount;
import net.sf.jmoney.model2.CapitalAccount;
import net.sf.jmoney.model2.Currency;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.IncomeExpenseAccount;
import net.sf.jmoney.model2.Session;
import net.sf.jmoney.model2.Transaction;

public class AmazonShipment {

	private List<AmazonOrderItem> items = new ArrayList<>();

	private String expectedDate;

	private Date deliveryDate;

	/** never null */
	private Transaction transaction;
	
	/** null if there is no charge (typically if the order was covered
	 * by a gift certificate), or if the charge has not yet been
	 * determined */
	Entry chargeEntry = null;
	
	/** 
	 * null only if no p&p entry, never null if the transaction
	 * has a p&p entry.
	 */
	Entry postageAndPackagingEntry = null;

	/** 
	 * null only if no giftcard entry, never null if the transaction
	 * has a giftcard entry.
	 */
	Entry giftcardEntry = null;

	/** 
	 * non-null only if a promotional discount was applied to this shipment
	 */
	Entry promotionEntry = null;

	private BankAccount giftcardAccount;

	private IncomeExpenseAccount promotionAccount;

	private boolean chargeAmountToBeDetermined;

	/**
	 * The account to which the charge for this shipment is to be made.
	 * If the data necessary to determine the account has not yet been
	 * imported then this will be a default 'unmatched' account.  This
	 * account is never null.
	 */
	private Account chargeAccount;

	/**
	 * This form is used when no transaction exists in the session for this
	 * order.  A new transaction is created.
	 * 
	 * @param orderNumber
	 * @param transaction
	 * @throws ImportException 
	 */
	public AmazonShipment(Session session) {
		// Create a new transaction to hold these entries.
		transaction = session.createTransaction();

		Currency thisCurrency = transaction.getSession().getCurrencyForCode("GBP");

		try {
			chargeAccount = AccountFinder.findUnmatchedAccount(transaction.getSession(), thisCurrency);
			giftcardAccount = AccountFinder.findGiftcardAccount(session, thisCurrency);
			promotionAccount = AccountFinder.findMiscellaneousAccount(session, thisCurrency);
		} catch (ImportException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		
		chargeAmountToBeDetermined = true;
	}
	
	/**
	 * This form is used when a transaction already exists for this order.
	 * 
	 * @param orderNumber
	 * @param transaction
	 * @throws ImportException 
	 */
	public AmazonShipment(Transaction transaction) {
		this.transaction = transaction;

		final Session session = transaction.getSession();
		Currency thisCurrency = session.getCurrencyForCode("GBP");
		try {
			chargeAccount = AccountFinder.findUnmatchedAccount(transaction.getSession(), thisCurrency);
			giftcardAccount = AccountFinder.findGiftcardAccount(session, thisCurrency);
			promotionAccount = AccountFinder.findMiscellaneousAccount(session, thisCurrency);
		} catch (ImportException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		
		for (Entry entry : transaction.getEntryCollection()) {
			if (isChargeEntry(entry)) {
				chargeEntry = entry;
			} else if (isPostageAndPackagingEntry(entry)) {
				postageAndPackagingEntry = entry;
			} else if (isGiftcardEntry(entry)) {
				giftcardEntry = entry;
			} else if (isPromotionEntry(entry)) {
				promotionEntry = entry;
			} else {
				items.add(new AmazonOrderItem(entry.getExtension(AmazonEntryInfo.getPropertySet(), true)));
			}
		}
		
		chargeAmountToBeDetermined = false;
	}

	private boolean isChargeEntry(Entry entry) {
		// TODO use account returned from finder.
		return (entry.getAccount() instanceof CapitalAccount && !isGiftcardEntry(entry) && !isPromotionEntry(entry))  
				|| entry.getAccount().getName().equals("Amazon unmatched (UK)");
	}

	private boolean isPostageAndPackagingEntry(Entry entry) {
		// TODO use finder class to get p&p account
		return entry.getAccount().getName().equals("Postage and Packaging (UK)");
	}

	private boolean isGiftcardEntry(Entry entry) {
		return entry.getAccount() == giftcardAccount;
	}

	private boolean isPromotionEntry(Entry entry) {
		// TODO is this test adequate given that we may be using
		// a general purpose miscellanous account for the promotional discounts?
		return entry.getAccount() == promotionAccount;
	}

	public void addItem(AmazonOrderItem item) {
		items.add(item);
	}
	
	public List<AmazonOrderItem> getItems() {
		return items;
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
			
			// And don't forget we must keep the transaction balanced.
			if (!chargeAmountToBeDetermined) {
				setChargeAmount(getChargeAmount() - postageAndPackagingAmount);
			}
		}
	}

	/**
	 * 
	 * 
	 * @return the amount charged to the charge account or null if
	 * 				this has not been determined
	 */
	public Long getChargeAmount() {
		if (chargeAmountToBeDetermined) {
			assert chargeEntry == null;
			return null;
		} else {
			return (chargeEntry == null) ? 0 : chargeEntry.getAmount();
		}
	}

	void setChargeAmount(long amount) {
		if (amount == 0) {
			if (chargeEntry != null) {
				transaction.deleteEntry(chargeEntry);
				chargeEntry = null;
			}
		} else {
			if (chargeEntry == null) {
				chargeEntry = transaction.createEntry();
				chargeEntry.setAccount(chargeAccount);
			}
			chargeEntry.setAmount(amount);
		}
		
		chargeAmountToBeDetermined = false;
	}

	public void setGiftcardAmount(long giftcardAmount) {
		if (giftcardEntry != null) {
			if (giftcardEntry.getAmount() != -giftcardAmount) {
				throw new RuntimeException("giftcard amounts mismatch");
			}
		} else {
			giftcardEntry = transaction.createEntry();
			giftcardEntry.setAccount(giftcardAccount);
			giftcardEntry.setMemo("Amazon");
			giftcardEntry.setAmount(-giftcardAmount);
			
			// And don't forget we must keep the transaction balanced.
			if (!chargeAmountToBeDetermined) {
				setChargeAmount(getChargeAmount() + giftcardAmount);
			}
		}
	}

	public void setPromotionAmount(long promotionAmount) {
		if (promotionEntry != null) {
			if (promotionEntry.getAmount() != -promotionAmount) {
				throw new RuntimeException("promotion amounts mismatch");
			}
		} else {
			promotionEntry = transaction.createEntry();
			promotionEntry.setAccount(promotionAccount);
			promotionEntry.setMemo("Amazon promotional discount");
			promotionEntry.setAmount(-promotionAmount);
			
			// And don't forget we must keep the transaction balanced.
			if (!chargeAmountToBeDetermined) {
				setChargeAmount(getChargeAmount() + promotionAmount);
			}
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
		
		if (this.chargeAccount != unmatchedAccount && this.chargeAccount != chargeAccount) {
			throw new RuntimeException("charge accounts mismatch");
		}
		this.chargeAccount = chargeAccount;
		
		if (chargeEntry != null) {
			chargeEntry.setAccount(chargeAccount);
		}
	}

	public Transaction getTransaction() {
		return transaction;
	}

}
