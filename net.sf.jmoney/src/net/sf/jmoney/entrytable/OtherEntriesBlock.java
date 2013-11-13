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

import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.IPropertyControl;

import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * This class represents a block that is used to display the fields from the
 * other entries in the transaction.
 *
 * If this is a simple transaction (one other entry and that entry is an
 * income/expense account) then the properties are displayed in place.
 *
 * If this is a split transaction then the words '--split--' is displayed with
 * a drop-down button.
 *
 * If this is a transfer then this appears the same as a simple transaction
 * except that the account is in square brackets.
 *
 * @author Nigel Westbury
 */
public class OtherEntriesBlock extends CellBlock<EntryData, BaseEntryRowControl> {

	final static int DROPDOWN_BUTTON_WIDTH = 15;

	private Block<Entry, ISplitEntryContainer> otherEntriesRootBlock;

	public OtherEntriesBlock(Block<Entry, ISplitEntryContainer> otherEntriesRootBlock) {
		super(
				otherEntriesRootBlock.minimumWidth + DROPDOWN_BUTTON_WIDTH,
				otherEntriesRootBlock.weight
		);

		this.otherEntriesRootBlock = otherEntriesRootBlock;

		otherEntriesRootBlock.initIndexes(0);
	}

    @SuppressWarnings("unchecked")
    @Override
	public IPropertyControl<EntryData> createCellControl(Composite parent, IObservableValue<? extends EntryData> master, RowControl rowControl, BaseEntryRowControl coordinator) {

	    /*
	     * Use a single row tracker for this
	     * table.  This needs to be generalized for, say, the reconciliation
	     * editor if there is to be a single row selection for both tables.
	     *
	     * This table uses the same cell focus tracker as the parent table.  This
	     * ensures that only one cell is shown to have the focus, regardless of the
	     * table or embedded table in which the cell appears.
	     */
	    RowSelectionTracker<BaseEntryRowControl> rowTracker = rowControl.selectionTracker;

	    // TODO: Would the cell focus tracker be more cleanly passed as a parameter?
	    // Perhaps even as a parameter to the constructor as it is going to be the
	    // same for all controls constructed from this object.
	    FocusCellTracker cellTracker = rowControl.focusCellTracker;

		final OtherEntriesControl control = new OtherEntriesControl(parent, master, rowControl, otherEntriesRootBlock, rowTracker, cellTracker);

		IPropertyControl<EntryData> cellControl = new IPropertyControl<EntryData>() {
			@Override
			public Control getControl() {
				return control;
			}
			@Override
			public void load(EntryData data) {
				control.load(data);
			}
			@Override
			public void save() {
				control.save();
			}
		};

		FocusListener controlFocusListener = new NonCellFocusListener<RowControl>(rowControl);

		// This is a little bit of a kludge.  Might be a little safer to implement a method
		// in IPropertyControl to add the focus listener?
		// downArrowButton should be private?
		control.downArrowButton.addFocusListener(controlFocusListener);

//			textControl.addKeyListener(keyListener);
//			textControl.addTraverseListener(traverseListener);

		return cellControl;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void createHeaderControls(Composite parent, EntryData entryData) {
		Composite composite = new Composite(parent, SWT.NONE);

		BlockLayout layout = new BlockLayout(otherEntriesRootBlock, true);
		composite.setLayout(layout);

		otherEntriesRootBlock.createHeaderControls(composite, null);
	}

	@Override
	void layout(int width) {
		this.width = width;

		/*
		 * This control has a drop-down button to the right of the cells in this
		 * control. We therefore must substact the width of the button in order
		 * to get the width into which the child cells must fit.
		 */
		otherEntriesRootBlock.layout(width - DROPDOWN_BUTTON_WIDTH);
	}
}
