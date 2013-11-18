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

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;

public class SplitEntryRowControl extends RowControl<Entry, SplitEntryRowControl> implements ISplitEntryContainer {

	// The lighter colors for the sub-entry lines
	protected static final Color normalColor = new Color(Display.getCurrent(),
			245, 245, 255);

	protected static final Color selectedRowColor = new Color(Display.getCurrent(), 215, 215, 255);

	protected static final Color selectedCellColor = new Color(Display
	.getCurrent(), 255, 255, 255);

	/**
	 * true if this row is the current selection, false otherwise
	 */
	private boolean isSelected = false;

	private PaintListener paintListener = new PaintListener() {
		@Override
		public void paintControl(PaintEvent e) {
			drawBorder(e.gc);
		}
	};

	public SplitEntryRowControl(final Composite parent, int style, Block<Entry, ? super SplitEntryRowControl> rootBlock, boolean isLinked, final RowSelectionTracker<SplitEntryRowControl> selectionTracker, final FocusCellTracker focusCellTracker, Entry entry) {
		super(parent, style, selectionTracker, focusCellTracker);

		/*
		 * We set the top and bottom margins to zero here because that ensures
		 * the controls inside this composite line up with the rows that are
		 * outside this composite and in the same row.
		 */
		BlockLayout<Entry> layout = new BlockLayout<Entry>(rootBlock, isLinked);
		layout.marginTop = 0;
		layout.marginBottom = 0;
		layout.verticalSpacing = 1;
		setLayout(layout);

		// Must setInput before carrying on.  This is because this class does not
		// take a master observable.  The Entry must be known and set when constructed.
		setInput(entry);
		
		init(this, this, rootBlock);

		addPaintListener(paintListener);
	}

	/**
	 * Draws a border around the row.  A light gray single line is
	 * drawn at the bottom if the row is not selected.  A black double
	 * line is drawn at the bottom and a black single line at the top and
	 * sides if the row is selected.
	 * @param gc 
	 */
	protected void drawBorder(GC gc) {
		Color oldColor = gc.getBackground();
		try {
			// Get the colors we need
			Display display = Display.getCurrent();
			Color blackColor = display
			.getSystemColor(SWT.COLOR_BLACK);
			// pretty black
			Color lineColor = display
			.getSystemColor(SWT.COLOR_WIDGET_DARK_SHADOW);
			// Looks white
			Color secondaryColor = display
			.getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW);
			// Fairly dark gray
			//Color hilightColor = display
			//.getSystemColor(SWT.COLOR_WIDGET_HIGHLIGHT_SHADOW);

			Point controlSize = getSize();

			// Draw the bottom line(s)
			if (isSelected) {
				gc.setBackground(blackColor);
				// top edge
				gc.fillRectangle(0, 0, controlSize.x, 1);
				// bottom edge
				gc.fillRectangle(0, controlSize.y - 2, controlSize.x, 2);
				// left edge
				gc.fillRectangle(0, 1, 1, controlSize.y-3);
				// right edge
				gc.fillRectangle(controlSize.x - 1, 1, 1, controlSize.y-3);
			} else {
				gc.setBackground(lineColor);
				gc.fillRectangle(0, controlSize.y - 1, controlSize.x, 1);
			}

			/* Now draw lines between the child controls.  This involves calling
			 * into the the block tree.  The reason for this is that if we
			 * draw lines to the left and right of each control then we are drawing
			 * twice as many lines as necessary, if we draw just on the left or just
			 * on the right then we would not get every case, and the end conditions
			 * would not be handled correctly.  Using the tree structure just gives
			 * us better control over the drawing.
			 * 
			 * This method is called on the layout because it uses the cached positions
			 * of the controls.
			 */
			gc.setBackground(secondaryColor);
			((BlockLayout)getLayout()).paintRowLines(gc, this);
		} finally {
			gc.setBackground(oldColor);
		}
	}

	@Override
	protected void setSelected(boolean isSelected) {
		this.isSelected = isSelected;
		Color backgroundColor = (isSelected ? selectedRowColor : normalColor); 
		setBackground(backgroundColor);
	}

	public void arrive(int currentColumn) {
		setSelected(true);
		getChildren()[currentColumn].setFocus();
	}

	/**
	 * This version is called when the selection changed as a result
	 * of the user clicking on a control in another row.  Therefore
	 * we do not set the cell selection.
	 */
    @Override	
	public void arrive() {
		setSelected(true);
	}

    @Override	
	public boolean canDepart() {
		setSelected(false);
		return true;
	}

	/**
	 * Gets the column that has the focus.
	 * 
	 * This method is used to preserve the column selection when cursoring up
	 * and down between rows.  If the column cannot be determined that simply
	 * return 0 so that the first column gets the focus in the new row.
	 * 
	 * @return the 0-based index of the cell in this row that has the focus
	 */
	public int getCurrentColumn() {
		// TODO: We can probably save this information in the focus
		// listener, which will work much better.  For example, if the
		// focus is not in the table but a cell is still in selected mode
		// then the correct value will be returned.
		Control [] children = getChildren();
		for (int columnIndex = 0; columnIndex < children.length; columnIndex++) {
			if (hasFocus(children[columnIndex])) {
				return columnIndex;
			}
		}

		return 0;
	}

	private boolean hasFocus(Control control) {
		if (control.isFocusControl()) return true;

		// We probably have to call recursively on child controls -
		// but let's try first without.
		return false;
	}

	@Override
	protected void scrollToShowRow() {
		/*
		 * There is no scrolling of these rows so the row is always visible and
		 * there is nothing to do.
		 */
	}

	@Override
	protected SplitEntryRowControl getThis() {
		return this;
	}
}
