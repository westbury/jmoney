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
public abstract class CellBlock<R> extends Block<R> {
	/**
	 * The index of this cell in the child list.
	 * This is not set until initIndexes is called.
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
	 * @return an IPropertyControl wrapper around an SWT control
	 */
	public abstract Control createCellControl(Composite parent, R blockInput, RowControl rowControl);

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
	public void createCellControls(Composite parent, R blockInput, RowControl rowControl) {
		// Just one cell to create, this, ourselves
		this.createCellControl(parent, blockInput, rowControl);
	}

	@Override
	protected void layout(int width) {
		this.width = width;
	}

	@Override
	protected int getHeightForGivenWidth(int width, int verticalSpacing, Control[] controls, boolean changed) {
		Control control = controls[index];
		return control.computeSize(width, SWT.DEFAULT, changed).y;
	}

	@Override
	protected void positionControls(int x, int y, int verticalSpacing, Control[] controls, R input, boolean changed) {
		Control control = controls[index];
		int height = control.computeSize(width, SWT.DEFAULT, changed).y;
		control.setBounds(x, y, width, height);
	}

	@Override
	protected int getHeight(int verticalSpacing, Control[] controls) {
		Control control = controls[index];
		return control.getSize().y;
	}

	@Override
	protected void paintRowLines(GC gc, int x, int y, int verticalSpacing, Control[] controls, R input) {
		// Nothing to do.
	}

	@Override
	protected void setInput(R input) {
		// By default, do nothing.
		// If, in a derived class, the header is affected by the input then this
		// method should be overridden.
	}
}
