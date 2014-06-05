package net.sf.jmoney.stocks.pages;

import net.sf.jmoney.entrytable.CellFocusListener;
import net.sf.jmoney.entrytable.ICellControl2;
import net.sf.jmoney.entrytable.IndividualBlock;
import net.sf.jmoney.entrytable.RowControl;
import net.sf.jmoney.model2.Session;
import net.sf.jmoney.stocks.model.Security;
import net.sf.jmoney.stocks.model.SecurityControl;
import net.sf.jmoney.stocks.model.SecurityInfo;

import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.core.databinding.observable.value.ComputedValue;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.property.value.IValueProperty;
import org.eclipse.core.internal.databinding.provisional.bind.Bind;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

public class SecurityBlock extends
		IndividualBlock<IObservableValue<StockEntryFacade>> {
	public SecurityBlock() {
		super("Stock", 50, 1);
	}

	@Override
	public Control createCellControl(Composite parent, final IObservableValue<StockEntryFacade> master, RowControl rowControl) {
		final SecurityControl<Security> control = new SecurityControl<Security>(parent, SecurityInfo.getPropertySet()) {
			@Override
			protected Session getSession() {
				return master.getValue().getMainEntry().getSession();
			}
		};

		IValueProperty<StockEntryFacade, Security> securityProperty = new PropertyOnObservable<StockEntryFacade, Security>(Security.class) {
			@Override
			protected IObservableValue<Security> getObservable(
					StockEntryFacade source) {
				return source.security();
			}
		};
		
		Bind.twoWay(securityProperty, master)
		.to(control.commodity());
		
		// Enable control only when applicable
		IObservableValue<Boolean> isSecurityApplicable = new ComputedValue<Boolean>() {
			@Override
			protected Boolean calculate() {
				return master.getValue() == null
						? false
								: master.getValue().isPurchaseOrSale()
						|| master.getValue().isDividend();
			}
		};
		Bind.oneWay(isSecurityApplicable)
		.to(BeanProperties.value("enabled", Boolean.class).observe(control));
		
		ICellControl2<StockEntryFacade> cellControl = new ICellControl2<StockEntryFacade>() {
			private StockEntryFacade data;

			@Override
			public Control getControl() {
				return control;
			}

			@Override
			public void load(StockEntryFacade data) {
				// TODO remove this method now we use databinding.
			}

			@Override
			public void save() {
				// TODO remove this method now we use databinding.
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

		/*
		 * The control may in fact be a composite control, in which case the
		 * composite control itself will never get the focus. Only the child
		 * controls will get the focus, so we add the listener recursively
		 * to all child controls.
		 */
		addFocusListenerRecursively(cellControl.getControl(), controlFocusListener);

		return cellControl.getControl();
	}

	private void addFocusListenerRecursively(Control control, FocusListener listener) {
		control.addFocusListener(listener);
		if (control instanceof Composite) {
			for (Control child: ((Composite)control).getChildren()) {
				addFocusListenerRecursively(child, listener);
			}
		}
	}
}