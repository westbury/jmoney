package net.sf.jmoney.stocks.pages;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import org.eclipse.core.databinding.observable.Diffs;
import org.eclipse.core.databinding.observable.value.AbstractObservableValue;
import org.eclipse.core.databinding.observable.value.ComputedValue;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.IValueChangeListener;
import org.eclipse.core.databinding.observable.value.ValueChangeEvent;
import org.eclipse.core.databinding.observable.value.ValueDiff;
import org.eclipse.core.databinding.observable.value.WritableValue;

import net.sf.jmoney.entrytable.InvalidUserEntryException;
import net.sf.jmoney.isolation.IListPropertyAccessor;
import net.sf.jmoney.isolation.IModelObject;
import net.sf.jmoney.isolation.IScalarPropertyAccessor;
import net.sf.jmoney.isolation.SessionChangeAdapter;
import net.sf.jmoney.isolation.SessionChangeListener;
import net.sf.jmoney.model2.Account;
import net.sf.jmoney.model2.Commodity;
import net.sf.jmoney.model2.Currency;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.EntryInfo;
import net.sf.jmoney.model2.Transaction;
import net.sf.jmoney.model2.TransactionInfo;
import net.sf.jmoney.stocks.model.Security;
import net.sf.jmoney.stocks.model.StockAccount;
import net.sf.jmoney.stocks.model.StockEntryInfo;
import net.sf.jmoney.stocks.types.BuyOrSellEntryType;
import net.sf.jmoney.stocks.types.TransactionType;

public class StockBuyOrSellFacade extends BaseEntryFacade {

	private IObservableValue<IObservableValue<Long>> computedQuantityObservable = new ComputedValue<IObservableValue<Long>>() {
		@Override
		protected IObservableValue<Long> calculate() {
			Entry currentPurchaseOrSaleEntry = purchaseOrSaleEntry.getValue();
			if (currentPurchaseOrSaleEntry != null) {
				return new ObservableQuantity(currentPurchaseOrSaleEntry);
			}
			return null;
		}
	};

	private class ObservableQuantity extends AbstractObservableValue<Long> {
		private final Entry currentPurchaseOrSaleEntry;

		private SessionChangeListener modelListener = new SessionChangeAdapter() {
			@Override
			public void objectChanged(IModelObject changedObject, IScalarPropertyAccessor changedProperty, Object oldValue,	Object newValue) {
				if (changedObject == currentPurchaseOrSaleEntry
						&& changedProperty == EntryInfo.getAmountAccessor()) {
					if (transactionType == TransactionType.Buy) {
						fireValueChange(Diffs.createValueDiff((Long)oldValue, (Long)newValue));
					} else if (transactionType == TransactionType.Sell) {
						fireValueChange(Diffs.createValueDiff(-(Long)oldValue, -(Long)newValue));
					} else {
						throw new RuntimeException();
					}
				}
			}
		};
		
		private ObservableQuantity(Entry currentPurchaseOrSaleEntry) {
			this.currentPurchaseOrSaleEntry = currentPurchaseOrSaleEntry;
		}

		@Override
		public Object getValueType() {
			return Long.class;
		}

		@Override
		protected Long doGetValue() {
			if (transactionType == TransactionType.Buy) {
				return currentPurchaseOrSaleEntry.getAmount();
			} else if (transactionType == TransactionType.Sell) {
				return -currentPurchaseOrSaleEntry.getAmount();
			} else {
				throw new RuntimeException();
			}
		}

		@Override
		protected void doSetValue(Long value) {
			if (value == null) {
				currentPurchaseOrSaleEntry.setAmount(0);
			} else if (transactionType == TransactionType.Buy) {
				currentPurchaseOrSaleEntry.setAmount(value);
			} else if (transactionType == TransactionType.Sell) {
				currentPurchaseOrSaleEntry.setAmount(-value);
			} else {
				throw new RuntimeException();
			}
		}

		@Override
		protected void firstListenerAdded() {
			currentPurchaseOrSaleEntry.getDataManager().addChangeListener(modelListener);
		}

		@Override
		protected void lastListenerRemoved() {
			currentPurchaseOrSaleEntry.getDataManager().removeChangeListener(modelListener);
		}
	}

	static private DateFormat dateFormat = new SimpleDateFormat("dd-MMM-yyyy");

	private TransactionType transactionType; // stock.buy or stock.sell
	
	/**
	 * the entry for the commission, or null if this is not a purchase or sale
	 * transaction or if no commission account is configured for this stock
	 * account because commissions are never charged on any purchases or sales
	 * in this account, and possibly null if there can be a commission but none
	 * has been entered for this entry
	 */
	private IObservableValue<Entry> commissionEntry;

	private IObservableValue<Entry> tax1Entry;
	private IObservableValue<Entry> tax2Entry;
	private IObservableValue<Entry> purchaseOrSaleEntry;

	private IObservableValue<Security> security = new WritableValue<>();
	
	/** bound to getPurchaseOrSaleEntry().getAmount() except this is always positive whereas
	 getPurchaseOrSaleEntry().getAmount() would be negative for a sale
	 */
	private final IObservableValue<Long> quantity = new RetargetableObservable<Long>(Long.class, computedQuantityObservable);

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
	 * Multiple observers may be using facades on the same transaction.  Therefore constructing a
	 * facade should not alter the transaction.  A facade should only be constructed once we know
	 * the transaction meets the restrictions of the facade (has all compulsory entries).  Otherwise
	 * we get horrible recursion as creating a facade invokes all sorts of other listeners to do stuff.
	 */

	/**
	 * TODO this absolutely must not change the transaction....
	 */
	public StockBuyOrSellFacade(Transaction transaction, TransactionType transactionType, String transactionName, StockAccount stockAccount) {
		super(transaction, transactionType, "");
		this.transactionType = transactionType;
		
		purchaseOrSaleEntry = observeEntry("acquisition-or-disposal");
		commissionEntry = observeEntry("commission");
		tax1Entry = observeEntry("tax1");
		tax2Entry = observeEntry("tax2");
		
		if (purchaseOrSaleEntry.getValue() == null) {
			// FIXME this can't change the model
			createEntry(BuyOrSellEntryType.AquisitionOrDisposal);
		}
		
		// Now the acquisition-or-disposal entry has been created, we can set the calculated
		// price. 
		sharePrice().setValue(calculatePrice());

		Commodity security = purchaseOrSaleEntry.getValue().getCommodity();
		if (security instanceof Security) {
			this.security.setValue((Security)security);
		}
		
		switch (transactionType) {
		case Buy:
			quantity.setValue(purchaseOrSaleEntry.getValue().getAmount());
			break;
		case Sell:
			quantity.setValue(-purchaseOrSaleEntry.getValue().getAmount());
			break;
		default:
			throw new RuntimeException("Bad case");
		}
		
		if (commissionEntry.getValue() != null) {
			commission.setValue(commissionEntry.getValue().getAmount());
		}
		if (tax1Entry.getValue() != null) {
			tax1.setValue(tax1Entry.getValue().getAmount());
		}
		if (tax2Entry.getValue() != null) {
			tax2.setValue(tax2Entry.getValue().getAmount());
		}

		/*
		 * As there is no entry when the quantity is zero, we maintain a writable value.
		 * This ensures the security is persisted even when the quantity of it is set to zero.
		 */
		// NO, this must come from datastore to this writable value.
//		takingOverSecurity.addValueChangeListener(new IValueChangeListener<Security>() {
//			@Override
//			public void handleValueChange(ValueChangeEvent<? extends Security> event) {
//				Security newSecurity = event.diff.getNewValue();
//				if (paymentInStockEntry.getValue() != null) {
//					StockEntryInfo.getSecurityAccessor().setValue(paymentInStockEntry.getValue(), newSecurity);
//				}
//			}
//		});

		this.security.addValueChangeListener(new IValueChangeListener<Security>() {
			@Override
			public void handleValueChange(ValueChangeEvent<? extends Security> event) {
				setSecurity(event.diff.getNewValue());
			}
		});
	}

	public Entry getPurchaseOrSaleEntry() {
		return this.purchaseOrSaleEntry.getValue();
	}

	/**
	 * @trackedGetter        
	 */
	public long getQuantityAmount() {
		Entry entry = purchaseOrSaleEntry.getValue();
		if (entry == null) {
			return 0;
		} else {
			return EntryInfo.getAmountAccessor().observe(entry).getValue();
		}
	}
	
	public void setQuantity(long amount) {
		if (this.transactionType == TransactionType.Buy) {
			purchaseOrSaleEntry.getValue().setAmount(amount);
		} else if (this.transactionType == TransactionType.Sell) {
			purchaseOrSaleEntry.getValue().setAmount(-amount);
		} else {
			throw new RuntimeException();
		}
	}

	/**
	 * @trackedGetter        
	 */
	public long getCommissionAmount() {
		Entry entry = commissionEntry.getValue();
		if (entry == null) {
			return 0;
		} else {
			return EntryInfo.getAmountAccessor().observe(entry).getValue();
		}
	}

	public void setCommissionAmount(long amount) {
		Entry entry = commissionEntry.getValue();
		if (amount == 0) {
			if (entry != null) {
				entry.getTransaction().deleteEntry(entry);
			}
		} else {
			if (entry == null) {
				entry = createEntry(BuyOrSellEntryType.Commission);
			}
			entry.setAmount(amount);
		}
	}

	/**
	 * @trackedGetter        
	 */
	public long getTax1Amount() {
		Entry entry = tax1Entry.getValue();
		if (entry == null) {
			return 0;
		} else {
			return EntryInfo.getAmountAccessor().observe(entry).getValue();
		}
	}

	public void setTax1Amount(long amount) {
		Entry entry = tax1Entry.getValue();
		if (amount == 0) {
			if (entry != null) {
				entry.getTransaction().deleteEntry(entry);
			}
		} else {
			if (entry == null) {
				entry = createEntry(BuyOrSellEntryType.Tax1);
			}
			entry.setAmount(amount);
		}
	}

	/**
	 * @trackedGetter        
	 */
	public long getTax2Amount() {
		Entry entry = tax2Entry.getValue();
		if (entry == null) {
			return 0;
		} else {
			return EntryInfo.getAmountAccessor().observe(entry).getValue();
		}
	}

	public void setTax2Amount(long amount) {
		Entry entry = tax2Entry.getValue();
		if (amount == 0) {
			if (entry != null) {
				entry.getTransaction().deleteEntry(entry);
			}
		} else {
			if (entry == null) {
				entry = createEntry(BuyOrSellEntryType.Tax2);
			}
			entry.setAmount(amount);
		}
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
		if (purchaseOrSaleEntry.getValue() == null) {
			return new BigDecimal(43);
		}
		BigDecimal totalShares = BigDecimal.valueOf(purchaseOrSaleEntry.getValue().getAmount())
				.movePointLeft(3);

		long totalCash = 0;
		for (Entry eachEntry: getMainEntry().getTransaction().getEntryCollection()) {
			if (eachEntry.getCommodityInternal() instanceof Currency) {
				totalCash += eachEntry.getAmount();
			}
		}

		if (totalCash == 0) {
			System.out.println("Transaction dated " + dateFormat.format(transaction.getDate()) + " is a buy/sell but cash adds up to zero!");
			return null;
		}
		if (totalShares.compareTo(BigDecimal.ZERO) == 0) {
			System.out.println("Transaction dated " + dateFormat.format(transaction.getDate()) + " is a buy/sell but no shares were aquired or disposed of!");
			return null;
		}
		/*
		 * Either we gain cash and lose stock, or we lose cash and gain
		 * stock. Hence we need to negate to get a positive value.
		 */
		BigDecimal price = BigDecimal.valueOf(-totalCash).movePointLeft(2).divide(totalShares, 4, RoundingMode.HALF_UP);
		return price;
	}

	public void specificValidation() throws InvalidUserEntryException {
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
		if (purchaseOrSaleEntry.getValue().getAmount() == 0) {
			throw new InvalidUserEntryException("The quantity of stock in a purchase or sale cannot be zero.", null);
		}
		if (commissionEntry.getValue() != null
				&& commissionEntry.getValue().getAmount() == 0) {
			getMainEntry().getTransaction().deleteEntry(commissionEntry.getValue());
			commissionEntry = null;
		}
		if (tax1Entry.getValue() != null
				&& tax1Entry.getValue().getAmount() == 0) {
			getMainEntry().getTransaction().deleteEntry(tax1Entry.getValue());
			tax1Entry = null;
		}
		if (tax2Entry != null
				&& tax2Entry.getValue().getAmount() == 0) {
			getMainEntry().getTransaction().deleteEntry(tax2Entry.getValue());
			tax2Entry = null;
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
		purchaseOrSaleEntry.getValue().setCommodity(security);
		if (commissionEntry.getValue() != null) {
			StockEntryInfo.getSecurityAccessor().setValue(commissionEntry.getValue(), security);
		}
		if (tax1Entry.getValue() != null) {
			StockEntryInfo.getSecurityAccessor().setValue(tax1Entry.getValue(), security);
		}
		if (tax2Entry.getValue() != null) {
			StockEntryInfo.getSecurityAccessor().setValue(tax2Entry.getValue(), security);
		}
	}

	@Override
	public Security getSecurity() {
		return security.getValue();
	}

	public IObservableValue<Security> security() {
		return security;
	}

}
