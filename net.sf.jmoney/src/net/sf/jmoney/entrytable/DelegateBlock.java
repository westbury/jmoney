package net.sf.jmoney.entrytable;

import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

public abstract class DelegateBlock<T, T2> extends Block<T> {

	private Block<T2> delegate;
	
	public DelegateBlock(Block<T2> delegate) {
		this.delegate = delegate;

		// TODO should these really be fields set in the constructor???
		// or just calculate when required?
		
		/*
		 * The minimumWidth, weight, and height are set in the
		 * constructor. These values can all be calculated from the list
		 * of child blocks.
		 */
		minimumWidth = delegate.minimumWidth;
		weight = delegate.weight;
	}
	
	protected abstract T2 convert(T blockInput);

	@Override
	public void createHeaderControls(Composite parent) {
			delegate.createHeaderControls(parent);
	}

	@Override
	public void createCellControls(Composite parent, T input, RowControl rowControl) {
		delegate.createCellControls(parent, convert(input), rowControl);
	}
	
	@Override
	protected void layout(int width) {
		// TODO Having width a public field is poor design.  Had it been a getter then
		// we could simply delegate both this method and the getter.
		if (this.width != width) {
			this.width = width;
			delegate.layout(width);
		}
	}

	@Override
	protected void positionControls(int x, int y, int verticalSpacing,
			Control[] controls, T input, boolean flushCache) {
		/*
		 * This method may be called with a null input when it is being used to
		 * layout the header controls and when no row is selected. We still need
		 * to layout the controls, though where there is a stack layout, or some
		 * other layout that depends on the row selection, then nothing will be
		 * shown.
		 */
		delegate.positionControls(x, y, verticalSpacing, controls, input == null ? null : convert(input), flushCache);
	}

	@Override
	protected void setInput(T input) {
		delegate.setInput(convert(input));
	}

	@Override
	protected int getHeight(int verticalSpacing, Control[] controls) {
		return delegate.getHeight(verticalSpacing, controls);
	}

	@Override
	protected void paintRowLines(GC gc, int x, int y, int verticalSpacing,
			Control[] controls, T input) {
		delegate.paintRowLines(gc, x, y, verticalSpacing, controls, convert(input));
	}

	@Override
	protected int getHeightForGivenWidth(int width, int verticalSpacing,
			Control[] controls, boolean changed) {
		return delegate.getHeightForGivenWidth(width, verticalSpacing, controls, changed);
	}

	@Override
	public int initIndexes(int startIndex) {
		return delegate.initIndexes(startIndex);
	}

}
