package net.sf.jmoney.stocks.pages;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import net.sf.jmoney.entrytable.BaseEntryRowControl;
import net.sf.jmoney.entrytable.Block;
import net.sf.jmoney.entrytable.FocusCellTracker;
import net.sf.jmoney.entrytable.InvalidUserEntryException;
import net.sf.jmoney.entrytable.RowSelectionTracker;
import net.sf.jmoney.entrytable.VirtualRowTable;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.TransactionManagerForAccounts;
import net.sf.jmoney.stocks.model.RatesTable;
import net.sf.jmoney.stocks.model.StockAccount;
import net.sf.jmoney.stocks.pages_new.DefaultProvidingContext;
import net.sf.jmoney.stocks.pages_new.DefaultValueBinding;

import org.eclipse.core.databinding.observable.value.ComputedValue;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.IValueChangeListener;
import org.eclipse.core.databinding.observable.value.ValueChangeEvent;
import org.eclipse.core.databinding.observable.value.WritableValue;
import org.eclipse.swt.widgets.Composite;

public class StockEntryRowControl extends BaseEntryRowControl<StockEntryData, StockEntryRowControl> {

	public enum TransactionType {
		Buy,
		Sell,
		Dividend,
		Transfer,
		Other
	}

	private DefaultProvidingContext context = null;

	private final ArrayList<ITransactionTypeChangeListener> transactionTypeChangeListeners = new ArrayList<ITransactionTypeChangeListener>();

	/**
	 * The share price currently shown to the user, or null if
	 * this is a new entry and no share price has been entered.
	 * <P>
	 * This is saved here (not fetched each time) because we often
	 * update other fields when fields are updated in order to keep
	 * the transaction balanced.  We need to know the previous price,
	 * not the price that would have balanced the transaction, when
	 * we do these calculations.
	 */
	private final IObservableValue<BigDecimal> sharePrice = new WritableValue<BigDecimal>();

	private final List<IPropertyChangeListener<BigDecimal>> stockPriceListeners = new ArrayList<IPropertyChangeListener<BigDecimal>>();

	private DefaultValueBinding<Long> withholdingTaxBinding = null;

	private DefaultValueBinding<Long> tax2Binding;

	private DefaultValueBinding<Long> tax1Binding;

	private DefaultValueBinding<Long> commissionBinding;

	private DefaultValueBinding<Long> quantityBinding;

	private DefaultValueBinding<BigDecimal> sharePriceBinding;

	public StockEntryRowControl(final Composite parent, int style, VirtualRowTable rowTable, Block<StockEntryData, ? super StockEntryRowControl> rootBlock, final RowSelectionTracker selectionTracker, final FocusCellTracker focusCellTracker) {
		super(parent, style, rowTable, rootBlock, selectionTracker, focusCellTracker);
		init(this, this, rootBlock);
	}

	@Override
	public void setInput(StockEntryData inputEntryData) {
		if (inputEntryData == null) {
			System.out.println("here");
		}
		/*
		 * We do this first even though it is done again when the super method
		 * is called.  This sets the 'input' value in the base object and means
		 * the following code can use that field.
		 */
		input = inputEntryData;

		// This must be done before we add the default bindings
		if (uncommittedEntryData.isPurchaseOrSale()) {
			sharePrice.setValue(uncommittedEntryData.calculatePrice());
		} else {
			sharePrice.setValue(null);
		}

		// Dispose of default value binding for previous input
		// Is this clean?
		if (context != null) {
			context.dispose();
		}

		context = new DefaultProvidingContext();

		//		if (inputEntryData.getTransactionType() == null) {
		//
		//			/*
		//			 * This is a new transaction so bind the default values
		//			 */
		//			bindDefaultNetAmount();
		//			bindDefaultQuantity();
		//			bindDefaultSharePrice();
		//			bindDefaultCommission();
		//			bindDefaultTax1();
		//			bindDefaultTax2();
		//			bindDefaultWithholdingTax();
		//		} else {
		//			/*
		//			 * This is an existing transaction.  Entries are bound to default values only if they are
		//			 * not applicable for the current transaction type.  So only if the user
		//			 * changes the transaction type will any values be updated with calculated values.
		//			 */
		//
		//			switch (inputEntryData.getTransactionType()) {
		//			case Buy:
		//			case Sell:
		//				bindDefaultWithholdingTax();
		//				break;
		//			case Dividend:
		//				bindDefaultQuantity();
		//				bindDefaultSharePrice();
		//				bindDefaultCommission();
		//				bindDefaultTax1();
		//				bindDefaultTax2();
		//				break;
		//			case Transfer:
		//				bindDefaultQuantity();
		//				bindDefaultSharePrice();
		//				bindDefaultCommission();
		//				bindDefaultTax1();
		//				bindDefaultTax2();
		//				bindDefaultWithholdingTax();
		//				break;
		//			case Other:
		//				bindDefaultQuantity();
		//				bindDefaultSharePrice();
		//				bindDefaultCommission();
		//				bindDefaultTax1();
		//				bindDefaultTax2();
		//				bindDefaultWithholdingTax();
		//				break;
		//			}
		//		}

		/*
		 * Bind the default values if and only if this is a new transaction
		 */
		if (committedEntryData.getEntry() == null) {
			bindDefaultNetAmount();

			if (committedEntryData != inputEntryData) {
				System.out.println("here");
			}
			inputEntryData.transactionType().addValueChangeListener(new IValueChangeListener<TransactionType>() {

				@Override
				public void handleValueChange(ValueChangeEvent<TransactionType> event) {
					if (event.diff.getOldValue() != null) {
						switch (event.diff.getOldValue()) {
						case Buy:
						case Sell:
							quantityBinding.dispose();
							sharePriceBinding.dispose();
							commissionBinding.dispose();
							tax1Binding.dispose();
							tax2Binding.dispose();
							break;
						case Dividend:
							withholdingTaxBinding.dispose();
							break;
						case Transfer:
							break;
						case Other:
							break;
						}
					} else {
						switch (event.diff.getNewValue()) {
						case Buy:
						case Sell:
							bindDefaultQuantity();
							bindDefaultSharePrice();
							bindDefaultCommission();
							bindDefaultTax1();
							bindDefaultTax2();
							break;
						case Dividend:
							bindDefaultWithholdingTax();
							break;
						case Transfer:
							break;
						case Other:
							break;
						}
					}
				}
			});
		}

		// TODO review this
		this.addTransactionTypeChangeListener(new ITransactionTypeChangeListener() {
			@Override
			public void transactionTypeChanged() {
				if (uncommittedEntryData.isPurchaseOrSale()) {
					sharePrice.setValue(uncommittedEntryData.calculatePrice());
				} else {
					sharePrice.setValue(null);
				}
			}
		});

		/*
		 * This must be called after we have set our own stuff up.  The reason
		 * being that this call loads controls (such as the stock price control).
		 * These controls will not load correctly if this object is not set up.
		 */
		super.setInput(inputEntryData);
	}

	private void bindDefaultSharePrice() {
		sharePriceBinding = context.bindDefault(uncommittedEntryData.sharePrice(), new ComputedValue<BigDecimal>() {
			@Override
			protected BigDecimal calculate() {
				/*
				 * The user would not usually enter the net amount for the
				 * transaction because it is hard to calculate backwards from this.
				 * The rates tables are all based on the gross amount. Also, there
				 * may be a number of calculated commissions and taxes and we would
				 * not know which to adjust. Therefore in most cases we leave the
				 * transaction unbalanced and force the user to correct it when the
				 * transaction is saved.
				 * 
				 * However, if there are no commission or taxes configured for the
				 * commodity type in this account then we can calculate backwards.
				 */
				StockAccount account = (StockAccount)getUncommittedEntryData().getEntry().getAccount();

				if (account.getCommissionAccount() == null
						&& account.getTax1Account() == null
						&& account.getTax2Account() == null) {

					/*
					 * We should not have problems with circular dependencies here.  One
					 * or other of quantity and share price must have been entered if either
					 * are to have a value.  Once one is entered that will no longer be
					 * bound to the default value.
					 */
					BigDecimal grossAmount = new BigDecimal(uncommittedEntryData.getEntry().getAmount()).movePointLeft(2);
					BigDecimal quantity = new BigDecimal(uncommittedEntryData.getPurchaseOrSaleEntry().getAmount());
					return grossAmount.divide(quantity);
				} else {
					return null;
				}
			}
		});
	}

	private void bindDefaultQuantity() {
		quantityBinding = context.bindDefault(uncommittedEntryData.commission(), new ComputedValue<Long>() {
			@Override
			protected Long calculate() {
				/*
				 * The user would not usually enter the net amount for the
				 * transaction because it is hard to calculate backwards from this.
				 * The rates tables are all based on the gross amount. Also, there
				 * may be a number of calculated commissions and taxes and we would
				 * not know which to adjust. Therefore in most cases we leave the
				 * transaction unbalanced and force the user to correct it when the
				 * transaction is saved.
				 * 
				 * However, if there are no commission or taxes configured for the
				 * commodity type in this account then we can calculate backwards.
				 */
				StockAccount account = (StockAccount)getUncommittedEntryData().getEntry().getAccount();

				if (sharePrice.getValue() == null) {
					return null;
				}

				if (account.getCommissionAccount() == null
						&& account.getTax1Account() == null
						&& account.getTax2Account() == null) {

					/*
					 * We should not have problems with circular dependencies here.  One
					 * or other of quantity and share price must have been entered if either
					 * are to have a value.  Once one is entered that will no longer be
					 * bound to the default value.
					 */
					BigDecimal grossAmount = new BigDecimal(uncommittedEntryData.getEntry().getAmount()).movePointLeft(2);
					BigDecimal quantity = grossAmount.divide(sharePrice.getValue());
					return quantity.movePointRight(3).longValue();
				} else {
					return null;
				}
			}
		});
	}

	private void bindDefaultNetAmount() {
		context.bindDefault(uncommittedEntryData.netAmount(), new ComputedValue<Long>() {
			@Override
			protected Long calculate() {
				Long grossAmount = calculateGrossAmount();
				if (grossAmount != null) {
					long totalExpenses =
							uncommittedEntryData.getCommission()
							+ uncommittedEntryData.getTax1Amount()
							+ uncommittedEntryData.getTax2Amount();
					if (input.getTransactionType() == TransactionType.Sell) {
						return grossAmount - totalExpenses;
					} else {
						return - grossAmount - totalExpenses;
					}
				} else {
					return null;
				}
			}
		});
	}

	private void bindDefaultCommission() {
		commissionBinding = context.bindDefault(uncommittedEntryData.commission(), new ComputedValue<Long>() {
			@Override
			protected Long calculate() {
				Long grossAmount = calculateGrossAmount();
				StockAccount account = (StockAccount)getUncommittedEntryData().getEntry().getAccount();
				if (grossAmount != null) {
					RatesTable commissionRates =
							(uncommittedEntryData.getTransactionType() == TransactionType.Buy)
							? account.getBuyCommissionRates()
									: account.getSellCommissionRates();
							if (account.getCommissionAccount() != null && commissionRates != null) {
								return commissionRates.calculateRate(grossAmount);
							} else {
								return null;
							}
				} else {
					return null;
				}
			}
		});
	}

	private void bindDefaultTax1() {
		tax1Binding = context.bindDefault(uncommittedEntryData.tax1(), new ComputedValue<Long>() {
			@Override
			protected Long calculate() {
				Long grossAmount = calculateGrossAmount();
				StockAccount account = (StockAccount)getUncommittedEntryData().getEntry().getAccount();
				if (grossAmount != null) {
					if (account.getTax1Account() != null && account.getTax1Rates() != null) {
						return account.getTax1Rates().calculateRate(grossAmount);
					} else {
						return null;
					}
				} else {
					return null;
				}
			}
		});
	}

	private void bindDefaultTax2() {
		tax2Binding = context.bindDefault(uncommittedEntryData.tax2(), new ComputedValue<Long>() {
			@Override
			protected Long calculate() {
				Long grossAmount = calculateGrossAmount();
				StockAccount account = (StockAccount)getUncommittedEntryData().getEntry().getAccount();
				if (grossAmount != null) {
					if (account.getTax2Account() != null && account.getTax2Rates() != null) {
						return account.getTax2Rates().calculateRate(grossAmount);
					} else {
						return null;
					}
				} else {
					return null;
				}
			}
		});
	}

	private void bindDefaultWithholdingTax() {
		withholdingTaxBinding = context.bindDefault(uncommittedEntryData.withholdingTax(), new ComputedValue<Long>() {
			@Override
			protected Long calculate() {
				long rate = 0L;
				long tax = uncommittedEntryData.getNetAmount() * rate / (100 - rate);
				return tax;
			}
		});
	}

	/*
	 * Notify listeners when the transaction type (purchase, sale, dividend etc.)
	 * changes.
	 */
	public void fireTransactionTypeChange() {
		for (ITransactionTypeChangeListener listener: transactionTypeChangeListeners) {
			listener.transactionTypeChanged();
		}
	}

	public void addTransactionTypeChangeListener(ITransactionTypeChangeListener listener) {
		transactionTypeChangeListeners.add(listener);
	}

	@Override
	protected StockEntryData createUncommittedEntryData(
			Entry entryInTransaction, TransactionManagerForAccounts transactionManager) {

		// If this is a new entry (this uncommitted entry is the new entry row) then
		// we must set the currency to the default currency for the account.
		if (entryInTransaction.getCommodity() == null) {
			StockAccount account = (StockAccount)entryInTransaction.getAccount();
			entryInTransaction.setCommodity(account.getCurrency());
		}

		StockEntryData entryData = new StockEntryData(entryInTransaction, transactionManager);
		return entryData;
	}

	@Override
	protected StockEntryRowControl getThis() {
		return this;
	}

	/**
	 * The net amount credited to or debited from the account has changed.
	 */
	@Override
	public void amountChanged() {
		//		netAmountIsFluid = false;
		//
		//		StockAccount account = (StockAccount)getUncommittedEntryData().getEntry().getAccount();
		//
		//		Entry entry = uncommittedEntryData.getEntry();
		//
		//		TransactionType transactionType = uncommittedEntryData.getTransactionType();
		//		if (transactionType == null) {
		//			/*
		//			 * The user has not yet entered enough information into the transaction
		//			 * to guess the transaction type.  In particular, the user has not selected
		//			 * the transaction type.
		//			 */
		//		} else {
		//			switch (transactionType) {
		//			case Buy:
		//			case Sell:
		//				/*
		//				 * The user would not usually enter the net amount for the
		//				 * transaction because it is hard to calculate backwards from this.
		//				 * The rates tables are all based on the gross amount. Also, there
		//				 * may be a number of calculated commissions and taxes and we would
		//				 * not know which to adjust. Therefore in most cases we leave the
		//				 * transaction unbalanced and force the user to correct it when the
		//				 * transaction is saved.
		//				 *
		//				 * However, if there are no commission or taxes configured for the
		//				 * commodity type in this account then we can calculate backwards.
		//				 */
		//				if (account.getCommissionAccount() == null
		//						&& account.getTax1Account() == null
		//						&& account.getTax2Account() == null) {
		//
		//					if (quantityIsFluid && !sharePriceIsFluid) {
		//						BigDecimal grossAmount = new BigDecimal(uncommittedEntryData.getEntry().getAmount()).movePointLeft(2);
		//						BigDecimal quantity = grossAmount.divide(sharePrice);
		//						uncommittedEntryData.getPurchaseOrSaleEntry().setAmount(quantity.movePointRight(3).longValue());
		//					}
		//
		//					if (sharePriceIsFluid && !quantityIsFluid) {
		//						BigDecimal grossAmount = new BigDecimal(uncommittedEntryData.getEntry().getAmount()).movePointLeft(2);
		//						BigDecimal quantity = new BigDecimal(uncommittedEntryData.getPurchaseOrSaleEntry().getAmount());
		//						sharePrice = grossAmount.divide(quantity);
		//					}
		//				}
		//				break;
		//			case Dividend:
		//				if (withholdingTaxIsFluid) {
		//					long rate = 0L;
		//					long tax = entry.getAmount() * rate / (100 - rate);
		//					uncommittedEntryData.setWithholdingTax(-tax);
		//				}
		//				long dividend = entry.getAmount() + uncommittedEntryData.getWithholdingTax();
		//				uncommittedEntryData.getDividendEntry().setAmount(-dividend);
		//				break;
		//			case Transfer:
		//				uncommittedEntryData.getTransferEntry().setAmount(-entry.getAmount());
		//				break;
		//			case Other:
		//				// If there are two entries in the transaction and
		//				// if both entries have accounts in the same currency or
		//				// one or other account is not known or one or other account
		//				// is a multi-currency account then we set the amount in
		//				// the other entry to be the same but opposite signed amount.
		//				if (entry.getTransaction().hasTwoEntries()) {
		//					Entry otherEntry = entry.getTransaction().getOther(entry);
		//					Commodity commodity1 = entry.getCommodityInternal();
		//					Commodity commodity2 = otherEntry.getCommodityInternal();
		//					if (commodity1 == null || commodity2 == null || commodity1.equals(commodity2)) {
		//						otherEntry.setAmount(-entry.getAmount());
		//					}
		//				}
		//				break;
		//			}
		//		}
	}

	/**
	 * @trackedGetter???
	 * @return
	 */
	private Long calculateGrossAmount() {
		// TODO: Can we clean this up a little?  Stock quantities are to three decimal places,
		// (long value is number of thousandths) hence why we shift the long value three places.
		long quantity = uncommittedEntryData.getQuantity();
		// remove this because the above always returns positive anyway.
		//		if (input.getTransactionType() == TransactionType.Sell) {
		//			quantity = -quantity;  // We use positive amounts here, regardless of whether buying or selling
		//		}
		if (sharePrice.getValue() == null || quantity == 0) {
			return null;
		} else {
			BigDecimal grossAmount1 = sharePrice.getValue().multiply(BigDecimal.valueOf(quantity).movePointLeft(3));
			BigDecimal grossAmount2 = grossAmount1.movePointRight(2).setScale(0, BigDecimal.ROUND_HALF_UP);
			long grossAmount = grossAmount2.longValue();
			return grossAmount;
		}
	}

	private void updateNetAmount() {
		//		if (netAmountIsFluid) {
		//			// TODO: Can we clean this up a little?  Stock quantities are to three decimal places,
		//			// (long value is number of thousanths) hence why we shift the long value three places.
		//			long lQuantity = uncommittedEntryData.getPurchaseOrSaleEntry().getAmount();
		//			BigDecimal quantity = BigDecimal.valueOf(lQuantity).movePointLeft(3);
		//			BigDecimal grossAmount1 = sharePrice.multiply(quantity);
		//			long amount = grossAmount1.movePointRight(2).longValue();
		//
		//			amount += uncommittedEntryData.getCommission();
		//			amount += uncommittedEntryData.getTax1Amount();
		//			amount += uncommittedEntryData.getTax2Amount();
		//
		//			uncommittedEntryData.getEntry().setAmount(-amount);
		//		}
	}

	@Override
	protected void specificValidation() throws InvalidUserEntryException {
		// TODO: We should remove this method and call the EntryData method directly.
		uncommittedEntryData.specificValidation();
	}

	public BigDecimal getSharePrice() {
		return sharePrice.getValue() == null ? BigDecimal.ZERO : sharePrice.getValue();
	}

	public void setSharePrice(BigDecimal sharePrice) {
		if (!sharePrice.equals(this.sharePrice.getValue())) {
			this.sharePrice.setValue(sharePrice);
			for (IPropertyChangeListener<BigDecimal> listener : stockPriceListeners) {
				listener.propertyChanged(sharePrice);
			}
		}
	}

	public void addStockPriceChangeListener(
			IPropertyChangeListener<BigDecimal> listener) {
		stockPriceListeners.add(listener);

	}
}