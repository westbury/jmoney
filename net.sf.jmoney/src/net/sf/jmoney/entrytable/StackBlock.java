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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import net.sf.jmoney.isolation.IDataManager;

/**
 * This class represents a block where alternative layouts are shown,
 * depending on the type of the transaction.
 * 
 * This class contains an abstract method, getTopBlock.  This method is
 * passed the row input.  An implementation must be provided that determines
 * which of the child blocks is to be shown.
 * 
 * Note that because a different block may be shown in each row, it is not possible
 * for the header to show all the column headers for all rows.  The header will
 * show the correct column headers only for the selected row, or will be blank if
 * no row is selected.
 * 
 * getTopBlock may return null for a particular row input.  In that case the block
 * will be blank for that row.
 *
 * @param <T>
 * @param <R>
 */
public abstract class StackBlock<R> extends CellBlock<R> {
	private List<Block<? super R>> children;

	/**
	 * Maps each child block to the child control of the stack composite
	 * that shows that block in the header.
	 */
	private Map<Block, Composite> childControls = new HashMap<Block, Composite>();

	/**
	 * Composite with StackLayout used in the header
	 */
	private Composite stackComposite;

	/**
	 * Layout used by the header control
	 */
	private StackLayout stackLayout; 
	
	public StackBlock(Block<? super R> child1, Block<? super R> child2) {
		super(0, 0);
		ArrayList<Block<? super R>> children = new ArrayList<Block<? super R>>();
		if (child1 != null) children.add(child1);
		if (child2 != null) children.add(child2);
		init(children);
	}
	
	public StackBlock(Block<? super R> child1, Block<? super R> child2, Block<? super R> child3) {
		super(0, 0);
		ArrayList<Block<? super R>> children = new ArrayList<Block<? super R>>();
		if (child1 != null) children.add(child1);
		if (child2 != null) children.add(child2);
		if (child3 != null) children.add(child3);
		init(children);
	}
	
	/**
	 * This method allows null values.  They will be ignored.  This is a convenience
	 * as it makes construction simpler when there is a case that may or may not require
	 * controls.  For example, if an account has a withholding tax then the dividend transaction
	 * type will need a block in the stack for it, but if the account does not have a withholding
	 * tax account then the block will not be needed.
	 * 
	 * @param child1
	 * @param child2
	 * @param child3
	 * @param child4
	 */
	public StackBlock(Block<? super R> child1, Block<? super R> child2, Block<? super R> child3, Block<? super R> child4) {
		super(0, 0);
		ArrayList<Block<? super R>> children = new ArrayList<Block<? super R>>();
		if (child1 != null) children.add(child1);
		if (child2 != null) children.add(child2);
		if (child3 != null) children.add(child3);
		if (child4 != null) children.add(child4);
		init(children);
	}
	
	public StackBlock(List<Block<? super R>> children) {
		super(0, 0);
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
		for (Block<?> child: children) {
			minimumWidth = Math.max(minimumWidth, child.minimumWidth);
			weight = Math.max(weight, child.weight);
			
			/*
			 * Ensure indexes are set.  The indexes for controls in each
			 * stack control start over at zero.
			 */
			child.initIndexes(0);
		}
	}

	// TODO: remove entryData parameter from this method.
	@Override
	public void createHeaderControls(Composite parent) {
		stackComposite = new Composite(parent, SWT.NULL);
		stackLayout = new StackLayout();
		stackComposite.setLayout(stackLayout);
		
		/*
		 * We create the header controls for all possible child controls.
		 * The reason being that the height of the header is the height
		 * of the tallest block in the stack.  (We don't want to header
		 * height changing as selections are made in the table).
		 */
		for (Block<?> child: children) {
			createHeaderForChild(child);
		}
	}

	private <R2> void createHeaderForChild(Block<R2> child) {
		Composite childControl = new Composite(stackComposite, SWT.NULL);
		childControl.setLayout(new BlockLayout<R2>(child, false, null));
		child.createHeaderControls(childControl);
		childControls.put(child, childControl);
	}

	/**
	 * Sets the top block in the header.
	 */
	// TODO: we need entryData so that we can determine the top block for any
	// stack controls nested inside this stack control.  That implies that we
	// don't need topBlock for this stack control.  Remove topBlock parameter.
	void setTopHeaderBlock(Block<? super R> topBlock, R input) {
		Composite topControl = childControls.get(topBlock);
		// now done in init....
//		if (topControl == null) {
//			topControl = new Composite(stackComposite, SWT.NULL);
//			topControl.setLayout(new BlockLayout<T>(topBlock, false));
//			topBlock.createHeaderControls(topControl, entryData);
//			childControls.put(topBlock, topControl);
//		}
		stackLayout.topControl = topControl;
		
		// Pass down because nested blocks may also be affected by the input.
		if (topBlock != null) {
			topBlock.setInput(input);
		}

		stackComposite.layout(true);  //????
		
	}

	// TODO: Do we really need this?
	protected abstract IDataManager getDataManager(R data);


	/**
	 * This method is called for both the header controls and the row controls.
	 * 
	 * The behavior is slightly different in each case.  For the header, the height
	 * is the maximum of all possible alternatives, because we don't want the header
	 * height jumping around.  For the rows it is the height of the current alternative,
	 * because some alternatives may be much bigger than others and we don't want lots
	 * of wasted space.
	 * 
	 * We have our own composites with StackLayout, and a different implementation is
	 * used in the header and the rows, so we can simply pass on the request.
	 */
	@Override
	protected int getHeightForGivenWidth(int width, int verticalSpacing, Control[] controls, boolean changed) {
		return super.getHeightForGivenWidth(width, verticalSpacing, controls, changed);
	}
	@Override
	protected int getHeight(int verticalSpacing, Control[] controls) {
		return super.getHeight(verticalSpacing, controls);
	}

	@Override
	protected void paintRowLines(GC gc, int left, int top, int verticalSpacing, Control[] controls, R input) {
//		if (input != null) {
			Block<? super R> topBlock = getTopBlock(input);
			if (topBlock != null) {
				topBlock.paintRowLines(gc, left, top, verticalSpacing, controls, input);
			}
//		}
	}
	
	protected abstract Block<? super R> getTopBlock(R data);

	@Override
	protected void setInput(R input) {
		Block<? super R> topBlock = getTopBlock(input);
		setTopHeaderBlock(topBlock, input);
	}
}
