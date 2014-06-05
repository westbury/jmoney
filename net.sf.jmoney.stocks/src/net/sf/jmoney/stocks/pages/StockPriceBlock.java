package net.sf.jmoney.stocks.pages;

import java.math.BigDecimal;

import net.sf.jmoney.entrytable.CellFocusListener;
import net.sf.jmoney.entrytable.ICellControl2;
import net.sf.jmoney.entrytable.IndividualBlock;
import net.sf.jmoney.entrytable.RowControl;
import net.sf.jmoney.stocks.model.StockAccount;

import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.property.value.IValueProperty;
import org.eclipse.core.internal.databinding.provisional.bind.Bind;
import org.eclipse.core.internal.databinding.provisional.bind.IBidiConverter;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.databinding.swt.SWTObservables;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;

public class StockPriceBlock extends
		IndividualBlock<IObservableValue<StockEntryFacade>> {
	private final StockAccount account;

	public StockPriceBlock(StockAccount account) {
		super("Price", 60, 1);
		this.account = account;
	}

	@Override
	public Control createCellControl(Composite parent, IObservableValue<StockEntryFacade> master, RowControl rowControl) {
		final Text control = new Text(parent, SWT.RIGHT);

		IBidiConverter<BigDecimal,String> amountToText = new IBidiConverter<BigDecimal,String>() {
			@Override
			public String modelToTarget(BigDecimal sharePrice) {
				if (sharePrice != null) {
					long lPrice = sharePrice.movePointRight(4).longValue();
					return account.getPriceFormatter().format(lPrice);
				} else {
					return "";
				}
			}

			@Override
			public BigDecimal targetToModel(String amountString) throws CoreException {
				if (amountString.trim().length() == 0) {
					return null;
				} else {
					long amount = account.getPriceFormatter().parse(amountString);
					return new BigDecimal(amount).movePointLeft(4);
				}
			}
		};

		IValueProperty<StockEntryFacade, BigDecimal> sharePriceProperty = new PropertyOnObservable<StockEntryFacade, BigDecimal>(BigDecimal.class) {
			@Override
			protected IObservableValue<BigDecimal> getObservable(StockEntryFacade source) {
				return source.sharePrice();
			}
		};
		
		Bind.twoWay(sharePriceProperty.observeDetail(master))
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
				// Nothing to do because it is bound
			}

			@Override
			public void save() {
				// Nothing to do because now bound
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