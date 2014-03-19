package net.sf.jmoney.entrytable;

import org.eclipse.core.databinding.observable.value.IObservableValue;

/**
 * This interface contains the minimum that must be implemented by anything
 * that contains base entry row controls (i.e. tables based on EntryData).
 */
public interface ICompositeTable<T, R extends RowControl<IObservableValue<T>,R,R>> {

	void rowDeselected(R rowControl);
	
	/**
	 * This may be needed only to update the header.

	 * @param input
	 * @param baseEntryRowControl
	 */
	void setCurrentRow(T input, R rowControl);
	
	void scrollToShowRow(R rowControl);
}
