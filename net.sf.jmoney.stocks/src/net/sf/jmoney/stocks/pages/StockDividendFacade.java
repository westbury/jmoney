package net.sf.jmoney.stocks.pages;

import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.IValueChangeListener;
import org.eclipse.core.databinding.observable.value.ValueChangeEvent;
import org.eclipse.core.databinding.observable.value.WritableValue;

import net.sf.jmoney.entrytable.InvalidUserEntryException;
import net.sf.jmoney.model2.Commodity;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.EntryInfo;
import net.sf.jmoney.model2.Transaction;
import net.sf.jmoney.stocks.model.Security;
import net.sf.jmoney.stocks.model.StockAccount;
import net.sf.jmoney.stocks.model.StockEntry;
import net.sf.jmoney.stocks.model.StockEntryInfo;
import net.sf.jmoney.stocks.pages.StockEntryRowControl.BuyOrSellEntryType;
import net.sf.jmoney.stocks.pages.StockEntryRowControl.DividendEntryType;
import net.sf.jmoney.stocks.pages.StockEntryRowControl.TransactionType;

public class StockDividendFacade extends BaseEntryFacade {

	private IObservableValue<Entry> dividendEntry;
	private IObservableValue<Entry> withholdingTaxEntry;

	private final IObservableValue<Security> security = new WritableValue<>();
	private final IObservableValue<Long> dividend = new WritableValue<Long>();
	private final IObservableValue<Long> withholdingTax = new WritableValue<Long>();


//	/**
//	 * The amount of the cash payment, or null if there is no cash payment,
//	 * but never zero and this observable should never be set to zero
//	 */
//	private final IObservableValue<Long> cashPayment = new WritableValue<Long>();

	/**
	 * The stock that is paying the dividend.
	 */
	private final IObservableValue<Security> dividendSecurity = new WritableValue<Security>();

	public StockDividendFacade(Transaction transaction, String transactionName, StockAccount stockAccount) {
		super(transaction, TransactionType.Dividend, "");

		dividendEntry = observeEntry("dividend");
		withholdingTaxEntry = observeEntry("withholding-tax");
		
		if (dividendEntry.getValue() == null) {
			createEntry(DividendEntryType.Dividend);
		}
		
		Security security = StockEntryInfo.getSecurityAccessor().getValue(dividendEntry.getValue());
		this.security.setValue(security);
		
		// TODO the following is incorrect because we must listen to the underlying datastore
		
		if (withholdingTaxEntry.getValue() != null) {
			withholdingTax.setValue(withholdingTaxEntry.getValue().getAmount());
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

	/**
	 * @return the entry in the transaction that represents the stock in the company
	 * 			being taken over.
	 */
	public StockEntry getGrossDividendEntry() {
		Entry baseEntry = dividendEntry.getValue();
		return baseEntry.getExtension(StockEntryInfo.getPropertySet(), true);
	}

	/**
	 * @trackedGetter        
	 */
	public long getDividendAmount() {
		Entry entry = dividendEntry.getValue();
		if (entry == null) {
			// This should not happen because there is always an entry for the dividend amount
			return 0;
		} else {
			return EntryInfo.getAmountAccessor().observe(entry).getValue();
		}
	}

	public void setDividendAmount(long amount) {
		Entry entry = dividendEntry.getValue();
		// The entry should always exist as there is always a dividend amount.
		entry.setAmount(amount);
	}

	/**
	 * @trackedGetter        
	 */
	public long getWithholdingTaxAmount() {
		Entry entry = withholdingTaxEntry.getValue();
		if (entry == null) {
			return 0;
		} else {
			return EntryInfo.getAmountAccessor().observe(entry).getValue();
		}
	}

	public void setWithholdingTaxAmount(long amount) {
		Entry entry = withholdingTaxEntry.getValue();
		if (amount == 0) {
			if (entry != null) {
				entry.getTransaction().deleteEntry(entry);
			}
		} else {
			if (entry == null) {
				entry = createEntry(DividendEntryType.WithholdingTax);
			}
			entry.setAmount(amount);
		}
	}

	/**
	 * @return the security for which the dividend is being paid.
	 * @trackedGetter        
	 */
	public Security getDividendSecurity() {
		return StockEntryInfo.getSecurityAccessor().getValue(dividendEntry.getValue());
	}

	public void setDividendSecurity(Security security) {
		
		if (this.withholdingTaxEntry.getValue() != null) {
			StockEntryInfo.getSecurityAccessor().setValue(dividendEntry.getValue(), security);
		} else {
			dividendSecurity.setValue(security);
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
		if (dividendEntry.getValue().getAmount() == 0) {
			throw new InvalidUserEntryException("The quantity of stock reliquished in a takeover cannot be zero.", null);
		}

		if (withholdingTaxEntry.getValue() != null
				&& withholdingTaxEntry.getValue().getAmount() == 0) {
			getMainEntry().getTransaction().deleteEntry(withholdingTaxEntry.getValue());
//			paymentInStockEntry.setValue() = null;  Not needed - should update from datastore
		}
	}

	public IObservableValue<Long> dividend() {
		return dividend;
	}

	public IObservableValue<Long> withholdingTax() {
		return withholdingTax;
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
		StockEntryInfo.getSecurityAccessor().setValue(dividendEntry.getValue(), security);
		if (withholdingTaxEntry.getValue() != null) {
			StockEntryInfo.getSecurityAccessor().setValue(withholdingTaxEntry.getValue(), security);
		}
	}

	/**
	 * This is used only so we can bind a single control to the security for the purpose of persisting the security when forcing the transaction type.
	 */
	public IObservableValue<Security> security() {
		return security;
	}

	/**
	 * This is used only for the purpose of persisting the security when forcing the transaction type.
	 */
	@Override
	public Security getSecurity() {
		return security.getValue();
	}

	public IObservableValue<Long> grossDividend() {
		// TODO this must be writable because it is set with default binding.
		// Should we have the default binding at all???????
		return new WritableValue<Long>();
	}

}
