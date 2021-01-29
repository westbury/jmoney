package net.sf.jmoney.stocks.pages;

public interface IEntryType {

	/**
	 * This is the last part of each triple in the entry type.
	 * 
	 * @return
	 */
	String getId();

	boolean isCompulsory();

}
