package net.sf.jmoney.entrytable;

public interface IContentProvider<T extends EntryData> {

	int getRowCount();

	T getElement(int rowNumber);

	int indexOf(T entryData);
}
