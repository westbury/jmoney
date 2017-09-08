package net.sf.jmoney.qif;

import net.sf.jmoney.importer.matcher.EntryData;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.Transaction;

public class QifEntryData extends EntryData {

	public String fitid;

	public String check = null;

	public void setCheck(String check) {
		this.check = check;
	}

	@Override
	public void assignPropertyValues(Transaction transaction, Entry entry1) {
		super.assignPropertyValues(transaction, entry1);

		entry1.setCheck(check);
	}

	@Override
	public void setDataIntoExistingEntry(Entry matchedEntry) {
		super.setDataIntoExistingEntry(matchedEntry);

		matchedEntry.setCheck(check);
	}

}