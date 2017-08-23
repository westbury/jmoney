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

/**
 * Class for all blocks that contain an amount that is part of
 * a stock sale/purchase transaction or a dividend transaction.
 * 
 * @author Nigel Westbury
 *
 */
public class EntryAmountBlock extends
		IndividualBlock<IObservableValue<StockEntryFacade>> {
	
	private IValueProperty<StockEntryFacade, Long> amountProperty;
	
	private IAmountFormatter formatter;

	public EntryAmountBlock(String label, IValueProperty<StockEntryFacade, Long> amountProperty, IAmountFormatter formatter) {
		super(label, EntryInfo.getAmountAccessor().getMinimumWidth(), EntryInfo.getAmountAccessor().getWeight());
		this.amountProperty = amountProperty;
		this.formatter = formatter;
	}

	@Override
	public Control createCellControl(Composite parent, IObservableValue<StockEntryFacade> master, RowControl rowControl) {
		final Text control = new Text(parent, SWT.RIGHT);

		IBidiWithExceptionConverter<Long, String> amountToText = new IBidiWithExceptionConverter<Long,String>() {
			@Override
			public String modelToTarget(Long amount) {
				if (amount == null) {
					return "";
				} else {
					return formatter.format(amount);
				}
			}

			@Override
			public Long targetToModel(String amountString) throws CoreException {
				if (amountString.trim().length() == 0) {
					return null;
				} else {
					return formatter.parse(amountString);
				}
			}
		};

		ControlStatusDecoration statusDecoration = new ControlStatusDecoration(
				control, SWT.LEFT | SWT.TOP);

		Bind.twoWay(amountProperty.observeDetail(master))
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