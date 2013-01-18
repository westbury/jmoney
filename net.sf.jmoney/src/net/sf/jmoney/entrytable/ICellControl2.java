package net.sf.jmoney.entrytable;

import net.sf.jmoney.model2.IPropertyControl;

/**
 * Interface implemented by all editable cells.
 * <P>
 * IPropertyControl objects may be controls representing single
 * properties that can be edited by the user, or they may be
 * 'composite' or 'container' cells.  If the former then the
 * cell should implement this extended interface.  This interface
 * allows the framework to tell the cell to show itself as being
 * the selected cell or to be an unselected cell.  Normally a selected
 * cell is white.
 *
 * @param <T>
 */
public interface ICellControl2<T> extends IPropertyControl<T> {

	/**
	 * Set this cell as the cell with the focus.
	 * <P>
	 * This usually just involves setting the color.
	 */
	void setSelected();

	/**
	 * Set this cell back to an un-selected state.
	 * <P>
	 * This usually just involves setting the color.
	 */
	void setUnselected();
}
