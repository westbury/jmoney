package net.sf.jmoney.amazon.copytext;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import net.sf.jmoney.amazon.AccountFinder;
import net.sf.jmoney.amazon.AmazonEntryInfo;
import net.sf.jmoney.importer.MatchingEntryFinder;
import net.sf.jmoney.importer.wizards.ImportException;
import net.sf.jmoney.isolation.ReferenceViolationException;
import net.sf.jmoney.model2.Account;
import net.sf.jmoney.model2.BankAccount;
import net.sf.jmoney.model2.CapitalAccount;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.EntryInfo;
import net.sf.jmoney.model2.IncomeExpenseAccount;
import net.sf.jmoney.model2.ScalarPropertyAccessor;
import net.sf.jmoney.model2.Session;
import net.sf.jmoney.model2.Transaction;

public class AmazonShipment {

	private List<AmazonOrderItem> items = new ArrayList<>();

	private String expectedDate;

	private Date deliveryDate;

	/** never null */
	private Transaction transaction;
	
	/**
	 * null if there is no charge (typically if the order was covered by a gift
	 * certificate), or if the charge amount has not yet been determined, or if
	 * the charge entry has been matched to a bank import (so on a statement or
	 * has a bank id code).
	 * <P>
	 * In other words, if this is set then it is a tentative entry that was
	 * created to balance the transaction. Such entries are allowed to be
	 * modified more.
	 * <P>
	 * Note that a charge entry may be tentative even if the transaction has been
	 * committed.
	 */
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

	private IncomeExpenseAccount postageAndPackagingAccount;

	private IncomeExpenseAccount unmatchedAccount;

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

	private AmazonOrder order;

	boolean returned;

	public boolean overseas = false;

	/**
	 * This form is used when no transaction exists in the session for this
	 * order.  A new transaction is created.
	 * 
	 * @param orderNumber
	 * @param transaction
	 * @throws ImportException 
	 */
	public AmazonShipment(AmazonOrder order, Session session, AccountFinder accountFinder) throws ImportException {
		this.order = order;
		
		// Create a new transaction to hold these entries.
		transaction = session.createTransaction();

		postageAndPackagingAccount = accountFinder.findPostageAndPackagingAccount();
		unmatchedAccount = accountFinder.findUnmatchedAccount();
		chargeAccount = accountFinder.findUnmatchedAccount();
		giftcardAccount = accountFinder.findGiftcardAccount();
		promotionAccount = accountFinder.findMiscellaneousAccount();

		chargeAmountToBeDetermined = true;
	}
	
	/**
	 * This form is used when a transaction already exists for this order.
	 * 
	 * @param orderNumber
	 * @param transaction
	 * @throws ImportException 
	 */
	public AmazonShipment(AmazonOrder order, Transaction transaction, AccountFinder accountFinder) throws ImportException {
		this.order = order;
		this.transaction = transaction;

		postageAndPackagingAccount = accountFinder.findPostageAndPackagingAccount();
		unmatchedAccount = accountFinder.findUnmatchedAccount();
		chargeAccount = accountFinder.findUnmatchedAccount();
		giftcardAccount = accountFinder.findGiftcardAccount();
		promotionAccount = accountFinder.findMiscellaneousAccount();
		
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
			
			matchChargeEntry();
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
		if (this.chargeAccount != unmatchedAccount 
				&& this.chargeAccount != chargeAccount 
				&& (chargeEntry.getExtension(net.sf.jmoney.reconciliation.ReconciliationEntryInfo.getPropertySet(), true).getStatement() != null || chargeEntry.getExtension(net.sf.jmoney.importer.model.ReconciliationEntryInfo.getPropertySet(), true).getUniqueId() != null)) {
			throw new RuntimeException("charge accounts mismatch, or changing charge account for entry that is reconciled.");
		}
		this.chargeAccount = chargeAccount;
		
		if (chargeEntry != null) {
			chargeEntry.setAccount(chargeAccount);
			
			matchChargeEntry();
		}
	}

	/**
	 * If a charge entry (amount has been determined), and an account set, then
	 * we auto-match now.
	 * <P>
	 * If a piece of information is later changed then this will be allowed provided
	 * no auto-match had occurred and the charge account entry has no statement, no
	 * unique id etc.  We need to define this a little better.
	 */
	private void matchChargeEntry() {
		/*
		 * Auto-match the new entry in the charge account the same way that any other
		 * entry would be auto-matched.  This combines the entry if the entry already exists in the
		 * charge account (typically because transactions have been downloaded from the bank and imported).
		 *
		 * An entry in the charge account has already been matched to an
		 * Amazon order if it has an order id set.  This matcher will not return
		 * entries that have already been matched.
		 *
		 * Although we have already eliminated orders that have already been imported,
		 * this test ensures we don't mess up when more than one order can match to the
		 * same debit in the charge account.  This is not likely but two orders of the same
		 * amount and the same or very close dates may cause this.
		 *
		 * Note that we search ten days ahead for a matching entry in the charge account.
		 * Although Amazon usually charge on the day of shipment, we don't actually know the
		 * shipment date, we have just the order and the delivery date.  We go forward ten days
		 * from the order date though it may be better to search from the order date to the delivery
		 * date or a few days after the delivery date.
		 */
		MatchingEntryFinder matchFinder = new MatchingEntryFinder() {
			@Override
			protected boolean doNotConsiderEntryForMatch(Entry entry) {
				return false;
			}
		};
		Entry matchedEntryInUnmatchedAccount = matchFinder.findMatch(unmatchedAccount, -chargeEntry.getAmount(), order.getOrderDate(), 10, null);

		/*
		 * Create an entry for the amount charged to the charge account.
		 */
		if (matchedEntryInUnmatchedAccount == null) {
			// Nothing to do here because we are not auto-matching.
		} else {

			/*
			 * It is possible there are three or more entries in the target transaction.
			 * This could happen because sometimes Amazon pay a refund in multiple payments
			 * and the user may already have combined these payments into a single transaction
			 * for the purpose of enabling the matching of the refund amount.
			 * 
			 * In any case there is really no reason to touch any of the other entries in the
			 * target transaction.
			 */
			Entry sourceEntry = chargeEntry;

			if (matchedEntryInUnmatchedAccount.getAmount() != -sourceEntry.getAmount()) {
				throw new RuntimeException("Something wrong");
			}
			
			Transaction sourceTransaction = sourceEntry.getTransaction();
			Transaction targetTransaction = matchedEntryInUnmatchedAccount.getTransaction();

			// Delete the 'unmatched' entry in the target transaction.
			targetTransaction.getEntryCollection().deleteEntry(matchedEntryInUnmatchedAccount);

			/*
			 * Set the transaction date to be the earlier of the two dates. If the
			 * unreconciled entry was manually entered then that date is likely to
			 * be the correct date of the transaction. However, it is possible that
			 * this transaction involves more than one account that can be imported
			 * (such as a transfer). In such a situation, the transaction date
			 * should be set to the earliest date.
			 */
			Date sourceDate = sourceTransaction.getDate();
			Date targetDate = targetTransaction.getDate();
			if (sourceDate.compareTo(targetDate) < 0) {
				targetTransaction.setDate(sourceDate);
			}

			/*
			 * All other properties are taken from the target transaction only if
			 * the property is null in the source transaction.
			 */
			// No, nothing to copy for the charge account entry or any other entries.
//			for (ScalarPropertyAccessor<?,? super Entry> propertyAccessor: EntryInfo.getPropertySet().getScalarProperties3()) {
//				copyPropertyConditionally(propertyAccessor, sourceEntry, targetEntry);
//			}

			/*
			 * Re-parent all the other entries from the source to the target.
			 */
			for (Entry entry: sourceTransaction.getEntryCollection()) {
				if (entry != sourceEntry) {
					// Cannot currently move within a transaction, so copy for time being.
//					targetTransaction.getEntryCollection().moveElement(entry);

					Entry targetOtherEntry = targetTransaction.createEntry();
					for (ScalarPropertyAccessor<?,? super Entry> propertyAccessor: EntryInfo.getPropertySet().getScalarProperties3()) {
						copyPropertyForcibly(propertyAccessor, entry, targetOtherEntry);
					}
				}
			}

			// TODO: update this object to point to the new entries.
			// What do we do about the charge account???  There may not even be
			// a single charge entry.  Basically once we have matched we just cannot
			// change the charge account at all.
		
			/*
			 * Having copied the relevant properties from the source entry, we now
			 * delete the entire source transaction.
			 */
			try {
				sourceTransaction.getSession().deleteTransaction(sourceTransaction);
			} catch (ReferenceViolationException e) {
				/*
				 * Neither transactions nor entries or any other object type
				 * contained in a transaction can have references to them. Therefore
				 * this exception should not happen. It is possible that third-party
				 * plug-ins might extend the model in a way that could cause this
				 * exception, in which case we probably will need to think about how
				 * we can be more user-friendly.
				 */
				throw new RuntimeException("This is an unlikely error and should not happen unless plug-ins are doing something complicated.", e);
			}

			
			
			
		}
	}

	// Copied from StatementSection
	/**
	 * Helper method to copy a property from the target entry to the source entry if the
	 * property is null in the source entry but not null in the target entry.
	 */
	private <V> void copyPropertyConditionally(ScalarPropertyAccessor<V,? super Entry> propertyAccessor, Entry sourceAccount, Entry targetAccount) {
		V targetValue = propertyAccessor.getValue(targetAccount);
		V sourceValue = propertyAccessor.getValue(sourceAccount);
		if (sourceValue != null) {
			if (targetValue == null
					|| !doesImportedValueHavePriority(propertyAccessor)) {
				propertyAccessor.setValue(targetAccount, sourceValue);
			}
		}
	}

	private <V> void copyPropertyForcibly(ScalarPropertyAccessor<V,? super Entry> propertyAccessor, Entry sourceEntry, Entry targetEntry) {
		V sourceValue = propertyAccessor.getValue(sourceEntry);
		propertyAccessor.setValue(targetEntry, sourceValue);
	}

	/**
	 * Properties for which the values from the bank are used in preference to
	 * values manually entered.  For all other properties, manually entered values
	 * take precedence.  This is used when merging transactions.
	 */
	static private boolean doesImportedValueHavePriority(ScalarPropertyAccessor propertyAccessor) {
		return propertyAccessor == EntryInfo.getValutaAccessor()
		|| propertyAccessor == EntryInfo.getAmountAccessor()
		|| propertyAccessor == EntryInfo.getCheckAccessor();
	}

	
	
	
	public Transaction getTransaction() {
		return transaction;
	}

	public void setReturned(boolean returned) {
		this.returned = returned;
	}

}
