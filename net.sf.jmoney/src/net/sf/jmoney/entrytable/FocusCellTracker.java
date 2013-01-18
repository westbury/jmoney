/**
 * 
 */
package net.sf.jmoney.entrytable;

public class FocusCellTracker {
	/**
	 * The control that is currently set up as the control
	 * with the focus.
	 */
	protected ICellControl2 cellControl = null;

	public void setFocusCell(ICellControl2 cellControl) {
		this.cellControl = cellControl;
	}

	public ICellControl2 getFocusCell() {
		return cellControl;
	}
}