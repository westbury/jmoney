package net.sf.jmoney.importer.model;

import java.util.List;
import java.util.Set;

import net.sf.jmoney.importer.matcher.BaseEntryData;
import net.sf.jmoney.importer.matcher.PatternMatch;
import net.sf.jmoney.importer.matcher.TransactionParamMetadata;
import net.sf.jmoney.model2.Account;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.Transaction;

public abstract class TransactionType<T extends BaseEntryData> implements Comparable<TransactionType<?>> {

	private String id;
	
	private String label;

	public TransactionType(String id, String label) {
		this.id = id;
		this.label = label;
	}

	public String getId() {
		return id;
	}

	public String getLabel() {
		return label;
	}

	@Override
	public int compareTo(TransactionType<?> otherType) {
		return label.compareToIgnoreCase(otherType.label);
	}

	public abstract List<TransactionParamMetadata> getParameters();

	public abstract void createTransaction(Transaction transaction, Entry entry1, T entryData, PatternMatch match);

	/**
	 * Looks to see if there is already an entry that is likely to match this entry.
	 * If this method returns true then this class can guarantee to be able to merge the data into
	 * the matching entry with no problematic conflicts.
	 * 
	 * @param account the account into which we are importing, being the account we search for a match
	 * @param numberOfDays
	 * @param ourEntries
	 * @return
	 */
	public abstract Entry findMatch(T entryData, Account account, int numberOfDays, Set<Entry> ourEntries);

	
}
