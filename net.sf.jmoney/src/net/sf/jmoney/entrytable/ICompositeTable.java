package net.sf.jmoney.entrytable;

/**
 * This interface contains the minimum that must be implemented by anything
 * that contains base entry row controls (i.e. tables based on EntryData).
 */
public interface ICompositeTable<T extends EntryData> {

	void rowDeselected(BaseEntryRowControl<T, ?> rowControl);
	
	/**
	 * This may be needed only to update the header.

	 * @param input
	 * @param uncommittedEntryData
	 */
	void setCurrentRow(T input, T uncommittedEntryData);
	
	void scrollToShowRow(BaseEntryRowControl<T, ?> rowControl);
}
