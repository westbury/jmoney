package net.sf.jmoney.stocks.pages;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;

import org.eclipse.core.databinding.observable.Diffs;
import org.eclipse.core.databinding.observable.value.AbstractObservableValue;
import org.eclipse.core.databinding.observable.value.ComputedValue;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.IValueChangeListener;
import org.eclipse.core.databinding.observable.value.ValueChangeEvent;
import org.eclipse.core.databinding.observable.value.WritableValue;
import org.eclipse.core.runtime.Assert;

import net.sf.jmoney.entrytable.EntryFacade;
import net.sf.jmoney.isolation.IListPropertyAccessor;
import net.sf.jmoney.isolation.IModelObject;
import net.sf.jmoney.isolation.IScalarPropertyAccessor;
import net.sf.jmoney.isolation.SessionChangeListener;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.EntryInfo;
import net.sf.jmoney.model2.Transaction.EntryCollection;
import net.sf.jmoney.stocks.model.Security;
import net.sf.jmoney.stocks.model.StockAccount;
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
public class StockEntryFacade implements EntryFacade {
 
	private final class TransactionTypeObservable extends AbstractObservableValue<TransactionType> {
		
		private TransactionType transactionTypeValue;

		/*
		 * This model listener keeps the transaction type up to date.
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
					if (newEntry.getTransaction() == getMainEntry().getTransaction()) {
						analyzeAndSetTransaction();
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
					if (deletedEntry.getTransaction() == getMainEntry().getTransaction()) {
						analyzeAndSetTransaction();
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
					if (changedEntry.getTransaction() == getMainEntry().getTransaction()) {
						if (changedProperty == EntryInfo.getTypeAccessor()) {
							analyzeAndSetTransaction();
						}
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
			
			private void analyzeAndSetTransaction() {
				TransactionType oldTransType = transactionTypeValue;
				TransactionType newTransType = analyzeTransaction();
				transactionTypeValue = newTransType;
				fireValueChange(Diffs.createValueDiff(oldTransType,	newTransType));
			}
		};

		public TransactionTypeObservable(Entry netAmountEntry) {
			this.transactionTypeValue = analyzeTransaction();
			netAmountEntry.getDataManager().addChangeListenerWeakly(modelListener);
		}

		@Override
		public Object getValueType() {
			return TransactionType.class;
		}

		@Override
		protected TransactionType doGetValue() {
			return this.transactionTypeValue;
		}

		@Override
		protected void doSetValue(TransactionType newTransType) {
			this.forceTransactionTo(newTransType);
		}
		
		/**
		 * This method must not modify the transaction.  This means we must only
		 * set a transaction-type specific facade if the transaction has all the
		 * compulsory entries for that transaction type set.
		 * <P>
		 * If this method were to change the transaction then weird stuff could
		 * occur as a transaction is being modified, as this method would be called
		 * by the listeners on each change, resulting in this method changing the transaction
		 * underneath the feet of the code making the original changes.
		 * 
		 * This method must fire the transaction type observable.  Think about multiple instances of
		 * this facade on the same transaction.  The transaction type can be set through any one and they
		 * all must synchronize with the underlying transaction in the model.
		 */
		private TransactionType analyzeTransaction() {
			/*
			 * Analyze the transaction to see which type of transaction this is.
			 */

			/*
			 * If just one entry then this is not a valid transaction, so must be
			 * a new transaction.  We set the transaction type to null which means
			 * no selection will be set in the transaction type combo.
			 */
			if (getMainEntry().getTransaction().getEntryCollection().size() == 1) {
				return null;
			} else {

				String transactionType = null;
				boolean conflictFound = false;
				for (Entry entry: getMainEntry().getTransaction().getEntryCollection()) {
					String[] values = entry.getType() != null ? entry.getType().split(",") : new String[0];
					for (String value : values) {
						String[] parts = value.split(":");
						if (parts[0].startsWith("stocks.")) {
							String thisTransactionType = parts[0] + ':' + parts[1];
							if (transactionType != null && !thisTransactionType.contentEquals(transactionType)) {
								conflictFound = true;
							} else {
								transactionType = thisTransactionType;
							}
						}
					}
				}

				if (transactionType != null && !conflictFound) {

					String transactionTypeOnly = transactionType.split(":")[0];
					Optional<TransactionType> matchingType = Arrays.asList(TransactionType.values()).stream().filter(eachType -> eachType.getId() != null && eachType.getId().equals(transactionTypeOnly)).findAny();
					if (matchingType.isPresent()) {
						TransactionType newType = matchingType.get();


						// Check all compulsory entries exist
						boolean anyTypesNotFound = false;
						for (String compulsoryEntryType : newType.getCompulsoryEntryTypes()) {
							boolean thisTypeFound = false;
							for (Entry entry: getMainEntry().getTransaction().getEntryCollection()) {
								String thisEntryType = entry.getType(transactionType);
								if (thisEntryType != null && thisEntryType.equals(compulsoryEntryType)) {
									thisTypeFound = true;
								}
							}
							anyTypesNotFound |= !thisTypeFound;
						}

						if (!anyTypesNotFound) {
							// Now we know all compulsory types exist, we can create the facade
							// without problem (i.e. without needing to modify the transaction)

							return newType;
						} else {
							return TransactionType.Other;
						}
					} else {
						return TransactionType.Other;
					}
				} else {
					return TransactionType.Other;
				}
			}
		}

		private void forceTransactionTo(TransactionType newTransType) {
			// This method does not itself set the transaction type.  When we change the transaction entries, the model listener
			// will analyze the transaction.  That way all facades on the transaction will update in the
			// same way.

			String oldTransTypeId = this.transactionTypeValue != null ? this.transactionTypeValue.getId() + ":" : null;
			String newTransTypeId = newTransType.getId() + ":";
			
			// Get the security from the old transaction, which must be done
			// before we start messing with this transaction.
			Security security = StockEntryFacade.this.facade == null ? null : facade.getSecurity();

			Set<String> compulsoryEntryTypes = new HashSet<>(newTransType.getCompulsoryEntryTypes());
			
			/*
			 * Remove entries that are not appropriate for the new transaction type.
			 */
			EntryCollection entries = getMainEntry().getTransaction().getEntryCollection();
			for (Iterator<Entry> iter = entries.iterator(); iter.hasNext(); ) {
				Entry entry = iter.next();

				if (entry == getMainEntry()) {
					// Maintain the main entry even if no previous transaction type.  This entry is the anchor entry and we want the instance
					// to remain.
					if (oldTransTypeId != null) {
						entry.setType(oldTransTypeId, null);
					}
					entry.setType(newTransTypeId, "cash");
					compulsoryEntryTypes.remove("cash");
				} else {	
					IEntryType[] validNewEntryTypes = newTransType.getEntryTypes();
					
					if (oldTransTypeId != null) {
						String typeOfThisEntry = entry.getType(oldTransTypeId);
						
						Optional<IEntryType> entryType = Arrays.stream(validNewEntryTypes).filter(e -> e.getId().contentEquals(typeOfThisEntry)).findFirst();
						if (entryType.isPresent()) {
							// Force this entry
							entry.setType(oldTransTypeId, null);
							entry.setType(newTransTypeId, typeOfThisEntry);
							entry.setAccount(entryType.get().getAssociatedAccount(account));
							
							// As the entry already exists, remove from the list of entries we must create
							compulsoryEntryTypes.remove(typeOfThisEntry);
						} else {
							iter.remove();
						}
					} else {
						iter.remove();
					}
				}
			}
			
			// Add the missing required entries
			for (String entryTypeId : compulsoryEntryTypes) {
				Optional<IEntryType> entryType = Arrays.stream(newTransType.getEntryTypes()).filter(e -> e.getId().equals(entryTypeId)).findFirst();
				this.createEntry(newTransTypeId, entryType.get());
			}
			
			// The model listener will now have analyzed the transaction and updated the transaction type
			Assert.isTrue(this.transactionTypeValue == newTransType);
			
			// Now set the security if applicable
			if (security != null) {
				switch (this.transactionTypeValue) {
				case Buy:
				case Sell:
					StockEntryFacade.this.buyOrSellFacade().getValue().security().setValue(security);
					break;
				case Dividend:
					StockEntryFacade.this.dividendFacade().getValue().security().setValue(security);
					break;
				default:
					break;
				}
			}
		}

		protected Entry createEntry(String transactionTypeAndName, IEntryType entryType) {
			Entry entry = getMainEntry().getTransaction().createEntry();
			entry.setType(transactionTypeAndName, entryType.getId());
			entry.setAccount(entryType.getAssociatedAccount(account));
			return entry;
		}
	}

	private StockAccount account;

	/**
	 * The net amount, being the amount deposited or withdrawn from the cash balance
	 * in this account.
	 */
	private Entry netAmountEntry;
	
	private final IObservableValue<TransactionType> transactionType;

	// Facade for specific transaction type
	private BaseEntryFacade facade;

	
	private IObservableValue<StockBuyOrSellFacade> tradeFacade = new WritableValue<>();
	private IObservableValue<StockDividendFacade> dividendFacade = new WritableValue<>();
	private IObservableValue<StockTakeoverFacade> takeoverFacade = new WritableValue<>();
	
	public StockEntryFacade(Entry netAmountEntry, StockAccount stockAccount) {
		this.netAmountEntry = netAmountEntry;
		this.account = stockAccount;
		
		transactionType = new TransactionTypeObservable(netAmountEntry);
		
		/*
		 * The above set the initial transaction type.
		 * 
		 * Now set the If the transaction type is changed then we must transform
		 * the transaction to match.
		 */
		transactionType.addValueChangeListener(new IValueChangeListener<TransactionType>() {
			@Override
			public void handleValueChange(ValueChangeEvent<? extends TransactionType> event) {
				TransactionType oldValue = event.diff.getOldValue();
				if (oldValue != null) {
					switch (oldValue) {
					case Buy:
					case Sell:
						tradeFacade.setValue(null);
						break;
					case Dividend:
						dividendFacade.setValue(null);
						break;
					case Takeover:
						break;
					default:
						break;
					}
				}

				// Must not force transaction here.  This must be read-only or it
				// could recurse horribly.  The transaction entries have been created by the time
				// the transaction type change event is fired, so we can simply construct the facades.
				// This object guarantees that the transaction has all the compulsory entries for the
				// transaction type.

				facade = null;  // Do we need this?
				TransactionType newValue = event.diff.getNewValue();
				if (newValue != null) {
					switch (newValue) {
					case Buy:
					case Sell:
						StockBuyOrSellFacade thisTradeFacade = new StockBuyOrSellFacade(getMainEntry().getTransaction(), newValue, "", account);
						facade = thisTradeFacade;   // But this assumes this is only place that upates tradeFacade
						tradeFacade.setValue(thisTradeFacade);
						break;
					case Dividend:
						dividendFacade.setValue(new StockDividendFacade(getMainEntry().getTransaction(), "", account));
						facade = dividendFacade.getValue();
						break;
					case Takeover:
						takeoverFacade.setValue(new StockTakeoverFacade(getMainEntry().getTransaction(), "", account));
						facade = takeoverFacade.getValue();
						break;
					default:
						break;
					}
				}

			}
		});
	}

	@Override
	public Entry getMainEntry() {
		return netAmountEntry;
	}
	
//	private StockDividendFacade forceTransactionToDividend() {
//		// Get the security from the old transaction, which must be done
//		// before we start messing with this transaction.
//		Security security = this.facade.getSecurity();
//
//		this.forceTransactionTo(TransactionType.Dividend);
//		StockDividendFacade facade = new StockDividendFacade(getMainEntry().getTransaction(), "", account);
//		this.facade = facade;
//		facade.setDividendSecurity(security);
//		return facade;	
//	}
//
//	private StockTakeoverFacade forceTransactionToTakeover() {
//		this.forceTransactionTo(TransactionType.Takeover);
//		StockTakeoverFacade facade = new StockTakeoverFacade(getMainEntry().getTransaction(), "", account);
//		this.facade = facade;
//		return facade;	
//	}
//
//	private StockBuyOrSellFacade forceTransactionToBuy() {
//		return forceTransactionToBuyOrSell(TransactionType.Buy);
//	}
//
//	private StockBuyOrSellFacade forceTransactionToSell() {
//		return forceTransactionToBuyOrSell(TransactionType.Sell);
//	}
//
//	private StockBuyOrSellFacade forceTransactionToBuyOrSell(TransactionType transactionType) {
//		// Get the security from the old transaction, which must be done
//		// before we start messing with this transaction.
//		Security security = this.facade == null ? null : facade.getSecurity();
//
//		assert this.transactionType.getValue() == transactionType;
//
//		this.forceTransactionTo(transactionType);
//		
//		StockBuyOrSellFacade facade = new StockBuyOrSellFacade(getMainEntry().getTransaction(), transactionType, "", account);
//		this.facade = facade;
//		facade.security().setValue(security);
//		return facade;	
//	}

	/**
	 * @trackedGetter
	 */
	public TransactionType getTransactionType() {
		return transactionType.getValue();
	}

	public IObservableValue<TransactionType> transactionType() {
		return transactionType;
	}

	public boolean isPurchaseOrSale() {
		return transactionType.getValue() == TransactionType.Buy
				|| transactionType.getValue() == TransactionType.Sell;
	}

	public IObservableValue<Boolean> observeWhenPurchaseOrSale() {
		return new ComputedValue<Boolean>() {
			@Override
			protected Boolean calculate() {
				return isPurchaseOrSale();
			}
		};
	}

	public boolean isDividend() {
		return transactionType.getValue() == TransactionType.Dividend;
	}

	public IObservableValue<StockBuyOrSellFacade> buyOrSellFacade() {
		return tradeFacade;
	}

	public IObservableValue<StockDividendFacade> dividendFacade() {
		return dividendFacade;
	}

	/**
	 * 
	 * @return a facade to a buy or sell transaction, or null if this is not a buy or sell transaction.
	 */
	public StockBuyOrSellFacade getBuyOrSellFacade() {
		return tradeFacade.getValue();
	}

	/**
	 * @return the net amount, being the amount credited or debited from the account
	 * @trackedGetter        
	 */
	public long getNetAmount() {
		return EntryInfo.getAmountAccessor().observe(netAmountEntry).getValue();
	}

	// readonly, which means this may be better as a tracked getter.
	public IObservableValue<Security> security() {
		return new ComputedValue<Security>() {
			@Override
			protected Security calculate() {
				if (buyOrSellFacade().getValue() != null) {
					return buyOrSellFacade().getValue().security().getValue();
				}
				if (dividendFacade().getValue() != null) {
					return dividendFacade().getValue().security().getValue();
				}
				return null;
			}
		};
	}

	public void setSecurity(Security security) {
		if (buyOrSellFacade().getValue() != null) {
			buyOrSellFacade().getValue().security().setValue(security);
		}
		if (dividendFacade().getValue() != null) {
			dividendFacade().getValue().security().setValue(security);
		}
	}
}
