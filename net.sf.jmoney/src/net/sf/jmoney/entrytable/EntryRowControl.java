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
import net.sf.jmoney.model2.TransactionManagerForAccounts;
import net.sf.jmoney.pages.entries.ForeignCurrencyDialog;

import org.eclipse.core.databinding.observable.value.ComputedValue;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.IValueChangeListener;
import org.eclipse.core.databinding.observable.value.ValueChangeEvent;
import org.eclipse.core.internal.databinding.provisional.bind.Bind;
import org.eclipse.swt.widgets.Composite;

public class EntryRowControl extends BaseEntryRowControl<EntryData, EntryRowControl> {

	@SuppressWarnings("unchecked")
	public EntryRowControl(final Composite parent, int style, ICompositeTable<EntryData> rowTable, Block<EntryData, ? super EntryRowControl> rootBlock, final RowSelectionTracker selectionTracker, final FocusCellTracker focusCellTracker) {
		super(parent, style, rowTable, rootBlock, selectionTracker, focusCellTracker);
		init(this, this, rootBlock);
	}

	@Override
	public void setInput(final EntryData inputEntryData) {
		/*
		 * We do this first even though it is done again when the super method
		 * is called.  This sets the 'input' value in the base object and means
		 * the following code can use that field.
		 */
		input.setValue(inputEntryData);

		/*
		 * Bind the default values if and only if this is a new transaction
		 */
		if (committedEntryData.getEntry() == null) {

			/*
			 * This is listening to changes from the model.  It may be that we really
			 * only want this processing to happen if the change comes from the UI.
			 * If the change comes through the model from somewhere else then we really
			 * should not be doing this.  However it is not currently a problem because
			 * we use transactions, and to fix it properly would require an API to listen
			 * to bindings rather than observables.  (So we get the change only if it
			 * originates from the target).
			 */


			/**
			 * If this transaction has two entries and the commodities of the two entries
			 * are not known to be different then the other entry is the value of this observable,
			 * otherwise this observable has a value of null.
			 */
			// TODO none of the getters in the following are tracked.  They need to be.
			IObservableValue<Entry> otherEntryObservable = new ComputedValue<Entry>() {
				@Override
				protected Entry calculate() {
					if (!inputEntryData.hasSplitEntries()) {
						Entry entry = inputEntryData.getEntry();
						Entry otherEntry = inputEntryData.getOtherEntry();
						Commodity commodity1 = entry.getCommodityInternal();
						Commodity commodity2 = otherEntry.getCommodityInternal();
						if (commodity1 == null || commodity2 == null || commodity1.equals(commodity2)) {
							return otherEntry;
						}
					}
					return null;
				}
			};

			if (otherEntryObservable.getValue() != null) {
				Entry entry = inputEntryData.getEntry();
				Entry otherEntry = inputEntryData.getOtherEntry();

				final IObservableValue<Long> target = EntryInfo.getAmountAccessor().observe(otherEntry);
				final IObservableValue<Long> source = EntryInfo.getAmountAccessor().observe(entry);
				IObservableValue<Long> sourceNegated = new ComputedValue<Long>() {
					@Override
					protected Long calculate() {
						return -source.getValue();
					}
				};

				Bind.oneWay(sourceNegated).untilTargetChanges().to(target);

				/*
				 * If any changes happen to the transaction which mean the other entry is no longer
				 * a suitable target, even if another entry should happen to become a suitable target,
				 * then we stop the binding.
				 *
				 * The way to stop a binding is to dispose the target observable.
				 */
				otherEntryObservable.addValueChangeListener(new IValueChangeListener<Entry>() {
					@Override
					public void handleValueChange(
							ValueChangeEvent<Entry> event) {
						if (!target.isDisposed()) {
							target.dispose();
						}
					}
				});
			}

//			inputEntryData.netAmount.addValueChangeListener(new IValueChangeListener<Long>() {
//				@Override
//				public void handleValueChange(ValueChangeEvent event) {
//					if (!inputEntryData.hasSplitEntries()) {
//						Entry entry = inputEntryData.getEntry();
//						Entry otherEntry = inputEntryData.getOtherEntry();
//						Commodity commodity1 = entry.getCommodityInternal();
//						Commodity commodity2 = otherEntry.getCommodityInternal();
//						if (commodity1 == null || commodity2 == null || commodity1.equals(commodity2)) {
//							otherEntry.setAmount(-entry.getAmount());
//						}
//					}
//				}
//			});

		}

		/*
		 * This must be called after we have set our own stuff up.  The reason
		 * being that this call loads controls (such as the stock price control).
		 * These controls will not load correctly if this object is not set up.
		 */
		super.setInput(inputEntryData);
	}

	@Override
	protected EntryData createUncommittedEntryData(Entry entryInTransaction,
			TransactionManagerForAccounts transactionManager) {
		final EntryData entryData = new EntryData(entryInTransaction, transactionManager);
		return entryData;
	}

	@Override
	protected EntryRowControl getThis() {
		return this;
	}

	@Override
	protected void specificValidation() throws InvalidUserEntryException {
		EntryData uncommittedEntryData = input.getValue();
		/*
		 * Check for zero amounts. A zero amount is normally a user
		 * error and will not be accepted. However, if this is not a
		 * split transaction and the currencies are different then we
		 * prompt the user for the amount of the other entry (the income
		 * and expense entry). This is very desirable because the
		 * foreign currency column (being used so little) is not
		 * displayed by default.
		 */
		// TODO: We could drop down the shell as though this is a split
		// entry whenever the currencies do not match.  This would expose
		// the amount of the other entry.
		if (!uncommittedEntryData.hasSplitEntries()
				&& uncommittedEntryData.getEntry().getAmount() != 0
				&& uncommittedEntryData.getOtherEntry().getAmount() == 0
				&& uncommittedEntryData.getOtherEntry().getCommodityInternal() != uncommittedEntryData.getEntry().getCommodityInternal()) {
			ForeignCurrencyDialog dialog = new ForeignCurrencyDialog(
					getShell(),
					uncommittedEntryData);
			dialog.open();
		} else {
			for (Entry entry: uncommittedEntryData.getEntry().getTransaction().getEntryCollection()) {
				if (entry.getAmount() == 0) {
					throw new InvalidUserEntryException(
							"A non-zero credit or debit amount must be entered.", //$NON-NLS-1$
							null);
				}
			}
		}
	}
}

