package net.sf.jmoney.property.pages;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Iterator;

import net.sf.jmoney.entrytable.EntryData;
import net.sf.jmoney.entrytable.InvalidUserEntryException;
import net.sf.jmoney.isolation.DataManager;
import net.sf.jmoney.isolation.TransactionManager;
import net.sf.jmoney.model2.Account;
import net.sf.jmoney.model2.Commodity;
import net.sf.jmoney.model2.Currency;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.EntryInfo;
import net.sf.jmoney.model2.ExtendableObject;
import net.sf.jmoney.model2.ScalarPropertyAccessor;
import net.sf.jmoney.model2.Transaction.EntryCollection;
import net.sf.jmoney.property.model.RealProperty;
import net.sf.jmoney.property.model.RealPropertyAccount;
import net.sf.jmoney.property.model.RealPropertyEntry;
import net.sf.jmoney.property.model.RealPropertyEntryInfo;
import net.sf.jmoney.property.pages.StockEntryRowControl.TransactionType;

public class StockEntryData extends EntryData {

	private RealPropertyAccount account;
	
	private TransactionType transactionType;

	private Entry mainEntry;
	private Entry purchaseOrSaleEntry;

	private boolean unknownTransactionType;

	public StockEntryData(Entry entry, DataManager dataManager) {
		super(entry, dataManager);


		// Note that there are two versions of this object for every row.
		// One contains the committed entry and the other contains the entry
		// being edited inside a transaction.  If this is the new entry row
		// and is the committed version then entry will be null, so we can't
		// analyze it and we can't determine the account.
		
		// TODO We should consider merging the two instances into one.
		
		// TODO Call this on-demand.
		if (entry != null) {
			account = (RealPropertyAccount)entry.getAccount();
			analyzeTransaction();
		}
	}

	private void analyzeTransaction() {
		/*
		 * Analyze the transaction to see which type of transaction this is.
		 */

		/*
		 * If just one entry then this is not a valid transaction, so must be
		 * a new transaction.  We set the transaction type to null which means
		 * no selection will be set in the transaction type combo.
		 */
		if (getEntry().getTransaction().getEntryCollection().size() == 1) {
			mainEntry = getEntry().getTransaction().getEntryCollection().iterator().next();
			transactionType = null;
		} else {

			for (Entry entry: getEntry().getTransaction().getEntryCollection()) {
				if (entry.getAccount() == account) {
					if (entry.getCommodityInternal() instanceof RealProperty) {
						if (purchaseOrSaleEntry != null) {
							unknownTransactionType = true;
						}
						purchaseOrSaleEntry = entry;
					} else if (entry.getCommodityInternal() instanceof Currency) {  //TODO: check for actual currency of account.
						if (mainEntry != null) {
							unknownTransactionType = true;
						}
						mainEntry = entry;
					}
				} else {
					unknownTransactionType = true;
				}
			}

			if (unknownTransactionType) {
				transactionType = TransactionType.Other;
			} else if (purchaseOrSaleEntry != null) {
				if (purchaseOrSaleEntry.getAmount() >= 0) {
					transactionType = TransactionType.Buy;
				} else {
					transactionType = TransactionType.Sell;
				}
			} else {
				transactionType = TransactionType.Other;
			}
		}
	}

	public void forceTransactionToBuy() {
		forceTransactionToBuyOrSell(TransactionType.Buy);
	}
	
	public void forceTransactionToSell() {
		forceTransactionToBuyOrSell(TransactionType.Sell);
	}
	
	private void forceTransactionToBuyOrSell(TransactionType transactionType) {
		this.transactionType = transactionType;
			
		EntryCollection entries = getEntry().getTransaction().getEntryCollection();
		for (Iterator<Entry> iter = entries.iterator(); iter.hasNext(); ) {
			Entry entry = iter.next();
			if (entry != mainEntry
					&& entry != purchaseOrSaleEntry) {
				iter.remove();
			}
		}
		
		if (purchaseOrSaleEntry == null) {
			purchaseOrSaleEntry = entries.createEntry();
			purchaseOrSaleEntry.setAccount(account);
			RealPropertyEntry stockEntry = purchaseOrSaleEntry.getExtension(RealPropertyEntryInfo.getPropertySet(), true);
			
			/*
			 * If this was an transaction connected with a stock but did not
			 * involve an entry that changed the amount of a stock (e.g. a
			 * dividend entry) then we want to use that stock as the stock that
			 * is now being bought or sold.  Otherwise we set the commodity to
			 * null (we can't leave it as a currency because that would result
			 * in an invalid transaction).
			 */
			stockEntry.setCommodity(stockEntry.getSecurity());
		}

		// Commission, tax 1, and tax 2 entries may be null in this transaction type.
		// They are created when needed if non-zero amounts are entered.

		// TODO: What is our strategy on changing values to keep
		// the transaction balanced.  Quicken has a dialog box that
		// asks the user what to adjust (with a 'recommended' choice
		// that in my experience is never the correct choice!).
		
//		dividendEntry.setAmount(-mainEntry.getAmount());
	}

	public void forceTransactionToCustom() {
		transactionType = TransactionType.Other;

		/*
		 * This method is not so much a 'force' as a 'set'.  The other 'force' methods
		 * have to modify the transaction, including the lose of information, in order
		 * to transform the transaction to the required type.  This method does not need
		 * to change the transaction data at all.  It does adjust the UI to give the user
		 * full flexibility.
		 * 
		 * Note that the user may edit the transaction so that it matches one of the
		 * types (buy, sell, dividend etc).  In that case, the transaction will appear
		 * as that type, not as a custom type, if it is saved and re-loaded.
		 */
		
		// Must be at least one entry
		EntryCollection entries = getEntry().getTransaction().getEntryCollection();
		if (entries.size() == 1) {
			entries.createEntry();
		}

		/*
		 * Forget the special entries. It may be that these would be useful to
		 * keep in case the user decides to go back to one of the set
		 * transaction types. However, the user may edit these entries, or
		 * delete them, and it is too complicated to worry about the
		 * consequences.
		 */
		purchaseOrSaleEntry = null;
	}

	public TransactionType getTransactionType() {
		return transactionType;
	}

	public boolean isPurchaseOrSale() {
		return transactionType == TransactionType.Buy
		|| transactionType == TransactionType.Sell;
	}

	/**
	 * @return the entry in the transaction that represents the gain or loss in
	 *         the number of shares, or null if this is not a purchase or sale
	 *         transaction
	 */
	public Entry getPurchaseOrSaleEntry() {
		return purchaseOrSaleEntry;
	}

	/*
	 * The price is calculated, not stored in the model. This method
	 * calculates the share price from the data in the model.  It does
	 * this by adding up all the cash entries to get the gross proceeds
	 * or cost and then dividing by the number of shares.
	 * 
	 * @return the calculated price to four decimal places, or null
	 * 		if the price cannot be calculated (e.g. if the share quantity
	 * 		is zero)
	 */
	public BigDecimal calculatePrice() {
		assert(isPurchaseOrSale());

		BigDecimal totalShares = BigDecimal.valueOf(purchaseOrSaleEntry.getAmount())
				.movePointLeft(3);

		long totalCash = 0;
		for (Entry eachEntry: getEntry().getTransaction().getEntryCollection()) {
			if (eachEntry.getCommodityInternal() instanceof Currency) {
				totalCash += eachEntry.getAmount();
			}
		}
		
		BigDecimal price = null;
		if (totalCash != 0 && totalShares.compareTo(BigDecimal.ZERO) != 0) {
			/*
			 * Either we gain cash and lose stock, or we lose cash and gain
			 * stock. Hence we need to negate to get a positive value.
			 */
			price = BigDecimal.valueOf(-totalCash).movePointLeft(2).divide(totalShares, 4, RoundingMode.HALF_UP);
		}
		
		return price;
	}

	public void specificValidation() throws InvalidUserEntryException {
		if (transactionType == null) {
			throw new InvalidUserEntryException("No transaction type selected.", null);
		}
		
		/*
		 * Check for zero amounts. Some fields may be zeroes (for example, commissions and
		 * withheld taxes), others may not (for example, quantity of stock sold).
		 * 
		 * We do leave entries with zero amounts.  This makes the code simpler
		 * because the transaction is already set up for the transaction type,
		 * and it is easier to determine the transaction type.  
		 * 
		 * It is possible that the total proceeds of a sale are zero.  Anyone who
		 * has disposed of shares in a sub-prime mortgage company in order to
		 * claim the capital loss will know that the commission may equal the sale
		 * price.  It is probably good that the transaction still shows up in
		 * the cash entries list for the account.
		 */
		switch (transactionType) {
		case Buy:
		case Sell:
			if (purchaseOrSaleEntry.getAmount() == 0) {
				throw new InvalidUserEntryException("The quantity of stock in a purchase or sale cannot be zero.", null);
			}
			break;
		case Other:
			// We don't allow any amounts to be zero except the listed entry
			// (the listed entry is used to ensure a transaction appears in this
			// list even if the transaction does not result in a change in the cash
			// balance).
			Entry mainEntry = getEntry();
			if (mainEntry.getTransaction().getEntryCollection().size() == 1) {
				// TODO: create another entry when 'other' selected and don't allow it to be
				// deleted, thus this check is not necessary.
				// TODO: should not be 'other' when no transaction has been selected
				// (should be null)
				throw new InvalidUserEntryException("Must have another entry.", null);
			}
			for (Entry entry : mainEntry.getTransaction().getEntryCollection()) {
				if (entry != mainEntry) {
					if (entry.getAmount() == 0) {
						throw new InvalidUserEntryException("The amount of an entry in this transaction cannot be zero.", null);
					}
				}
			}
			break;
		}
	}

	/**
	 * Copies data from the given object into this object.  This method is used
	 * only when duplicating a transaction.  This object will be the object for the 'new entry'
	 * row that appears at the bottom on the transaction table.
	 */
	@Override
	public void copyFrom(EntryData sourceEntryData) {
//		StockEntryData sourceEntryData = ()sourceEntryData2;
		
		Entry selectedEntry = sourceEntryData.getEntry();
		
		Entry newEntry = getEntry();
		TransactionManager transactionManager = (TransactionManager)newEntry.getDataManager();
		
//		newEntry.setMemo(selectedEntry.getMemo());
//		newEntry.setAmount(selectedEntry.getAmount());

		/*
		 * Copy all values that are numbers, flags, text, or references to accounts or commodities.
		 * We do not copy dates or statement numbers.
		 */
		for (ScalarPropertyAccessor accessor : EntryInfo.getPropertySet().getScalarProperties3()) {
			Object value = selectedEntry.getPropertyValue(accessor);
			if (value instanceof Integer
					|| value instanceof Long
					|| value instanceof Boolean
					|| value instanceof String) {
				newEntry.setPropertyValue(accessor, value);
			}
			if (value instanceof Commodity
					|| value instanceof Account) {
				newEntry.setPropertyValue(accessor, transactionManager.getCopyInTransaction((ExtendableObject)value));
			}
		}
		
		/*
		 * In the bank account entries, the new entry row will always have a second entry created.
		 * In other enty types such as a stock entry, the new entry row will have only one row.
		 */
		Entry thisEntry = getSplitEntries().isEmpty()
		? null : getOtherEntry();

		for (Entry origEntry: sourceEntryData.getSplitEntries()) {
			if (thisEntry == null) {
				thisEntry = getEntry().getTransaction().createEntry();
			}
//			thisEntry.setAccount(transactionManager.getCopyInTransaction(origEntry.getAccount()));
//			thisEntry.setMemo(origEntry.getMemo());
//			thisEntry.setAmount(origEntry.getAmount());
			
			/*
			 * Copy all values that are numbers, flags, text, or references to accounts or commodities.
			 * We do not copy dates or statement numbers.
			 */
			for (ScalarPropertyAccessor accessor : EntryInfo.getPropertySet().getScalarProperties3()) {
				Object value = origEntry.getPropertyValue(accessor);
				if (value instanceof Integer
						|| value instanceof Long
						|| value instanceof Boolean
						|| value instanceof String) {
				thisEntry.setPropertyValue(accessor, value);
				}
				if (value instanceof Commodity
						|| value instanceof Account) {
				thisEntry.setPropertyValue(accessor, transactionManager.getCopyInTransaction((ExtendableObject)value));
				}
			}
			thisEntry = null;
		}

		// Hack, because analyze assumes this has not yet been set.
		mainEntry = null;
		
		analyzeTransaction();
	}
}
