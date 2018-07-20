package net.sf.jmoney.stocks.pages;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Iterator;

import org.eclipse.core.databinding.observable.value.ComputedValue;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.IValueChangeListener;
import org.eclipse.core.databinding.observable.value.ValueChangeEvent;
import org.eclipse.core.databinding.observable.value.WritableValue;
import org.eclipse.core.runtime.Assert;

import net.sf.jmoney.entrytable.EntryFacade;
import net.sf.jmoney.entrytable.InvalidUserEntryException;
import net.sf.jmoney.isolation.IListPropertyAccessor;
import net.sf.jmoney.isolation.IModelObject;
import net.sf.jmoney.isolation.IScalarPropertyAccessor;
import net.sf.jmoney.isolation.ReferenceViolationException;
import net.sf.jmoney.isolation.SessionChangeListener;
import net.sf.jmoney.model2.CapitalAccount;
import net.sf.jmoney.model2.Currency;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.EntryInfo;
import net.sf.jmoney.model2.Transaction.EntryCollection;
import net.sf.jmoney.stocks.model.Security;
import net.sf.jmoney.stocks.model.StockAccount;
import net.sf.jmoney.stocks.model.StockEntryInfo;
import net.sf.jmoney.stocks.pages.StockEntryRowControl.TransactionType;

/*
 * This class is a wrapper for a transaction in a stock account.  It is created on an as-needed basis, i.e.
 * when the entry is scrolled into view. 
 * 
 * Latest thinking: This object exposes an interface that has a transaction type, and then,
 * various properties such as dividend tax amount, share purchase quantity, share price etc.
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

	private Entry dividendEntry;
	private Entry withholdingTaxEntry;

	/**
	 * the entry for the commission, or null if this is not a purchase or sale
	 * transaction or if no commission account is configured for this stock
	 * account because commissions are never charged on any purchases or sales
	 * in this account, and possibly null if there can be a commission but none
	 * has been entered for this entry
	 */
	private Entry commissionEntry;

	private Entry tax1Entry;
	private Entry tax2Entry;
	private Entry purchaseOrSaleEntry;
	private Entry transferEntry;

	private boolean unknownTransactionType;

	// bound to getPurchaseOrSaleEntry().getAmount() except this is always positive whereas
	// getPurchaseOrSaleEntry().getAmount() would be negative for a sale
	private final IObservableValue<Long> quantity = new WritableValue<Long>();

	private final IObservableValue<Long> dividend = new WritableValue<Long>();

	private final IObservableValue<Long> withholdingTax = new WritableValue<Long>();

	private final IObservableValue<BigDecimal> sharePrice = new WritableValue<BigDecimal>();

	/**
	 * The amount in the commission entry, or null if there is no commission entry,
	 * but never zero and this observable should never be set to zero
	 */
	private final IObservableValue<Long> commission = new WritableValue<Long>();

	/**
	 * The amount in the tax 1 entry, or null if there is no tax 1 entry,
	 * but never zero and this observable should never be set to zero
	 */
	private final IObservableValue<Long> tax1 = new WritableValue<Long>();

	/**
	 * The amount in the tax 2 entry, or null if there is no tax 2 entry,
	 * but never zero and this observable should never be set to zero
	 */
	private final IObservableValue<Long> tax2 = new WritableValue<Long>();

	/**
	 * The security.  This goes in a different place in the transaction depending
	 * on the transaction type.  If a purchase or sale, it is the amount of the entry
	 * representing the change in the account stock balances.  If it is a dividend then
	 * it goes in the security property which is really just a secondary information property.
	 */
	private final IObservableValue<Security> security = new WritableValue<Security>();

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
					if (newEntry.getAccount() == account.getCommissionAccount()) {
						if (commissionEntry != null) {
							throw new RuntimeException("already has a commission entry!");
						}
						commissionEntry = newEntry;
						commission.setValue(newEntry.getAmount());
					} else if (newEntry.getAccount() == account.getTax1Account()) {
						if (tax1Entry != null) {
							throw new RuntimeException("already has a tax 1 entry!");
						}
						tax1Entry = newEntry;
						tax1.setValue(newEntry.getAmount());
					} else if (newEntry.getAccount() == account.getTax2Account()) {
						if (tax2Entry != null) {
							throw new RuntimeException("already has a tax 2 entry!");
						}
						tax2Entry = newEntry;
						tax2.setValue(newEntry.getAmount());
					}
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
					if (deletedEntry.getAccount() == account.getCommissionAccount()) {
						if (commissionEntry == null) {
							throw new RuntimeException("but no commission entry was set!");
						}
						commissionEntry = null;
						commission.setValue(null);
					} else if (deletedEntry.getAccount() == account.getTax1Account()) {
						if (tax1Entry == null) {
							throw new RuntimeException("but no tax 1 entry was set!");
						}
						tax1Entry = null;
						tax1.setValue(null);
					} else if (deletedEntry.getAccount() == account.getTax2Account()) {
						if (tax2Entry == null) {
							throw new RuntimeException("but no tax 1 entry was set!");
						}
						tax2Entry = null;
						tax2.setValue(null);
					}
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
					if (changedProperty == EntryInfo.getAmountAccessor()) {
						Long newAmount = (Long)newValue;

						if (changedEntry.getAccount() == account.getCommissionAccount()) {
							if (commissionEntry == null) {
								throw new RuntimeException("but no commission entry was set!");
							}
							commission.setValue(newAmount);
						} else if (changedEntry.getAccount() == account.getTax1Account()) {
							if (tax1Entry == null) {
								throw new RuntimeException("but no tax 1 entry was set!");
							}
							tax1.setValue(newAmount);
						} else if (changedEntry.getAccount() == account.getTax2Account()) {
							if (tax2Entry == null) {
								throw new RuntimeException("but no tax 1 entry was set!");
							}
							tax2.setValue(newAmount);
						}
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

		//				private void fireCommmissionChangedEvent(Long newValue) {
		//					for (IPropertyChangeListener<Long> listener  : commissionChangeListeners) {
		//						listener.propertyChanged(newValue);
		//					}
		//				}
		//
		//				private void fireTax1ChangedEvent(Long newValue) {
		//					for (IPropertyChangeListener<Long> listener  : tax1ChangeListeners) {
		//						listener.propertyChanged(newValue);
		//					}
		//				}
		//
		//				private void fireTax2ChangedEvent(Long newValue) {
		//					for (IPropertyChangeListener<Long> listener  : tax2ChangeListeners) {
		//						listener.propertyChanged(newValue);
		//					}
		//				}
	};

	public StockEntryFacade(Entry netAmountEntry, StockAccount stockAccount) {
		this.netAmountEntry = netAmountEntry;
		this.account = stockAccount;
		
		analyzeTransaction();

		/*
		 * The above set the initial transaction type.
		 * 
		 * If the transaction type is changed then we must transform
		 * the transaction to match.
		 */
		transactionType.addValueChangeListener(new IValueChangeListener<TransactionType>() {

			@Override
			public void handleValueChange(
					ValueChangeEvent<? extends TransactionType> event) {
				TransactionType transactionType2 = event.diff.getNewValue();
				switch (transactionType2) {
				case Buy:
					forceTransactionToBuy();
					break;
				case Sell:
					forceTransactionToSell();
					break;
				case Dividend:
					forceTransactionToDividend();
					break;
				case Transfer:
					forceTransactionToTransfer();
					break;
				case Other:
					forceTransactionToCustom();
					break;
				}
			}
		});

		netAmountEntry.getDataManager().addChangeListenerWeakly(modelListener);

		quantity.addValueChangeListener(new IValueChangeListener<Long>() {
			@Override
			public void handleValueChange(ValueChangeEvent<? extends Long> event) {
				assert(event.diff.getNewValue() != null);
				assert(isPurchaseOrSale());
				if (getTransactionType() == TransactionType.Buy) {
					purchaseOrSaleEntry.setAmount(event.diff.getNewValue());
				} else {
					purchaseOrSaleEntry.setAmount(-event.diff.getNewValue());
				}
			}
		});

		commission.addValueChangeListener(new IValueChangeListener<Long>() {
			@Override
			public void handleValueChange(ValueChangeEvent<? extends Long> event) {
				if (event.diff.getNewValue() == null) {
					if (commissionEntry != null) {
						try {
							getMainEntry().getTransaction().getEntryCollection().deleteElement(commissionEntry);
						} catch (ReferenceViolationException e) {
							// This should not happen because entries are never referenced
							throw new RuntimeException("Internal error", e);
						}
						commissionEntry = null;
					}
				} else {
					if (commissionEntry == null) {
						commissionEntry = getMainEntry().getTransaction().createEntry();
						commissionEntry.setAccount(account.getCommissionAccount());
						StockEntryInfo.getSecurityAccessor().setValue(commissionEntry, (Security)purchaseOrSaleEntry.getCommodity());
					}
					commissionEntry.setAmount(event.diff.getNewValue());
				}
			}
		});

		tax1.addValueChangeListener(new IValueChangeListener<Long>() {
			@Override
			public void handleValueChange(ValueChangeEvent<? extends Long> event) {
				if (event.diff.getNewValue() == null) {
					if (tax1Entry != null) {
						try {
							getMainEntry().getTransaction().getEntryCollection().deleteElement(tax1Entry);
						} catch (ReferenceViolationException e) {
							// This should not happen because entries are never referenced
							throw new RuntimeException("Internal error", e);
						}
						tax1Entry = null;
					}
				} else {
					if (tax1Entry == null) {
						tax1Entry = getMainEntry().getTransaction().createEntry();
						tax1Entry.setAccount(account.getTax1Account());
						StockEntryInfo.getSecurityAccessor().setValue(tax1Entry, (Security)purchaseOrSaleEntry.getCommodity());
					}
					tax1Entry.setAmount(event.diff.getNewValue());
				}
			}
		});

		tax2.addValueChangeListener(new IValueChangeListener<Long>() {
			@Override
			public void handleValueChange(ValueChangeEvent<? extends Long> event) {
				if (event.diff.getNewValue() == null) {
					if (tax2Entry != null) {
						try {
							getMainEntry().getTransaction().getEntryCollection().deleteElement(tax2Entry);
						} catch (ReferenceViolationException e) {
							// This should not happen because entries are never referenced
							throw new RuntimeException("Internal error", e);
						}
						tax2Entry = null;
					}
				} else {
					if (tax2Entry == null) {
						tax2Entry = getMainEntry().getTransaction().createEntry();
						tax2Entry.setAccount(account.getTax2Account());
						StockEntryInfo.getSecurityAccessor().setValue(tax2Entry, (Security)purchaseOrSaleEntry.getCommodity());
					}
					tax2Entry.setAmount(event.diff.getNewValue());
				}
			}
		});

		withholdingTax.addValueChangeListener(new IValueChangeListener<Long>() {
			@Override
			public void handleValueChange(ValueChangeEvent<? extends Long> event) {
				if (event.diff.getNewValue() == null) {
					if (withholdingTaxEntry != null) {
						try {
							getMainEntry().getTransaction().getEntryCollection().deleteElement(withholdingTaxEntry);
						} catch (ReferenceViolationException e) {
							// This should not happen because entries are never referenced
							throw new RuntimeException("Internal error", e);
						}
						withholdingTaxEntry = null;
					}
				} else {
					if (withholdingTaxEntry == null) {
						withholdingTaxEntry = getMainEntry().getTransaction().createEntry();
						withholdingTaxEntry.setAccount(account.getWithholdingTaxAccount());
						// Why did we have this line?
						//							StockEntryInfo.getSecurityAccessor().setValue(withholdingTaxEntry, (Security)purchaseOrSaleEntry.getCommodity());
					}
					withholdingTaxEntry.setAmount(event.diff.getNewValue());
				}
			}
		});

		dividend.addValueChangeListener(new IValueChangeListener<Long>() {
			@Override
			public void handleValueChange(ValueChangeEvent<? extends Long> event) {
				if (event.diff.getNewValue() == null) {
					if (dividendEntry != null) {
						try {
							getMainEntry().getTransaction().getEntryCollection().deleteElement(withholdingTaxEntry);
						} catch (ReferenceViolationException e) {
							// This should not happen because entries are never referenced
							throw new RuntimeException("Internal error", e);
						}
						dividendEntry = null;
					}
				} else {
					if (dividendEntry == null) {
						dividendEntry = getMainEntry().getTransaction().createEntry();
						dividendEntry.setAccount(account.getDividendAccount());
					}
					dividendEntry.setAmount(event.diff.getNewValue());
				}
			}
		});

		security.addValueChangeListener(new IValueChangeListener<Security>() {
			@Override
			public void handleValueChange(ValueChangeEvent<? extends Security> event) {
				// TODO inline this?
				setSecurity(event.diff.getNewValue());
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

			for (Entry entry: getMainEntry().getTransaction().getEntryCollection()) {
				if (entry.getAccount() == account.getDividendAccount()) {
					if (dividendEntry != null) {
						unknownTransactionType = true;
					}
					dividendEntry = entry;
				} else if (entry.getAccount() == account.getDividendAccount()) {
					if (dividendEntry != null) {
						unknownTransactionType = true;
					}
					dividendEntry = entry;
					dividend.setValue(entry.getAmount());
				} else if (entry.getAccount() == account.getWithholdingTaxAccount()) {
					if (withholdingTaxEntry != null) {
						unknownTransactionType = true;
					}
					withholdingTaxEntry = entry;
					withholdingTax.setValue(entry.getAmount());
				} else if (entry.getAccount() == account.getCommissionAccount()) {
					if (commissionEntry != null) {
						unknownTransactionType = true;
					}
					commissionEntry = entry;
					commission.setValue(entry.getAmount());
				} else if (entry.getAccount() == account.getTax1Account()) {
					if (tax1Entry != null) {
						unknownTransactionType = true;
					}
					tax1Entry = entry;
					tax1.setValue(entry.getAmount());
				} else if (entry.getAccount() == account.getTax2Account()) {
					if (tax2Entry != null) {
						unknownTransactionType = true;
					}
					tax2Entry = entry;
					tax2.setValue(entry.getAmount());
				} else if (entry.getAccount() == account) {
					if (entry.getCommodityInternal() instanceof Security) {
						if (purchaseOrSaleEntry != null) {
							unknownTransactionType = true;
						}
						purchaseOrSaleEntry = entry;
					} else if (entry.getCommodityInternal() instanceof Currency) {  //TODO: check for actual currency of account.
						// The only entry affecting the currency balance in this account
						// should be the main entry.
						if (entry != getMainEntry()) {
							unknownTransactionType = true;
						}
					}
				} else if (entry.getAccount() instanceof CapitalAccount
						&& entry.getAccount() != account
						&& entry.getCommodityInternal() == account.getCurrency()) {
					if (transferEntry != null) {
						unknownTransactionType = true;
					}
					transferEntry = entry;
				} else {
					unknownTransactionType = true;
				}
			}

			if (unknownTransactionType) {
				transactionType.setValue(TransactionType.Other);
			} else if (dividendEntry != null
					&& commissionEntry == null
					&& tax1Entry == null
					&& tax2Entry == null
					&& purchaseOrSaleEntry == null
					&& transferEntry == null) {
				transactionType.setValue(TransactionType.Dividend);
				security.setValue(StockEntryInfo.getSecurityAccessor().getValue(dividendEntry));
			} else if (dividendEntry == null
					&& withholdingTaxEntry == null
					&& purchaseOrSaleEntry != null
					&& transferEntry == null) {
				if (purchaseOrSaleEntry.getAmount() >= 0) {
					transactionType.setValue(TransactionType.Buy);
					quantity.setValue(purchaseOrSaleEntry.getAmount());
				} else {
					transactionType.setValue(TransactionType.Sell);
					quantity.setValue(-purchaseOrSaleEntry.getAmount());
				}
				security.setValue((Security)purchaseOrSaleEntry.getCommodityInternal());
			} else if (dividendEntry == null
					&& withholdingTaxEntry == null
					&& commissionEntry == null
					&& tax1Entry == null
					&& tax2Entry == null
					&& purchaseOrSaleEntry == null
					&& transferEntry != null) {
				transactionType.setValue(TransactionType.Transfer);
				security.setValue(null);
			} else {
				transactionType.setValue(TransactionType.Other);
				security.setValue(null);
			}
		}
	}

	@Override
	public Entry getMainEntry() {
		return netAmountEntry;
	}
	
	private void forceTransactionToDividend() {
		assert transactionType.getValue() == TransactionType.Dividend;

		// Get the security from the old transaction, which must be done
		// before we start messing with this transaction.
		Security securityValue = security.getValue();

		/*
		 * Remove entries that are not appropriate for a 'dividend' transaction.
		 */
		EntryCollection entries = getMainEntry().getTransaction().getEntryCollection();
		for (Iterator<Entry> iter = entries.iterator(); iter.hasNext(); ) {
			Entry entry = iter.next();
			if (entry != getMainEntry()
					&& entry != dividendEntry
					&& entry != withholdingTaxEntry) {
				iter.remove();
			}
		}

		commissionEntry = null;
		tax1Entry = null;
		tax2Entry = null;
		purchaseOrSaleEntry = null;
		transferEntry = null;

		if (dividendEntry == null) {
			dividendEntry = entries.createEntry();
			dividendEntry.setAccount(account.getDividendAccount());
			StockEntryInfo.getSecurityAccessor().setValue(dividendEntry, securityValue);
		}

		long grossDividend = -getMainEntry().getAmount();
		if (withholdingTaxEntry != null) {
			grossDividend += withholdingTaxEntry.getAmount();
		}
		dividendEntry.setAmount(grossDividend);
	}

	private void forceTransactionToBuy() {
		forceTransactionToBuyOrSell(TransactionType.Buy);
	}

	private void forceTransactionToSell() {
		forceTransactionToBuyOrSell(TransactionType.Sell);
	}

	private void forceTransactionToBuyOrSell(TransactionType transactionType) {
		assert this.transactionType.getValue() == transactionType;

		EntryCollection entries = getMainEntry().getTransaction().getEntryCollection();
		for (Iterator<Entry> iter = entries.iterator(); iter.hasNext(); ) {
			Entry entry = iter.next();
			if (entry != getMainEntry()
					&& entry != commissionEntry
					&& entry != tax1Entry
					&& entry != tax2Entry
					&& entry != purchaseOrSaleEntry) {
				iter.remove();
			}
		}

		dividendEntry = null;
		withholdingTaxEntry = null;
		transferEntry = null;

		if (purchaseOrSaleEntry == null) {
			purchaseOrSaleEntry = entries.createEntry();
			purchaseOrSaleEntry.setAccount(account);
			purchaseOrSaleEntry.setCommodity(security.getValue());
		}

		// Commission, tax 1, and tax 2 entries may be null in this transaction type.
		// They are created when needed if non-zero amounts are entered.

		// TODO: What is our strategy on changing values to keep
		// the transaction balanced.  Quicken has a dialog box that
		// asks the user what to adjust (with a 'recommended' choice
		// that in my experience is never the correct choice!).

		//		dividendEntry.setAmount(-mainEntry.getAmount());
	}

	private void forceTransactionToTransfer() {
		assert transactionType.getValue() == TransactionType.Transfer;

		EntryCollection entries = getMainEntry().getTransaction().getEntryCollection();
		for (Iterator<Entry> iter = entries.iterator(); iter.hasNext(); ) {
			Entry entry = iter.next();
			if (entry != getMainEntry()
					&& entry != transferEntry) {
				iter.remove();
			}
		}

		dividendEntry = null;
		withholdingTaxEntry = null;
		commissionEntry = null;
		tax1Entry = null;
		tax2Entry = null;
		purchaseOrSaleEntry = null;

		if (transferEntry == null) {
			transferEntry = entries.createEntry();
		}
		transferEntry.setAmount(-getMainEntry().getAmount());
	}

	private void forceTransactionToCustom() {
		assert transactionType.getValue() == TransactionType.Other;

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
		EntryCollection entries = getMainEntry().getTransaction().getEntryCollection();
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
		dividendEntry = null;
		withholdingTaxEntry = null;
		commissionEntry = null;
		tax1Entry = null;
		tax2Entry = null;
		purchaseOrSaleEntry = null;
		transferEntry = null;
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

	public Entry getDividendEntry() {
		Assert.isTrue(isDividend());
		return dividendEntry;
	}

	/**
	 * @return the net amount, being the amount credited or debited from the account
	 * @trackedGetter        
	 */
	public long getNetAmount() {
		return EntryInfo.getAmountAccessor().observe(netAmountEntry).getValue();
	}

	/**
	 * @return the withholding tax amount in a dividend transaction if a
	 *         withholding tax account is configured for the account, being zero if
	 *         no entry exists in the transaction in the withholding tax account
	 * @trackedGetter        
	 */
	public long getWithholdingTax() {
		assert(isDividend());
		assert(account.getWithholdingTaxAccount() != null);
		return (withholdingTax.getValue() == null ? 0 : withholdingTax.getValue());
	}

	public void setWithholdingTax(long withholdingTaxAmount) {
		assert(isDividend());
		assert(account.getWithholdingTaxAccount() != null);
		withholdingTax.setValue(withholdingTaxAmount == 0 ? null : withholdingTaxAmount);
	}

	/**
	 * @return the entry in the transaction that represents the gain or loss in
	 *         the number of shares, or null if this is not a purchase or sale
	 *         transaction
	 */
	public Entry getPurchaseOrSaleEntry() {
		return purchaseOrSaleEntry;
	}

	/**
	 * @return the commission amount in a purchase or sale transaction if a
	 *         commission account is configured for the account, being zero if
	 *         no entry exists in the transaction in the commission account
	 */
	public long getCommission() {
		assert(isPurchaseOrSale());
		return commission.getValue() == null ? 0 : commission.getValue();
	}

	public void setCommission(long commissionAmount) {
		assert(isPurchaseOrSale());
		assert(account.getCommissionAccount() != null);
		commission.setValue(commissionAmount == 0 ? null : commissionAmount);
	}

	/**
	 * @return the tax 1 amount in a purchase or sale transaction if a tax 1 is
	 *         configured for the account, being zero if no entry exists in the
	 *         transaction in the tax 1 account
	 */
	public void setTax1Amount(long tax1Amount) {
		assert(isPurchaseOrSale());
		assert(account.getTax1Account() != null);
		tax1.setValue(tax1Amount == 0 ? null : tax1Amount);
	}

	public void setTax2Amount(long tax2Amount) {
		assert(isPurchaseOrSale());
		assert(account.getTax2Account() != null);
		tax2.setValue(tax2Amount == 0 ? null : tax2Amount);
	}

	/**
	 * @return the entry in the transaction that is the other entry
	 * 		in a transfer transaction, or null if this is not a transfer
	 * 		transaction
	 */
	public Entry getTransferEntry() {
		return transferEntry;
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
		for (Entry eachEntry: getMainEntry().getTransaction().getEntryCollection()) {
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
		switch (transactionType.getValue()) {
		case Buy:
		case Sell:
			if (purchaseOrSaleEntry.getAmount() == 0) {
				throw new InvalidUserEntryException("The quantity of stock in a purchase or sale cannot be zero.", null);
			}
			if (commissionEntry != null
					&& commissionEntry.getAmount() == 0) {
				getMainEntry().getTransaction().deleteEntry(commissionEntry);
				commissionEntry = null;
			}
			if (tax1Entry != null
					&& tax1Entry.getAmount() == 0) {
				getMainEntry().getTransaction().deleteEntry(tax1Entry);
				tax1Entry = null;
			}
			if (tax2Entry != null
					&& tax2Entry.getAmount() == 0) {
				getMainEntry().getTransaction().deleteEntry(tax2Entry);
				tax2Entry = null;
			}
			break;
		case Dividend:
			if (dividendEntry.getAmount() == 0) {
				throw new InvalidUserEntryException("The amount of a dividend cannot be zero.", null);
			}
			if (withholdingTaxEntry != null
					&& withholdingTaxEntry.getAmount() == 0
					&& withholdingTaxEntry.getMemo() == null) {
				getMainEntry().getTransaction().deleteEntry(withholdingTaxEntry);
				withholdingTaxEntry = null;
			}
			break;
		case Transfer:
			if (transferEntry.getAmount() == 0) {
				throw new InvalidUserEntryException("The amount of a transfer cannot be zero.", null);
			}
			break;
		case Other:
			// We don't allow any amounts to be zero except the listed entry
			// (the listed entry is used to ensure a transaction appears in this
			// list even if the transaction does not result in a change in the cash
			// balance).
			Entry mainEntry = getMainEntry();
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
	 * This method is called when the user edits the security.
	 * <P>
	 * The purchase and sale entry contains the security as the commodity of the value in
	 * the amount field.  For the other entries the security is only a reference.  For example
	 * for a dividend payment the commodity must be set to the currency of the dividend payment.
	 *
	 * @param security
	 */
	private void setSecurity(Security security) {
		if (isPurchaseOrSale()) {
			purchaseOrSaleEntry.setCommodity(security);
			if (commissionEntry != null) {
				StockEntryInfo.getSecurityAccessor().setValue(commissionEntry, security);
			}
			if (tax1Entry != null) {
				StockEntryInfo.getSecurityAccessor().setValue(tax1Entry, security);
			}
			if (tax2Entry != null) {
				StockEntryInfo.getSecurityAccessor().setValue(tax2Entry, security);
			}
		} else if (isDividend()) {
			StockEntryInfo.getSecurityAccessor().setValue(dividendEntry, security);
			if (withholdingTaxEntry != null) {
				StockEntryInfo.getSecurityAccessor().setValue(withholdingTaxEntry, security);
			}
		}
	}

	public IObservableValue<Long> commission() {
		return commission;
	}

	public IObservableValue<Long> quantity() {
		return quantity;
	}

	public IObservableValue<BigDecimal> sharePrice() {
		return sharePrice;
	}

	public IObservableValue<Long> tax1() {
		return tax1;
	}

	public IObservableValue<Long> tax2() {
		return tax2;
	}

	public IObservableValue<Long> dividend() {
		return dividend;
	}

	public IObservableValue<Long> withholdingTax() {
		return withholdingTax;
	}

	public IObservableValue<Security> security() {
		return security;
	}

	/**
	 *
	 * @return the quantity of shares being bought or sold, always being a
	 *         positive number, or undefined if the transaction is not a
	 *         purchase or sale transaction
	 */
	public long getQuantity() {
		return quantity.getValue() == null ? 0 : quantity.getValue();
	}

	public long getTax1Amount() {
		assert(isPurchaseOrSale());
		return tax1.getValue() == null ? 0 : tax1.getValue();
	}

	/**
	 * @return the tax 2 amount in a purchase or sale transaction if a tax 2 is
	 *         configured for the account, being zero if no entry exists in the
	 *         transaction in the tax 2 account
	 */
	public long getTax2Amount() {
		assert(isPurchaseOrSale());
		return tax2.getValue() == null ? 0 : tax2.getValue();
	}

	public void setQuantity(long quantityAmount) {
		this.quantity.setValue(quantityAmount == 0 ? null : quantityAmount);
	}
}
