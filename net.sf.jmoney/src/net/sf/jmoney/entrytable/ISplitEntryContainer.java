package net.sf.jmoney.entrytable;

/**
 * Interface to be implemented by anything that contains
 * the 'other' entries, i.e. the SplitEntryRowControl objects
 * and also the OtherEntryControl object.
 */
public interface ISplitEntryContainer {
	/*
	 * Currently this interface is empty, and we could technically
	 * remove this interface and use, say, Object to parameterize the
	 * co-ordinator type in the split entry blocks.
	 * 
	 * However, it may be that stuff will need to be added so
	 * keep this interface.
	 */
}
