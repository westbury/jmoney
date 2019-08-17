package net.sf.jmoney.stocks.pages;

import org.eclipse.core.databinding.observable.value.AbstractObservableValue;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.ValueDiff;

import net.sf.jmoney.entrytable.EntryFacade;
import net.sf.jmoney.isolation.IListPropertyAccessor;
import net.sf.jmoney.isolation.IModelObject;
import net.sf.jmoney.isolation.IScalarPropertyAccessor;
import net.sf.jmoney.isolation.SessionChangeListener;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.EntryInfo;
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
 
	private final class ObservableEntry extends AbstractObservableValue<Entry> {
		private String entryId;

		/*
		 * This listener updates all our writable values.
		 *
		 * The model keeps only a weak reference to this listener, removing it
		 * if no one else is referencing the listener.  We therefore must maintain
		 * a reference for as long as this object exists.
		 */
		private SessionChangeListener modelListener = new SessionChangeListener() {

			@Override
			public void objectInserted(IModelObject newObject) {
				if (newObject instanceof Entry) {
					Entry newEntry = (Entry)newObject;
					if (newEntry.getTransaction() == transaction
							&& newEntry.getType(transactionTypeAndName) == entryId) {
						updateValue();
					}
				}
			}

			@Override
			public void objectCreated(IModelObject newObject) {
				// TODO Auto-generated method stub

			}

			@Override
			public void objectRemoved(IModelObject deletedObject) {
				if (deletedObject instanceof Entry) {
					Entry deletedEntry = (Entry)deletedObject;
					if (deletedEntry.getTransaction() == transaction
							&& deletedEntry.getType(transactionTypeAndName) == entryId) {
						updateValue();
					}
				}
			}

			@Override
			public void objectDestroyed(IModelObject deletedObject) {
				// TODO Auto-generated method stub

			}

			@Override
			public void objectChanged(IModelObject changedObject,
					IScalarPropertyAccessor changedProperty, Object oldValue,
					Object newValue) {
				if (changedObject instanceof Entry) {
					Entry changedEntry = (Entry)changedObject;
					if (changedEntry.getTransaction() == transaction
							&& changedProperty == EntryInfo.getTypeAccessor()) {
						updateValue();
					}
				}
			}

			@Override
			public void objectMoved(IModelObject movedObject,
					IModelObject originalParent,
					IModelObject newParent,
					IListPropertyAccessor originalParentListProperty,
					IListPropertyAccessor newParentListProperty) {
				// TODO Auto-generated method stub

			}

			@Override
			public void performRefresh() {
				// TODO Auto-generated method stub

			}
			
			private void updateValue() {
				Entry oldValue = entry;
				Entry newValue = entry = calculateEntry();
				ValueDiff<Entry> diff = new ValueDiff<Entry>() {

					@Override
					public Entry getOldValue() {
						return oldValue;
					}

					@Override
					public Entry getNewValue() {
						return newValue;
					}};
				ObservableEntry.this.fireValueChange(diff);
			}
		};

		
		private Entry entry;

		public ObservableEntry(String entryId) {
			this.entryId = entryId;
			this.entry = this.calculateEntry();
		}
		
		private Entry calculateEntry() {
			Entry matchingEntry = null;
			for (Entry entry : transaction.getEntryCollection()) {
				if (entry.getType(transactionTypeAndName).equals(entryId)) {
					if (matchingEntry != null) {
						throw new RuntimeException("can't have two entries of same id");
					}
					matchingEntry = entry;
				}
			}
			return matchingEntry;
		}

		@Override
		public Object getValueType() {
			return Entry.class;
		}

		@Override
		protected Entry doGetValue() {
			return entry;
		}
	}

//	private StockAccount account;

	/**
	 * The net amount, being the amount deposited or withdrawn from the cash balance
	 * in this account.
	 */
//	private Entry netAmountEntry;
	private IObservableValue<Entry> netAmountEntry;
	
	protected Transaction transaction;

	protected String transactionTypeAndName;

	public BaseEntryFacade(Transaction transaction, TransactionType transactionType, String transactionName) {
		this.transaction = transaction;
		this.transactionTypeAndName = transactionType.getId() + ":" + transactionName;
		this.netAmountEntry = observeEntry("cash");
	}


//	public BaseEntryFacade(Entry netAmountEntry, StockAccount stockAccount) {
//		this.netAmountEntry = netAmountEntry;
//		this.account = stockAccount;
//		
//	}

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
	 * Doing any of the above will of course result in the observable value firing a change event.

	 * @param entryId
	 * @return
	 */
	// TODO better as a trackedGetter?
	protected IObservableValue<Entry> observeEntry(String entryId) {
		return new ObservableEntry(entryId);
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
