package net.sf.jmoney.importer.matcher;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import net.sf.jmoney.importer.MatchingEntryFinder;
import net.sf.jmoney.importer.model.ReconciliationEntryInfo;
import net.sf.jmoney.model2.Account;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.PropertyAccessor;
import net.sf.jmoney.model2.Transaction;

public class EntryData extends BaseEntryData {

	public Date clearedDate = null;
	public Date valueDate = null;
	public String check = null;
	private String memo = null;
	private String type = null;
	private String name = null;
	private String payee = null;
	public String uniqueId = null;
	private Map<PropertyAccessor, Object> propertyMap = new HashMap<PropertyAccessor, Object>();

	public void setClearedDate(Date clearedDate) {
		this.clearedDate  = clearedDate;
	}
	public void setValueDate(Date valueDate) {
		this.valueDate = valueDate;
	}
	public void setCheck(String check) {
		this.check = check;
	}
	public void setMemo(String memo) {
		this.memo = memo;
	}
	public void setType(String type) {
		this.type = type;
	}
	public void setName(String name) {
		this.name = name;
	}
	public void setPayee(String payee) {
		this.payee = payee;
	}
	public void setAmount(long amount) {
		this.amount = amount;
	}
	public void setUniqueId(String uniqueId) {
		this.uniqueId = uniqueId;
	}
	public void setProperty(PropertyAccessor propertyAccessor, Object value) {
		propertyMap.put(propertyAccessor, value);
	}

	/**
	 * This method is given a transaction with two entries.
	 * This method assigns the properties from the bank statement
	 * import to the properties in the transaction.
	 * <P>
	 * Other than the account and memo properties, no properties
	 * will have been set in the transaction  before this method
	 * is called.
	 *
	 * @param transaction
	 * @param entry1 the 'other' entry, typically being the charge account,
	 * 			never null
	 * @param entry2 the entry whose description and category is to be determined, typically an entry in an
	 * 					income and expense account, never null
	 */
	@Override
	public void assignPropertyValues(Transaction transaction, Entry entry1) {
		if (valueDate == null) {
			transaction.setDate(clearedDate);
			entry1.setValuta(clearedDate);
		} else {
			transaction.setDate(valueDate);
			entry1.setValuta(clearedDate);
		}

		entry1.setCheck(check);
		ReconciliationEntryInfo.getUniqueIdAccessor().setValue(entry1, uniqueId);

		entry1.setAmount(amount);
	}

	@Override
	public String toString() {
		return "[a:"+amount+";n:"+name+"]";
	}

	/**
	 * Returns the text that is to be used for pattern matching.
	 * The patterns entered by the user are matched against the text
	 * returned by this method.
	 *
	 * @return the text which may be empty but must never be null
	 */
	public String getTextToMatch() {
		// Get values at this time.  We can't get them earlier because
		// they may not have been set.
		if (entry != null) {
			fillFromEntry();
		}
		
		String text = "";
		if (memo != null) {
			text += "memo=" + memo;
		}
		if (type != null) {
			text += "type=" + type;
		}
		if (name != null) {
			/*
			 * This is a bit of a hack, but if there is no payee then
			 * we use the name as the payee.  The reason for this is that
			 * Citiards.com put the reference under 'name' if OFX but it comes out
			 * as the payee if QIF.  We want to be able to use the same matching
			 * rules regardless of which format the user used.
			 */
			if (payee == null) {
   				text += "payee=" + name;
			} else {
				text += "name=" + name;
			}
		}
		if (payee != null) {
			text += "payee=" + payee;
		}

		BigDecimal myAmount = new BigDecimal(amount).scaleByPowerOfTen(-2);
		text += "amount=" + myAmount;
		return text;
	}

	@Override
	public void fillFromEntry() {
		valueDate = entry.getTransaction().getDate();
		clearedDate = entry.getValuta();
		setMemo(entry.getMemo());
		setAmount(entry.getAmount());
		setUniqueId(ReconciliationEntryInfo.getUniqueIdAccessor().getValue(entry));
	}

	public String getName() {
		return name;
	}

	/**
	 * Returns a string suitable for matching against
	 * the patterns.
	 * 
	 * @return the memo which may be empty but cannot
	 * 			be null
	 */
	public String getMemo() {
		return memo == null ? "" : memo;
	}
	
	/**
	 * The memo if no patterns match
	 */
	@Override
	public String getDefaultMemo() {
		return memo==null? (name==null?payee: name):memo;
	}

	/**
	 * The description if no patterns match
	 */
	@Override
	public String getDefaultDescription() {
		return payee == null?(memo==null?name:memo):payee;
	}

	public Date getClearedDate() {
		return clearedDate;
	}
	
	public void setEntry(Entry entry) {
		this.entry = entry;
	}

	private Date getImportedDate() {
		return (valueDate != null)
		? valueDate
				: clearedDate;
	}
	
	@Override
	public String getTextForRegexMatching() {
		return memo;
	}

	@Override
	public void setDataIntoExistingEntry(Entry matchedEntry) {
		matchedEntry.setValuta(getImportedDate());  // ????
		matchedEntry.setCheck(check);
		// TODO is this line correct?
		ReconciliationEntryInfo.getUniqueIdAccessor().setValue(matchedEntry, uniqueId);
	}
	@Override
	public Entry findMatch(Account account, int numberOfDays, Set<Entry> ourEntries) {
		
	Date importedDate = getImportedDate();
	
	MatchingEntryFinder matchFinder = new MatchingEntryFinder() {
		@Override
		protected boolean doNotConsiderEntryForMatch(Entry entry) {
			/*
			 * If this given entry is in our map then it means we have just added it.  That means we have multiple identical
			 * entries in the import file.  In that case we want to be sure that we keep the multiple entries as they are
			 * genuine duplicates.
			 * 
			 * 'already matched' means don't consider this prior entry when looking for entries that might match this entry.
			 */
			return ourEntries.contains(entry) || ReconciliationEntryInfo.getUniqueIdAccessor().getValue(entry) != null;
		}
	};
	return matchFinder.findMatch(account, amount, importedDate, 5, check);
	}
}
