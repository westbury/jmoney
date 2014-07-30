package net.sf.jmoney.importer.matcher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.jmoney.importer.MatchingEntryFinder;
import net.sf.jmoney.importer.model.MemoPattern;
import net.sf.jmoney.importer.model.PatternMatcherAccount;
import net.sf.jmoney.importer.model.ReconciliationEntryInfo;
import net.sf.jmoney.importer.model.TransactionType;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.Session;
import net.sf.jmoney.model2.Transaction;

public class ImportMatcher {

	private PatternMatcherAccount account;

	private List<MemoPattern> sortedPatterns;

	private List<ImportEntryProperty> importEntryProperties;

	private List<TransactionType> applicableTransactionTypes;

	public ImportMatcher(PatternMatcherAccount account, List<ImportEntryProperty> importEntryProperties, List<TransactionType> applicableTransactionTypes) {
		this.account = account;
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
	public void matchAndFill(EntryData entryData, Entry entry1, Entry entry2, String defaultMemo, String defaultDescription) {
   		for (MemoPattern pattern: sortedPatterns) {
   			
			boolean unmatchedFound = false;
			Object [] args = null;
			for (ImportEntryProperty importEntryProperty : importEntryProperties) {
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

   				transactionType.createTransaction(entry1, entry2, pattern, args);

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
   		if (entry2.getAccount() == null) {
   			entry2.setAccount(account.getDefaultCategory());
   			if (entry1 != null) {
   				entry1.setMemo(defaultMemo == null ? null : convertToMixedCase(defaultMemo));
   			}
			entry2.setMemo(defaultDescription == null ? null : convertToMixedCase(defaultDescription));
   		}
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
	 * @param entryData
	 * @param transactionManager
	 * @param session
	 * @param statement
	 * @return the entry for this transaction.
	 */
	public Entry process(net.sf.jmoney.importer.matcher.EntryData entryData, Session session) {
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
		Date importedDate = (entryData.valueDate != null)
		? entryData.valueDate
				: entryData.clearedDate;

		MatchingEntryFinder matchFinder = new MatchingEntryFinder() {
			@Override
			protected boolean alreadyMatched(Entry entry) {
				return ReconciliationEntryInfo.getUniqueIdAccessor().getValue(entry) != null;
			}
		};
		Entry matchedEntry = matchFinder.findMatch(account.getBaseObject(), entryData.amount, importedDate, 5, entryData.check);
		if (matchedEntry != null) {
			matchedEntry.setValuta(importedDate);
			matchedEntry.setCheck(entryData.check);
			// TODO is this line correct?
			ReconciliationEntryInfo.getUniqueIdAccessor().setValue(matchedEntry, entryData.uniqueId);
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
		Entry entry2;
		if (entryData.entry == null) {
			transaction = session.createTransaction();
			entry1 = transaction.createEntry();
			entry2 = transaction.createEntry();
			entry1.setAccount(account.getBaseObject());
			
			// Set values that don't depend on matching
			entryData.assignPropertyValues(transaction, entry1, entry2);
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
		matchAndFill(entryData, entry1, entry2, entryData.getDefaultMemo(), entryData.getDefaultDescription());

   		return entry1;
	}
}
