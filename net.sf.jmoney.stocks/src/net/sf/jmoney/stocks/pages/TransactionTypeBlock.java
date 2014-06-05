package net.sf.jmoney.stocks.pages;

import net.sf.jmoney.entrytable.CellFocusListener;
import net.sf.jmoney.entrytable.ICellControl2;
import net.sf.jmoney.entrytable.IndividualBlock;
import net.sf.jmoney.entrytable.RowControl;
import net.sf.jmoney.stocks.pages.StockEntryRowControl.TransactionType;

import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.property.value.IValueProperty;
import org.eclipse.core.internal.databinding.provisional.bind.Bind;
import org.eclipse.jface.databinding.viewers.ViewersObservables;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

public class TransactionTypeBlock extends
			IndividualBlock<IObservableValue<StockEntryFacade>> {
		public TransactionTypeBlock() {
			super("Action", 50, 1);
		}

		@Override
		public Control createCellControl(Composite parent, final IObservableValue<StockEntryFacade> master, RowControl rowControl) {
			final CCombo control = new CCombo(parent, SWT.NONE);
			ComboViewer viewer = new ComboViewer(control);
			viewer.setContentProvider(ArrayContentProvider.getInstance());
			viewer.setLabelProvider(new LabelProvider() {
				@Override
				public String getText(Object element) {
					return ((TransactionType)element).toString();
				}
			});
			viewer.setInput(TransactionType.values());
			IValueProperty<StockEntryFacade, TransactionType> transactionProperty = new PropertyOnObservable<StockEntryFacade, TransactionType>(TransactionType.class) {
				@Override
				protected IObservableValue<TransactionType> getObservable(
						StockEntryFacade source) {
					return source.transactionType();
				}
			};
			
			Bind.twoWay(transactionProperty, master)
			.to(ViewersObservables.<TransactionType>observeSingleSelection(viewer));
			
//				master.addValueChangeListener(new IValueChangeListener() {
//
//					@Override
//					public void handleValueChange(ValueChangeEvent event) {
//						System.out.println(master.getValue().getTransactionType().toString());
//						// TODO Auto-generated method stub
//						
//					}});
			ICellControl2<StockEntryFacade> cellControl = new ICellControl2<StockEntryFacade>() {

				@Override
				public Control getControl() {
					return control;
				}

				@Override
				public void load(StockEntryFacade data) {
					// TODO Auto-generated method stub
				}

				@Override
				public void save() {
					// TODO Auto-generated method stub

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
			control.addFocusListener(controlFocusListener);

			return cellControl.getControl();
		}
	}