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
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

/**
 * This composite contains the properties from the other entry
 * when there is only one other entry (i.e. the transaction is
 * not split).
 * 
 * This composite is placed inside the composite represented by
 * OtherEntriesControl.
 */
public class OtherEntryControl extends CellContainer<Entry, ISplitEntryContainer> {

	private PaintListener paintListener = new PaintListener() {
		@Override
		public void paintControl(PaintEvent e) {
			drawBorder(e.gc);
		}
	};

	public OtherEntryControl(final Composite parent,RowControl rowControl, int style, Block<Entry, ISplitEntryContainer> rootBlock, boolean isLinked, final RowSelectionTracker<BaseEntryRowControl> selectionTracker, final FocusCellTracker focusCellTracker) {
		super(parent, style);

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

		 ISplitEntryContainer coordinator = new ISplitEntryContainer() {};
		 
		init(rowControl, coordinator, rootBlock);

		addPaintListener(paintListener);
	}

	/**
	 * Draws the lines between the controls.
	 * 
	 * @param gc
	 */
	protected void drawBorder(GC gc) {
		Color oldColor = gc.getBackground();
		try {

			// Get the colors we need
			Display display = Display.getCurrent();
			// Looks white
			Color secondaryColor = display
					.getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW);

			/*
			 * Now draw lines between the child controls. This involves calling
			 * into the the block tree. The reason for this is that if we draw
			 * lines to the left and right of each control then we are drawing
			 * twice as many lines as necessary, if we draw just on the left or
			 * just on the right then we would not get every case, and the end
			 * conditions would not be handled correctly. Using the tree
			 * structure just gives us better control over the drawing.
			 * 
			 * This method is called on the layout because it uses the cached
			 * positions of the controls.
			 */
			gc.setBackground(secondaryColor);
			((BlockLayout) getLayout()).paintRowLines(gc, this);
		} finally {
			gc.setBackground(oldColor);
		}
	}
}
