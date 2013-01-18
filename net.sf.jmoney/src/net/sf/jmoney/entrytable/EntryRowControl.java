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

import net.sf.jmoney.isolation.TransactionManager;
import net.sf.jmoney.model2.Commodity;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.pages.entries.ForeignCurrencyDialog;

import org.eclipse.swt.widgets.Composite;

public class EntryRowControl extends BaseEntryRowControl<EntryData, EntryRowControl> {

	@SuppressWarnings("unchecked")
	public EntryRowControl(final Composite parent, int style, ICompositeTable<EntryData> rowTable, Block<EntryData, ? super EntryRowControl> rootBlock, final RowSelectionTracker selectionTracker, final FocusCellTracker focusCellTracker) {
		super(parent, style, rowTable, rootBlock, selectionTracker, focusCellTracker);
		init(this, this, rootBlock);
	}

	@Override
	protected EntryData createUncommittedEntryData(Entry entryInTransaction,
			TransactionManager transactionManager) {
		EntryData entryData = new EntryData(entryInTransaction, transactionManager);
		return entryData;
	}

	@Override
	protected EntryRowControl getThis() {
		return this;
	}

	@Override
	public void amountChanged() {
		
		// If there are two entries in the transaction and
		// if both entries have accounts in the same currency or
		// one or other account is not known or one or other account
		// is a multi-currency account then we set the amount in
		// the other entry to be the same but opposite signed amount.
		Entry entry = uncommittedEntryData.getEntry();
		if (entry.getTransaction().hasTwoEntries()) {
			Entry otherEntry = entry.getTransaction().getOther(entry);
			Commodity commodity1 = entry.getCommodityInternal();
			Commodity commodity2 = otherEntry.getCommodityInternal();
			if (commodity1 == null || commodity2 == null || commodity1.equals(commodity2)) {
				otherEntry.setAmount(-entry.getAmount());
			}
		}
	}

	@Override
	protected void specificValidation() throws InvalidUserEntryException {
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
	
