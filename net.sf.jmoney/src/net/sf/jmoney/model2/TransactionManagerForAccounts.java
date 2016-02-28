package net.sf.jmoney.model2;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import net.sf.jmoney.isolation.DeltaListIterator;
import net.sf.jmoney.isolation.DeltaListManager;
import net.sf.jmoney.isolation.IModelObject;
import net.sf.jmoney.isolation.IObjectKey;
import net.sf.jmoney.isolation.IScalarPropertyAccessor;
import net.sf.jmoney.isolation.ModifiedObject;
import net.sf.jmoney.isolation.TransactionManager;
import net.sf.jmoney.isolation.UncommittedObjectKey;

import org.eclipse.core.runtime.Assert;

public class TransactionManagerForAccounts extends TransactionManager implements IDataManagerForAccounts {

	// TODO: At some time, review this and ensure that we
	// really do need the session object here.
	private Session uncommittedSession;
	
	public TransactionManagerForAccounts(IDataManagerForAccounts baseDataManager) {
		super(baseDataManager);
		
		this.uncommittedSession = getCopyInTransaction(baseDataManager.getSession());
	}

	/**
	 * @return a session object representing an uncommitted
	 * 			session object managed by this transaction manager
	 */
    @Override	
	public Session getSession() {
		return uncommittedSession;
	}

    /**
	 * @param account
	 * @return
	 */
    @Override	
	public boolean hasEntries(Account account) {
		return !new ModifiedAccountEntriesList(account).isEmpty();
	}

	/**
	 * @param account
	 * @return
	 */
    @Override	
	public Collection<Entry> getEntries(Account account) {
		return new ModifiedAccountEntriesList(account);
	}

	private class ModifiedAccountEntriesList extends AbstractCollection<Entry> {
		
		Account account;
		
		ModifiedAccountEntriesList(Account account) {
			this.account = account;
		}
		
		@Override
		public int size() {
			Vector<Entry> addedEntries = new Vector<Entry>();
			Vector<IObjectKey> removedEntries = new Vector<IObjectKey>();
			buildAddedAndRemovedEntryLists(addedEntries, removedEntries);
			
			IObjectKey committedAccountKey = ((UncommittedObjectKey)account.getObjectKey()).getCommittedObjectKey();
			if (committedAccountKey == null) {
				// This is a new account created in this transaction
				Assert.isTrue(removedEntries.isEmpty());
				return addedEntries.size();
			} else {
				Account committedAccount = (Account)committedAccountKey.getObject();
				Collection<Entry> committedCollection = committedAccount.getEntries();
				return committedCollection.size() + addedEntries.size() - removedEntries.size();
			}
		}

		@Override
		public Iterator<Entry> iterator() {
			// Build the list of differences between the committed
			// list and the list in this transaction.
			
			// This is done each time an iterator is requested.
			
			Vector<Entry> addedEntries = new Vector<Entry>();
			Vector<IObjectKey> removedEntries = new Vector<IObjectKey>();
			buildAddedAndRemovedEntryLists(addedEntries, removedEntries);
			
			IObjectKey committedAccountKey = ((UncommittedObjectKey)account.getObjectKey()).getCommittedObjectKey();
			if (committedAccountKey == null) {
				// This is a new account created in this transaction
				Assert.isTrue(removedEntries.isEmpty());
				return addedEntries.iterator();
			} else {
				Account committedAccount = (Account)committedAccountKey.getObject();
				Collection<Entry> committedCollection = committedAccount.getEntries();
				return new DeltaListIterator<Entry>(TransactionManagerForAccounts.this, committedCollection.iterator(), addedEntries, removedEntries);
			}
		}

		private void buildAddedAndRemovedEntryLists(Vector<Entry> addedEntries,	Vector<IObjectKey> removedEntries) {
			// Process all the new objects added within this transaction
			for (DeltaListManager<?,?> modifiedList: modifiedLists) {
				
				// Find all entries added to existing transactions
				if (modifiedList.listAccessor == TransactionInfo.getEntriesAccessor()) {
					for (IModelObject newObject: modifiedList.getAddedObjects()) {
						Entry newEntry = (Entry)newObject;
						if (account.equals(newEntry.getAccount())) {
							addedEntries.add(newEntry);
						}
					}
				}

				// Find all entries in new transactions.
				if (modifiedList.listAccessor == SessionInfo.getTransactionsAccessor()) {
					for (IModelObject newObject: modifiedList.getAddedObjects()) {
						Transaction newTransaction = (Transaction)newObject;
						for (Entry newEntry: newTransaction.getEntryCollection()) {
							if (account.equals(newEntry.getAccount())) {
								addedEntries.add(newEntry);
							}
						}
					}
				}
			}
			
			/*
			 * Process all the changed and deleted objects. (Deleted objects are
			 * processed here and not from the deletedObjects list in modified
			 * lists in the above code. This ensures that objects that are
			 * deleted due to the deletion of the parent are also processed).
			 */
			for (Map.Entry<IObjectKey, ModifiedObject> mapEntry: modifiedObjects.entrySet()) {
				IObjectKey committedKey = mapEntry.getKey();
				ModifiedObject newValues = mapEntry.getValue();
				
				IModelObject committedObject = committedKey.getObject();
				
				if (committedObject instanceof Entry) {
					Entry entry = (Entry)committedObject;
					if (!newValues.isDeleted()) {
						Map<IScalarPropertyAccessor, Object> propertyMap = newValues.getMap();
						
						// Object has changed property values.
						if (propertyMap.containsKey(EntryInfo.getAccountAccessor())) {
							boolean wasInIndex = account.equals(entry.getAccount());
							boolean nowInIndex = account.equals(((IObjectKey)propertyMap.get(EntryInfo.getAccountAccessor())).getObject());
							if (wasInIndex) {
								if (!nowInIndex) {
									removedEntries.add(entry.getObjectKey());
								}
							} else {
								if (nowInIndex) {
									// Note that addedEntries must contain objects that
									// are being managed by the transaction manager
									// (not the committed versions).
									addedEntries.add(getCopyInTransaction(entry));
								}
							}
						}
					} else {
						// Object has been deleted.
						if (entry.getAccount().equals(account)) {
							removedEntries.add(entry.getObjectKey());
						}
					}
				}
			}
		}
	}

	@Override
	public List<Entry> getEntries(Date startDate, Date endDate, Long amount,
			String memo) {
		throw new UnsupportedOperationException();
	}

}
