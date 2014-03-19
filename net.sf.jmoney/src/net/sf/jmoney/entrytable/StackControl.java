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

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

/**
 * This is used in rows only, not for headers.
 * (anything to be gained by using it in headers too????)
 */
public class StackControl<R> extends Composite {

	private RowControl rowControl;
	
	private R input;
	
	private StackBlock<R> stackBlock;
	
	/**
	 * Maps each child block to the child control of the stack composite
	 * that shows that block in the header.
	 */
	private Map<Block, CellContainer<R>> childControls = new HashMap<Block, CellContainer<R>>();

	private CompressedStackLayout stackLayout;
	
//	private IObservableValue<? extends T> master;
	
	public StackControl(Composite parent, RowControl rowControl, R input, StackBlock<R> stackBlock) {
		super(parent, SWT.NONE);
		this.rowControl = rowControl;
		// Need to save master because of lazy creation of child blocks
		this.input = input;
		this.stackBlock = stackBlock;
//		this.master = master; 
	
		
		stackLayout = new CompressedStackLayout();
		setLayout(stackLayout);
	}

	/**
	 * Set the control (associated with the given block) to
	 * the top of the stack, creating it if necessary, and always
	 * loading it.
	 * <P>
	 * This method may change the preferred height of the row.  It is
	 * the caller's responsibility to resize the row to its preferred
	 * height after calling this method.
	 * 
	 * @param topBlock
	 */
	public void setTopBlock(Block<? super R> topBlock) {
		// First set the top control in this row
		CellContainer<R> topControl;
		if (topBlock == null) {
			topControl = null;  // Causes nothing to show in stacked composite
		} else {
			topControl = childControls.get(topBlock); 
			if (topControl == null) {
				topControl = new CellContainer<R>(this, input);
				final BlockLayout<R> childLayout = new BlockLayout<R>(topBlock, false, input);
				topControl.setLayout(childLayout);
				
				topControl.init(rowControl, topBlock); 
				
				final Composite finalTopControl = topControl;
				topControl.addPaintListener(new PaintListener() {
					@Override
					public void paintControl(PaintEvent e) {
						Color oldColor = e.gc.getBackground();
						// TODO: move colors to single location
						Color secondaryColor = Display.getDefault()
						.getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW);

						try {
							e.gc.setBackground(secondaryColor);
							childLayout.paintRowLines(e.gc, finalTopControl);
						} finally {
							e.gc.setBackground(oldColor);
						}
					}
				});

				childControls.put(topBlock, topControl);
			}

//			topControl.setInput(input);
		}
		stackLayout.topControl = topControl;
		
		/*
		 * Re-layout with the new top control. Note that we don't re-layout the
		 * controls inside the child controls because changing the input does
		 * not change the layout of the blocks.
		 */ 
		layout(false);
		
		// Fire event that tells header to change
		if (rowControl instanceof BaseEntryRowControl
				&& ((BaseEntryRowControl)rowControl).isSelected()) {
			stackBlock.setTopHeaderBlock(topBlock, input);
		}
	}

	public void setFocusListener(FocusListener controlFocusListener) {
		// TODO Auto-generated method stub
		
	}
}
