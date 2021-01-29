package net.sf.jmoney.stocks.pages;

import org.eclipse.core.databinding.observable.value.AbstractObservableValue;
import org.eclipse.core.databinding.observable.value.ValueDiff;

import net.sf.jmoney.isolation.IListPropertyAccessor;
import net.sf.jmoney.isolation.IModelObject;
import net.sf.jmoney.isolation.IScalarPropertyAccessor;
import net.sf.jmoney.isolation.SessionChangeListener;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.EntryInfo;
import net.sf.jmoney.model2.Transaction;

/**
 * This class is an observer of an entry in a given transaction with a given type id.
 * The value of the observable is the <code>Entry</code> object, or null if no object exists
 * in the transaction with the given type id.
 * <P>
 * This observable is read only.  Do not attempt to set the observable, it will not do anything.
 * Adding an entry with the given type id using the normal model access methods will result in the
 * value of this observable changing.
 */
class ObservableEntry extends AbstractObservableValue<Entry> {
	private String entryId;
	private Transaction transaction;
	private String transactionTypeAndName;
	
	/*
	 * This listener listens to the model for changes that may result in a change to the value
	 * of this observable.
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
			if (newValue != oldValue) {
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
		}
	};

	
	private Entry entry;

	public ObservableEntry(String entryId, Transaction transaction, String transactionTypeAndName) {
		this.entryId = entryId;
		this.transaction = transaction;
		this.transactionTypeAndName = transactionTypeAndName;
		this.entry = this.calculateEntry();
		
		transaction.getDataManager().addChangeListener(modelListener);
	}
	
	private Entry calculateEntry() {
		Entry matchingEntry = null;
		for (Entry entry : transaction.getEntryCollection()) {
			// Note that the type set in the entry may be null 
			if (entryId.equals(entry.getType(transactionTypeAndName))) {
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
	
	@Override
	public void dispose() {
		transaction.getDataManager().removeChangeListener(modelListener);
	}
}