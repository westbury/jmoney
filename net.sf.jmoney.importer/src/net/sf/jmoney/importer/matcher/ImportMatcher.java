package net.sf.jmoney.importer.matcher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.jmoney.importer.model.MemoPattern;
import net.sf.jmoney.importer.model.TransactionType;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.Session;
import net.sf.jmoney.model2.Transaction;

public class ImportMatcher<T extends BaseEntryData> {

	private IPatternMatcher account;

	private List<MemoPattern> sortedPatterns;

	private List<ImportEntryProperty<T>> importEntryProperties;

	private List<TransactionType> applicableTransactionTypes;

	public ImportMatcher(IPatternMatcher matcherInsideTransaction, List<ImportEntryProperty<T>> importEntryProperties, List<TransactionType> applicableTransactionTypes) {
		this.account = matcherInsideTransaction;
		this.importEntryProperties = importEntryProperties;
		this.applicableTransactionTypes = applicableTransactionTypes;

		/*
		 * Get the patterns sorted into order.  It is important that we test patterns in the
		 * correct order because an entry may match both a general pattern and a more specific
		 * pattern.
		 */
		sortedPatterns = new ArrayList<MemoPattern>(account.getPatternCollection());
		Collections.sort(sortedPatterns, new Comparator<MemoPattern>(){
			@Override
			public int compare(MemoPattern pattern1, MemoPattern pattern2) {
				return pattern1.getOrderingIndex() - pattern2.getOrderingIndex();
			}
		});

	}

	/**
	 * 
	 * @param text
	 * @param entry1 the 'other' entry, typically being the charge account,
	 * 			may be null
	 * @param entry2 the entry whose description and category is to be determined,
	 * 			never null
	 * @param defaultMemo
	 * @param defaultDescription
	 */
	public void matchAndFill(T entryData, Transaction transaction, Entry entry1, String defaultMemo, String defaultDescription) {
   		for (MemoPattern pattern: sortedPatterns) {
   			
			boolean unmatchedFound = false;
			Object [] args = null;
			for (ImportEntryProperty<T> importEntryProperty : importEntryProperties) {
				String importEntryPropertyValue = importEntryProperty.getCurrentValue(entryData);
				Pattern compiledPattern = pattern.getCompiledPattern(importEntryProperty.id);

				if (compiledPattern != null && importEntryPropertyValue != null) {
					Matcher m = compiledPattern.matcher(importEntryPropertyValue);
					if (!m.matches()) {
						unmatchedFound = true;
						break;
					}

					/*
					 * Only 'memo' provides arguments.
					 */
					if (importEntryProperty.id.equals("memo")) {
						/*
						 * Group zero is the entire string and the groupCount method
						 * does not include that group, so there is really one more group
						 * than the number given by groupCount.
						 *
						 * This code also tidies up the imported text.
						 */
						args = new Object[m.groupCount()+1];
						for (int i = 0; i <= m.groupCount(); i++) {
							// Not sure why it can be null, but it happened...
							args[i] = m.group(i) == null ? null : ImportMatcher.convertToMixedCase(m.group(i));
						}
					}
				}
			}
			
			if (!unmatchedFound) {
   				String transactionId = pattern.getTransactionTypeId();
   				
   				TransactionType transactionType = null;
   				for (TransactionType type : applicableTransactionTypes) {
   					if (type.getId().equals(transactionId)) {
   						transactionType = type;
   						break;
   					}
   				}
 
   				transactionType.createTransaction(transaction, entry1, entryData, pattern, args);

   				break;
   			}
   		}

		/*
		 * If nothing matched, set the default account, the memo, and the
		 * description (the memo in the other account) but no other property.
		 * 
		 * The account may already have been set.  That will be the case, for
		 * example, in the Paypal import because the default account depends on
		 * the currency.
		 */
//   		if (entry2.getAccount() == null) {
//   			entry2.setAccount(account.getDefaultCategory());
//   			if (entry1 != null) {
//   				entry1.setMemo(defaultMemo == null ? null : convertToMixedCase(defaultMemo));
//   			}
//			entry2.setMemo(defaultDescription == null ? null : convertToMixedCase(defaultDescription));
//   		}

		entry1.setMemo(defaultMemo == null ? null : convertToMixedCase(defaultMemo));

		Entry otherEntry = transaction.createEntry();
		otherEntry.setAccount(account.getDefaultCategory());
		otherEntry.setMemo(defaultDescription == null ? null : convertToMixedCase(defaultDescription));
		otherEntry.setAmount(-entryData.amount);
	}

	/**
	 * This method tidies up the imported text.
	 * <P>
	 * Most banks put everything in upper case, so those are converted to mixed case.
	 * Furthermore we replace multiple spaces with a comma followed by a single
	 * space.
	 *
	 * @param uppperCaseText
	 * @return
	 */
	public static String convertToMixedCase(String uppperCaseText) {
		StringBuffer x = new StringBuffer();

		boolean lastWasLetter = false;
		int numberOfSpaces = 0;
		char [] array = uppperCaseText.toCharArray();
		for (int i = 0; i < array.length; i++) {
			char y = array[i];
			
			if (y == ' ') {
				numberOfSpaces++;
				lastWasLetter = false;
			} else {
				if (numberOfSpaces > 0) {
					if (numberOfSpaces > 1) {
						x.append(',');
					}
					x.append(' ');
					numberOfSpaces = 0;
				}

				if (Character.isLetter(y)) {
					if (lastWasLetter) {
						x.append(Character.toLowerCase(y));
					} else {
						/*
						 * Start of a new word.  We check for some special short
						 * words that we don't want to capitalize.
						 */
						boolean isSpecial = false;
						String [] specialWords = new String[] { "at", "on", "for", "and" };
						for (String specialWord : specialWords) {
							
							int endIndex = i + specialWord.length();
							if (uppperCaseText.substring(i).toLowerCase().startsWith(specialWord)
								&& endIndex < array.length && !Character.isLetter(array[endIndex])) {
								isSpecial = true;
								break;
							}
						}
						if (isSpecial) {
							x.append(Character.toLowerCase(y));
						} else {
						x.append(y);
						}
					}
					lastWasLetter = true;
				} else {
					x.append(y);
					lastWasLetter = false;
				}
			}
		}
		return x.toString();
	}

	/**
	 *
	 * This method looks to see if there is already an entry that matches this entry.  If there is then it is assumed to
	 * be the same entry.  The other entry may already be there because:
	 * <UL>
	 * <LI> This import file or a file with an overlapping import range has already been imported</LI>
	 * <LI> This entry was entered manually</LI>
	 * <LI> Entries for another account were imported and this entry was the other entry for a transfer</LI>
	 * </UL>
	 * The only exception is that if a matching entry is found in this import then we do import the duplicate.  After all,
	 * if an import contains two identical transactions then it is likely a genuine duplicate.  The ourEntries set is
	 * used to detect this situation.
	 * 
	 * @param entryData
	 * @param transactionManager
	 * @param session
	 * @param ourEntries a set of entries that have been added in this import up to now, being a read-only set
	 * @return the entry for this transaction.
	 */
	public Entry process(T entryData, Session session, final Set<Entry> ourEntries) {
		// Fill the fields from the entry.  This is for convenience
		// so other places just use the EntryData fields.  This needs
		// cleaning up.
		if (entryData.entry != null) {
			entryData.fillFromEntry();
		}

		
		// No auto-matching if the transaction has already been created.
		// It will just find itself!
		// TODO auto-matching should be done earlier if other processes/imports
		// are putting entries into the Paypal accounts.
		if (entryData.entry == null) {
			/*
			 * First we try auto-matching.
			 *
			 * If we have an auto-match then we don't have to create a new
			 * transaction at all. We just update a few properties in the
			 * existing entry.
			 */
			Entry matchedEntry = entryData.findMatch(account.getBaseObject(), 5, ourEntries);	
			if (matchedEntry != null) {
				entryData.setDataIntoExistingEntry(matchedEntry);
				return matchedEntry;
			}
		}
		
		/*
		 * Two possibilities here.  If there is an 'entry' then the
		 * transaction has already been created and we should use it.
		 * If no 'entry' then we must create a transaction.
		 * 
		 * TODO tidy this up by always creating the transaction before
		 * calling this method.
		 */
		Transaction transaction;
		Entry entry1;
		if (entryData.entry == null) {
			transaction = session.createTransaction();
			entry1 = transaction.createEntry();
			entry1.setAccount(account.getBaseObject());
			
			// Set values that don't depend on matching
			entryData.assignPropertyValues(transaction, entry1);
		} else {
			transaction = entryData.entry.getTransaction();
			entry2 = entryData.entry;
			
			// If the transaction was already created then we don't update
			// properties in the other entry.
			entry1 = null;
		}

   		/*
   		 * Scan for a match in the patterns.  If a match is found,
   		 * use the values for memo, description etc. from the pattern.
   		 */
		matchAndFill(entryData, transaction, entry1, entryData.getDefaultMemo(), entryData.getDefaultDescription());

   		return entry1;
	}
}
