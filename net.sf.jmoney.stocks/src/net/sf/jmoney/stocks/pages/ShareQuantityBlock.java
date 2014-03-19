package net.sf.jmoney.stocks.pages;

import net.sf.jmoney.entrytable.CellFocusListener;
import net.sf.jmoney.entrytable.ICellControl2;
import net.sf.jmoney.entrytable.IndividualBlock;
import net.sf.jmoney.entrytable.RowControl;
import net.sf.jmoney.fields.IAmountFormatter;
import net.sf.jmoney.model2.EntryInfo;
import net.sf.jmoney.stocks.model.StockAccount;
import net.sf.jmoney.stocks.pages.StockEntryRowControl.TransactionType;

import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.internal.databinding.provisional.bind.Bind;
import org.eclipse.core.internal.databinding.provisional.bind.IBidiConverter;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.databinding.swt.SWTObservables;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.statushandlers.StatusManager;

public class ShareQuantityBlock extends
			IndividualBlock<IObservableValue<StockEntryFacade>> {
		private final StockAccount account;

		public ShareQuantityBlock(StockAccount account) {
			super("Quantity", EntryInfo.getAmountAccessor().getMinimumWidth(), EntryInfo.getAmountAccessor().getWeight());
			this.account = account;
		}

		@Override
		public Control createCellControl(Composite parent, final IObservableValue<StockEntryFacade> master, RowControl rowControl) {
			final Text control = new Text(parent, SWT.RIGHT);

			IBidiConverter<Long,String> amountToText = new IBidiConverter<Long,String>() {
				@Override
				public String modelToTarget(Long quantity) {
					StockEntryFacade stockEntryFacade = master.getValue();
					
					IAmountFormatter formatter = getFormatter(stockEntryFacade);

//						long quantity = stockEntryFacade.getPurchaseOrSaleEntry().getAmount();
					if (stockEntryFacade.getTransactionType() == TransactionType.Sell) {
//							quantity = -quantity;
					}
					
					return formatter.format(quantity);
				}

				@Override
				public Long targetToModel(String amountString) throws CoreException {
					if (amountString.trim().length() == 0) {
						return null;
					} else {
						try {
							StockEntryFacade stockEntryFacade = master.getValue();
							IAmountFormatter formatter = getFormatter(stockEntryFacade);
							return formatter.parse(amountString);
						} catch (CoreException e) {
							StatusManager.getManager().handle(e.getStatus());
							return null;
						}
					}
				}

				private IAmountFormatter getFormatter(StockEntryFacade stockEntryFacade) {
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

			PropertyOnObservable<Long> shareQuantityProperty = new PropertyOnObservable<Long>(Long.class) {
				@Override
				protected IObservableValue<Long> getObservable(StockEntryFacade source) {
					return source.quantity();
				}
			};
			
			Bind.twoWay(shareQuantityProperty.observeDetail(master))
			.convertWithTracking(amountToText)
			.to(SWTObservables.observeText(control, SWT.Modify));

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