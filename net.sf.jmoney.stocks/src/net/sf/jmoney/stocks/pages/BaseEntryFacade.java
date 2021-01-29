package net.sf.jmoney.stocks.pages;

import org.eclipse.core.databinding.observable.value.IObservableValue;

import net.sf.jmoney.entrytable.EntryFacade;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.Transaction;
import net.sf.jmoney.stocks.model.Security;
import net.sf.jmoney.stocks.pages.StockEntryRowControl.TransactionType;

/*
 * This class is a wrapper for a transaction in a stock account.  It is created on an as-needed basis, i.e.
 * when the entry is scrolled into view. 
 * 
 * This object exposes an interface that has a transaction type, and then,
 * various properties such as dividend tax amount, share purchase quantity, share price etc.
 *
 *Latest thinking: We have a separate facade for each transaction type.  1) we don't have all those
 * inapplicable properties, 2) this class is too big and will nicely split up, and 3) plugins should be
 * able to add transaction types, which means new transaction types cannot be in this single class.
 * 
 * Also, transactions could contain multiple transaction types, so for each transaction template seen in a
 * transaction, a facade can be exposed.  For existing transactions, exposing a facade is straight forward.
 * (except that a single entry may be in fact internally be a list of entries which means its properties cannot
 * be set).  When adding a new transaction type, we must first set the type on each pre-existing entry ourselves.
 * The facade will then use that entry.  Otherwise the facade will create a new entry.
 *  
 * These may be inapplicable for certain transaction types, in which case the values will bind
 * to null.
 *
 * The StockEntryRowControl does all the setting of default values.  This object does not do that,
 * it just checks that the transaction balances.
 */
public abstract class BaseEntryFacade implements EntryFacade {
 
	private IObservableValue<Entry> netAmountEntry;
	
	protected Transaction transaction;

	protected String transactionTypeAndName;

	public BaseEntryFacade(Transaction transaction, TransactionType transactionType, String transactionName) {
		this.transaction = transaction;
		this.transactionTypeAndName = transactionType.getId() + ":" + transactionName;
		this.netAmountEntry = observeEntry("cash");
		
		if (netAmountEntry.getValue() == null) {
			findOrCreateEntryWithId("cash");
		}
	}

	/**
	 * Provides an observable on an entry that is the entry with the given
	 * id in the transaction, or null if no entry in the transaction has the
	 * given id.
	 * 
	 * This is a one-way binding.  The reason for the binding being one-way is that it is
	 * not clear how the transaction should be updated should the observable to set to a different
	 * entry or be set to null (delete entry, clear the transaction type ?)
	 * 
	 * Other specific methods update the transaction:
	 * - create an entry with a given entry id
	 * - delete an entry (call method on transaction directly)
	 * - remove the transaction type and entry id (if we ever want to do this, call method on entry directly)
	 * 
	 * Doing any of the above will result in the observable value firing a change event.

	 * @param entryId
	 * @return
	 */
	// TODO better as a trackedGetter?
	protected IObservableValue<Entry> observeEntry(String entryId) {
		return new ObservableEntry(entryId, transaction, transactionTypeAndName);
	}

	/**
	 * Creates an entry of the given entry type.
	 * 
	 * This method assumes the entry does not already exist.
	 * 
	 * @param entryId
	 * @throws RuntimeException if an entry already exists with the given id
	 */
	protected Entry createEntry(String entryId) {
		Entry entry = this.transaction.createEntry();
		entry.setType(this.transactionTypeAndName, entryId);
		return entry;
	}

	/**
	 * Looks for an entry with a type id that has the required entry id, even if the transaction
	 * type is different.
	 * <P>
	 * This is useful when forcing a transaction of one type to a transaction
	 * of another type.  When changing a transaction type, we want to keep as much information
	 * in the transaction as possible.  For example, the "cash" entry may contain information
	 * pertaining to the bank account transaction which we do not want to lose.
	 * <P>
	 * We only re-use another entry if the transaction type has a 'stock.' prefix and the
	 * transaction id is blank (so we don't tread on other plugin's transaction types).
	 *   
	 * @param entryId
	 * @return
	 */
	protected Entry findOrCreateEntryWithId(String entryId) {
		for (Entry entry: transaction.getEntryCollection()) {
			String[] values = entry.getType() != null ? entry.getType().split(",") : new String[0];
			for (String value : values) {
				String[] parts = value.split(":");
				if (parts[0].startsWith("stocks.") && parts[1].equals("") & parts[2].contentEquals(entryId)) {
					entry.setType(transactionTypeAndName, entryId);
					return entry;
				}
			}
		}

		// Only if no suitable entry is found do we create a new entry
		return createEntry(entryId);
	}
	
	@Override
	public Entry getMainEntry() {
		// TODO what if this entry is deleted from a transaction?
		return netAmountEntry.getValue();
	}

//	/**
//	 * @return the net amount, being the amount credited or debited from the account
//	 * @trackedGetter        
//	 */
//	public long getNetAmount() {
//		return EntryInfo.getAmountAccessor().observe(netAmountEntry).getValue();
//	}

	/**
	 * This is used only so that when the user forces a transaction from one transaction type to
	 * another, the 'security' (or main security) is carried across. 
	 * @return
	 */
	public abstract Security getSecurity();

}
