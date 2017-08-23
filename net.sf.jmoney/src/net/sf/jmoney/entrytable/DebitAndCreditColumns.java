/*
 *
 *  JMoney - A Personal Finance Manager
 *  Copyright (c) 2007 Nigel Westbury <westbury@users.sf.net>
 *
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 */

package net.sf.jmoney.entrytable;

import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.internal.databinding.provisional.bind.Bind;
import org.eclipse.core.internal.databinding.provisional.bind.IBidiWithStatusConverter;
import org.eclipse.jface.databinding.fieldassist.ControlStatusDecoration;
import org.eclipse.jface.databinding.swt.SWTObservables;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;

import net.sf.jmoney.fields.IAmountFormatter;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.EntryInfo;
import net.sf.jmoney.resources.Messages;

/**
 * Represents a table column that is either the debit or the credit column. Use
 * two instances of this class instead of a single instance of the
 * <code>PropertyBlock</code> class if you want the amount to be displayed in
 * separate debit and credit columns.
 */
public class DebitAndCreditColumns extends IndividualBlock<IObservableValue<Entry>> {

	private class DebitAndCreditCellControl implements ICellControl2<Entry> {
		private Text textControl;

		public DebitAndCreditCellControl(Composite parent, RowControl rowControl, final IObservableValue<Entry> entryObservable) {
			this.textControl = new Text(parent, SWT.TRAIL);

			IObservableValue<Long> amountObservable = EntryInfo.getAmountAccessor().observeDetail(entryObservable);

			IBidiWithStatusConverter<Long, String> creditAndDebitSplitConverter = new CreditAndDebitSplitConverter(formatter, isDebit, amountObservable);
			
			ControlStatusDecoration statusDecoration = new ControlStatusDecoration(
					textControl, SWT.LEFT | SWT.TOP);

			Bind.twoWay(amountObservable)
			.convert(creditAndDebitSplitConverter)
			.to(SWTObservables.observeText(textControl, SWT.Modify), statusDecoration::update);
			
			Bind.bounceBack(creditAndDebitSplitConverter)
			.to(SWTObservables.observeText(textControl, SWT.FocusOut));
			
			FocusListener controlFocusListener = new CellFocusListener<RowControl>(rowControl, this);
			textControl.addFocusListener(controlFocusListener);
//				textControl.addKeyListener(keyListener);
//				textControl.addTraverseListener(traverseListener);

			textControl.addTraverseListener(new TraverseListener() {
				@Override
				public void keyTraversed(TraverseEvent e) {
					switch (e.detail) {
					case SWT.TRAVERSE_ARROW_PREVIOUS:
						if (e.keyCode == SWT.ARROW_UP) {
							e.doit = false;
							e.detail = SWT.TRAVERSE_NONE;
						}
						break;
					case SWT.TRAVERSE_ARROW_NEXT:
						if (e.keyCode == SWT.ARROW_DOWN) {
							e.doit = true;
						}
						break;
					}
				}
			});
		}

		@Override
		public Control getControl() {
			return textControl;
		}

		@Override
		public void load(Entry data) {
			// No longer used
		}

		@Override
		public void save() {
			// No longer used
		}

		@Override
		public void setSelected() {
			textControl.setBackground(RowControl.selectedCellColor);
		}

		@Override
		public void setUnselected() {
			textControl.setBackground(null);
		}
	}

	private String id;
	
	/**
	 * The commodity of the amounts in the credit and debit columns.
	 */
	private IAmountFormatter formatter;
	
	private boolean isDebit;

	public static DebitAndCreditColumns createCreditColumn(IAmountFormatter formatter) {
    	return new DebitAndCreditColumns("credit", Messages.DebitAndCreditColumns_CreditName, formatter, false);  //$NON-NLS-1$
	}

	public static DebitAndCreditColumns createDebitColumn(IAmountFormatter formatter) {
    	return new DebitAndCreditColumns("debit", Messages.DebitAndCreditColumns_DebitName, formatter, true);      //$NON-NLS-1$
	}

	private DebitAndCreditColumns(String id, String name, IAmountFormatter formatter, boolean isDebit) {
		super(name, 70, 2);
		this.id = id;
		this.formatter = formatter;
		this.isDebit = isDebit;
	}

	public String getId() {
		return id;
	}

    @Override
	public Control createCellControl(Composite parent, final IObservableValue<Entry> blockInput, RowControl rowControl) {
		ICellControl2<Entry> cellControl = new DebitAndCreditCellControl(parent, rowControl, blockInput);

		return cellControl.getControl();
    }

	public int compare(EntryData entryData1, EntryData entryData2) {
		long amount1 = entryData1.getEntry().getAmount();
		long amount2 = entryData2.getEntry().getAmount();

		int result;
		if (amount1 < amount2) {
			result = -1;
		} else if (amount1 > amount2) {
			result = 1;
		} else {
			result = 0;
		}

		// If debit column then reverse.  Ascending sort should
		// result in the user seeing ascending numbers in the
		// sorted column.
		if (isDebit) {
			result = -result;
		}

		return result;
	}

	public static Block<IObservableValue<Entry>> createDebitAndCreditColumns(IAmountFormatter formatter) {
		CellBlock<IObservableValue<Entry>> debitColumnManager = DebitAndCreditColumns.createDebitColumn(formatter);
		CellBlock<IObservableValue<Entry>> creditColumnManager = DebitAndCreditColumns.createCreditColumn(formatter);

		return new HorizontalBlock<IObservableValue<Entry>>(
    					debitColumnManager,
    					creditColumnManager
		);
	}
}