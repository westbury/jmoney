package net.sf.jmoney.entrytable;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

public abstract class RowControl<T, R extends RowControl<T,R>> extends CellContainer<T,R> {

	public static final Color selectedCellColor = new Color(Display
			.getCurrent(), 255, 255, 255);

	public RowSelectionTracker<R> selectionTracker;
	
	protected FocusCellTracker focusCellTracker;
	
	protected abstract void setSelected(boolean isSelected);
	
	public RowControl(Composite parent, int style, RowSelectionTracker<R> selectionTracker, FocusCellTracker focusCellTracker) {
		super(parent, style);

		this.selectionTracker = selectionTracker;
		this.focusCellTracker = focusCellTracker;
		
		/*
		 * By default the child controls get the same background as
		 * this composite.
		 */
		setBackgroundMode(SWT.INHERIT_FORCE);
	}

	protected abstract R getThis();

	/**
	 * This version is called when the selection changed as a result
	 * of the user clicking on a control in another row.  Therefore
	 * we do not set the cell selection.
	 */
	public void arrive() {
		setSelected(true);
	}

	/**
	 * This method should be called whenever a row is to lose selection.
	 * It makes whatever changes are necessary to the display of the row
	 * and saves if necessary the data in the row.
	 * 
	 * @return true if this method succeeded (or failed in some way that
	 * 		is not the user's fault and that the user cannot correct), 
	 * 		false if this method could not save the data because the user
	 * 		has not properly entered the data and so selection should remain
	 * 		on the row (in which case this method will display an appropriate
	 * 		message to the user)
	 */
	public boolean canDepart() {
//		if (focusCellTracker.getFocusCell() != null) {
//			focusCellTracker.getFocusCell().getControl().setBackground(null);
//			focusCellTracker.setFocusCell(null);
//		}
		
		setSelected(false);
		return true;
	}
	
	/**
	 * This method is called when the selected row may not be visible (because the
	 * user scrolled the table) but we want to make it visible again because there
	 * was an error in it and we were unable to move the selection off the row.
	 */
	protected abstract void scrollToShowRow();
	
	
}
