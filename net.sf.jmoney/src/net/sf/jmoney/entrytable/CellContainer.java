/*
 *
 *  JMoney - A Personal Finance Manager
 *  Copyright (c) 2008 Nigel Westbury <westbury@users.sf.net>
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

import java.util.HashMap;
import java.util.Map;

import net.sf.jmoney.model2.IPropertyControl;

import org.eclipse.swt.widgets.Composite;

public class CellContainer<T,R> extends Composite {

	/**
	 * the current input, being always a non-null value if this row
	 * is active and undefined if this row is inactive 
	 */
	protected T input;
	
	// Although currently the keys of this map are never used
	// (and it may as well be a list of the values only), a map
	// allows us to do stuff like move the focus to the control
	// in error during transaction validation.
	protected Map<CellBlock, IPropertyControl<? super T>> controls = new HashMap<CellBlock, IPropertyControl<? super T>>();

	public CellContainer(Composite parent, int style) {
		super(parent, style);
	}

	/**
	 * This method creates the controls.
	 * <P>
	 * This method must always be called by the constructor of the final derived
	 * classes of this class. Why do we not just call it from the constructor of
	 * this class? The reason is because the controls that are created by this
	 * method have a back reference to this object. These back references are
	 * typed (using generics) to the final derived type. These controls will
	 * expect field initializers and possibly constructor initialization to have
	 * been done on the final derived type. However, at the time the base
	 * constructor is called, neither will have been initialized.
	 */
	protected void init(RowControl rowControl, R coordinator, Block<? super T, ? super R> rootBlock) {
		for (CellBlock<? super T, ? super R> cellBlock: rootBlock.buildCellList()) {
			// Create the control with no content set.
			final IPropertyControl<? super T> cellControl = cellBlock.createCellControl(this, rowControl, coordinator);
			controls.put(cellBlock, cellControl);

			if (input != null) {
				cellControl.load(input);
			}
		}
	}

	public void setInput(T input) {
		this.input = input;

		for (final IPropertyControl<? super T> control: controls.values()) {
			control.load(input);
		}
	}

	public T getInput() {
		return input;
	}
}
