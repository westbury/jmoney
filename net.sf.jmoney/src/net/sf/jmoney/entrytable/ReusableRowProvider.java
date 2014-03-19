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

import java.util.LinkedList;

import org.eclipse.swt.widgets.Composite;

public class ReusableRowProvider implements IRowProvider<EntryData, EntryRowControl> {

	private Block<? super EntryRowControl> rootBlock;

	private VirtualRowTable<EntryData, EntryRowControl> rowTable;
	
	private RowSelectionTracker rowSelectionTracker;
	
	private FocusCellTracker focusCellTracker;
	
	/**
	 * a list of row objects of rows that had been in use but are no longer
	 * visible. These a free for re-use, thus avoiding the need to create new
	 * controls.
	 */
	private LinkedList<EntryRowControl> spareRows = new LinkedList<EntryRowControl>();

	public ReusableRowProvider(Block<? super EntryRowControl> rootBlock) {
		this.rootBlock = rootBlock;
	}
	
	@Override
	public void init(VirtualRowTable<EntryData, EntryRowControl> rowTable, RowSelectionTracker rowSelectionTracker, FocusCellTracker focusCellTracker) {
		this.rowTable = rowTable;
		this.rowSelectionTracker = rowSelectionTracker;
		this.focusCellTracker = focusCellTracker;
	}
	
	@Override
	public EntryRowControl getNewRow(Composite parent, EntryData entryData) {
		EntryRowControl rowControl;
		
		if (spareRows.size() > 0) {
			rowControl = spareRows.removeFirst();
			rowControl.setVisible(true);
		} else {
			rowControl = new EntryRowControl(parent, rowTable, rootBlock, rowSelectionTracker, focusCellTracker);
		}
		
		rowControl.setRowInput(entryData);
		
		return rowControl;
	}
	
	@Override
	public void releaseRow(EntryRowControl rowControl) {
		rowControl.setVisible(false);
		spareRows.add(rowControl);
	}
}
