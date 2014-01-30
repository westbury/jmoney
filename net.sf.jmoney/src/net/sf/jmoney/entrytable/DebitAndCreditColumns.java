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

import net.sf.jmoney.model2.Commodity;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.EntryInfo;
import net.sf.jmoney.model2.IPropertyControl;
import net.sf.jmoney.resources.Messages;

import org.eclipse.core.databinding.observable.value.ComputedValue;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.internal.databinding.provisional.bind.Bind;
import org.eclipse.core.internal.databinding.provisional.bind.IBidiConverter;
import org.eclipse.jface.databinding.swt.SWTObservables;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;

/**
 * Represents a table column that is either the debit or the credit column. Use
 * two instances of this class instead of a single instance of the
 * <code>PropertyBlock</code> class if you want the amount to be displayed in
 * separate debit and credit columns.
 */
public class DebitAndCreditColumns extends IndividualBlock<EntryData, BaseEntryRowControl> {

	private class DebitAndCreditCellControl implements ICellControl2<EntryData> {
		private Text textControl;

		public DebitAndCreditCellControl(Composite parent, RowControl rowControl, final IObservableValue<? extends EntryData> entryData) {
			this.textControl = new Text(parent, SWT.TRAIL);

			final IObservableValue<Entry> entryObservable = new ComputedValue<Entry>() {
				@Override
				protected Entry calculate() {
					if (entryData.getValue() == null) {
						// No data set, so doesn't really matter what we return,
						// we just can't NPE.
						return null;
					} else {
						return entryData.getValue().getEntry();
					} 
				}
			};
			
			IObservableValue<Long> amountObservable = EntryInfo.getAmountAccessor().observeDetail(entryObservable);

			IBidiConverter<Long, String> creditAndDebitSplitConverter = new CreditAndDebitSplitConverter(commodity, isDebit, amountObservable);
			
			Bind.twoWay(amountObservable)
			.convert(creditAndDebitSplitConverter)
			.to(SWTObservables.observeText(textControl, SWT.Modify));
			
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
		public void load(EntryData data) {
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
	private Commodity commodity;
	
	private boolean isDebit;

	public static DebitAndCreditColumns createCreditColumn(Commodity commodityForFormatting) {
    	return new DebitAndCreditColumns("credit", Messages.DebitAndCreditColumns_CreditName, commodityForFormatting, false);  //$NON-NLS-1$
	}

	public static DebitAndCreditColumns createDebitColumn(Commodity commodityForFormatting) {
    	return new DebitAndCreditColumns("debit", Messages.DebitAndCreditColumns_DebitName, commodityForFormatting, true);      //$NON-NLS-1$
	}

	private DebitAndCreditColumns(String id, String name, Commodity commodity, boolean isDebit) {
		super(name, 70, 2);
		this.id = id;
		this.commodity = commodity;
		this.isDebit = isDebit;
	}

	public String getId() {
		return id;
	}

    @Override
	public Control createCellControl(Composite parent, IObservableValue<? extends EntryData> master, RowControl rowControl, BaseEntryRowControl coordinator) {

		ICellControl2<EntryData> cellControl = new DebitAndCreditCellControl(parent, rowControl, master);

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
}