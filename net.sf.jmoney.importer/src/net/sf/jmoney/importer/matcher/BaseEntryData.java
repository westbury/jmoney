package net.sf.jmoney.importer.matcher;

import java.util.Set;

import net.sf.jmoney.model2.Account;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.Transaction;

/**
 * This object contains the data imported for a single 'row' of data
 * from an input source.  A single row will typically map to a single transaction
 * with an account entry and an income/expense entry.  However it may be that
 * a single input row maps to a transaction with multiple split entries.  For example,
 * if the input row contains the data for a stock purchase then the resulting transaction
 * may contain entries for gross cost, commission, taxes, and net cost.  It may also be
 * that the caller places multiple input rows into the same transaction.  For example the
 * import of Paypal or Amazon data may have transactions split into multiple items, with an entry with
 * the charged amount and an entry for each item in the order.  In this case there is an
 * instance of this object for each item in the order. 
 * <P>
 * This is an abstract class.  A concrete class should be created for each
 * type of source.  The actual columns of data available from the source
 * depends on the type of source. 
 * 
 * @author Nigel
 *
 */
public abstract class BaseEntryData {

	/**
	 * If an entry has already been created, it is set here.
	 * This is more flexible because an import could have information
	 * about itemized entries (e.g. a Paypal import) in which case
	 * creating the transaction from the EntryData will not work.
	 */
	public Entry entry = null;

	public long amount = 0;  // Use getter???

	public abstract void fillFromEntry();

	public abstract String getTextForRegexMatching();

	public abstract void setDataIntoExistingEntry(Entry matchedEntry);

	public abstract Entry findMatch(Account account, int numberOfDays, Set<Entry> ourEntries);

	public abstract void assignPropertyValues(Transaction transaction, Entry entry1);

	/**
	 * The memo if no patterns match
	 */
	public abstract String getDefaultMemo();

	/**
	 * The description if no patterns match
	 */
	public abstract String getDefaultDescription();

}
