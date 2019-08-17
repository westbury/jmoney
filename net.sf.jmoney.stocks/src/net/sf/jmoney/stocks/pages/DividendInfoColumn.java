package net.sf.jmoney.stocks.pages;

import org.eclipse.core.databinding.observable.value.ComputedValue;
import org.eclipse.core.databinding.observable.value.IObservableValue;

import net.sf.jmoney.entrytable.Block;
import net.sf.jmoney.entrytable.DelegateBlock;
import net.sf.jmoney.stocks.model.StockAccount;

class DividendInfoColumn
		extends DelegateBlock<IObservableValue<StockEntryFacade>, IObservableValue<StockDividendFacade>> {
	DividendInfoColumn(StockAccount account) {
		super(delegate(account));
	}

	private static Block<IObservableValue<StockDividendFacade>> delegate(StockAccount account) {
		PropertyOnObservable<StockDividendFacade, Long> withholdingTaxProperty = new PropertyOnObservable<StockDividendFacade, Long>(
				Long.class) {
			@Override
			protected IObservableValue<Long> getObservable(StockDividendFacade source) {
				return source.withholdingTax();
			}
		};

		return account.getWithholdingTaxAccount() == null 
				? new EntriesSection.BlankBlock<StockDividendFacade>()
				: new EntryAmountBlock<StockDividendFacade>("Withholding Tax", withholdingTaxProperty,
								account.getWithholdingTaxAccount().getCurrency());
	}
	
	@Override
	protected IObservableValue<StockDividendFacade> convert(
			IObservableValue<StockEntryFacade> blockInput) {
		return new ComputedValue<StockDividendFacade>() {
			@Override
			protected StockDividendFacade calculate() {
				return blockInput.getValue().dividendFacade().getValue();
			}
		};
	}
}