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
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Layout;


public class BlockLayout<T> extends Layout {

	private Block<? super T,?> block;

	private boolean linkedToHeader;
	
	private T entryData;
	
	/**
	 * marginTop specifies the number of pixels of vertical margin
	 * that will be placed along the top edge of the layout.
	 *
	 * The default value is 0.
	 */
	public int marginTop = 0;

	/**
	 * marginBottom specifies the number of pixels of vertical margin
	 * that will be placed along the bottom edge of the layout.
	 *
	 * The default value is 0.
	 */
	public int marginBottom = 0;

	/**
	 * verticalSpacing specifies the number of pixels between the bottom
	 * edge of one cell and the top edge of its neighbouring cell underneath.
	 *
	 * The default value is 1.
	 */
 	public int verticalSpacing = 1;
	
 	/**
 	 * 
 	 * @param block
 	 * @param linkedToHeader if false then rows and headers are layed out according
 	 * 			to the passed hints.  We don't really know if the rows will be laid out
 	 * 			before or after the header, so they are all layed out independently.
 	 * 			If true then the hints are ignored and the widths are always taken from
 	 * 			the blocks.
 	 */
	public BlockLayout(Block<? super T,?> block, boolean linkedToHeader) {
		this.block = block;
		this.linkedToHeader = linkedToHeader;
		this.entryData = null;
	}

	public void setInput(T entryData) {
		this.entryData = entryData;
	}
	
	@Override
	protected Point computeSize(Composite composite, int wHint, int hHint, boolean flushCache) {
		int width;
		if (linkedToHeader) {
			width = block.width + Block.marginLeft + Block.marginRight;
		} else {
			if (wHint == SWT.DEFAULT) {
				width = block.minimumWidth + Block.marginLeft + Block.marginRight;
			} else {
				width = wHint; 
			}
		}
		
		int widthOfRootBlock = width - Block.marginLeft - Block.marginRight;
		int heightOfRootBlock = block.getHeightForGivenWidth(widthOfRootBlock, verticalSpacing, composite.getChildren(), flushCache);
		int height = heightOfRootBlock + marginTop + marginBottom;
		return new Point(width, height);
	}

	@Override
	protected boolean flushCache (Control control) {
		// We don't currently cache anything in this layout (though
		// perhaps we should).
		return true;
	}

	@Override
	protected void layout(Composite composite, boolean flushCache) {
		if (!linkedToHeader) {
			Rectangle rect = composite.getClientArea();
			block.layout(rect.width - Block.marginLeft - Block.marginRight);
		}
		
		Control [] children = composite.getChildren();
		block.positionControls(Block.marginLeft, marginTop, verticalSpacing, children, entryData, flushCache);
	}

	public void paintRowLines(GC gc, Composite composite) {
		block.paintRowLines(gc, Block.marginLeft, marginTop, verticalSpacing, composite.getChildren(), entryData);
	}
}
