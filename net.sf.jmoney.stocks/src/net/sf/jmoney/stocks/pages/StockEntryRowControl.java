package net.sf.jmoney.stocks.pages;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import net.sf.jmoney.entrytable.BaseEntryRowControl;
import net.sf.jmoney.entrytable.Block;
import net.sf.jmoney.entrytable.EntryData;
import net.sf.jmoney.entrytable.FocusCellTracker;
import net.sf.jmoney.entrytable.InvalidUserEntryException;
import net.sf.jmoney.entrytable.RowSelectionTracker;
import net.sf.jmoney.entrytable.VirtualRowTable;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.EntryInfo;
import net.sf.jmoney.stocks.model.RatesTable;
import net.sf.jmoney.stocks.model.StockAccount;

import org.eclipse.core.databinding.observable.value.ComputedValue;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.WritableValue;
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
public class StockEntryRowControl extends BaseEntryRowControl<EntryData, StockEntryRowControl> {

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
	private IObservableValue<StockEntryFacade> stockEntryFacade = new WritableValue<StockEntryFacade>();

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

	public StockEntryRowControl(final Composite parent, int style, VirtualRowTable<EntryData, StockEntryRowControl> rowTable, Block<StockEntryRowControl> rootBlock, final RowSelectionTracker<StockEntryRowControl> selectionTracker, final FocusCellTracker focusCellTracker) {
		super(parent, rowTable, rootBlock, selectionTracker, focusCellTracker);
		init(this, rootBlock);
	}

	/**
	 * The input to this class is the StockEntryFacade with uncommitted model objects,
	 * i.e. model objects from a transaction that has not been committed. 
	 */
	@Override
	public void setRowInput(EntryData inputEntryData) {
		if (inputEntryData == null) {
			System.out.println("here");
		}
		/*
		 * We do this first even though it is done again when the super method
		 * is called.  This sets the 'input' value in the base object and means
		 * the following code can use that field.
		 */
		/*
		 * This must be called after we have set our own stuff up.  The reason
		 * being that this call loads controls (such as the stock price control).
		 * These controls will not load correctly if this object is not set up.
		 */
		super.setRowInput(inputEntryData);

		
		
		// If this is a new entry (this uncommitted entry is the new entry row) then
		// we must set the currency to the default currency for the account.
		// TODO is this the right place for this???
		Entry entryInTransaction = uncommittedEntry.getValue();
		if (entryInTransaction.getCommodity() == null) {
			StockAccount account = (StockAccount)entryInTransaction.getAccount();
			entryInTransaction.setCommodity(account.getCurrency());
		}
		
		// Create the facade
		StockEntryFacade facade = new StockEntryFacade(uncommittedEntry.getValue());
		stockEntryFacade.setValue(facade);
		
		// This must be done before we add the default bindings
		if (facade.isPurchaseOrSale()) {
			facade.sharePrice().setValue(facade.calculatePrice());
		} else {
			facade.sharePrice().setValue(null);
		}

		/*
		 * Bind the default values if and only if this is a new transaction
		 */
		if (inputEntryData.getEntry() == null) {
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
		final StockEntryFacade newEntryFacade = stockEntryFacade.getValue();
		StockAccount account = (StockAccount)newEntryFacade.getMainEntry().getAccount();

		if (isGrossSameAsNetAmount(account)) {
			Bind.oneWay(new ComputedValue<BigDecimal>() {
				@Override
				protected BigDecimal calculate() {
					if (newEntryFacade.isPurchaseOrSale()) {
						if (newEntryFacade.getQuantity() == 0) {
							return null;
						}

						/*
						 * We should not have problems with circular dependencies here.  One
						 * or other of quantity and share price must have been entered if either
						 * are to have a value.  Once one is entered that will no longer be
						 * bound to the default value.
						 */
						BigDecimal grossAmount = new BigDecimal(newEntryFacade.getMainEntry().getAmount()).movePointLeft(2);
						BigDecimal quantity = new BigDecimal(newEntryFacade.getQuantity());
						return grossAmount.divide(quantity, 4, BigDecimal.ROUND_HALF_UP);
					} else {
						// Not a purchase or sale transaction
						return null;
					}
				}
			}).untilTargetChanges().to(newEntryFacade.sharePrice());
		} else {
			// No default value for the share price
		}
	}

	private void bindDefaultQuantity() {
		final StockEntryFacade newEntryFacade = stockEntryFacade.getValue();

		ComputedValue<Long> quantityDefault = new ComputedValue<Long>() {
			@Override
			protected Long calculate() {
				if (newEntryFacade.isPurchaseOrSale()) {
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
					StockAccount account = (StockAccount)newEntryFacade.getMainEntry().getAccount();

					BigDecimal sharePrice = newEntryFacade.sharePrice().getValue();
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
						BigDecimal grossAmount = new BigDecimal(newEntryFacade.getMainEntry().getAmount()).movePointLeft(2);
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
		.to(newEntryFacade.quantity());
	}

	// All transaction types???
	private void bindDefaultNetAmount() {
		final StockEntryFacade newEntryFacade = stockEntryFacade.getValue();

		ComputedValue<Long> netAmountDefault = new ComputedValue<Long>() {
			@Override
			protected Long calculate() {
				// Must not return null because this is bound to property of type 'long'.
				
				//				if (newEntryData.isPurchaseOrSale()) {
				Long grossAmount = calculateGrossAmount(newEntryFacade);
				StockAccount account = (StockAccount)newEntryFacade.getMainEntry().getAccount();
				if (grossAmount != null) {
					long totalExpenses =
							newEntryFacade.getCommission()
							+ newEntryFacade.getTax1Amount()
							+ newEntryFacade.getTax2Amount();
					if (newEntryFacade.getTransactionType() == TransactionType.Sell) {
						return grossAmount - totalExpenses;
					} else {
						return - grossAmount - totalExpenses;
					}
				} else {
					return 0L;
				}
				//				} else {
				//					// Not a purchase or sale transaction
				//					return null;
				//				}
			}
		};

		Bind.oneWay(netAmountDefault)
		.untilTargetChanges()
		.to(EntryInfo.getAmountAccessor().observe(newEntryFacade.getMainEntry()));
	}

	private void bindDefaultCommission() {
		final StockEntryFacade newEntryFacade = stockEntryFacade.getValue();

		ComputedValue<Long> commissionDefault = new ComputedValue<Long>() {
			@Override
			protected Long calculate() {
				if (newEntryFacade.isPurchaseOrSale()) {
					Long grossAmount = calculateGrossAmount(newEntryFacade);
					StockAccount account = (StockAccount)newEntryFacade.getMainEntry().getAccount();
					if (grossAmount != null) {
						RatesTable commissionRates =
								(newEntryFacade.getTransactionType() == TransactionType.Buy)
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
		.to(newEntryFacade.commission());
	}

	private void bindDefaultTax1() {
		final StockEntryFacade newEntryFacade = stockEntryFacade.getValue();

		ComputedValue<Long> tax1Default = new ComputedValue<Long>() {
			@Override
			protected Long calculate() {
				if (newEntryFacade.isPurchaseOrSale()) {
					Long grossAmount = calculateGrossAmount(newEntryFacade);
					StockAccount account = (StockAccount)newEntryFacade.getMainEntry().getAccount();
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
		.to(newEntryFacade.tax1());
	}

	private void bindDefaultTax2() {
		final StockEntryFacade newEntryFacade = stockEntryFacade.getValue();

		ComputedValue<Long> tax2Default = new ComputedValue<Long>() {
			@Override
			protected Long calculate() {
				if (newEntryFacade.isPurchaseOrSale()) {
					Long grossAmount = calculateGrossAmount(newEntryFacade);
					StockAccount account = (StockAccount)newEntryFacade.getMainEntry().getAccount();
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
		.to(newEntryFacade.tax2());
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
		final StockEntryFacade thisEntryFacade = stockEntryFacade.getValue();

		Bind.oneWay(new ComputedValue<Long>() {
			@Override
			protected Long calculate() {
				if (thisEntryFacade.isDividend()) {
					long dividend = - thisEntryFacade.getNetAmount() - thisEntryFacade.getWithholdingTax();
					return dividend;
				} else {
					return null;
				}
			}
		}).to(thisEntryFacade.dividend());
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
	private Long calculateGrossAmount(StockEntryFacade newEntryData) {
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
		stockEntryFacade.getValue().specificValidation();
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

	public IObservableValue<StockEntryFacade> observeEntryFacade() {
		return stockEntryFacade;
	}

}