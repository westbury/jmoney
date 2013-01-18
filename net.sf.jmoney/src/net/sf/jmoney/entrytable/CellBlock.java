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

import java.util.ArrayList;
import java.util.Collection;

import net.sf.jmoney.model2.IPropertyControl;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * Represents a block that represents a single child control in
 * both the header and the row controls.  This may represent a
 * single piece of data with header text(e.g. the IndividualBlock derived class) or
 * it may represent a composite column (e.g. the OtherEntriesBlock).
 */
public abstract class CellBlock<T,R> extends Block<T,R> {
	/**
	 * The index of this cell in the list returned by buildCellList.
	 * This is not set until buildCellList is called.
	 */
	private int index;
	
	/**
	 * Create a control for editing the value in this cell.
	 * 
	 * @param parent the parent composite, usually being the RowControl
	 * 		but may be a child composite in certain circumstances
	 * @param rowControl the row control that contains this cell, this
	 * 		parameter being used so that we know which row should become
	 * 		the selected row when this cell gets focus
	 * @param coordinator an object that can be used so that controls
	 * 		can communicate which each other, each cell block being
	 * 		parameterized with a coordinator class (R) and each cell
	 * 		being given a class of that type
	 * @return an IPropertyControl wrapper around an SWT control
	 */
	public abstract IPropertyControl<T> createCellControl(Composite parent, RowControl rowControl, R coordinator);

	public CellBlock(int minimumWidth, int weight) {
		this.minimumWidth = minimumWidth;
		this.weight = weight;
	}

	@Override
	public int initIndexes(int startIndex) {
		index = startIndex;
		return 1;
	}
	
	@Override
	public Collection<CellBlock<? super T,? super R>> buildCellList() {
		ArrayList<CellBlock<? super T,? super R>> cellList = new ArrayList<CellBlock<? super T,? super R>>();
		cellList.add(this);
		return cellList;
	}

	@Override
	void layout(int width) {
		this.width = width;
	}

	@Override
	int getHeightForGivenWidth(int width, int verticalSpacing, Control[] controls, boolean changed) {
		Control control = controls[index];
		return control.computeSize(width, SWT.DEFAULT, changed).y;
	}

	@Override
	void positionControls(int x, int y, int verticalSpacing, Control[] controls, T entryData, boolean changed) {
		Control control = controls[index];
		int height = control.computeSize(width, SWT.DEFAULT, changed).y;
		control.setBounds(x, y, width, height);
	}

	@Override
	int getHeight(int verticalSpacing, Control[] controls) {
		Control control = controls[index];
		return control.getSize().y;
	}

	@Override
	void paintRowLines(GC gc, int x, int y, int verticalSpacing, Control[] controls, T entryData) {
		// Nothing to do.
	}

	@Override
	void setInput(T input) {
		// By default, do nothing.
		// If, in a derived class, the header is affected by the input then this
		// method should be overridden.
	}
}
