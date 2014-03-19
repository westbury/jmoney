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

public class HorizontalBlock<R> extends Block<R> {
	private List<Block<? super R>> children;

	public HorizontalBlock(Block<? super R> child1, Block<? super R> child2) {
		List<Block<? super R>> children = new ArrayList<Block<? super R>>();
		children.add(child1);
		children.add(child2);
		init(children);
	}
	
	public HorizontalBlock(Block<? super R> child1, Block<? super R> child2, Block<? super R> child3) {
		List<Block<? super R>> children = new ArrayList<Block<? super R>>();
		children.add(child1);
		children.add(child2);
		children.add(child3);
		init(children);
	}
	
	public HorizontalBlock(Block<? super R> child1, Block<? super R> child2, Block<? super R> child3, Block<? super R> child4) {
		List<Block<? super R>> children = new ArrayList<Block<? super R>>();
		children.add(child1);
		children.add(child2);
		children.add(child3);
		children.add(child4);
		init(children);
	}
	
	public HorizontalBlock(Block<? super R> child1, Block<? super R> child2, Block<? super R> child3, Block<? super R> child4, Block<? super R> child5) {
		List<Block<? super R>> children = new ArrayList<Block<? super R>>();
		children.add(child1);
		children.add(child2);
		children.add(child3);
		children.add(child4);
		children.add(child5);
		init(children);
	}
	
	public HorizontalBlock(Block<? super R> child1, Block<? super R> child2, Block<? super R> child3, Block<? super R> child4, Block<? super R> child5, Block<? super R> child6) {
		List<Block<? super R>> children = new ArrayList<Block<? super R>>();
		children.add(child1);
		children.add(child2);
		children.add(child3);
		children.add(child4);
		children.add(child5);
		children.add(child6);
		init(children);
	}

	/**
	 * This form of the constructor is not type safe because
	 * arrays are not compatible with generics and unfortunately
	 * Java uses arrays for variable parameters.
	 */
	public HorizontalBlock(Block<? super R>... childrenArray) {
		ArrayList<Block<? super R>> children = new ArrayList<Block<? super R>>(); 
		for (Block<? super R> child: childrenArray) {
			children.add(child);
		}
		init (children);
	}
	
	public HorizontalBlock(List<Block<? super R>> children) {
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
			minimumWidth += child.minimumWidth;
			weight += child.weight;
		}
		
		// Add spacing for each gap between child blocks (the number of gaps
		// will be one less than the number of child blocks).
		minimumWidth += (children.size() - 1) * Block.horizontalSpacing;
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
	protected void layout(int width) {
		if (this.width != width) {
			this.width = width;

			int[] widths = distributeWidth(width);

			for (int i = 0; i < children.size(); i++) {
				children.get(i).layout(widths[i]);
			}
		}
	}

	private int[] distributeWidth(int width) {
		int [] widths = new int[children.size()];
		for (int i = 0; i < children.size(); i++) {
			widths[i] = children.get(i).minimumWidth;
		}

		// Do we have extra width and columns with expansion weight?
		if (weight > 0 && width > minimumWidth) {
			// Now distribute the rest to the columns with weight.
			int rest = width - minimumWidth;
			int totalDistributed = 0;
			for (int i = 0; i < children.size(); i++) {
				int pixels = children.get(i).weight * rest / weight;
				totalDistributed += pixels;
				widths[i] += pixels;
			}

			/*
			 * We may still have a few pixels left to allocate
			 * because the above divisions round the pixel count
			 * downwards. Distribute any remaining pixels to columns
			 * with weight. The number of pixels left can never be
			 * more than the number of columns with non-zero
			 * weights.
			 */
			int diff = rest - totalDistributed;
			for (int i = 0; i < children.size() && diff > 0; i++) {
				if (children.get(i).weight > 0) {
					++widths[i];
					--diff;
				}
			}
		}
		return widths;
	}

	@Override
	protected void positionControls(int left, int top, int verticalSpacing, Control[] controls, R rowInput, boolean flushCache) {
		int x = left;
		for (Block<? super R> child: children) {
			child.positionControls(x, top, verticalSpacing, controls, rowInput, flushCache);
			x += child.width + Block.horizontalSpacing;
		}
	}

	@Override
	protected int getHeight(int verticalSpacing, Control[] controls) {
		int height = 0; 
		for (Block<? super R> child: children) {
			height = Math.max(height, child.getHeight(verticalSpacing, controls));
		}
		return height;
	}

	@Override
	protected void paintRowLines(GC gc, int left, int top, int verticalSpacing, Control[] controls, R rowInput) {
		/* Paint the vertical lines between the controls.
		 * 
		 * We need to make nested calls in case there are nested blocks that
		 * need separator lines within them.
		 */
		int x = left;
		for (int i = 0; i < children.size(); i++) {
			children.get(i).paintRowLines(gc, x, top, verticalSpacing, controls, rowInput);
			
			// Draw a vertical separator line only if this is not the last control.
			if (i != children.size() - 1) {
				x += children.get(i).width;
				gc.fillRectangle(x, top, Block.horizontalSpacing, getHeight(verticalSpacing, controls));
				x += Block.horizontalSpacing;
			}
		}
	}

	@Override
	protected int getHeightForGivenWidth(int width, int verticalSpacing, Control[] controls, boolean changed) {
		int[] widths = distributeWidth(width);
		int height = 0; 
		for (int i = 0; i < children.size(); i++) {
			height = Math.max(height, children.get(i).getHeightForGivenWidth(widths[i], verticalSpacing, controls, changed));
		}
		return height;
	}

	@Override
	protected void setInput(R input) {
		for (Block<? super R> child: children) {
			child.setInput(input);
		}
	}
}
