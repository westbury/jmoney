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

package net.sf.jmoney.transactionDialog;

import net.sf.jmoney.entrytable.CellFocusListener;
import net.sf.jmoney.entrytable.CreditAndDebitSplitConverter;
import net.sf.jmoney.entrytable.EntryData;
import net.sf.jmoney.entrytable.ICellControl2;
import net.sf.jmoney.entrytable.IndividualBlock;
import net.sf.jmoney.entrytable.RowControl;
import net.sf.jmoney.entrytable.SplitEntryRowControl;
import net.sf.jmoney.model2.Commodity;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.EntryInfo;
import net.sf.jmoney.model2.IPropertyControl;
import net.sf.jmoney.resources.Messages;

import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.internal.databinding.provisional.bind.Bind;
import org.eclipse.core.internal.databinding.provisional.bind.IBidiConverter;
import org.eclipse.jface.databinding.swt.SWTObservables;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
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
class SplitEntryDebitAndCreditColumns extends IndividualBlock<Entry, SplitEntryRowControl> {

	private class DebitAndCreditCellControl implements ICellControl2<Entry> {
		private Text textControl;

		public DebitAndCreditCellControl(Text textControl, IObservableValue<? extends Entry> master) {
			this.textControl = textControl;

			IObservableValue<Long> amountObservable = EntryInfo.getAmountAccessor().observeDetail(master);

			IBidiConverter<Long, String> creditAndDebitSplitConverter = new CreditAndDebitSplitConverter(commodity, isDebit, amountObservable);
			
			Bind.twoWay(amountObservable)
			.convert(creditAndDebitSplitConverter)
			.to(SWTObservables.observeText(textControl, SWT.Modify));
			
			Bind.bounceBack(creditAndDebitSplitConverter)
			.to(SWTObservables.observeText(textControl, SWT.FocusOut));
			
			// TODO check that disposing the Text disposed the binding
			textControl.addDisposeListener(new DisposeListener() {
				@Override
				public void widgetDisposed(DisposeEvent e) {
					System.out.println("here");
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
	private Commodity commodity;
	private boolean isDebit;

	public static SplitEntryDebitAndCreditColumns createCreditColumn(Commodity commodityForFormatting) {
    	return new SplitEntryDebitAndCreditColumns("credit", Messages.SplitEntryDebitAndCreditColumns_Credit, commodityForFormatting, false);  //$NON-NLS-1$
	}

	public static SplitEntryDebitAndCreditColumns createDebitColumn(Commodity commodityForFormatting) {
    	return new SplitEntryDebitAndCreditColumns("debit", Messages.SplitEntryDebitAndCreditColumns_Debit, commodityForFormatting, true);      //$NON-NLS-1$
	}

	private SplitEntryDebitAndCreditColumns(String id, String name, Commodity commodity, boolean isDebit) {
		super(name, 70, 2);
		this.id = id;
		this.commodity = commodity;
		this.isDebit = isDebit;
	}

	public String getId() {
		return id;
	}

    @Override
	public IPropertyControl<Entry> createCellControl(Composite parent, IObservableValue<? extends Entry> master, RowControl rowControl, SplitEntryRowControl coordinator) {

		final Text textControl = new Text(parent, SWT.TRAIL);

		ICellControl2<Entry> cellControl = new DebitAndCreditCellControl(textControl, master);

		FocusListener controlFocusListener = new CellFocusListener<RowControl>(rowControl, cellControl);
		textControl.addFocusListener(controlFocusListener);

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

    	return cellControl;
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