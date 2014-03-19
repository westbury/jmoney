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

public class BalanceColumn extends IndividualBlock<IObservableValue<EntryData>> {

	private class BalanceCellControl implements IPropertyControl<EntryData> {
		private final Label balanceLabel;

		private BalanceCellControl(Label balanceLabel, final IObservableValue<EntryData> entryData) {
			this.balanceLabel = balanceLabel;
			
			IObservableValue<String> balanceText = new ComputedValue<String>() {
				@Override
				protected String calculate() {
					/*
					 * If no input the control will not be visible, but this will still be calculated
					 * and we can't throw a null pointer exception.
					 */
					if (entryData.getValue() == null) {
						return "";
					}
					
					/*
					 * If this is the new entry row then don't display a balance.  The balance column is based
					 * on committed data and does not update as the user types credit and debit amounts. 
					 */
					if (entryData.getValue().getEntry() == null) {
						return "";
					}
					
					long previousBalance = entryData.getValue().getBalance();
					long thisEntryAmount = entryData.getValue().getEntry().getAmount();
					long newBalance = previousBalance + thisEntryAmount;
					return commodityForFormatting.format(newBalance);
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
			balanceLabel.setText(commodityForFormatting.format(entryData.getBalance() + entryData.getEntry().getAmount()));
		}

		@Override
		public void save() {
			// Not editable so nothing to do
		}

		public void setFocusListener(FocusListener controlFocusListener) {
			// Nothing to do
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
	public Control createCellControl(Composite parent, IObservableValue<EntryData> blockInput, RowControl rowControl) {
		final Label balanceLabel = new Label(parent, SWT.TRAIL);

		BalanceCellControl cellControl = new BalanceCellControl(balanceLabel, blockInput);

		return cellControl.getControl();
	}

	public String getId() {
		return "balance"; //$NON-NLS-1$
	}

}

