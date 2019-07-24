package net.sf.jmoney.stocks.pages;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;

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
 
	private StockAccount account;

	/**
	 * The net amount, being the amount deposited or withdrawn from the cash balance
	 * in this account.
	 */
	private Entry netAmountEntry;
	
	private final IObservableValue<TransactionType> transactionType = new WritableValue<TransactionType>();

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
				if (newEntry.getTransaction() == getMainEntry().getTransaction()) {
					analyzeTransaction();
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
					analyzeTransaction();
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
						analyzeTransaction();
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
	};

	// Facade for specific transaction type
	private BaseEntryFacade facade;

	
	private IObservableValue<StockBuyOrSellFacade> tradeFacade = new WritableValue<StockBuyOrSellFacade>();
	private IObservableValue<StockDividendFacade> dividendFacade = new WritableValue<StockDividendFacade>();

	public StockEntryFacade(Entry netAmountEntry, StockAccount stockAccount) {
		this.netAmountEntry = netAmountEntry;
		this.account = stockAccount;
		
		analyzeTransaction();

		netAmountEntry.getDataManager().addChangeListenerWeakly(modelListener);
		
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
				
				facade = null;  // Do we need this?
				TransactionType newValue = event.diff.getNewValue();
				switch (newValue) {
				case Buy:
					forceTransactionToBuy();
					tradeFacade.setValue(new StockBuyOrSellFacade(getMainEntry().getTransaction(), newValue, "", account));
					facade = tradeFacade.getValue();   // But this assumes this is only place that upates tradeFacade
					break;
				case Sell:
					forceTransactionToSell();
					tradeFacade.setValue(new StockBuyOrSellFacade(getMainEntry().getTransaction(), newValue, "", account));
					facade = tradeFacade.getValue();   // But this assumes this is only place that upates tradeFacade
					break;
				case Dividend:
					forceTransactionToDividend();
					dividendFacade.setValue(new StockDividendFacade(getMainEntry().getTransaction(), "", account));
					facade = dividendFacade.getValue();
					break;
				case Takeover:
					forceTransactionToTakeover();
					break;
				default:
					break;
				}
				

			}
		});
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
		if (getMainEntry().getTransaction().getEntryCollection().size() == 1) {
			transactionType.setValue(null);
		} else {

			TransactionType newType = null;
			for (Entry entry: getMainEntry().getTransaction().getEntryCollection()) {
				String[] values = entry.getType() != null ? entry.getType().split(",") : new String[0];
				boolean conflictFound = false;
				for (String value : values) {
					String[] parts = value.split(":");
					if (parts[0].startsWith("stocks.")) {
						Optional<TransactionType> matchingType = Arrays.asList(TransactionType.values()).stream().filter(eachType -> eachType.getId() != null && eachType.getId().equals(parts[0])).findAny();
						if (matchingType.isPresent()) {
							if (newType != null && matchingType.get() != newType) {
								conflictFound = true;
							} else {
								newType = matchingType.get();
							}
							this.transactionType.setValue(matchingType.get());
						} else {
							this.transactionType.setValue(TransactionType.Other);
						}
					}
				}

				if (newType != null && !conflictFound) {
					this.transactionType.setValue(newType);
				} else {
					this.transactionType.setValue(null);
				}

			}
		}
	}

	@Override
	public Entry getMainEntry() {
		return netAmountEntry;
	}
	
	private StockDividendFacade forceTransactionToDividend() {
		// Get the security from the old transaction, which must be done
		// before we start messing with this transaction.
		Security security = this.facade.getSecurity();

		this.forceTransactionTo(TransactionType.Dividend);
		StockDividendFacade facade = new StockDividendFacade(getMainEntry().getTransaction(), "", account);
		this.facade = facade;
		facade.setDividendSecurity(security);
		return facade;	
	}

	private StockTakeoverFacade forceTransactionToTakeover() {
		this.forceTransactionTo(TransactionType.Takeover);
		StockTakeoverFacade facade = new StockTakeoverFacade(getMainEntry().getTransaction(), "", account);
		this.facade = facade;
		return facade;	
	}

	private void forceTransactionTo(TransactionType newTransType) {
		String oldTransTypeId = this.transactionType.getValue().getId() + ":";
		String newTransTypeId = newTransType.getId() + ":";
		/*
		 * Remove entries that are not appropriate for the new transaction type.
		 */
		EntryCollection entries = getMainEntry().getTransaction().getEntryCollection();
		for (Iterator<Entry> iter = entries.iterator(); iter.hasNext(); ) {
			Entry entry = iter.next();
			
			String oldEntryType = entry.getType(oldTransTypeId);
			
			Set<String> validNewEntryTypes = newTransType.getEntryTypes();
			
			if (validNewEntryTypes.contains(oldEntryType)) {
				// Force this entry
				entry.setType(oldTransTypeId, null);
				entry.setType(newTransTypeId, oldEntryType);
			} else {
				iter.remove();
			}
		}
	}

	private StockBuyOrSellFacade forceTransactionToBuy() {
		return forceTransactionToBuyOrSell(TransactionType.Buy);
	}

	private StockBuyOrSellFacade forceTransactionToSell() {
		return forceTransactionToBuyOrSell(TransactionType.Sell);
	}

	private StockBuyOrSellFacade forceTransactionToBuyOrSell(TransactionType transactionType) {
		// Get the security from the old transaction, which must be done
		// before we start messing with this transaction.
		Security security = this.facade.getSecurity();

		assert this.transactionType.getValue() == transactionType;

		this.forceTransactionTo(transactionType);
		
		StockBuyOrSellFacade facade = new StockBuyOrSellFacade(getMainEntry().getTransaction(), transactionType, "", account);
		this.facade = facade;
		facade.security().setValue(security);
		return facade;	
	}

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

	public StockBuyOrSellFacade getBuyOrSellFacade() {
		Assert.isTrue(isPurchaseOrSale());
		return tradeFacade.getValue();
	}

	/**
	 * @return the net amount, being the amount credited or debited from the account
	 * @trackedGetter        
	 */
	public long getNetAmount() {
		return EntryInfo.getAmountAccessor().observe(netAmountEntry).getValue();
	}

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

}
