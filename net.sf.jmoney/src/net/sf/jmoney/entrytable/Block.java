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

import java.util.Collection;

import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

public abstract class Block<T, R> {
 	/**
	 * marginLeft specifies the number of pixels of horizontal margin
	 * that will be placed along the left edge of the layout.
	 *
	 * The default value is 1.
	 */
	public static final int marginLeft = 1;

	/**
	 * marginRight specifies the number of pixels of horizontal margin
	 * that will be placed along the right edge of the layout.
	 *
	 * The default value is 1.
	 */
	public static final int marginRight = 1;

	/**
	 * horizontalSpacing specifies the number of pixels between the right
	 * edge of one cell and the left edge of its neighboring cell to
	 * the right.
	 *
	 * The default value is 1.
	 */
	public static final int horizontalSpacing = 1;

 	protected int minimumWidth;
	protected int weight;

	protected int width;

	public abstract void createHeaderControls(Composite parent, T entryData);

	public abstract Collection<CellBlock<? super T,? super R>> buildCellList();
		
	abstract void layout(int width);

	abstract void positionControls(int x, int y, int verticalSpacing, Control [] controls, T entryData, boolean flushCache);

	abstract void setInput(T input);
	
	/**
	 * Calculate the height of this block. Because variable height rows are
	 * supported, the height may vary from row to row and thus depends on the
	 * controls in the row.
	 * 
	 * This method assumes that the contained controls have all been set to
	 * their correct size. This method does not resize controls. Therefore this
	 * method should only be called after <code>positionControls</code> has
	 * been called.
	 * 
	 * @param controls
	 *            a list of controls in a row
	 * @return the height of this block
	 */
	abstract int getHeight(int verticalSpacing, Control[] controls);

	/**
	 * Paints the lines between the controls.
	 * 
	 * This method assumes that the contained controls have all been set to
	 * their correct size. Therefore this method should only be called after
	 * <code>positionControls</code> has been called.
	 * 
	 * @param controls
	 *            a list of controls in a row
	 */
	abstract void paintRowLines(GC gc, int x, int y, int verticalSpacing, Control[] controls, T entryData);

	/**
	 * Given a width, calculate the preferred height.
	 * 
	 * @param width
	 * @param verticalSpacing
	 * @param controls
	 *            a list of controls in a row
	 * @param changed
	 *            <code>true</code> if the control's contents have changed,
	 *            and <code>false</code> otherwise
	 * @return the preferred height
	 */
	abstract int getHeightForGivenWidth(int width, int verticalSpacing, Control[] controls, boolean changed);

	/**
	 * This method must be called after construction of the root block.
	 * It traverses over the sub-blocks and sets the indexes of any
	 * cell blocks it finds.  These indexes will match the index of the
	 * cell block in the array returned by <code>buildCellList</code>.
	 *  
	 * @param startIndex 0 if the root block, appropriate value for sub-blocks
	 * @return the number of cell blocks in this block, this value being the
	 * 		amount by which the caller must increment startIndex before passing
	 * 		it on to the next child block
	 */
	abstract public int initIndexes(int startIndex);
}
