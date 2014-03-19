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
import java.util.List;

import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

public class VerticalBlock<R> extends Block<R> {
	private List<Block<? super R>> children;

	public VerticalBlock(Block<? super R> child1, Block<? super R> child2) {
		List<Block<? super R>> children = new ArrayList<Block<? super R>>();
		children.add(child1);
		children.add(child2);
		init(children);
	}
	
	public VerticalBlock(Block<? super R> child1, Block<? super R> child2, Block<? super R> child3) {
		List<Block<? super R>> children = new ArrayList<Block<? super R>>();
		children.add(child1);
		children.add(child2);
		children.add(child3);
		init(children);
	}
	
	public VerticalBlock(List<Block<? super R>> children) {
		init(children);
	}

	private void init(List<Block<? super R>> children) {
		this.children = children;

		/*
		 * The minimumWidth, weight, and height are set in the
		 * constructor. These values can all be calculated from the list
		 * of child blocks.
		 */
		minimumWidth = 0;
		weight = 0;
		for (Block<? super R> child: children) {
			minimumWidth = Math.max(minimumWidth, child.minimumWidth);
			weight = Math.max(weight, child.weight);
		}
	}

	@Override
	public int initIndexes(int startIndex) {
		int totalCount = 0;
		for (Block<? super R> child: children) {
			int count = child.initIndexes(startIndex + totalCount);
			totalCount += count;
		}
		return totalCount;
	}

	@Override
	public void createCellControls(Composite parent, R input, RowControl rowControl) {
		for (Block<? super R> child: children) {
			child.createCellControls(parent, input, rowControl);
		}
	}

	@Override
	public void createHeaderControls(Composite parent) {
		for (Block<? super R> child: children) {
			child.createHeaderControls(parent);
		}
	}

	@Override
	protected int getHeightForGivenWidth(int width, int verticalSpacing, Control[] controls, boolean changed) {
		int height = 0; 
		for (Block<? super R> child: children) {
			height += child.getHeightForGivenWidth(width, verticalSpacing, controls, changed);
		}
		height += (children.size() - 1) * verticalSpacing;
		return height;
	}

	@Override
	protected void layout(int width) {
		if (this.width != width) {
			this.width = width;
			for (Block<? super R> child: children) {
				child.layout(width);
			}
		}
	}

	@Override
	protected void positionControls(int left, int top, int verticalSpacing, Control[] controls, R input, boolean flushCache) {
		int y = top;
		for (Block<? super R> child: children) {
			child.positionControls(left, y, verticalSpacing, controls, input, flushCache);
			y += child.getHeight(verticalSpacing, controls) + verticalSpacing;
		}
	}

	@Override
	protected int getHeight(int verticalSpacing, Control[] controls) {
		int height = 0; 
		for (Block<? super R> child: children) {
			height += child.getHeight(verticalSpacing, controls);
		}
		height += (children.size() - 1) * verticalSpacing;
		return height;
	}

	@Override
	protected void paintRowLines(GC gc, int left, int top, int verticalSpacing, Control[] controls, R input) {
		/* Paint the horizontal lines between the controls.
		 * 
		 * We need to make nested calls in case there are nested blocks that
		 * need separator lines within them.
		 */
		int y = top;
		for (int i = 0; i < children.size(); i++) {
			children.get(i).paintRowLines(gc, left, y, verticalSpacing, controls, input);
			
			// Draw a horizontal separator line only if this is not the last control.
			if (i != children.size() - 1) {
				y += children.get(i).getHeight(verticalSpacing, controls);
				gc.fillRectangle(left, y, width, verticalSpacing);
				y += verticalSpacing;
			}
		}
	}

	@Override
	protected void setInput(R input) {
		for (Block<? super R> child: children) {
			child.setInput(input);
		}
	}
}
