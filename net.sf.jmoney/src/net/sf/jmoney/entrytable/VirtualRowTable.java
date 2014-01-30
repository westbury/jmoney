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
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Slider;

public class VirtualRowTable<T extends EntryData> extends Composite implements ICompositeTable<T> {

	/** the number of rows in the underlying model */
	int rowCount;

	/**
	 * the 0-based index of the object in the underlying model that is currently
	 * the top row in the visible area
	 */ 
	int topVisibleRow = 0;

	/**
	 * the set of row objects for all the rows that are either currently visible or
	 * is the selected row (control for the selected row is maintained even when scrolled
	 * out of view because it may contain unsaved data or may have data that cannot be saved
	 * because it is not valid)
	 * <P>
	 * The 'committed' entry data is mapped (the 'uncommitted' entry data being internal to
	 * the row control)
	 */
	Map<EntryData, BaseEntryRowControl> rows = new HashMap<EntryData, BaseEntryRowControl>();

	/**
	 * the list of row objects for all the rows that used to be visible and may be re-used,
	 * or may be released if no longer visible.  The <code>scrollToGivenFix</code> method will
	 * move the row controls to this field, then fetch the row controls it needs for the new
	 * scroll position (which moves the row control back from this field to the rows field),
	 * then releases any rows left in this field.
	 */
	Map<EntryData, BaseEntryRowControl> previousRows = new HashMap<EntryData, BaseEntryRowControl>();
	
	/**
	 * the currently selected row, as a 0-based index into the underlying rows,
	 * or -1 if no row is selected. To get the selected row as an index into the
	 * <code>rows</code> list, you must subtract currentVisibleTopRow from
	 * this value.
	 */
	int currentRow = -1;

	/**
	 * The user is allowed to scroll the selected row off the client area.  There may
	 * be uncommitted changes in this row.  These changes are not committed until the user
	 * attempts to select another row.  Therefore we must keep the row control even though
	 * it is not visible.
	 * <P> 
	 *  This field always represents the same row as <code>currentRow</code>. 
	 */
	RowSelectionTracker rowTracker;
	
	IContentProvider<T> contentProvider;

	IRowProvider<T> rowProvider;

	private Header<T> header;
	
	private Composite contentPane;
	
	/**
	 * Size of the contentPane, cached for performance reasons only
	 */
	private Point clientAreaSize;

	/**
	 * Must be non-null.
	 */
	private Slider vSlider;

	/**
	 * The position of the slider.  This content pane and the slider both
	 * update each other.  For example, if page down is pressed in this
	 * content pane then the slider must be updated, but if the slider is
	 * dragged then this content pane must be updated.
	 * <P>
	 * This class listens to the slider for changes.  However, if this content pane
	 * updates the slider then we don't want the listener to process the change.
	 * We can avoid this from happened if this class always sets the new position
	 * in <code>sliderPosition</code> before changing the slider and if this class's
	 * listener checks the value against <code>sliderPosition</code> before processing. 
	 */
	private int sliderPosition = 0;

	protected FocusCellTracker focusCellTracker = new FocusCellTracker();
	
	/**
	 * This composite creates a two by two grid.  The header in the
	 * top left, the table of rows in the bottom left, the vertical
	 * scroll bar in the bottom right, and a blank cell in the top
	 * right.  If no vertical scroll bar is required then its size
	 * is set to zero so the header and rows take up the full
	 * width.  This layout ensures that the header is always the same
	 * width as the rows, which is good if the columns are going to
	 * line up with the header. 
	 * 
	 * @param parent
	 * @param rootBlock
	 * @param contentProvider
	 * @param rowTracker 
	 */
	// TODO: tidy up EntriesTable parameter.  Perhaps we need to remove EntriesTable altogether?
	public VirtualRowTable(Composite parent, Block<? super T, ?> rootBlock, EntriesTable entriesTable, IContentProvider<T> contentProvider, IRowProvider<T> rowProvider, RowSelectionTracker rowTracker) {
		super(parent, SWT.NONE);
		this.contentProvider = contentProvider;
		this.rowProvider = rowProvider;
		this.rowTracker = rowTracker;
		
		GridLayout layout = new GridLayout(2, false);
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		layout.horizontalSpacing = 0;
		layout.verticalSpacing = 0;
		setLayout(layout);
	
		header = new Header<T>(this, SWT.NONE, rootBlock);
		Composite blankPane = new Composite(this, SWT.NONE);
		contentPane = createContentPane(this);
		vSlider = new Slider(this, SWT.VERTICAL);
		
		header.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		blankPane.setLayoutData(new GridData(0, 0));
		contentPane.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		vSlider.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true));

		vSlider.addSelectionListener(sliderSelectionListener);

		rowCount = contentProvider.getRowCount();
		sliderPosition = 0;
	}

	/**
	 * Refreshes the content of the rows.  The set of rows is
	 * assumed unchanged.
	 */
	public void refreshContentOfAllRows() {
		// TODO Auto-generated method stub
		
	}

	/**
	 * Similar to <code>refreshContentOfAllRows</code>
	 * but updates the balances only.
	 */
	public void refreshBalancesOfAllRows() {
		for (EntryData entryData: rows.keySet()) {
			rows.get(entryData).refreshBalance();
		}
	}

	// TODO: Verify first parameter needed.
	// Can we clean this up?
	@Override
	public void setCurrentRow(T committedEntryData, T uncommittedEntryData) {
		currentRow = contentProvider.indexOf(committedEntryData);
		
		header.setInput(uncommittedEntryData);
	}	

	/**
	 * Deletes the given row.
	 * 
	 * The row must be deleted from the underlying content.  This
	 * method is not responsible for doing that.  This method does
	 * update the display, decrement the row count.
`	 * 
	 * If the row being deleted is the selected row then any uncommitted
	 * changes are discarded without warning (it is assumed that the
	 * caller gave sufficient warning to the user).
	 * 
	 * @param data
	 */
	public void deleteRow(int index) {
		if (index == currentRow) {
			currentRow = -1;
		}
		
		// Three cases
//		if (index < topVisibleRow) {
//			topVisibleRow--;
//		} else if (index >= topVisibleRow + rows.size()) {
//			// nothing to do in this case
//		} else {
//			EntryData entryData
//			BaseEntryRowControl removedRow = rows.remove(index - topVisibleRow);
//			rowProvider.releaseRow(removedRow);
//		}
		
		rowCount--;
		adjustVerticalScrollBar();

		refreshBalancesOfAllRows();
	}

	/**
	 * Inserts the given row.
	 * <P> 
	 * The row must have been inserted into the underlying content.  This
	 * method is not responsible for doing that.  This method does
	 * update the display, increment the row count and adjusting the scroll
	 * bar.
	 * <P>
	 * This method does not affect the current selection.  It is possible that 
	 * a row is inserted by another view/editor while a row is being edited.
	 * In such a case, the editing of the row is not affected.
	 * 
	 * @param index the index into the content of this new row
	 */
	public void insertRow(int index) {
		rowCount++;
		adjustVerticalScrollBar();
	}

	/**
	 * Sets the focus to the given column and row.
	 *  
	 * @param x
	 * @param y
	 */
	public void setSelection(int x, int y) {
		// TODO Auto-generated method stub
		
	}

	/**
	 * 
	 * @return the selected row, or -1 if no row is selected
	 */
	public int getSelection() {
		return currentRow;
	}

	private Composite createContentPane(Composite parent) {
		final Composite composite = new Composite(parent, SWT.NONE);

		// This composite has not yet been sized.  Initialize the cached size to zeroes.
		clientAreaSize = new Point(0, 0);

		composite.addControlListener(new ControlAdapter() {
		    @Override	
			public void controlResized(ControlEvent e) {
				Point newSize = composite.getSize();

				if (newSize.x != clientAreaSize.x) {
					// Width has changed.  Update the sizes of the row controls.
					for (BaseEntryRowControl rowControl: rows.values()) {
						int rowHeight = rowControl.computeSize(newSize.x, SWT.DEFAULT).y;
						rowControl.setSize(newSize.x, rowHeight);
					}
				}

				clientAreaSize = newSize;
				
				/*
				 * Adjust the vertical scroller (make it invisible if all the rows
				 * fit in the client area, or change the thumb bar height)
				 */
				adjustVerticalScrollBar();

				/*
				 * Refresh. This method refreshes the display according to the
				 * current slider position. If the rows don't change height then
				 * this method is not necessary. However, changing the width
				 * could potentially change the preferred height of each row.
				 */
				scrollToSliderPosition();
			}
		});
		
		// EXPERIMENTAL:
		composite.addTraverseListener(new TraverseListener() {
			@Override
			public void keyTraversed(TraverseEvent e) {
				/*
				 * FEATURE IN SWT: When SWT needs to resolve a mnemonic (accelerator)
				 * character, it recursively calls the traverse event down all
				 * controls in the containership hierarchy.  If e.doit is false,
				 * no control has yet matched the mnemonic, and we don't have to
				 * do anything since we don't do mnemonic matching and no mnemonic
				 * has matched.
				 */
				if (e.doit) {
//					parent.keyTraversed(TableRow.this, e);
				}
			}
		});

		return composite;
	}

	/*
	 * Adjust the vertical scroller (make it invisible if all the rows
	 * fit in the client area, or change the thumb bar height).
	 * 
	 * Once that is done, adjust the scrolled area to match the scroll
	 * bar.
	 */
	protected void adjustVerticalScrollBar() {
		/*
		 * Calculate the average height of all the visible (or partially
		 * visible) rows.
		 */
		int totalHeight = 0;
		for (Control rowControl: rows.values()) {
			totalHeight += rowControl.getSize().y;
		}
		double averageHeight = ((double)totalHeight) / rows.size();
		
		double heightAllRows = rowCount * averageHeight;
		
		if (heightAllRows <= clientAreaSize.y) {
			vSlider.setVisible(false);
		} else {
			vSlider.setThumb((int)(vSlider.getMaximum() * clientAreaSize.y / heightAllRows));
			vSlider.setVisible(true);
		}
		
		/*
		 * If the thumb control height is changed, the range of values
		 * for the scroll bar position will also change.  We refresh our
		 * cache of the scroll bar position.  This must be done before calling
		 * scrollToSliderPosition.
		 */
		sliderPosition = vSlider.getSelection();
		
		/*
		 * After adjusting the height of the scroller thumb, we now adjust
		 * the scrolled area so that it matches the scroll bar.
		 */
		scrollToSliderPosition();
	}

	/**
	 * Scroll the table so that the given row in fully visible. The table is
	 * scrolled by the least amount possible, which means:
	 * <ol>
	 * <li>The row is already fully visible - do not scroll</li>
	 * <li>The row is partially or fully off the top - scroll so the row is
	 * aligned with the top of the visible area</li>
	 * <li>The row is partially or fully off the bottom - scroll so the row is
	 * aligned with the bottom of the visible area</li>
	 * </ol>
	 * 
	 * If the given row is bigger than the visible area then it will not be
	 * possible to fully show that row. In such case, the table is scrolled to
	 * show the top of the row if there are no visible rows above the selected row,
	 * otherwise to show the bottom of the row.
	 * 
	 * @param index
	 *            of the row to show, where 0 is the index of the first row
	 *            in the underlying list of rows
	 */
	void scrollToShowRow(int rowIndex) {
		if (rowIndex <= topVisibleRow) {
			setTopRow(rowIndex);
		} else if (rowIndex >= topVisibleRow+rows.size() - 1) {
			setBottomRow(rowIndex);
		}
	}

	/**
	 * Scroll the view to the given fix and update the scroll-bar
	 * to reflect the new position of the visible area.
	 * 
	 * @param anchorRowNumber
	 * @param anchorRowPosition
	 */
	void scrollToGivenFix(int anchorRowNumber, int anchorRowPosition) {
		scrollViewToGivenFix(anchorRowNumber, anchorRowPosition);

		// Having updated the view, we must move the scroll bar to match.

		/*
		 * This code is all about calculating the position of the vertical
		 * scroll bar that matches the visible content of the visible area.
		 * This is a little bit complex, but I think this code is correct.
		 */

		for (int rowIndex = topVisibleRow; rowIndex < topVisibleRow + rows.size(); rowIndex++) {
			Rectangle bounds = getRowControl(rowIndex).getBounds();

			// so p = (r * height(r) - top(r)) / (n * height(r) - cah)

			double p = ((double)(rowIndex * bounds.height - bounds.y)) / (rowCount * bounds.height - clientAreaSize.y);

//			double start = ((double)row) / rowCount;
//			double end = ((double)(row + 1)) / rowCount;
//			System.out.println("p = " + p + ", start = " + start + ", end = " + end + ", height = " + bounds.height);
			if (p >= ((double)rowIndex) / rowCount && p <= ((double)(rowIndex + 1)) / rowCount) {
				double maximum = vSlider.getMaximum() - vSlider.getThumb();
				sliderPosition = (int)(p * maximum);
				vSlider.setSelection(sliderPosition);
				return;
			}
		}
		System.out.println("no match!!!"); //$NON-NLS-1$
	}

	/**
	 * This method scrolls the table. It updates the list of TableRow
	 * objects and sets the positions appropriately.
	 * 
	 * A row and its position is specified. This method will position rows
	 * above and below in order to fill the visible area.
	 * 
	 * Normally the row must be specified as a valid index into the list of
	 * underlying row objects. That is, it must be not less than zero and
	 * less than the number of rows in the underlying list. However, there
	 * is one exception. This method does allow the index to be equal to the
	 * number of rows (representing a row after the last row) if the
	 * position of that row is at or below the bottom of the visible area.
	 * 
	 * This method does not handle adjustment of the focus or anything like
	 * that. That is up to the caller.
	 * 
	 * @param anchorRowIndex
	 *            the index of a row to be displayed, where the index is a
	 *            0-based index into the underlying model.
	 * @param anchorRowPosition
	 *            the position at which the top of the given row is to be
	 *            positioned, relative to the top of the visible client area
	 */
	private void scrollViewToGivenFix(int anchorRowIndex, int anchorRowPosition) {
		/*
		 * Save the previous set of visible rows.  In a small scroll, a lot of the rows
		 * that were previously visible will remain visible, so we keep these controls.
		 */
		previousRows = rows;
		rows = new HashMap<EntryData, BaseEntryRowControl>();
		
		/*
		 * The <code>rows</code> field contains a list of consecutive rows
		 * and <code>topVisibleRow</code> contains the absolute index
		 * of the first row in the list.  These fields must be updated for
		 * the new scroll position.
		 */

		/*
		 * We add the following rows before we add the prior rows. The reason
		 * for this is as follows. If there are not enough prior rows then we
		 * align the first row with the top of the client area and if there are
		 * not enough following rows then we align the last row with the bottom
		 * of the client area. However, if there are not enough rows to fill the
		 * client area then we want want the rows to be aligned with the top of
		 * the client area with a blank area at the bottom. By going upwards
		 * last, we ensure the top alignment overrules the bottom alignment.
		 */
		
		/*
		 * Add following rows until we reach the bottom of the client area.  If we
		 * run out of rows in the underlying model then re-align the bottom row
		 * with the bottom. 
		 */
		int rowPosition = anchorRowPosition;
		int myAnchorRowPosition = anchorRowPosition;
		int rowIndex = anchorRowIndex;
		while (rowPosition < clientAreaSize.y) {
			if (rowIndex == rowCount) {
				// We have run out of rows.  Re-align to bottom.
			    	myAnchorRowPosition += (clientAreaSize.y - rowPosition);
				break;
			}

			Control rowControl = getRowControl(rowIndex);
			int rowHeight = rowControl.getSize().y;
			
			rowIndex++;
			rowPosition += rowHeight;
		}

		/*
		 * Add prior rows until we reach the top of the client area.  If we
		 * run out of rows in the underlying model then we must scroll to
		 * the top.
		 */
		rowPosition = myAnchorRowPosition;
		rowIndex = anchorRowIndex;
		while (rowPosition > 0) {
			if (rowIndex == 0) {
				// We have run out of rows.  Re-align to top.
				rowPosition = 0;
				break;
			}

			rowIndex--;
			Control rowControl = getRowControl(rowIndex);
			int rowHeight = rowControl.getSize().y;

			rowPosition -= rowHeight;
		}

		topVisibleRow = rowIndex; 
		int newTopRowOffset = rowPosition;

		/*
		 * We have to move the controls front-to-back if we're scrolling
		 * forwards and back-to-front if we're scrolling backwards to avoid ugly
		 * screen refresh artifacts.
		 * 
		 * However, the problem is that 'scrolling forwards' and 'scrolling
		 * backwards' are not well-defined. For example, what happens if a row
		 * in the middle of the visible area is being deleted. The rows below
		 * scroll up a bit to fill the gap. But suppose the rows below are small
		 * and there are not enough to fill the gap. The rows above would then
		 * have to move down to fill the gap. An unlikely situation, but this
		 * demonstrates the difficulty in determining in a well defined way
		 * whether we are scrolling up or down.
		 * 
		 * The solution is that we make two passes through the controls. First
		 * iterate through the row controls moving only the controls that are
		 * being moved upwards, then iterate in reverse moving only the controls
		 * that are being moved downwards.
		 */
		int topPosition = newTopRowOffset;
		rowIndex = topVisibleRow;
		
		while (topPosition < clientAreaSize.y && rowIndex < rowCount) {
			BaseEntryRowControl rowControl = getRowControl(rowIndex);
			int rowHeight = rowControl.getSize().y;
			if (rowControl.getBounds().y >= topPosition) {
				rowControl.setBounds(0, topPosition, clientAreaSize.x, rowHeight);
			}
			topPosition += rowHeight;
			rowIndex++;
		}

		while (topPosition > 0) {
			rowIndex--;
			BaseEntryRowControl rowControl = getRowControl(rowIndex);
			int rowHeight = rowControl.getSize().y;
			topPosition -= rowHeight;
			if (rowControl.getBounds().y < topPosition) {
				rowControl.setBounds(0, topPosition, clientAreaSize.x, rowHeight);
			}
		}

		/*
		 * We must keep the selected row, even if it is not visible.
		 * 
		 * Note that we want to make the selected control invisible but we do
		 * not set the visible property to false. This is because all rows in
		 * the rows map are assumed to be visible and the visible property is
		 * not set on when this row becomes visible. Also, we must be sure not
		 * to mess with the height, because this is not re-calculated each time,
		 * so we move it off just before the top of the visible area. This
		 * ensures it remains not visible even if the client area is re-sized.
		 */
		BaseEntryRowControl selectedRow = (BaseEntryRowControl)rowTracker.getSelectedRow();
		if (selectedRow != null) {
			EntryData selectedEntryData = selectedRow.committedEntryData;
			if (previousRows.containsKey(selectedEntryData)) {
				rows.put(selectedEntryData, selectedRow);
				previousRows.remove(selectedEntryData);

				int rowHeight = selectedRow.getSize().y;
				selectedRow.setLocation(0, -rowHeight);
			}
		}
		
		/*
		 * Remove any previous rows that are now unused.
		 * 
		 * It is important to clear the map of previousRows because otherwise code
		 * outside this method that attempts to get a row control may end up with a
		 * row control that has already been released.
		 */
		for (BaseEntryRowControl<T, ?> rowControl: previousRows.values()) {
			rowProvider.releaseRow(rowControl);
		}
		previousRows.clear();
	}

	void setTopRow(int topRow) {
		scrollToGivenFix(topRow, 0);
	}

	private void setBottomRow(int bottomRow) {
		scrollToGivenFix(bottomRow + 1, clientAreaSize.y);
	}

	/**
	 * Returns the TableRow object for the given row, creating a row
	 * if it does not exist.
	 * 
	 * This method lays out the rows given the current width of the
	 * client area.  Callers can rely on the size of the row control
	 * being set correctly.
	 * 
	 * @param rowIndex the 0-based index of the required row, based
	 * 			on the rows in the underlying model
	 * @return
	 */
	private BaseEntryRowControl getRowControl(int rowIndex) {
		T entryData = contentProvider.getElement(rowIndex);

		BaseEntryRowControl rowControl = rows.get(entryData);
		if (rowControl == null) {
			rowControl = previousRows.remove(entryData);
			if (rowControl == null) {
				// we must create a new row object
				rowControl = rowProvider.getNewRow(contentPane, entryData);
				int rowHeight = rowControl.computeSize(clientAreaSize.x, SWT.DEFAULT).y;
				rowControl.setSize(clientAreaSize.x, rowHeight);
			}

			rows.put(entryData, rowControl);
		}
		
		return rowControl;
	}

	/**
	 * Page up will scroll the table so that the row above the first
	 * fully visible row becomes the last row, with the bottom of the row
	 * aligned with the bottom of the visible area.  If there is no fully
	 * visible row but there are two partially visible rows then the table
	 * will be scrolled so that the bottom of the top row is aligned with
	 * the bottom of the visible area.  If there is only one row visible and
	 * it is only partially visible then the table is scrolled by the visible height.
	 */
	public void doPageUp() {
		if (currentRow == -1) {
			return;
		}

		if (currentRow > 0) {
			BaseEntryRowControl selectedRow = getSelectedRow();

			if (!selectedRow.canDepart()) {
				return;
			}

			int currentColumn = selectedRow.getCurrentColumn();

			/*
			 * Get previous row until we reach a row that, if positioned at the
			 * top of the visible area, puts the bottom of the previous current
			 * row at the bottom or below the bottom of the visible area.
			 */
			int totalHeight = 0;
			do {
				int rowHeight = getRowControl(currentRow).getBounds().height;
				totalHeight += rowHeight;
				if (totalHeight >= clientAreaSize.y) {
					break;
				}
				currentRow--;
			} while (currentRow > 0);

			scrollToShowRow(currentRow);
			getRowControl(currentRow).arrive(currentColumn);
		}
	}

	public void doPageDown() {
		if (currentRow == -1) {
			return;
		}

		if (currentRow < rowCount - 1) {
			BaseEntryRowControl selectedRow = getSelectedRow();

			if (!selectedRow.canDepart()) {
				return;
			}

			int currentColumn = selectedRow.getCurrentColumn();

			/*
			 * Get previous row until we reach a row that, if positioned at the
			 * bottom of the visible area, puts the top of the previous current
			 * row at the top or above the top of the visible area.
			 */
			int totalHeight = 0;
			do {
				int rowHeight = getRowControl(currentRow).getBounds().height;
				totalHeight += rowHeight;
				if (totalHeight >= clientAreaSize.y) {
					break;
				}
				currentRow++;
			} while (currentRow < rowCount - 1);

			scrollToShowRow(currentRow);
			getRowControl(currentRow).arrive(currentColumn);
		}
	}

	public void doRowUp() {
		if (currentRow == -1) {
			return;
		}

		if (currentRow > 0) {
			BaseEntryRowControl selectedRow = getSelectedRow();

			if (!selectedRow.canDepart()) {
				return;
			}

			int currentColumn = selectedRow.getCurrentColumn();

			currentRow--;
			scrollToShowRow(currentRow);
			getRowControl(currentRow).arrive(currentColumn);
		}
	}

	public void doRowDown() {
		if (currentRow == -1) {
			return;
		}

		if (currentRow < rowCount - 1) {
			BaseEntryRowControl selectedRow = getSelectedRow();

			if (!selectedRow.canDepart()) {
				return;
			}

			int currentColumn = selectedRow.getCurrentColumn();

			currentRow++;
			scrollToShowRow(currentRow);
			getRowControl(currentRow).arrive(currentColumn);
		}
	}

	private BaseEntryRowControl getSelectedRow() {
		if (currentRow == -1) {
			return null;
		} else {
			EntryData entryData = contentProvider.getElement(currentRow);
			return rows.get(entryData);
		}
	}

	/**
	 * @param portion
	 */
	private void scrollToSliderPosition() {
		double maximum = vSlider.getMaximum() - vSlider.getThumb();
		double portion = sliderPosition / maximum;

		/*
		 * find the 'anchor' row.  All other visible rows are positioned upwards
		 * and downwards from this control.
		 */
		double rowDouble = portion * rowCount;
		int anchorRowNumber = Double.valueOf(Math.floor(rowDouble)).intValue();
		double rowRemainder = rowDouble - anchorRowNumber;

		if (anchorRowNumber == rowCount) {
			scrollViewToGivenFix(anchorRowNumber, clientAreaSize.y);
		} else {
			// TODO: We have a problem here.  If a row has just been deleted, and this
			// method is being called from the deleteRow method, then anchorRowNumber
			// may in fact not be valid.  It could potentially cause an 'out of bounds'
			// exception, but is in any case incorrect.  The anchorRowNumber in the
			// following call must be within the range given by the content provider
			// but it is not always.
			BaseEntryRowControl anchorRowControl = getRowControl(anchorRowNumber);
			int anchorRowHeight = anchorRowControl.getSize().y;
			int anchorRowPosition = (int)(portion * clientAreaSize.y - anchorRowHeight * rowRemainder);
			anchorRowControl.setSize(clientAreaSize.x, anchorRowHeight);
			scrollViewToGivenFix(anchorRowNumber, anchorRowPosition);
		}
	}

	/**
	 * Sets the focus to the given row and column.
	 * 
	 * @param row
	 * @param column
	 * @return true if the new row selection could be made, false if there
	 * 		are issues with a previously selected row that prevent the change
	 * 		in selection from being made
	 */
//	public boolean setSelection(BaseEntryRowControl row,
//			CellBlock column) {
//		BaseEntryRowControl currentRowControl = getSelectedRow();
//		if (row != currentRowControl) {
//			if (currentRowControl != null) {
//				if (!currentRowControl.canDepart()) {
//					return false;
//				}
//			}
//			
//			row.arrive();  // Causes the selection colors etc.  Focus is already set.
//			currentRow = rows.indexOf(row) + topVisibleRow; 
//		}
//		return true;
//	}

	/**
	 * The SelectionListener for the table's vertical slider control.
	 * 
	 * Note that the selection never changes when the table is scrolled using
	 * the scroll bar.  The UI would be rather confusing otherwise, because
	 * then dragging the scroll bar would either cause a dialog to pop up asking if
	 * the changes in the current selection should be committed or would cause
	 * the changes to be committed without warning.  The usual convention with
	 * tables is to allow the selection to be scrolled off the screen, so that
	 * is what we do.
	 */
	private SelectionListener sliderSelectionListener = new SelectionListener() {
		@Override
		public void widgetSelected(SelectionEvent e) {
			switch (e.detail) {
			case SWT.ARROW_DOWN:
			{
				/*
				 * Scroll down so that the next row below the top visible row
				 * becomes the top visible row.
				 */
				scrollToGivenFix(topVisibleRow + 1, 0);
			}
			break;

			case SWT.ARROW_UP:
			{
				/*
				 * Scroll up so that the next row above the top visible row
				 * becomes the top visible row.
				 */
				if (topVisibleRow > 0) {
					scrollToGivenFix(topVisibleRow - 1, 0);
				}
			}
			break;

			case SWT.PAGE_DOWN:
			{
				/*
				 * Page down so that the lowest visible row (or
				 * partially visible row) becomes the top row.
				 * 
				 *  However, if the lowest visible row is also the top
				 *  row (i.e. the row is so high that it fills the visible
				 *  area) then scroll up by 90% of the height of the visible
				 *  area.
				 */
				int bottomRow = topVisibleRow + rows.size() - 1;
				if (rows.size() == 1) {
					Control rowControl = rows.get(contentProvider.getElement(bottomRow)); 
					scrollToGivenFix(bottomRow, rowControl.getBounds().y - clientAreaSize.y * 90 / 100);
				} else {
					scrollToGivenFix(bottomRow, 0);
				}
			}
			break;

			case SWT.PAGE_UP:
			{
				/*
				 * Page up so that the first visible row (or
				 * partially visible row) becomes the bottom row,
				 * aligned with the bottom of the visible area.
				 * 
				 *  However, if the first visible row is also the bottom
				 *  row (i.e. the row is so high that it fills the visible
				 *  area) then scroll down by 90% of the height of the visible
				 *  area.
				 */
				if (rows.size() == 1) {
					Control rowControl = rows.get(contentProvider.getElement(topVisibleRow)); 
					scrollToGivenFix(topVisibleRow, rowControl.getBounds().y + clientAreaSize.y * 90 / 100);
				} else {
					scrollToGivenFix(topVisibleRow+1, clientAreaSize.y);
				}
			}
			break;

			case SWT.NONE:
			case SWT.DRAG:
			default:
				// Assume scroll bar dragged.
			{
				/*
				 * The JavaDoc is incorrect.  It states that the selection can take
				 * any value from 'minimum' to 'maximum'.  In fact the largest value
				 * it can take is maximum - thumb (it has this value when the slider
				 * is scrolled completely to the bottom).
				 * 
				 * We cannot rely on the thumb size because it changes.  The thumb size
				 * will be appropriate for the last position but may not be correct
				 * for the new position.  We therefore calculate a proportion between
				 * 0 and 1.
				 */
				int selection = vSlider.getSelection();
				if (selection == sliderPosition) {
					return;
				}
				sliderPosition = selection;

				scrollToSliderPosition();
			}
			}
		}

		@Override
		public void widgetDefaultSelected(SelectionEvent e) {
			widgetSelected(e);
		}
	};

	/**
	 * This method is called when the content changes.
	 * Specific changes to the content such as a row insert
	 * or delete can be more efficiently refreshed using the
	 * deleteRow and insertRow methods.
	 * 
	 * This method is called when content is sorted or filtered.
	 * 
	 */
	// TODO: Are there more efficent methods following JFace conventions
	// to do this?
	public void refreshContent() {
		rowCount = contentProvider.getRowCount();
		// TODO: ensure selected row remains selected and visible
		// (if a sort.  Now if a filter then it may not remain visible).
		setTopRow(0);
	}

	/**
	 * This method moves a row up or down the screen to another position.
	 * This would happen when a property is changed and that affects the
	 * position of the entry in the sort order.
	 * <P>
	 * The movement is animated, meaning the control is moved continuously over
	 * the period of about 0.5 seconds to its new position and at the same time
	 * the intervening controls are moved in the opposite direction.  This is done
	 * because when controls jump to another position, the user does not know
	 * what is going on.
	 * <P>
	 * The screen is not scrolled, meaning controls above and below
	 * the affected area do not move.  Technically the scroll bar
	 * might have to be adjusted if the controls are not all the same
	 * height, but the amount would be so small that we don't bother.
	 * <P>
	 * If the moved control's new position is off the visible area then
	 * the animation will adjust the speed of its movement so that it
	 * moves to a position just off the screen.  This is so because otherwise
	 * if the new control's position was a long long way away then it
	 * would move so fast that the user would not see the movement.
	 * 
	 * @param originalIndex 
	 * @param newIndex 
	 */
	public void moveRow(int originalIndex, int newIndex) {
		if (newIndex < originalIndex) {
			new MoveRowUpProcessor(originalIndex, newIndex).moveRow();
		} else {
			new MoveRowDownProcessor(originalIndex, newIndex).moveRow();
		}
	}

	/**
	 * This method is called when an attempt to leave the selected row fails.
	 * We want to be sure that the selected row becomes visible because
	 * the user needs to correct the errors in the row.
	 */
	@Override
	public void scrollToShowRow(BaseEntryRowControl<T, ?> rowControl) {
		int rowIndex = contentProvider.indexOf(rowControl.getContent());
		scrollToShowRow(rowIndex);
	}

	/**
	 * This method is called whenever a row in this table ceases to be a selected row,
	 * regardless of whether the row is currently visible or not.
	 */
	@Override
	public void rowDeselected(BaseEntryRowControl<T, ?> rowControl) {
		/*
		 * If the row is not visible then we can release it.  However,
		 * there is no easy way of knowing whether it is visible. (We know
		 * it will be in the rows map because the selected row is always
		 * in that map whether visible or not).
		 */
	}

	/**
	 * This method will return a row control for a given EntryData
	 * object.
	 * 
	 * It may be that the row is not visible.  In that case the view
	 * is scrolled to make it visible.  The row is then selected.
	 * 
	 * This method is currently used only by 'duplicate transaction'
	 * action and is called only on the new entry row.  We may therefore
	 * want to review this method at some later time.
	 *  
	 * @param newEntryRow
	 * @return
	 */
	public BaseEntryRowControl getRowControl(T entryData) {
		// Crappy code...
		scrollToShowRow(contentProvider.indexOf(entryData));
		BaseEntryRowControl rowControl = rows.get(entryData);
		rowControl.getChildren()[0].setFocus();
		return rowControl;
	}

	/**
	 * This method should be called when the preferred height of a row may have changed.
	 * <P>
	 * The changed row will most likely also be the selected row. However, it is
	 * possible that it is not. We keep the top of the selected row at the same
	 * position, moving all rows below it up or down. However, if the table is
	 * scrolled to the bottom or near the bottom and the row height is being
	 * reduced then this may result in a blank space at the bottom of the table.
	 * In that case we re-adjust the rows so the table is fully scrolled to the
	 * bottom (which would result in the top of the changed row being moved
	 * down).
	 * 
	 * We queue up size refreshes and update the list asynchronously.  This has
	 * the following advantages:
	 * <UL>
	 * <LI> if multiple updates are made to the model by the same run on the UI thread
	 * then the table is updated only once</LI>
	 * <LI>if something causes a change to be fired while input is being set in a row,
	 * recursion could occur.  This happens because this method may result in a new row
	 * being created because the creation of this row has not completed and is not in
	 * the list...
	 *  
	 * @param rowControl
	 */
	public void refreshSize(BaseEntryRowControl<T, ?> rowControl) {
		/*
		 * If the row is not in our currently active list, ignore it.  This is actually
		 * quite important because this method can be called while input is being set
		 * into a row control that is not yet active, and this can cause problems, including problems with
		 * recursion, if we try to process it.
		 */
		if (!rows.containsKey(rowControl.committedEntryData)) {
			return;
		}
		
		int rowTop = rowControl.getLocation().y;
		
		// NOTE: This code does not do what the javadoc says it should do when
		// this row is not the selected row.  It keeps the top of the changed row
		// at the same position, not the top of the selected row.  Is this worth
		// worrying about?
		
		int rowHeight = rowControl.computeSize(clientAreaSize.x, SWT.DEFAULT).y;
		rowControl.setSize(clientAreaSize.x, rowHeight);

		int rowIndex = contentProvider.indexOf(rowControl.getContent());
		scrollToGivenFix(rowIndex, rowTop);
	}

	private class MoveRowProcessor {
		int originalIndex;
		int newIndex;

		public MoveRowProcessor(int originalIndex, int newIndex) {
			this.originalIndex = originalIndex;
			this.newIndex = newIndex;
		}

		int topOf(Control control) {
			return control.getLocation().y;
		}
		
		int bottomOf(Control control) {
			return control.getLocation().y + control.getSize().y;
		}

		/**
		 * Move the given control from the starting location to the ending location.  At the same time
		 * move all the intermediate controls by the given amount (which will always be in the opposite
		 * direction and by an amount equal to the height of the first given control).
		 * 
		 * @param movingRow
		 * @param startLocation
		 * @param endLocation
		 * @param intermediateRows
		 * @param moveDelta
		 */
		void moveControl(final Control movingControl, final int startLocation, final int endLocation, final List<Control> intermediateControls, final int moveDelta) {
			final int numberOfSteps = 20;

			// Ensure movingRow is on top	
			movingControl.moveAbove(null); 
			
			movingControl.setLocation(0, startLocation); 
			
			/*
			 * We start a thread that sets up each of ten positions
			 * and then runs a process on the UI thread to reflect that
			 * position.  Note that if the UI thread can't keep up then
			 * some positions may be skipped.  This ensures the correct
			 * speed is kept.
			 */
			final Display display = Display.getCurrent();
			new Thread() {
				@Override
				public void run() {
					int position = 0;
					int deltaRemaining = moveDelta;
					int deltaRemaining2 = endLocation - startLocation;
					do {
						final int delta = deltaRemaining / (numberOfSteps - position); 
						final int delta2 = deltaRemaining2 / (numberOfSteps - position);
						
						display.asyncExec(new Runnable() {
							@Override
							public void run() {
								// The intermediate controls
								for (Control intermediateControl : intermediateControls) {
									int y = intermediateControl.getLocation().y;
									intermediateControl.setLocation(0, y + delta); 
								}
								
								// The moving control
								int y = movingControl.getLocation().y;
								movingControl.setLocation(0, y + delta2); 
							}
						});

						deltaRemaining -= delta;
						deltaRemaining2 -= delta2;
						
						try {
							sleep(500);  // was 100
						} catch (InterruptedException e) {
							return;
						}

						position++;
					} while (position < numberOfSteps);
				}
			}.start();
		}
		
	}
	
	private class MoveRowUpProcessor extends MoveRowProcessor {

		public MoveRowUpProcessor(int originalIndex, int newIndex) {
			super(originalIndex, newIndex);
			assert(newIndex < originalIndex);
		}

		/** 
		 * @param originalIndex
		 * @param newIndex
		 */
		private void moveRow() {
			// TODO: this is not correct.  Might be, for example, a selected row.
			int bottomVisibleRow = topVisibleRow + rows.size() - 1;
			
			/*
			 * The movingRow might never be visible.  However if it
			 * is moving from below the visible area to above the visible
			 * area then we need to know its height (as that is the amount
			 * by which the visible controls will scroll), which means we
			 * must create it.
			 * 
			 * If the moving control is below the visible area and remains below,
			 * or is above and remains above, then we don't really need to
			 * know the height of the control.  However it is simpler to create
			 * it anyway as it is not worth optimizing these rare cases.
			 */
			Control movingRow = getRowControlFromPreMoveIndex(originalIndex);
			int movingRowHeight = movingRow.getSize().y;

			/*
			 * If a control is outside the visible area during the entire movement
			 * then there is no need to create the control.
			 * 
			 * We move both lowerIntermediate and upperIntermediate inwards (towards
			 * each other) as needed to cut out controls that are never visible.
			 */
			
			/*
			 * The lower bound (bottom of the visible area) is never visible if it
			 * is not visible at the start of the movement.  This is because the intermediate controls
			 * are moving downwards. 
			 */
			int lowerIntermediate = originalIndex - 1;
			if (lowerIntermediate > bottomVisibleRow) {
				lowerIntermediate = bottomVisibleRow;
			}
			
			/*
			 * An intermediate control is never visible if the bottom of the control is still at or
			 * above the top of the screen after it has been scrolled down by <code>movingRowHeight</code>.
			 * 
			 *  Note that this code may result in the creation of new controls.  These controls
			 *  will initially be positioned such that they are not visible.  They will all
			 *  become visible during the scrolling process.
			 */
			int topmostIntermediate = newIndex;
			int upperIntermediate = topmostIntermediate;
			{
				int index = topVisibleRow;
				Control intermediateRow = getRowControlFromPreMoveIndex(index);
				int y = topOf(intermediateRow);
				while (index > topmostIntermediate && y > -movingRowHeight) {
					index--;

					intermediateRow = getRowControlFromPreMoveIndex(index);
					int height = intermediateRow.getSize().y;
					y -= height;
					intermediateRow.setLocation(0, y);

					upperIntermediate = index;
				}
			}

			/*
			 * At this point, lowerIntermediate is the upper of the last
			 * row being moved or the last row that would be at least partially
			 * above the bottom of the visible area at any time during the scroll.
			 * It may be a row that is above the top of the visible area.
			 * 
			 * Likewise, upperIntermediate is the lower of the first
			 * row being moved or the first row that would be at least partially
			 * below the top of the visible area at any time during the scroll.
			 * It may be a row that is below the bottom of the visible area.
			 * 
			 * Because of the conditions not checked, upperIntermediate may in
			 * fact be below lowerIntermediate.  Such a condition indicates that
			 * lowerIntermediate is above the top of the visible area or upperIntermediate
			 * is below the bottom of the visible area.  Either way, we have nothing to
			 * scroll.
			 */
			if (upperIntermediate > lowerIntermediate) {
				return;
			}

			/*
			 * The moving control may be moving well above the visible area, or may be
			 * moving from well below the visible area.  However we want to show the user
			 * its movement and don't want it to move so fast for a short time that it
			 * may be visible.  We therefore act as though it is moving from just outside
			 * the visible area.
			 */
			int startLocation = bottomOf(getRowControlFromPreMoveIndex(lowerIntermediate));
			int endLocation = topOf(getRowControlFromPreMoveIndex(upperIntermediate));

			/*
			 * Now we know the range of intermediate controls that need
			 * to be moved.
			 * 
			 * It may not really be necessary to build a list of these.
			 * We could just pass on the lower and upper bounds and let
			 * everyone fetch these from the content provider, but just
			 * in case the content provider is not too efficient....
			 */
			List<Control> intermediateRows = new ArrayList<Control>();
			for (int index = upperIntermediate; index <= lowerIntermediate; index++) {
//				T entryData = contentProvider.getElement(index);
//				Control intermediateRow = rows.get(entryData);
				Control intermediateRow = getRowControlFromPreMoveIndex(index);
				intermediateRows.add(intermediateRow);
			}
			
			moveControl(movingRow, startLocation, endLocation, intermediateRows, movingRowHeight);
		}
		
		/**
		 * This method must be called after the content has been updated.
		 * However, that makes it confusing here when we are getting the control
		 * for a given index prior to doing the moves.  The controls are located in
		 * their pre-move positions, but the index to be used to get a control must
		 * be the post-movement index.
		 * 
		 * To help avoid this confusion, this method is given an index and will return
		 * the row that was at that index before the rows were moved.
		 */
		Control getRowControlFromPreMoveIndex(int index) {
			if (index < newIndex) {
				return getRowControl(index);
			} else if (index < originalIndex) {
				return getRowControl(index + 1);
			} else if (index == originalIndex) {
				return getRowControl(newIndex);
			} else {
				return getRowControl(index);
			}
		}
		
	}

	private class MoveRowDownProcessor extends MoveRowProcessor {

		public MoveRowDownProcessor(int originalIndex, int newIndex) {
			super(originalIndex, newIndex);
			assert(newIndex > originalIndex);
		}

		/** 
		 * @param originalIndex
		 * @param newIndex
		 */
		private void moveRow() {
			// TODO: this is not correct.  Might be, for example, a selected row.
			int bottomVisibleRow = topVisibleRow + rows.size() - 1;
			
			/*
			 * The movingRow might never be visible.  However if it
			 * is moving from below the visible area to above the visible
			 * area then we need to know its height (as that is the amount
			 * by which the visible controls will scroll), which means we
			 * must create it.
			 * 
			 * If the moving control is below the visible area and remains below,
			 * or is above and remains above, then we don't really need to
			 * know the height of the control.  However it is simpler to create
			 * it anyway as it is not worth optimizing these rare cases.
			 */
			Control movingRow = getRowControlFromPreMoveIndex(originalIndex);
			int movingRowHeight = movingRow.getSize().y;

			/*
			 * If a control is outside the visible area during the entire movement
			 * then there is no need to create the control.
			 * 
			 * We move both lowerIntermediate and upperIntermediate inwards (towards
			 * each other) as needed to cut out controls that are never visible.
			 */
			
			/*
			 * The upper bound (top of the visible area) is never visible if it
			 * is not visible at the start of the movement.  This is because the intermediate controls
			 * are moving upwards. 
			 */
			int upperIntermediate = originalIndex + 1;
			if (upperIntermediate < topVisibleRow) {
				upperIntermediate = topVisibleRow;
			}
			
			/*
			 * An intermediate control is never visible if the top of the control is still at or
			 * below the bottom of the screen after it has been scrolled up by <code>movingRowHeight</code>.
			 * 
			 *  Note that this code may result in the creation of new controls.  These controls
			 *  will initially be positioned such that they are not visible.  They will all
			 *  become visible during the scrolling process.
			 */
			int bottommostIntermediate = newIndex;
			int lowerIntermediate = bottommostIntermediate;
			{
				int index = bottomVisibleRow;
				Control intermediateRow = getRowControlFromPreMoveIndex(index);
				int y = bottomOf(intermediateRow);
				while (index < bottommostIntermediate && y < clientAreaSize.y + movingRowHeight) {
					index++;

					intermediateRow = getRowControlFromPreMoveIndex(index);
					int height = intermediateRow.getSize().y;
					y += height;
					intermediateRow.setLocation(0, y);

					lowerIntermediate = index;
				}
			}

			/*
			 * At this point, lowerIntermediate is the upper of the last
			 * row being moved or the last row that would be at least partially
			 * above the bottom of the visible area at any time during the scroll.
			 * It may be a row that is above the top of the visible area.
			 * 
			 * Likewise, upperIntermediate is the lower of the first
			 * row being moved or the first row that would be at least partially
			 * below the top of the visible area at any time during the scroll.
			 * It may be a row that is below the bottom of the visible area.
			 * 
			 * Because of the conditions not checked, upperIntermediate may in
			 * fact be below lowerIntermediate.  Such a condition indicates that
			 * lowerIntermediate is above the top of the visible area or upperIntermediate
			 * is below the bottom of the visible area.  Either way, we have nothing to
			 * scroll.
			 */
			if (upperIntermediate > lowerIntermediate) {
				return;
			}

			/*
			 * The moving control may be moving well above the visible area, or may be
			 * moving from well below the visible area.  However we want to show the user
			 * its movement and don't want it to move so fast for a short time that it
			 * may be visible.  We therefore act as though it is moving from just outside
			 * the visible area.
			 */
			int startLocation = topOf(getRowControlFromPreMoveIndex(upperIntermediate)) - movingRowHeight;
			int endLocation = bottomOf(getRowControlFromPreMoveIndex(lowerIntermediate)) - movingRowHeight;

			/*
			 * Now we know the range of intermediate controls that need
			 * to be moved.
			 * 
			 * It may not really be necessary to build a list of these.
			 * We could just pass on the lower and upper bounds and let
			 * everyone fetch these from the content provider, but just
			 * in case the content provider is not too efficient....
			 */
			List<Control> intermediateRows = new ArrayList<Control>();
			for (int index = upperIntermediate; index <= lowerIntermediate; index++) {
//				T entryData = contentProvider.getElement(index);
//				Control intermediateRow = rows.get(entryData);
				Control intermediateRow = getRowControlFromPreMoveIndex(index);
				intermediateRows.add(intermediateRow);
			}
			
			moveControl(movingRow, startLocation, endLocation, intermediateRows, -movingRowHeight);
		}
		
		/**
		 * This method must be called after the content has been updated.
		 * However, that makes it confusing here when we are getting the control
		 * for a given index prior to doing the moves.  The controls are located in
		 * their pre-move positions, but the index to be used to get a control must
		 * be the post-movement index.
		 * 
		 * To help avoid this confusion, this method is given an index and will return
		 * the row that was at that index before the rows were moved.
		 */
		private Control getRowControlFromPreMoveIndex(int index) {
			if (index < originalIndex) {
				return getRowControl(index);
			} else if (index == originalIndex) {
				return getRowControl(newIndex);
			} else if (index <= newIndex) {
				return getRowControl(index - 1);
			} else {
				return getRowControl(index);
			}
		}
	}
}

