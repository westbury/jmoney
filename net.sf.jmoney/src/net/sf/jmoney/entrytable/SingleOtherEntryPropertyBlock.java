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
import net.sf.jmoney.model2.ScalarPropertyAccessor;

import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * Represents a property that can be displayed in the entries table, where
 * the property comes from the 'other' entry and thus may be in a split entries
 * table.
 * <P>
 * Because there may be more than one split, the input for this property is the Entry
 * object (not the EntryData object).  If the transaction is not split then this Entry
 * object will be the other entry in the transaction.
 */
public class SingleOtherEntryPropertyBlock extends IndividualBlock<Entry, ISplitEntryContainer> {
	private ScalarPropertyAccessor<?,?> accessor;
	
	public SingleOtherEntryPropertyBlock(ScalarPropertyAccessor accessor) {
		super(
				accessor.getDisplayName(),
				accessor.getMinimumWidth(),
				accessor.getWeight()
		);

		this.accessor = accessor;
	}

	public SingleOtherEntryPropertyBlock(ScalarPropertyAccessor accessor, String displayName) {
		super(
				displayName,
				accessor.getMinimumWidth(),
				accessor.getWeight()
		);

		this.accessor = accessor;
	}

	public String getId() {
		return accessor.getName();
	}

    @Override	
	public IPropertyControl<Entry> createCellControl(Composite parent, RowControl rowControl, ISplitEntryContainer coordinator) {
		final IPropertyControl propertyControl = accessor.createPropertyControl(parent);
		
		ICellControl2<Entry> cellControl = new ICellControl2<Entry>() {
			@Override
			public Control getControl() {
				return propertyControl.getControl();
			}

			@SuppressWarnings("unchecked")
			@Override
			public void load(Entry entry) {
				propertyControl.load(entry);
			}

			@Override
			public void save() {
				propertyControl.save();
			}

			@Override
			public void setSelected() {
				propertyControl.getControl().setBackground(RowControl.selectedCellColor);
			}

			@Override
			public void setUnselected() {
				propertyControl.getControl().setBackground(null);
			}
		};
		
		// TODO: remove parameterization from following???
		FocusListener controlFocusListener = new CellFocusListener<RowControl>(rowControl, cellControl);

		// This is a little bit of a kludge.  Might be a little safer to implement a method
		// in IPropertyControl to add the focus listener?
		addFocusListenerRecursively(propertyControl.getControl(), controlFocusListener);
		
//			textControl.addKeyListener(keyListener);
//			textControl.addTraverseListener(traverseListener);
		
		return cellControl;
	}

    /**
	 * Add listeners to each control.
	 * 
	 * @param control The control to listen to.
	 */
	private void addFocusListenerRecursively(Control control, FocusListener listener) {
		control.addFocusListener(listener);
		
		if (control instanceof Composite) {
			Composite composite = (Composite) control;
			for (Control childControl : composite.getChildren()) {
				addFocusListenerRecursively(childControl, listener);
			}
		}
	}
}