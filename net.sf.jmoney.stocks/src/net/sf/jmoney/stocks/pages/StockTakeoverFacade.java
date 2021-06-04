package net.sf.jmoney.stocks.pages;

import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.WritableValue;

import net.sf.jmoney.entrytable.InvalidUserEntryException;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.EntryInfo;
import net.sf.jmoney.model2.Transaction;
import net.sf.jmoney.stocks.model.Security;
import net.sf.jmoney.stocks.model.StockAccount;
import net.sf.jmoney.stocks.model.StockEntryInfo;
import net.sf.jmoney.stocks.pages.StockEntryRowControl.TakeoverEntryType;
import net.sf.jmoney.stocks.pages.StockEntryRowControl.TransactionType;

public class StockTakeoverFacade extends BaseEntryFacade {

	private IObservableValue<Entry> relinquishedStockEntry;
	private IObservableValue<Entry> paymentInStockEntry;

	/**
	 * The amount of the cash payment, or null if there is no cash payment,
	 * but never zero and this observable should never be set to zero
	 */
	private final IObservableValue<Long> cashPayment = new WritableValue<Long>();

	/**
	 * The stock that is being used to pay for the take-over, or null if the takeover was paid for
	 * entirely in cash.
	 * 
	 * This tracks the underlying entry in the datastore, if there is such an entry.
	 * If there is no such entry then this is the data source.
	 */
	private final IObservableValue<Security> takingOverSecurity = new WritableValue<Security>();

	/**
	 * The stock that is being taken over.
	 */
	private final IObservableValue<Security> takenOverSecurity = new WritableValue<Security>();

	public StockTakeoverFacade(Transaction transaction, String transactionName, StockAccount stockAccount) {
		super(transaction, TransactionType.Takeover, "");

		relinquishedStockEntry = observeEntry("takenOver");
		paymentInStockEntry = observeEntry("takingOver");
		
		if (relinquishedStockEntry.getValue() == null) {
			createEntry(TakeoverEntryType.TakenOver);
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


	}

	public IObservableValue<Security> takingOverSecurity() {
		return takingOverSecurity;
	}

	public IObservableValue<Security> takenOverSecurity() {
		return takenOverSecurity;
	}

	/**
	 * @return the entry in the transaction that represents the stock in the company
	 * 			being taken over.
	 */
	public Entry getRelinquishedStockEntry() {
		return relinquishedStockEntry.getValue();
	}

	/**
	 * @trackedGetter        
	 */
	public long getRelinquishedStockQuantity() {
		Entry entry = relinquishedStockEntry.getValue();
		if (entry == null) {
			// This should not happen because there is always relinquished stock
			return 0;
		} else {
			return EntryInfo.getAmountAccessor().observe(entry).getValue();
		}
	}

	public void setRelinquishedStockQuantity(long quantity) {
		Entry entry = relinquishedStockEntry.getValue();
		// The entry should always exist as there is always relinquished stock.
		entry.setAmount(quantity);
	}

	/**
	 * @trackedGetter        
	 */
	public long getPaymentInStockQuantity() {
		Entry entry = paymentInStockEntry.getValue();
		if (entry == null) {
			return 0;
		} else {
			return EntryInfo.getAmountAccessor().observe(entry).getValue();
		}
	}

	public void setPaymentInStockQuantity(long quantity) {
		Entry entry = paymentInStockEntry.getValue();
		if (quantity == 0) {
			if (entry != null) {
				entry.getTransaction().deleteEntry(entry);
			}
		} else {
			if (entry == null) {
				entry = createEntry(TakeoverEntryType.TakingOver);
			}
			entry.setAmount(quantity);
		}
	}

	/**
	 * @return the security in the taking-over company, being that given in payment.
	 * @trackedGetter        
	 */
	public Security getTakingOverSecurity() {
		return takingOverSecurity.getValue();
	}

	public void setTakingOverSecurity(Security security) {
		
		if (this.paymentInStockEntry.getValue() != null) {
			StockEntryInfo.getSecurityAccessor().setValue(paymentInStockEntry.getValue(), security);
		} else {
			takingOverSecurity.setValue(security);
		}
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
		if (relinquishedStockEntry.getValue().getAmount() == 0) {
			throw new InvalidUserEntryException("The quantity of stock reliquished in a takeover cannot be zero.", null);
		}

		if (paymentInStockEntry.getValue() != null
				&& paymentInStockEntry.getValue().getAmount() == 0) {
			getMainEntry().getTransaction().deleteEntry(paymentInStockEntry.getValue());
//			paymentInStockEntry.setValue() = null;  Not needed - should update from datastore
		}
	}

	/**
	 * This is used only to persist the 'security' across transaction types, so when forcing a transaction
	 * type from one to another, the security is used in the new transaction.
	 */
	@Override
	public Security getSecurity() {
		return null;
	}

}
