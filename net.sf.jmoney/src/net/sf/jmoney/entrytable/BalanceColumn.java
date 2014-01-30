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
import net.sf.jmoney.model2.IPropertyControl;
import net.sf.jmoney.resources.Messages;

import org.eclipse.core.databinding.observable.value.ComputedValue;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.internal.databinding.provisional.bind.Bind;
import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.databinding.swt.SWTObservables;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

public class BalanceColumn extends IndividualBlock<EntryData, BaseEntryRowControl> {

	private class BalanceCellControl implements IPropertyControl<EntryData>, IBalanceChangeListener {
		private final Label balanceLabel;
//		private EntryData entryData = null;

		private BalanceCellControl(Label balanceLabel, final IObservableValue<? extends EntryData> entryData) {
			this.balanceLabel = balanceLabel;
			
			IObservableValue<String> balanceText = new ComputedValue<String>() {
				@Override
				protected String calculate() {
					/*
					 * If no input the control will not be visible, but this will still be calculated
					 * and we can't throw a null pointer exception.
					 */
				return entryData.getValue() == null 
						? "" : commodityForFormatting.format(entryData.getValue().getBalance() + entryData.getValue().getEntry().getAmount());
				}
			};
			
			Bind.oneWay(balanceText).to(SWTObservables.observeText(balanceLabel));
		}

		@Override
		public Control getControl() {
			return balanceLabel;
		}

		@Override
		public void load(EntryData entryData) {
//			this.entryData = entryData;
			balanceLabel.setText(commodityForFormatting.format(entryData.getBalance() + entryData.getEntry().getAmount()));
		}

		@Override
		public void save() {
			// Not editable so nothing to do
		}

		public void setFocusListener(FocusListener controlFocusListener) {
			// Nothing to do
		}

		@Override
		public void balanceChanged() {
			/*
			 * The balance in the EntryData object has changed. This happens if
			 * the amount in a previous entry changes, or a previous entry is
			 * inserted or removed.
			 */
			// This should be done now because all are tracked observables
//			balanceLabel.setText(commodityForFormatting.format(entryData.getBalance() + entryData.getEntry().getAmount()));
		}
	}

	private Commodity commodityForFormatting;

	public BalanceColumn(Commodity commodityForFormatting) {
		super(Messages.BalanceColumn_Name, 70, 2);
		Assert.isNotNull(commodityForFormatting);
		this.commodityForFormatting = commodityForFormatting;
	}

	public int compare(EntryData entryData1, EntryData entryData2) {
		// Entries lists cannot be sorted based on the balance.
		// The caller should not do this.
		throw new RuntimeException("internal error - attempt to sort on balance"); //$NON-NLS-1$
	}

    @Override
	public Control createCellControl(Composite parent, IObservableValue<? extends EntryData> master, RowControl rowControl, BaseEntryRowControl coordinator) {
		final Label balanceLabel = new Label(parent, SWT.TRAIL);

		BalanceCellControl cellControl = new BalanceCellControl(balanceLabel, master);

		coordinator.addBalanceChangeListener(cellControl);

		return cellControl.getControl();
	}

	public String getId() {
		return "balance"; //$NON-NLS-1$
	}
}

