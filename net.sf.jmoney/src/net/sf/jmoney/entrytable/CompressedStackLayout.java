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

import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Layout;

/**
 * This Layout is similar to StackLayout in that it stacks all the controls one on top of the other
 * and the control specified in topControl is visible and all other controls are not visible.
 * Users must set the topControl value to flip between the visible items and then call 
 * layout() on the composite which has the StackLayout.
 * <P>
 * Where this layout differs is that the preferred size of the control is the
 * preferred size of the top control, ignoring all other controls.  Therefore the
 * preferred size may change as the top control is changed.
 */
public class CompressedStackLayout extends Layout {
	/**
	 * The Control that is displayed at the top of the stack.
	 * All other controls that are children of the parent composite will not be visible.
	 */
	public Control topControl;

	@Override
	protected Point computeSize(Composite composite, int wHint, int hHint, boolean flushCache) {
		if (topControl == null) {
			/*
			 * It would be better if we could return zero. However, the code in
			 * Composite.ComputeSize will, for some reason, set the size to the
			 * default size of 64 if it sees the computed size is zero.
			 */
			return new Point(1, 1);
		} else {
			return topControl.computeSize(wHint, hHint, flushCache);
		}
	}

	@Override
	protected boolean flushCache(Control control) {
		return true;
	}

	@Override
	protected void layout(Composite composite, boolean flushCache) {
		Control children[] = composite.getChildren();
		for (int i = 0; i < children.length; i++) {
			children[i].setVisible(children[i] == topControl);
		}
		
		if (topControl != null) {
			Rectangle rect = composite.getClientArea();
			topControl.setBounds(rect);
		}
	}
}
