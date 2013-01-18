/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Nigel Westbury - fixes needed to get this to work
 *          in the JMoney project
 *******************************************************************************/
package net.sf.jmoney.pages.entries;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.viewers.ColumnLayoutData;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
/**
 * A layout for a table.
 * <P>
 * <code>TableColumn</code> objects have no setLayoutData method.
 * Therefore layout data must be set for each column using setData
 * with a string of "layoutData".  Every column must have layout data
 * set and the layout data must be of type ColumnLayoutData.
 */
public class TableLayout extends Layout {
	
	/**
	 * Widths of columns as set by the previous call to <code>layout</code>
	 * (if any).  Any differences between the width of a column and the width
	 * in this array indicates that the user has changed the width of a column.
	 * This information allows <code>layout</code> to adjust its algorithm so that
	 * it is not un-doing changes made by the user.
	 */
	private int[] widths = null;
	
	private boolean userOverride = false;

	/**
	 * The amount of extra space to allow for the first column.
	 * This column contains the expand/collapse icons and space
	 * for those icons must be included in the width assigned
	 * by this layout to the first column.
	 * <P>
	 * The correct value for Windows is 16.  
	 */
	private final int expandCollapseIconWidth = 16;
	
	/**
	 * Creates a new table layout.
	 */
	public TableLayout() {
	}
	
	/* (non-Javadoc)
	 * Method declared on Layout.
	 */
    @Override	
	public Point computeSize(Composite c, int wHint, int hHint, boolean flush) {
		if (wHint != SWT.DEFAULT && hHint != SWT.DEFAULT)
			return new Point(wHint, hHint);
		
		Table table = (Table) c;
		// To avoid recursions.
		table.setLayout(null);
		// Use native layout algorithm
		Point result = table.computeSize(wHint, hHint, flush);
		table.setLayout(this);
		
		int width = 0;
		for (int i = 0; i < table.getColumnCount(); i++) {
			ColumnLayoutData layoutData = (ColumnLayoutData) table.getColumn(i).getData("layoutData"); //$NON-NLS-1$
			if (layoutData instanceof ColumnPixelData) {
				ColumnPixelData col = (ColumnPixelData) layoutData;
				width += col.width;
			}
			else if (layoutData instanceof ColumnWeightData) {
				ColumnWeightData col = (ColumnWeightData) layoutData;
				width += col.minimumWidth;
			} else {
				Assert.isTrue(false, "Unknown column layout data");//$NON-NLS-1$
			}
		}
		if (width > result.x)
			result.x = width;
		return result;
	}

	/* (non-Javadoc)
	 * Method declared on Layout.
	 */
    @Override	
	public void layout(Composite c, boolean flush) {
		// Only do initial layout.  Trying to maintain proportions when resizing is too hard,
		// causes lots of widget flicker, causes scroll bars to appear and occasionally stick around (on Windows),
		// requires hooking column resizing as well, and may not be what the user wants anyway.
		//	if (!firstTime) 
		//	return;
		
		if (userOverride) {
			return;
		}
		
		int width = c.getClientArea().width;
		
		Item [] tableColumns;
		if (c instanceof Table) {
			Table table = (Table) c;
			tableColumns = table.getColumns();
		} else {
			Tree table = (Tree) c;
			tableColumns = table.getColumns();
		}
		
		// XXX: Layout is being called with an invalid value the first time
		// it is being called on Linux. This method resets the
		// Layout to null so we make sure we run it only when
		// the value is OK.
		if (width <= 1)
			return;
		
		int size = tableColumns.length;
		
		// If widths is not null then it will contain the column widths
		// last calculated by this method.  If these do not exactly match
		// the current column widths then the user has changed the
		// column widths by dragging the edges.  In this situation we do
		// not do anything here because we do not want to fight against the
		// user's changes.
		if (widths != null && widths.length == size) {
			for (int i = 0; i < size; i++) {
				int currentWidth;
				if (tableColumns[i] instanceof TableColumn) {
					currentWidth = ((TableColumn)tableColumns[i]).getWidth();
				} else {
					currentWidth = ((TreeColumn)tableColumns[i]).getWidth();
				}
				
				if (currentWidth != widths[i]) {
//nrw					userOverride = true;
//nrw					return;
				}
			}
		}
			
		widths = new int[size];
		int fixedWidth = 0;
		int totalWeight = 0;
		
		// If the width of a column was changed by the user then we
		// first change the weights to values that would have resulted
		// in the chosen column widths.  However, if a column width is
		// set to a made larger by the user then that size
		// becomes the new minimum size.  If a column was made smaller
		// 
		
		// First calculate the minimum space
		for (int i = 0; i < size; i++) {
			ColumnLayoutData col = (ColumnLayoutData) tableColumns[i].getData("layoutData"); //$NON-NLS-1$
			if (col instanceof ColumnWeightData) {
				ColumnWeightData cw = (ColumnWeightData) col;
				int minimumWidth = cw.minimumWidth;
				if (i == 0) {
					minimumWidth += expandCollapseIconWidth;
				}
				widths[i] = minimumWidth;
				fixedWidth += minimumWidth;
				totalWeight += cw.weight;
			} else {
				Assert.isTrue(false, "Unknown column layout data");//$NON-NLS-1$
			}
		}
		
		// Do we have columns that have a weight
		if (totalWeight > 0 && width > fixedWidth) {
			// Now distribute the rest to the columns with weight.
			int rest = width - fixedWidth;
			int totalDistributed = 0;
			for (int i = 0; i < size; i++) {
				ColumnLayoutData col = (ColumnLayoutData) tableColumns[i].getData("layoutData"); //$NON-NLS-1$
				if (col instanceof ColumnWeightData) {
					ColumnWeightData cw = (ColumnWeightData) col;
					int pixels = cw.weight * rest / totalWeight;
					totalDistributed += pixels;
					widths[i] += pixels;
				}
			}
			
			// We may still have a few pixels left to allocate because
			// the above divisions round the pixel count downwards.
			// Distribute any remaining pixels to columns with weight.
			// The number of pixels left can never be more than the
			// number of columns with non-zero weights.
			int diff = rest - totalDistributed;
			for (int i = 0; i < size && diff > 0; i++) {
				ColumnLayoutData col = (ColumnLayoutData) tableColumns[i].getData("layoutData"); //$NON-NLS-1$
				if (col instanceof ColumnWeightData) {
					++widths[i];
					--diff;
				}
			}
		}
		
		for (int i = 0; i < size; i++) {
			if (tableColumns[i] instanceof TableColumn) {
				((TableColumn)tableColumns[i]).setWidth(widths[i]);
			} else {
				((TreeColumn)tableColumns[i]).setWidth(widths[i]);
			}
		}
	}
}