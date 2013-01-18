/*
*
*  JMoney - A Personal Finance Manager
*  Copyright (c) 2004 Nigel Westbury <westbury@users.sourceforge.net>
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

package net.sf.jmoney;

import org.eclipse.core.runtime.Assert;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Layout;

/**
 * A layout in which one of the child controls is visible
 * and fills the entire parent control and all the other
 * child controls are not visible.
 * <P>
 * This layout has no corressponding data associated with
 * each child control.  The child control to be shown is
 * selected by calling the <code>show</code> method on this
 * layout.  This method may be called multiple times to
 * change the visible child control.
 * 
 * @author Nigel Westbury
 */
public class AlternativeContentLayout extends Layout {
	Control currentlyShowing = null;
	
	@Override
	public void layout(Composite editor, boolean force) {
		Rectangle bounds = editor.getClientArea();
		
		Control [] children = editor.getChildren();
		for (int i = 0; i < children.length; i++) {
			if (children[i].equals(currentlyShowing)) {
				children[i].setBounds(0, 0, bounds.width, bounds.height);
			} else {
				children[i].setBounds(0, 0, 0, 0);
			}
		}
		
		// TODO: check if this following code is correct
		if (force && currentlyShowing instanceof Composite) {
			((Composite)currentlyShowing).layout(force);	
		}
	}

    @Override	
	public Point computeSize(Composite editor, int wHint, int hHint, boolean force) {
		Control [] children = editor.getChildren();
		int maxWidth = 0;
		int maxHeight = 0;
		for (int i = 0; i < children.length; i++) {
			Point contentsSize = children[i].computeSize(wHint, hHint, force);
			if (contentsSize.x > maxWidth) {
				maxWidth = contentsSize.x; 
			}
			if (contentsSize.y > maxHeight) {
				maxHeight = contentsSize.y; 
			}
		}
		
		return new Point(maxWidth, maxHeight);			
	}
	
	public void show(Control childControl) {
		Assert.isTrue(childControl.getParent().getLayout() == this);
		
		if (currentlyShowing != null) {
			currentlyShowing.setSize(0, 0);
		}
		currentlyShowing = childControl;
		if (currentlyShowing != null) {
			currentlyShowing.setSize(childControl.getParent().getSize());
		}
	}
}
