package net.sf.jmoney.importer.model;

import java.util.List;

import net.sf.jmoney.importer.matcher.BaseEntryData;
import net.sf.jmoney.importer.matcher.TransactionParamMetadata;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.Transaction;

public abstract class TransactionType implements Comparable<TransactionType> {

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
	public int compareTo(TransactionType otherType) {
		return label.compareToIgnoreCase(otherType.label);
	}

	public abstract List<TransactionParamMetadata> getParameters();

	public abstract void createTransaction(Transaction transaction, Entry entry1, BaseEntryData entryData, MemoPattern pattern, Object[] args);
	
}
