package net.sf.jmoney.stocks.pages;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.databinding.observable.value.ComputedValue;
import org.eclipse.core.databinding.observable.value.IObservableValue;

import net.sf.jmoney.entrytable.Block;
import net.sf.jmoney.entrytable.DelegateBlock;
import net.sf.jmoney.entrytable.HorizontalBlock;
import net.sf.jmoney.entrytable.IndividualBlock;
import net.sf.jmoney.entrytable.VerticalBlock;
import net.sf.jmoney.stocks.model.StockAccount;

final class TradeInfoColumn
		extends DelegateBlock<IObservableValue<StockEntryFacade>, IObservableValue<StockBuyOrSellFacade>> {
	TradeInfoColumn(StockAccount account) {
		super(delegate(account));
	}

	private static Block<IObservableValue<StockBuyOrSellFacade>> delegate(StockAccount account) {
		List<Block<? super IObservableValue<StockBuyOrSellFacade>>> expenseColumns = new ArrayList<Block<? super IObservableValue<StockBuyOrSellFacade>>>();

		if (account.getCommissionAccount() != null) {
			PropertyOnObservable<StockBuyOrSellFacade, Long> commissionProperty = new PropertyOnObservable<StockBuyOrSellFacade, Long>(
					Long.class) {
				@Override
				protected IObservableValue<Long> getObservable(StockBuyOrSellFacade source) {
					return source.commission();
				}
			};

			final Block<IObservableValue<StockBuyOrSellFacade>> commissionColumn = new EntryAmountBlock<StockBuyOrSellFacade>(
					"Commission", commissionProperty, account.getCommissionAccount().getCurrency());

			expenseColumns.add(commissionColumn);
		}

		if (account.getTax1Name() != null && account.getTax1Account() != null) {
			PropertyOnObservable<StockBuyOrSellFacade, Long> tax1Property = new PropertyOnObservable<StockBuyOrSellFacade, Long>(
					Long.class) {
				@Override
				protected IObservableValue<Long> getObservable(StockBuyOrSellFacade source) {
					return source.tax1();
				}
			};

			final Block<IObservableValue<StockBuyOrSellFacade>> tax1Column = new EntryAmountBlock<StockBuyOrSellFacade>(
					account.getTax1Name(), tax1Property, account.getTax1Account().getCurrency());

			expenseColumns.add(tax1Column);
		}

		if (account.getTax2Name() != null && account.getTax2Account() != null) {
			PropertyOnObservable<StockBuyOrSellFacade, Long> tax2Property = new PropertyOnObservable<StockBuyOrSellFacade, Long>(
					Long.class) {
				@Override
				protected IObservableValue<Long> getObservable(StockBuyOrSellFacade source) {
					return source.tax2();
				}
			};

			final Block<IObservableValue<StockBuyOrSellFacade>> tax2Column = new EntryAmountBlock<StockBuyOrSellFacade>(
					account.getTax2Name(), tax2Property, account.getTax2Account().getCurrency());

			expenseColumns.add(tax2Column);
		}

		IndividualBlock<IObservableValue<StockBuyOrSellFacade>> priceColumn = new StockPriceBlock(account);
		IndividualBlock<IObservableValue<StockBuyOrSellFacade>> shareQuantityColumn = new ShareQuantityBlock(account);

		final Block<IObservableValue<StockBuyOrSellFacade>> purchaseOrSaleInfoColumn = new VerticalBlock<IObservableValue<StockBuyOrSellFacade>>(
				priceColumn, shareQuantityColumn,
				new HorizontalBlock<IObservableValue<StockBuyOrSellFacade>>(expenseColumns));

		return purchaseOrSaleInfoColumn;
	}
	
	@Override
	protected IObservableValue<StockBuyOrSellFacade> convert(
			IObservableValue<StockEntryFacade> blockInput) {
		return new ComputedValue<StockBuyOrSellFacade>() {
			@Override
			protected StockBuyOrSellFacade calculate() {
				IObservableValue<StockBuyOrSellFacade> tradeFacade = blockInput.getValue().buyOrSellFacade();
				return tradeFacade != null ? tradeFacade.getValue() : null;
			}
		};
	}
}