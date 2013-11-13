/**
 * 
 */
package net.sf.jmoney.entrytable;

import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;

public class CellFocusListener<R extends RowControl> extends FocusAdapter {
	private R rowControl;
	private ICellControl2<?> cellControl;
	private RowSelectionTracker<R> selectionTracker;
	private FocusCellTracker focusCellTracker;

	public CellFocusListener(R rowControl, ICellControl2<?> cellControl) {
		this.rowControl = rowControl;
		this.cellControl = cellControl;
		this.selectionTracker = rowControl.selectionTracker;
		this.focusCellTracker = rowControl.focusCellTracker;
	}
	
    @Override	
	public void focusGained(FocusEvent e) {
		final ICellControl2<?> previousFocus = focusCellTracker.getFocusCell();
		if (cellControl == previousFocus) {
			/*
			 * The focus has changed to a different control as far as SWT is
			 * concerned, but the focus is still within the same cell
			 * control. This can happen if the cell control is a composite
			 * that contains multiple child controls, such as the date
			 * control. Focus may move from the text box of a date control
			 * to the button in the date control, but focus has not left the
			 * cell. We take no action in this situation.
			 * 
			 * This can also happen if focus was lost to a control outside
			 * of the table. This does not change the focus cell within the
			 * table so when focus is returned to the table we will not see
			 * a cell change here.
			 */
			return;
		}

		/*
		 * It is important to set the new focus cell straight away. The
		 * reason is that if, for example, a dialog box is shown (such as
		 * may happen in the selectionTracker.setSelection method below)
		 * then focus will move away from the control to the dialog then
		 * back again when the dialog is closed. If the new focus is already
		 * set then nothing will happen the second time the control gets
		 * focus (because of the test above).
		 */
		focusCellTracker.setFocusCell(cellControl);

		/*
		 * Make sure any changes in the control are written back to the model.
		 */
		// Should all be bound now
//		if (previousFocus != null) {
//			previousFocus.save();
//		}

		/*
		 * Opening dialog boxes (as may be done by the
		 * selectionTracker.setSelection method below) and calling setFocus
		 * both cause problems if done from within the focusGained method.
		 * We therefore queue up a new task on this same thread to check
		 * whether the row selection can change and either update the
		 * display (background colors and borders) to show the row selection
		 * or revert the focus to the original cell.
		 */
		cellControl.getControl().getDisplay().asyncExec(new Runnable() {
			@Override
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

					cellControl.setSelected();
				} else {
					/*
					 * The row selection change was rejected so restore the original cell selection.
					 */
					
					selectionTracker.getSelectedRow().scrollToShowRow();
					
					// TODO: Should we be restoring selection to the cell that needs correcting?
					focusCellTracker.setFocusCell(previousFocus);
					if (previousFocus != null) {
					    previousFocus.getControl().setFocus();
					}
				}
			}
		});
	}
}