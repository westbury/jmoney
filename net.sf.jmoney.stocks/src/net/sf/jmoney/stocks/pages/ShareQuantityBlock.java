package net.sf.jmoney.stocks.pages;

import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.property.value.IValueProperty;
import org.eclipse.core.internal.databinding.provisional.bind.Bind;
import org.eclipse.core.internal.databinding.provisional.bind.IBidiWithExceptionConverter;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.databinding.fieldassist.ControlStatusDecoration;
import org.eclipse.jface.databinding.swt.SWTObservables;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;

import net.sf.jmoney.entrytable.CellFocusListener;
import net.sf.jmoney.entrytable.ICellControl2;
import net.sf.jmoney.entrytable.IndividualBlock;
import net.sf.jmoney.entrytable.RowControl;
import net.sf.jmoney.fields.IAmountFormatter;
import net.sf.jmoney.model2.EntryInfo;
import net.sf.jmoney.stocks.model.StockAccount;

public class ShareQuantityBlock extends
			IndividualBlock<IObservableValue<StockBuyOrSellFacade>> {
		private final StockAccount account;

		public ShareQuantityBlock(StockAccount account) {
			super("Quantity", EntryInfo.getAmountAccessor().getMinimumWidth(), EntryInfo.getAmountAccessor().getWeight());
			this.account = account;
		}

		@Override
		public Control createCellControl(Composite parent, final IObservableValue<StockBuyOrSellFacade> master, RowControl rowControl) {
			final Text control = new Text(parent, SWT.RIGHT);

			// FIXME this is not correct.  Entering new buy/sell transactions is broken.
			IBidiWithExceptionConverter<Long,String> amountToText = new IBidiWithExceptionConverter<Long,String>() {
				@Override
				public String modelToTarget(Long quantity) {
					StockBuyOrSellFacade stockEntryFacade = master.getValue();
					if (stockEntryFacade == null) {
						return "<quantity>";
					}
					IAmountFormatter formatter = getFormatter(stockEntryFacade);

					return quantity == null ? null : formatter.format(quantity);
				}

				@Override
				public Long targetToModel(String amountString) throws CoreException {
					if (amountString.trim().length() == 0) {
						return null;
					} else {
						StockBuyOrSellFacade stockEntryFacade = master.getValue();
							IAmountFormatter formatter = getFormatter(stockEntryFacade);
			        		return formatter.parse(amountString);
					}
				}

				/**
				 * The transaction must be a stock purchase or sale.
				 * 
				 * @param stockEntryFacade
				 * @return
				 */
				private IAmountFormatter getFormatter(StockBuyOrSellFacade stockEntryFacade) {
					IAmountFormatter formatter = stockEntryFacade.getPurchaseOrSaleEntry().getCommodity();
					if (formatter == null) {
						/*
						 * The user has not yet selected the stock. As the
						 * way the quantity of a stock is formatted may
						 * potentially depend on the stock, we do not know
						 * exactly how to format and parse the quantity.
						 * However in practice it is unlikely to differ
						 * between different stock in the same account so we
						 * use a default formatter from the account.
						 */
						formatter = account.getQuantityFormatter();
					}
					return formatter;
				}
			};

			IValueProperty<StockBuyOrSellFacade, Long> shareQuantityProperty = new PropertyOnObservable<StockBuyOrSellFacade, Long>(Long.class) {
				@Override
				protected IObservableValue<Long> getObservable(StockBuyOrSellFacade source) {
					return source.quantity();
				}
			};
			
			ControlStatusDecoration statusDecoration = new ControlStatusDecoration(
					control, SWT.LEFT | SWT.TOP);

			Bind.twoWay(shareQuantityProperty.observeDetail(master))
			.convertWithTracking(amountToText)
			.to(SWTObservables.observeText(control, SWT.Modify), statusDecoration::update);

			Bind.bounceBack(amountToText)
			.to(SWTObservables.observeText(control, SWT.FocusOut));
			
			
			ICellControl2<StockEntryFacade> cellControl = new ICellControl2<StockEntryFacade>() {

				@Override
				public Control getControl() {
					return control;
				}

				@Override
				public void load(StockEntryFacade data) {
					// Nothing to do because we now use databinding
				}

				@Override
				public void save() {
					// Nothing to do because we now use databinding
				}

				@Override
				public void setSelected() {
					control.setBackground(RowControl.selectedCellColor);
				}

				@Override
				public void setUnselected() {
					control.setBackground(null);
				}
			};

			FocusListener controlFocusListener = new CellFocusListener<RowControl>(rowControl, cellControl);
			control.addFocusListener(controlFocusListener);

			return cellControl.getControl();
		}
	}