package net.sf.jmoney.stocks.pages;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.databinding.observable.value.AbstractObservableValue;
import org.eclipse.core.databinding.observable.value.ComputedValue;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.IValueChangeListener;
import org.eclipse.core.databinding.observable.value.ValueChangeEvent;
import org.eclipse.core.databinding.observable.value.ValueDiff;
import org.eclipse.core.databinding.observable.value.WritableValue;
import org.eclipse.core.internal.databinding.provisional.bind.Bind;
import org.eclipse.swt.widgets.Composite;

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

	/**
	 * This observable value is designed for the destination of a default value.  It must meet
	 * the following criteria:
	 * 
	 * - values can be set.  However if the value is currently not applicable then the value is dropped.
	 * It is ok to drop the value because the computed value that is the source of the default value should
	 * be returning null if the transaction type is such that the value is not applicable.
	 * 
	 * - value changes should be fired, but this is used only for the purpose of stopping the binding if the
	 * destination is manually changed.  The old and new values will not matter.  Also no event should be
	 * fired if the change was due to the property becoming inapplicable or applicable.
	 */
	private abstract class AbstractTargetForDefault<T, F> extends AbstractObservableValue<T> {
		protected final StockEntryFacade newEntryFacade;

		protected IValueChangeListener<T> listener = new IValueChangeListener<T>() {
			@Override
			public void handleValueChange(ValueChangeEvent<? extends T> event) {
				// New diff that is <T> instead of <? extends T>
				ValueDiff<T> diff = new ValueDiff<T>() {
					@Override
					public T getOldValue() {
						return event.diff.getOldValue();
					}

					@Override
					public T getNewValue() {
						return event.diff.getNewValue();
					}
				}; 
				AbstractTargetForDefault.this.fireValueChange(diff);
			}
			
		};
		
		private AbstractTargetForDefault(StockEntryFacade newEntryFacade) {
			this.newEntryFacade = newEntryFacade;
			
			transactionSpecificFacade(newEntryFacade).addValueChangeListener(new IValueChangeListener<F>() {
				@Override
				public void handleValueChange(ValueChangeEvent<? extends F> event) {
					if (event.diff.getOldValue() != null) {
						IObservableValue<T> target = getTarget(event.diff.getOldValue());
						target.removeValueChangeListener(listener);
					}
					if (event.diff.getNewValue() != null) {
						IObservableValue<T> target = getTarget(event.diff.getNewValue());
						target.addValueChangeListener(listener);
					}
				}
			});
		}

		@Override
		protected T doGetValue() {
			if (newEntryFacade.getTransactionType() == TransactionType.Buy
					|| newEntryFacade.getTransactionType() == TransactionType.Sell) {
				return getTarget(this.transactionSpecificFacade(newEntryFacade).getValue()).getValue();
			} else {
				return null;
			}
		}

		@Override
		protected void doSetValue(T value) {
			if (newEntryFacade.getTransactionType() == TransactionType.Buy
					|| newEntryFacade.getTransactionType() == TransactionType.Sell) {
				getTarget(this.transactionSpecificFacade(newEntryFacade).getValue()).setValue(value);
			}
		}
		
		protected abstract IObservableValue<F> transactionSpecificFacade(StockEntryFacade facade);

		protected abstract IObservableValue<T> getTarget(F facade);
	}

	/**
	 * This observable value is designed for the destination of a default value.  It must meet
	 * the following criteria:
	 * 
	 * - values can be set.  However if the value is currently not applicable then the value is dropped.
	 * It is ok to drop the value because the computed value that is the source of the default value should
	 * be returning null if the transaction type is such that the value is not applicable.
	 * 
	 * - value changes should be fired, but this is used only for the purpose of stopping the binding if the
	 * destination is manually changed.  The old and new values will not matter.  Also no event should be
	 * fired if the change was due to the property becoming inapplicable or applicable.
	 */
	private abstract class AbstractTradeDefault<T> extends AbstractTargetForDefault<T, StockBuyOrSellFacade> {

		private AbstractTradeDefault(StockEntryFacade newEntryFacade) {
			super(newEntryFacade);
		}

		@Override
		protected IObservableValue<StockBuyOrSellFacade> transactionSpecificFacade(StockEntryFacade facade) {
			return facade.buyOrSellFacade();
		}
	}

	private final class SharePriceDefault extends AbstractTradeDefault<BigDecimal> {
		private SharePriceDefault(StockEntryFacade newEntryFacade) {
			super(newEntryFacade);
		}
		
		@Override
		public Object getValueType() {
			return BigDecimal.class;
		}

		@Override
		protected IObservableValue<BigDecimal> getTarget(StockBuyOrSellFacade facade) {
			return facade.sharePrice();
		}
	}
	
	private final class ShareQuantityDefault extends AbstractTradeDefault<Long> {
		private ShareQuantityDefault(StockEntryFacade newEntryFacade) {
			super(newEntryFacade);
		}
		
		@Override
		public Object getValueType() {
			return Long.class;
		}

		@Override
		protected IObservableValue<Long> getTarget(StockBuyOrSellFacade facade) {
			return facade.quantity();
		}
	}
	
	private final class CommissionDefault extends AbstractTradeDefault<Long> {
		private CommissionDefault(StockEntryFacade newEntryFacade) {
			super(newEntryFacade);
		}
		
		@Override
		public Object getValueType() {
			return Long.class;
		}

		@Override
		protected IObservableValue<Long> getTarget(StockBuyOrSellFacade facade) {
			return facade.commission();
		}
	}
	
	private final class Tax1Default extends AbstractTradeDefault<Long> {
		private Tax1Default(StockEntryFacade newEntryFacade) {
			super(newEntryFacade);
		}
		
		@Override
		public Object getValueType() {
			return Long.class;
		}

		@Override
		protected IObservableValue<Long> getTarget(StockBuyOrSellFacade facade) {
			return facade.tax1();
		}
	}
	
	private final class Tax2Default extends AbstractTradeDefault<Long> {
		private Tax2Default(StockEntryFacade newEntryFacade) {
			super(newEntryFacade);
		}
		
		@Override
		public Object getValueType() {
			return Long.class;
		}

		@Override
		protected IObservableValue<Long> getTarget(StockBuyOrSellFacade facade) {
			return facade.tax2();
		}
	}
	
	private abstract class AbstractDividendDefault<T> extends AbstractTargetForDefault<T, StockDividendFacade> {

		private AbstractDividendDefault(StockEntryFacade newEntryFacade) {
			super(newEntryFacade);
		}

		@Override
		protected IObservableValue<StockDividendFacade> transactionSpecificFacade(StockEntryFacade facade) {
			return facade.dividendFacade();
		}
	}

	private final class GrossDividendDefault extends AbstractDividendDefault<Long> {
		private GrossDividendDefault(StockEntryFacade newEntryFacade) {
			super(newEntryFacade);
		}
		
		@Override
		public Object getValueType() {
			return BigDecimal.class;
		}

		@Override
		protected IObservableValue<Long> getTarget(StockDividendFacade facade) {
			return facade.grossDividend();
		}
	}
	
	
	public enum TransactionType {
		Buy("stocks.buy", BuyOrSellEntryType.values()),
		Sell("stocks.sell", BuyOrSellEntryType.values()),
		Dividend("stocks.dividend", DividendEntryType.values()),
		Takeover("stocks.takeover", TakeoverEntryType.values()),
		Other(null, new IEntryType[0]);   // all other transaction types use this.  Should it just be a null type?

		private String id;
		private Set<String> entryTypes;
		private Set<String> compulsoryEntryTypes;
		
		TransactionType(String id, IEntryType[] entryTypes) {
			this.id = id;

			this.entryTypes = new HashSet<>();
			for (IEntryType entryType : entryTypes) {
				this.entryTypes.add(entryType.getId());
			}

			this.compulsoryEntryTypes = new HashSet<>();
			for (IEntryType entryType : entryTypes) {
				if (entryType.isCompulsory()) {
					this.compulsoryEntryTypes.add(entryType.getId());
				}
			}
		}

		public Set<String> getEntryTypes() {
			return this.entryTypes;
		}

		public Set<String> getCompulsoryEntryTypes() {
			return this.compulsoryEntryTypes;
		}

		/**
		 * This is the first part of each triple in the entry type.
		 * 
		 * @return
		 */
		public String getId() {
			return id;
		}
	}

	public enum BuyOrSellEntryType implements IEntryType {
		Cash("cash", true),
		AquisitionOrDisposal("acquisition-or-disposal", true),
		Commission("commission", false),
		Tax1("tax1", false),
		Tax2("tax2", false);

		public final String id;
		public final boolean isCompulsory;
		
		BuyOrSellEntryType(String id, boolean isCompulsory) {
			this.id = id;
			this.isCompulsory = isCompulsory;
		}
		
		@Override
		public String getId() {
			return id;
		}

		@Override
		public boolean isCompulsory() {
			return isCompulsory;
		}
	}

	public enum DividendEntryType implements IEntryType {
		Cash("cash", true),
		Dividend("dividend", true),
		WithholdingTax("withholding-tax", false);

		public final String id;
		public final boolean isCompulsory;
		
		DividendEntryType(String id, boolean isCompulsory) {
			this.id = id;
			this.isCompulsory = isCompulsory;
		}
		
		@Override
		public String getId() {
			return id;
		}

		@Override
		public boolean isCompulsory() {
			return isCompulsory;
		}
	}

	public enum TakeoverEntryType implements IEntryType {
		Cash("cash", true),
		TakenOver("takenOver", true),
		TakingOver("takingOver", false);

		public final String id;
		public final boolean isCompulsory;
		
		TakeoverEntryType(String id, boolean isCompulsory) {
			this.id = id;
			this.isCompulsory = isCompulsory;
		}
		
		@Override
		public String getId() {
			return id;
		}

		@Override
		public boolean isCompulsory() {
			return isCompulsory;
		}
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

		/* Find the stock account, which is this account.
		*/
		StockAccount stockAccount = (StockAccount)entryInTransaction.getAccount();
				
		// Create the facade
		StockEntryFacade facade = new StockEntryFacade(entryInTransaction, stockAccount);
		stockEntryFacade.setValue(facade);
		
		// This must be done before we add the default bindings
		// Should now be done when facade created
//		if (facade.isPurchaseOrSale()) {
//			facade.sharePrice().setValue(facade.calculatePrice());
//		} else {
//			facade.sharePrice().setValue(null);
//		}

		/*
		 * Bind the default values if and only if this is a new transaction
		 */
		// TODO check these bindings are disposed when the input replaced
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
			ComputedValue<BigDecimal> sharePriceDefault = new ComputedValue<BigDecimal>() {
				@Override
				protected BigDecimal calculate() {
					StockBuyOrSellFacade newPurchaseOrSaleFacade = newEntryFacade.getBuyOrSellFacade();
					if (newPurchaseOrSaleFacade != null) {
						Long quantity1 = newPurchaseOrSaleFacade.quantity().getValue();
						if (quantity1 == null || quantity1 == 0) {
							return null;
						}

						/*
						 * We should not have problems with circular dependencies here.  One
						 * or other of quantity and share price must have been entered if either
						 * are to have a value.  Once one is entered that will no longer be
						 * bound to the default value.
						 */
						BigDecimal grossAmount = new BigDecimal(newEntryFacade.getMainEntry().getAmount()).movePointLeft(2);
						BigDecimal quantity = new BigDecimal(quantity1);
						return grossAmount.divide(quantity, 4, BigDecimal.ROUND_HALF_UP);
					} else {
						// Not a purchase or sale transaction
						return null;
					}
				}
			};
			
			IObservableValue<BigDecimal> sharePrice = new SharePriceDefault(newEntryFacade);
			
			Bind.oneWay(sharePriceDefault)
			.untilTargetChanges()
			.to(sharePrice);
		} else {
			// No default value for the share price
		}
	}

	private void bindDefaultQuantity() {
		final StockEntryFacade newEntryFacade = stockEntryFacade.getValue();

		ComputedValue<Long> quantityDefault = new ComputedValue<Long>() {
			@Override
			protected Long calculate() {
				StockBuyOrSellFacade newPurchaseOrSaleFacade = newEntryFacade.getBuyOrSellFacade();
				if (newPurchaseOrSaleFacade != null) {

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

					BigDecimal sharePrice = newPurchaseOrSaleFacade.sharePrice().getValue();
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

		IObservableValue<Long> shareQuantity = new ShareQuantityDefault(newEntryFacade);
		
		Bind.oneWay(quantityDefault)
		.untilTargetChanges()
		.to(shareQuantity);
	}

	// All transaction types???
	private void bindDefaultNetAmount() {
		final StockEntryFacade newEntryFacade = stockEntryFacade.getValue();

		ComputedValue<Long> netAmountDefault = new ComputedValue<Long>() {
			@Override
			protected Long calculate() {
				// Must not return null because this is bound to property of type 'long'.
				StockBuyOrSellFacade facade = newEntryFacade.getBuyOrSellFacade();
				if (facade == null) {
					return 0L;
				} else {
					Long grossAmount = calculateGrossAmount(facade);
					if (grossAmount != null) {
						long totalExpenses = 0;
							totalExpenses =
									facade.getCommissionAmount()
									+ facade.getTax1Amount()
									+ facade.getTax2Amount();
							if (newEntryFacade.getTransactionType() == TransactionType.Sell) {
								return grossAmount - totalExpenses;
							} else {
								return - grossAmount - totalExpenses;
							}
					} else {
						return 0L;
					}
				}
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
				StockBuyOrSellFacade facade = newEntryFacade.getBuyOrSellFacade();
				if (facade != null) {
					Long grossAmount = calculateGrossAmount(facade);
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

		IObservableValue<Long> target = new CommissionDefault(newEntryFacade);
		
		Bind.oneWay(commissionDefault)
		.untilTargetChanges()
		.to(target);
	}

	private void bindDefaultTax1() {
		final StockEntryFacade newEntryFacade = stockEntryFacade.getValue();

		ComputedValue<Long> tax1Default = new ComputedValue<Long>() {
			@Override
			protected Long calculate() {
				StockBuyOrSellFacade facade = newEntryFacade.getBuyOrSellFacade();
				if (facade != null) {
					Long grossAmount = calculateGrossAmount(facade);
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

		IObservableValue<Long> target = new Tax1Default(newEntryFacade);
		
		Bind.oneWay(tax1Default)
		.untilTargetChanges()
		.to(target);
	}

	private void bindDefaultTax2() {
		final StockEntryFacade newEntryFacade = stockEntryFacade.getValue();

		ComputedValue<Long> tax2Default = new ComputedValue<Long>() {
			@Override
			protected Long calculate() {
				StockBuyOrSellFacade facade = newEntryFacade.getBuyOrSellFacade();
				if (facade != null) {
					Long grossAmount = calculateGrossAmount(facade);
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

		IObservableValue<Long> target = new Tax2Default(newEntryFacade);
		
		Bind.oneWay(tax2Default)
		.untilTargetChanges()
		.to(target);
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

		IObservableValue<Long> target = new GrossDividendDefault(thisEntryFacade);
		
		Bind.oneWay(new ComputedValue<Long>() {
			@Override
			protected Long calculate() {
				StockDividendFacade facade = thisEntryFacade.dividendFacade().getValue();
				if (facade != null) {
					long dividend = - thisEntryFacade.getNetAmount() - facade.getWithholdingTaxAmount();
					return dividend;
				} else {
					return null;
				}
			}
		}).to(target);
	}

	@Override
	protected StockEntryRowControl getThis() {
		return this;
	}

	/**
	 * @trackedGetter???
	 * @param newEntryFacade 
	 * @return
	 */
	private Long calculateGrossAmount(StockBuyOrSellFacade newEntryFacade) {
		// Stock quantities are to three decimal places,
		// (long value is number of thousandths) hence why we shift the long value three places.
		Long quantity1 = newEntryFacade.quantity().getValue();
		long quantity = quantity1 == null ? 0L : quantity1;
		BigDecimal sharePrice = newEntryFacade.sharePrice().getValue();
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
		// We need to know what specific validation is actually done before we can sort this out.
//		stockEntryFacade.getValue().specificValidation();
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