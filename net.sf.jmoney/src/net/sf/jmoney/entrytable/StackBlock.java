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

import net.sf.jmoney.isolation.DataManager;
import net.sf.jmoney.isolation.SessionChangeListener;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

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
public abstract class StackBlock<T, R> extends CellBlock<T,R> {
	private List<Block<? super T,? super R>> children;

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
	
	public StackBlock(Block<? super T,? super R> child1, Block<? super T,? super R> child2) {
		super(0, 0);
		ArrayList<Block<? super T,? super R>> children = new ArrayList<Block<? super T,? super R>>();
		children.add(child1);
		children.add(child2);
		init(children);
	}
	
	public StackBlock(Block<? super T,? super R> child1, Block<? super T,? super R> child2, Block<? super T,? super R> child3) {
		super(0, 0);
		ArrayList<Block<? super T,? super R>> children = new ArrayList<Block<? super T,? super R>>();
		children.add(child1);
		children.add(child2);
		children.add(child3);
		init(children);
	}
	
	public StackBlock(Block<? super T,? super R> child1, Block<? super T,? super R> child2, Block<? super T,? super R> child3, Block<? super T,? super R> child4) {
		super(0, 0);
		ArrayList<Block<? super T,? super R>> children = new ArrayList<Block<? super T,? super R>>();
		children.add(child1);
		children.add(child2);
		children.add(child3);
		children.add(child4);
		init(children);
	}
	
	public StackBlock(List<Block<? super T,? super R>> children) {
		super(0, 0);
		init(children);
	}
	
	private void init(List<Block<? super T,? super R>> children) {
		this.children = children;

		/*
		 * The minimumWidth, weight, and height are set in the
		 * constructor. These values can all be calculated from the list
		 * of child blocks.
		 */
		minimumWidth = 0;
		weight = 0;
		for (Block<? super T,? super R> child: children) {
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
	public void createHeaderControls(Composite parent, T entryData) {
		stackComposite = new Composite(parent, SWT.NULL);
		stackLayout = new StackLayout();
		stackComposite.setLayout(stackLayout);
		
		/*
		 * We create the header controls for all possible child controls.
		 * The reason being that the height of the header is the height
		 * of the tallest block in the stack.  (We don't want to header
		 * height changing as selections are made in the table).
		 */
		for (Block<? super T,? super R> child: children) {
			Composite childControl = new Composite(stackComposite, SWT.NULL);
			childControl.setLayout(new BlockLayout<T>(child, false));
			child.createHeaderControls(childControl, entryData);
			childControls.put(child, childControl);
		}
	}

	/**
	 * Sets the top block in the header.
	 */
	// TODO: we need entryData so that we can determine the top block for any
	// stack controls nested inside this stack control.  That implies that we
	// don't need topBlock for this stack control.  Remove topBlock parameter.
	void setTopHeaderBlock(Block<? super T,? super R> topBlock, T entryData) {
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
			topBlock.setInput(entryData);
		}

		stackComposite.layout(true);  //????
		
	}

	/**
	 * Given a row input element, this method returns a listener that, when
	 * session change events are fired on it, will determine if those changes
	 * cause a change in the top block for a row with the given input.
	 * If a change does occur, the top control in the given stack control
	 * is updated.
	 * 
	 * @param entryData
	 * @param stackControl
	 * @return
	 */
	protected abstract SessionChangeListener createListener(T entryData, StackControl<T,R> stackControl);
	
	// TODO: Do we really need this?
	protected abstract DataManager getDataManager(T data);


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
	int getHeightForGivenWidth(int width, int verticalSpacing, Control[] controls, boolean changed) {
		return super.getHeightForGivenWidth(width, verticalSpacing, controls, changed);
	}
	@Override
	int getHeight(int verticalSpacing, Control[] controls) {
		return super.getHeight(verticalSpacing, controls);
	}

	@Override
	void paintRowLines(GC gc, int left, int top, int verticalSpacing, Control[] controls, T entryData) {
		if (entryData != null) {
			getTopBlock(entryData).paintRowLines(gc, left, top, verticalSpacing, controls, entryData);
		}
	}
	
	protected abstract Block<? super T, ? super R> getTopBlock(T data);

	@Override
	void setInput(T input) {
		Block<? super T, ? super R> topBlock = getTopBlock(input);
		setTopHeaderBlock(topBlock, input);
	}
}
