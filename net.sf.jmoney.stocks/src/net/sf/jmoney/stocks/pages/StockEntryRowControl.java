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

import org.eclipse.core.databinding.observable.value.ComputedValue;
import org.eclipse.core.internal.databinding.provisional.bind.Bind;
import org.eclipse.swt.widgets.Composite;

/**
 * Default binding only ever applies for a new entry.  The user cannot select away from the new entry without
 * saving the new entry.  At that point the new entry is now a pre-existing entry, so there is no default
 * data binding.  This makes default binding a little easier.  We perform default binding on the EntryData itself
 * (not the observable, so no master-detail.  When the new entry is saved, the default binding is disposed.
 * 
 * @author Nigel
 *
 */
public class StockEntryRowControl extends BaseEntryRowControl<StockEntryData, StockEntryRowControl> {

	public enum TransactionType {
		Buy,
		Sell,
		Dividend,
		Transfer,
		Other
	}

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
	//	private final IObservableValue<BigDecimal> sharePrice = new WritableValue<BigDecimal>();

	private final List<IPropertyChangeListener<BigDecimal>> stockPriceListeners = new ArrayList<IPropertyChangeListener<BigDecimal>>();

	//	private Binding dividendBinding = null;

	//	private DefaultValueBinding<Long> withholdingTaxBinding = null;

	//	private DefaultValueBinding<Long> tax2Binding;
	//
	//	private DefaultValueBinding<Long> tax1Binding;
	//
	//	private DefaultValueBinding<Long> commissionBinding;
	//
	//	private DefaultValueBinding<Long> quantityBinding;
	//
	//	private DefaultValueBinding<BigDecimal> sharePriceBinding;

	public StockEntryRowControl(final Composite parent, int style, VirtualRowTable rowTable, Block<StockEntryData, ? super StockEntryRowControl> rootBlock, final RowSelectionTracker selectionTracker, final FocusCellTracker focusCellTracker) {
		super(parent, style, rowTable, rootBlock, selectionTracker, focusCellTracker);
		init(this, this, rootBlock);
	}

	/**
	 * The input to this class is the StockEntryData with uncommitted model objects,
	 * i.e. model objects from a transaction that has not been committed. 
	 */
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
		input.setValue(inputEntryData);

		// This must be done before we add the default bindings
		if (input.getValue().isPurchaseOrSale()) {
			input.getValue().sharePrice().setValue(input.getValue().calculatePrice());
		} else {
			input.getValue().sharePrice().setValue(null);
		}

		/*
		 * Bind the default values if and only if this is a new transaction
		 */
		if (committedEntryData.getEntry() == null) {
			bindDefaultNetAmount();

			// Purchases and sales:
			bindDefaultQuantity();
			bindDefaultSharePrice();
			bindDefaultCommission();
			bindDefaultTax1();
			bindDefaultTax2();

			// Dividends:
			bindDividend();
			bindDefaultWithholdingTax();


			if (committedEntryData != inputEntryData) {
				System.out.println("here");
			}

			/*
			 * Note it is possible that bindings will be null even though they are
			 * applicable for the transaction type.  This is because this may be a
			 * pre-existing transaction which the user switched to a different transaction
			 * type.  There are no default value bindings for pre-existing transactions
			 * unless the type of the transaction is changed.
			 *
			 * NOT SURE IF THIS IS CORRECT...
			 */

			/*
			 * Actually we now leave all bindings active regardless on the transaction type.  The default
			 * value just calculates to null which is bound to a control that is not visible.  This is
			 * a lot simpler. 
			 */
			//			inputEntryData.transactionType().addValueChangeListener(new IValueChangeListener<TransactionType>() {
			//
			//				@Override
			//				public void handleValueChange(ValueChangeEvent<TransactionType> event) {
			//					if (event.diff.getOldValue() != null) {
			//						switch (event.diff.getOldValue()) {
			//						case Buy:
			//						case Sell:
			//							quantityBinding.dispose();
			//							// Note that a default for the share price is not always provided
			//							if (sharePriceBinding != null) {
			//								sharePriceBinding.dispose();
			//							}
			//							commissionBinding.dispose();
			//							tax1Binding.dispose();
			//							tax2Binding.dispose();
			//							break;
			//						case Dividend:
			//							dividendBinding.dispose();
			////							withholdingTaxBinding.dispose();
			//							break;
			//						case Transfer:
			//							break;
			//						case Other:
			//							break;
			//						}
			//					}
			//
			//					switch (event.diff.getNewValue()) {
			//					case Buy:
			//					case Sell:
			//						bindDefaultQuantity();
			//						bindDefaultSharePrice();
			//						bindDefaultCommission();
			//						bindDefaultTax1();
			//						bindDefaultTax2();
			//						break;
			//					case Dividend:
			//						bindDividend();
			//						bindDefaultWithholdingTax();
			//						break;
			//					case Transfer:
			//						break;
			//					case Other:
			//						break;
			//					}
			//				}
			//			});
		}

		/*
		 * This must be called after we have set our own stuff up.  The reason
		 * being that this call loads controls (such as the stock price control).
		 * These controls will not load correctly if this object is not set up.
		 */
		super.setInput(inputEntryData);
	}

	private boolean isGrossSameAsNetAmount(StockAccount account) {
		return account.getCommissionAccount() == null
				&& account.getTax1Account() == null
				&& account.getTax2Account() == null;
	}

	private void bindDefaultSharePrice() {
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
		final StockEntryData newEntryData = input.getValue();
		StockAccount account = (StockAccount)getUncommittedEntryData().getEntry().getAccount();

		if (isGrossSameAsNetAmount(account)) {
			Bind.oneWay(new ComputedValue<BigDecimal>() {
				@Override
				protected BigDecimal calculate() {
					if (newEntryData.isPurchaseOrSale()) {
						if (input.getValue().getQuantity() == 0) {
							return null;
						}

						/*
						 * We should not have problems with circular dependencies here.  One
						 * or other of quantity and share price must have been entered if either
						 * are to have a value.  Once one is entered that will no longer be
						 * bound to the default value.
						 */
						BigDecimal grossAmount = new BigDecimal(input.getValue().getNetAmount()).movePointLeft(2);
						BigDecimal quantity = new BigDecimal(input.getValue().getQuantity());
						return grossAmount.divide(quantity, 4, BigDecimal.ROUND_HALF_UP);
					} else {
						// Not a purchase or sale transaction
						return null;
					}
				}
			}).untilTargetChanges().to(input.getValue().sharePrice());
		} else {
			// No default value for the share price
		}
	}

	private void bindDefaultQuantity() {
		final StockEntryData newEntryData = input.getValue();

		ComputedValue<Long> quantityDefault = new ComputedValue<Long>() {
			@Override
			protected Long calculate() {
				if (newEntryData.isPurchaseOrSale()) {
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
					StockAccount account = (StockAccount)newEntryData.getEntry().getAccount();

					BigDecimal sharePrice = newEntryData.sharePrice().getValue();
					if (sharePrice == null) {
						return null;
					}

					if (isGrossSameAsNetAmount(account)) {
						/*
						 * We should not have problems with circular dependencies here.  One
						 * or other of quantity and share price must have been entered if either
						 * are to have a value.  Once one is entered that will no longer be
						 * bound to the default value.
						 */
						BigDecimal grossAmount = new BigDecimal(newEntryData.getEntry().getAmount()).movePointLeft(2);
						BigDecimal quantity = grossAmount.divide(sharePrice);
						return quantity.movePointRight(3).longValue();
					} else {
						return null;
					}
				} else {
					// Not a purchase or sale transaction
					return null;
				}
			}
		};

		Bind.oneWay(quantityDefault)
		.untilTargetChanges()
		.to(newEntryData.quantity());
	}

	// All transaction types???
	private void bindDefaultNetAmount() {
		final StockEntryData newEntryData = input.getValue();

		ComputedValue<Long> netAmountDefault = new ComputedValue<Long>() {
			@Override
			protected Long calculate() {
				//				if (newEntryData.isPurchaseOrSale()) {
				Long grossAmount = calculateGrossAmount(newEntryData);
				StockAccount account = (StockAccount)newEntryData.getEntry().getAccount();
				if (grossAmount != null) {
					long totalExpenses =
							newEntryData.getCommission()
							+ newEntryData.getTax1Amount()
							+ newEntryData.getTax2Amount();
					if (newEntryData.getTransactionType() == TransactionType.Sell) {
						return grossAmount - totalExpenses;
					} else {
						return - grossAmount - totalExpenses;
					}
				} else {
					return null;
				}
				//				} else {
				//					// Not a purchase or sale transaction
				//					return null;
				//				}
			}
		};

		Bind.oneWay(netAmountDefault)
		.untilTargetChanges()
		.to(newEntryData.netAmount());
	}

	private void bindDefaultCommission() {
		final StockEntryData newEntryData = input.getValue();

		ComputedValue<Long> commissionDefault = new ComputedValue<Long>() {
			@Override
			protected Long calculate() {
				if (newEntryData.isPurchaseOrSale()) {
					Long grossAmount = calculateGrossAmount(newEntryData);
					StockAccount account = (StockAccount)newEntryData.getEntry().getAccount();
					if (grossAmount != null) {
						RatesTable commissionRates =
								(newEntryData.getTransactionType() == TransactionType.Buy)
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
				} else {
					// Not a purchase or sale transaction
					return null;
				}
			}
		};

		Bind.oneWay(commissionDefault)
		.untilTargetChanges()
		.to(newEntryData.commission());
	}

	private void bindDefaultTax1() {
		final StockEntryData newEntryData = input.getValue();

		ComputedValue<Long> tax1Default = new ComputedValue<Long>() {
			@Override
			protected Long calculate() {
				if (newEntryData.isPurchaseOrSale()) {
					Long grossAmount = calculateGrossAmount(newEntryData);
					StockAccount account = (StockAccount)newEntryData.getEntry().getAccount();
					if (grossAmount != null) {
						if (account.getTax1Account() != null && account.getTax1Rates() != null) {
							return account.getTax1Rates().calculateRate(grossAmount);
						} else {
							return null;
						}
					} else {
						return null;
					}
				} else {
					// Not a purchase or sale transaction
					return null;
				}
			}
		};

		Bind.oneWay(tax1Default)
		.untilTargetChanges()
		.to(newEntryData.tax1());
	}

	private void bindDefaultTax2() {
		final StockEntryData newEntryData = input.getValue();

		ComputedValue<Long> tax2Default = new ComputedValue<Long>() {
			@Override
			protected Long calculate() {
				if (newEntryData.isPurchaseOrSale()) {
					Long grossAmount = calculateGrossAmount(newEntryData);
					StockAccount account = (StockAccount)newEntryData.getEntry().getAccount();
					if (grossAmount != null) {
						if (account.getTax2Account() != null && account.getTax2Rates() != null) {
							return account.getTax2Rates().calculateRate(grossAmount);
						} else {
							return null;
						}
					} else {
						return null;
					}
				} else {
					// Not a purchase or sale transaction
					return null;
				}
			}
		};

		Bind.oneWay(tax2Default)
		.untilTargetChanges()
		.to(newEntryData.tax2());
	}

	private void bindDefaultWithholdingTax() {
		// Actually don't do this.  Withholding tax is often used in brokerage accounts
		// to withhold foreign tax.  Most securities are not foreign, so the default value
		// should be null.  If a brokerage is withholding tax on all or most securities then
		// we need another solution, perhaps a default withholding tax rate as a property of
		// the account.

		//		withholdingTaxBinding = context.bindDefault(input.getValue().withholdingTax(), new ComputedValue<Long>() {
		//			@Override
		//			protected Long calculate() {
		//				long rate = 0L;
		//				long tax = input.getValue().getNetAmount() * rate / (100 - rate);
		//				return tax == 0 ? null : tax;
		//			}
		//		});
	}

	/**
	 * This one is a normal binding, not a default binding, because there is no amount
	 * shown to the user for the gross dividend amount.
	 */
	private void bindDividend() {
		Bind.oneWay(new ComputedValue<Long>() {
			@Override
			protected Long calculate() {
				if (input.getValue().isDividend()) {
					long dividend = - input.getValue().getNetAmount() - input.getValue().getWithholdingTax();
					return dividend;
				} else {
					return null;
				}
			}
		}).to(input.getValue().dividend());
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
	 * @trackedGetter???
	 * @param newEntryData 
	 * @return
	 */
	private Long calculateGrossAmount(StockEntryData newEntryData) {
		// TODO: Can we clean this up a little?  Stock quantities are to three decimal places,
		// (long value is number of thousandths) hence why we shift the long value three places.
		long quantity = newEntryData.getQuantity();
		// remove this because the above always returns positive anyway.
		//		if (input.getTransactionType() == TransactionType.Sell) {
		//			quantity = -quantity;  // We use positive amounts here, regardless of whether buying or selling
		//		}
		BigDecimal sharePrice = newEntryData.sharePrice().getValue();
		if (sharePrice == null || quantity == 0) {
			return null;
		} else {
			BigDecimal grossAmount1 = sharePrice.multiply(BigDecimal.valueOf(quantity).movePointLeft(3));
			BigDecimal grossAmount2 = grossAmount1.movePointRight(2).setScale(0, BigDecimal.ROUND_HALF_UP);
			long grossAmount = grossAmount2.longValue();
			return grossAmount;
		}
	}

	private void updateNetAmount() {
		//		if (netAmountIsFluid) {
		//			// TODO: Can we clean this up a little?  Stock quantities are to three decimal places,
		//			// (long value is number of thousanths) hence why we shift the long value three places.
		//			long lQuantity = input.getValue().getPurchaseOrSaleEntry().getAmount();
		//			BigDecimal quantity = BigDecimal.valueOf(lQuantity).movePointLeft(3);
		//			BigDecimal grossAmount1 = sharePrice.multiply(quantity);
		//			long amount = grossAmount1.movePointRight(2).longValue();
		//
		//			amount += input.getValue().getCommission();
		//			amount += input.getValue().getTax1Amount();
		//			amount += input.getValue().getTax2Amount();
		//
		//			input.getValue().getEntry().setAmount(-amount);
		//		}
	}

	@Override
	protected void specificValidation() throws InvalidUserEntryException {
		// TODO: We should remove this method and call the EntryData method directly.
		input.getValue().specificValidation();
	}

	//	public BigDecimal getSharePrice() {
	//		return sharePrice.getValue() == null ? BigDecimal.ZERO : sharePrice.getValue();
	//	}
	//
	//	public IObservableValue<BigDecimal> sharePrice() {
	//		return sharePrice;
	//	}
	//
	//	public void setSharePrice(BigDecimal sharePrice) {
	//		if (!sharePrice.equals(this.sharePrice.getValue())) {
	//			this.sharePrice.setValue(sharePrice);
	//			for (IPropertyChangeListener<BigDecimal> listener : stockPriceListeners) {
	//				listener.propertyChanged(sharePrice);
	//			}
	//		}
	//	}

	public void addStockPriceChangeListener(
			IPropertyChangeListener<BigDecimal> listener) {
		stockPriceListeners.add(listener);

	}
}