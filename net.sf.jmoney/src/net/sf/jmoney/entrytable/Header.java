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

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

public class Header<B> extends Composite {

	private Block<? super B> rootBlock;
	
	public Header(Composite parent, int style, Block<? super B> rootBlock) {
		super(parent, style);
		this.rootBlock = rootBlock;
		
		BlockLayout layout = new BlockLayout<B>(rootBlock, false, null);
		setLayout(layout);

		setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GRAY));
		
		// TODO: This is not needed anymore.  It was called to set indexes but
		// those are not done by this method.
//		rootBlock.buildCellList();
		
		/*
		 * Create all the header controls.  We don't know the current row input but we don't
		 * need to.  The header is always at the maximum height with all header controls initially
		 * in a created state.
		 */
		rootBlock.createHeaderControls(this);
	}

	/**
	 * The header rows may depend on the row selection.
	 * This method adjusts the headers to match a row with the given input.
	 * 
	 * @param input
	 */
	public void setInput(B input) {
		rootBlock.setInput(input);
	}
	
	protected boolean sortOnColumn(IndividualBlock<EntryRowControl> sortProperty, int sortDirection) {
		// TODO: Get this working.  This method is not even called
		// currently.
		
//		entriesTable.sort(sortProperty, sortDirection == SWT.UP);
//
//		// TODO: Is there a better way of getting the table?
//		entriesTable.table.refreshContent();
        
        return true;
	}

}
