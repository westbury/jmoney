package net.sf.jmoney.stocks.pages;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.IValueChangeListener;
import org.eclipse.core.databinding.observable.value.ValueChangeEvent;
import org.eclipse.core.databinding.observable.value.WritableValue;

import net.sf.jmoney.entrytable.InvalidUserEntryException;
import net.sf.jmoney.model2.Currency;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.EntryInfo;
import net.sf.jmoney.model2.Transaction;
import net.sf.jmoney.stocks.model.Security;
import net.sf.jmoney.stocks.model.StockAccount;
import net.sf.jmoney.stocks.model.StockEntryInfo;
import net.sf.jmoney.stocks.pages.StockEntryRowControl.TransactionType;

public class StockBuyOrSellFacade extends BaseEntryFacade {

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
	
	// bound to getPurchaseOrSaleEntry().getAmount() except this is always positive whereas
	// getPurchaseOrSaleEntry().getAmount() would be negative for a sale
	private final IObservableValue<Long> quantity = new WritableValue<Long>();

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


	public StockBuyOrSellFacade(Transaction transaction, TransactionType transactionTypeName, String transactionName, StockAccount stockAccount) {
		super(transaction, TransactionType.Dividend, "");

		purchaseOrSaleEntry = observeEntry("acquisition-or-disposal");
		commissionEntry = observeEntry("commission");
		tax1Entry = observeEntry("tax1");
		tax2Entry = observeEntry("tax2");
		
		if (purchaseOrSaleEntry.getValue() == null) {
			createEntry("acquisition-or-disposal");
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

		security.addValueChangeListener(new IValueChangeListener<Security>() {
			@Override
			public void handleValueChange(ValueChangeEvent<? extends Security> event) {
				setSecurity(event.diff.getNewValue());
			}
		});
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
				entry = createEntry("commission");
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
				entry = createEntry("tax1");
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
				entry = createEntry("tax2");
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
		BigDecimal totalShares = BigDecimal.valueOf(purchaseOrSaleEntry.getValue().getAmount())
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
