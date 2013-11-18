/**
 * 
 */
package net.sf.jmoney.entrytable;

import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;

/**
 * This focus listener should be set on controls that are not cells so would
 * not change the cell selection but are inside rows and so should change
 * the row selection.
 * <P>
 * An example would be the button that brings down the drop-down shell containing
 * the split entries.  This listener should be added to the button so that if the
 * button gets focus, the containing row becomes the selected row.  If this is not
 * done, the drop-down shell would appear but the top level row selection would not
 * change.
 *
 * @param <R>
 */
class NonCellFocusListener<R extends RowControl> extends FocusAdapter {
	private R rowControl;
	private RowSelectionTracker<R> selectionTracker;
	private FocusCellTracker focusCellTracker;

	// TODO: remove this version?
	public NonCellFocusListener(R rowControl, RowSelectionTracker<R> selectionTracker, FocusCellTracker focusCellTracker) {
		this.rowControl = rowControl;
		this.selectionTracker = selectionTracker;
		this.focusCellTracker = focusCellTracker;
	}
	
	@SuppressWarnings("unchecked")
	public NonCellFocusListener(R rowControl) {
		this.rowControl = rowControl;
		this.selectionTracker = rowControl.selectionTracker;
		this.focusCellTracker = rowControl.focusCellTracker;
	}
	
    @Override	
	public void focusGained(FocusEvent e) {
		final ICellControl2<?> previousFocus = focusCellTracker.getFocusCell();
		if (previousFocus != null) {
			System.out.println("here"); //$NON-NLS-1$
			/*
			 * No cell has the focus, so we don't need to clear that out.
			 */
			/*
			 * It is important to clear the previous focus cell straight away. The
			 * reason is that if, for example, a dialog box is shown (such as
			 * may happen in the selectionTracker.setSelection method below)
			 * then focus will move away from the control to the dialog then
			 * back again when the dialog is closed. If the cell focus has alread
			 * been cleared in the cell focus tracker
			 * then we don't repeat this.
			 */
			focusCellTracker.setFocusCell(null);

			/*
			 * Make sure any changes in the control are written back to the model.
			 */
			previousFocus.save();
		}

		/*
		 * Opening dialog boxes (as may be done by the
		 * selectionTracker.setSelection method below) and calling setFocus
		 * both cause problems if done from within the focusGained method.
		 * We therefore queue up a new task on this same thread to check
		 * whether the row selection can change and either update the
		 * display (background colors and borders) to show the row selection
		 * or revert the focus to the original cell.
		 */
		rowControl.getDisplay().asyncExec(new Runnable() {
			@Override
			@SuppressWarnings("null")
			public void run() {
				boolean success = selectionTracker.setSelection(rowControl, /*TODO: cellBlock*/null);
				if (success) {
					/*
					 * The row selection will have been set by the setSelection method
					 * but we must also update the cell selection.
					 */ 
					if (previousFocus != null) {
						previousFocus.setUnselected();
					}
				} else {
					/*
					 * The row selection change was rejected so restore the original cell selection.
					 */
					
					selectionTracker.getSelectedRow().scrollToShowRow();
					
					// TODO: Should we be restoring selection to the cell that needs correcting?
					focusCellTracker.setFocusCell(previousFocus);
					
					/*
					 * previousFocus could potentially be null here because a
					 * row can be selected and changed to an error state without
					 * ever selecting a cell. This can be done by pressing the
					 * drop-down button to get the split entries and editing in
					 * the split entries.
					 */
					if (previousFocus != null) {
						previousFocus.getControl().setFocus();
					}
				}
			}
		});
	}
}