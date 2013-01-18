package net.sf.jmoney.importer.matcher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;

import net.sf.jmoney.importer.MatchingEntryFinder;
import net.sf.jmoney.importer.model.MemoPattern;
import net.sf.jmoney.importer.model.MemoPatternInfo;
import net.sf.jmoney.importer.model.PatternMatcherAccount;
import net.sf.jmoney.importer.model.ReconciliationEntryInfo;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.Session;
import net.sf.jmoney.model2.Transaction;

public class ImportMatcher {

	private PatternMatcherAccount account;
	
	private List<MemoPattern> sortedPatterns;

	public ImportMatcher(PatternMatcherAccount account) {
		this.account = account;
		
		/*
		 * Get the patterns sorted into order.  It is important that we test patterns in the
		 * correct order because an entry may match both a general pattern and a more specific
		 * pattern.
		 */
		sortedPatterns = new ArrayList<MemoPattern>(account.getPatternCollection());
		Collections.sort(sortedPatterns, new Comparator<MemoPattern>(){
			public int compare(MemoPattern pattern1, MemoPattern pattern2) {
				return pattern1.getOrderingIndex() - pattern2.getOrderingIndex();
			}
		});
		
	}

	public void matchAndFill(String text, Entry entry1, Entry entry2, String defaultMemo, String defaultDescription) {
   		for (MemoPattern pattern: sortedPatterns) {
   			Matcher m = pattern.getCompiledPattern().matcher(text);
   			System.out.println(pattern.getPattern() + ", " + text);
   			if (m.matches()) {
   				/*
   				 * Group zero is the entire string and the groupCount method
   				 * does not include that group, so there is really one more group
   				 * than the number given by groupCount.
   				 * 
   				 * This code also tidies up the imported text.
   				 */
   				Object [] args = new Object[m.groupCount()+1];
   				for (int i = 0; i <= m.groupCount(); i++) {
   					args[i] = convertToMixedCase(m.group(i));
   				}
   				
   				
   				String format = pattern.getPropertyValue(MemoPatternInfo.getCheckAccessor());
   				
   				// TODO: What effect does the locale have in the following?
   				if (pattern.getCheck() != null) {
   					entry1.setCheck(
   							new java.text.MessageFormat(
   									pattern.getCheck(), 
   									java.util.Locale.US)
   							.format(args));
   				}
   				
   				if (pattern.getMemo() != null) {
   					entry1.setMemo(
   							new java.text.MessageFormat(
   									pattern.getMemo(), 
   									java.util.Locale.US)
   							.format(args));
   				}
   				
   				if (pattern.getDescription() != null) {
       				entry2.setMemo(
       						new java.text.MessageFormat(
       								pattern.getDescription(), 
       								java.util.Locale.US)
       								.format(args));
   				}
   				
           		entry2.setAccount(pattern.getAccount());
           		
           		break;
   			}
   		}
   		
		/*
		 * If nothing matched, set the default account, the memo, and the
		 * description (the memo in the other account) but no other property.
		 */
   		if (entry2.getAccount() == null) {
   			entry2.setAccount(account.getDefaultCategory());
			entry1.setMemo(defaultMemo == null ? null : convertToMixedCase(defaultMemo));
			entry2.setMemo(defaultDescription == null ? null : convertToMixedCase(defaultDescription));
   		}
	}

	/**
	 * Most banks put everything This code tidies up the imported text. Most
	 * banks put everything in upper case, so those are converted to mixed case.
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
		for (char y : uppperCaseText.toCharArray()) {
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
						x.append(y);
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
				return entry.getPropertyValue(ReconciliationEntryInfo.getUniqueIdAccessor()) != null;
			}
		};
		Entry matchedEntry = matchFinder.findMatch(account.getBaseObject(), entryData.amount, importedDate, entryData.check);
		if (matchedEntry != null) {
			matchedEntry.setValuta(importedDate);
			matchedEntry.setCheck(entryData.check);
			// TODO is this line correct?
			matchedEntry.setPropertyValue(ReconciliationEntryInfo.getUniqueIdAccessor(), entryData.uniqueId);
			return matchedEntry;
		}
		
   		Transaction transaction = session.createTransaction();
   		Entry entry1 = transaction.createEntry();
   		Entry entry2 = transaction.createEntry();
   		entry1.setAccount(account.getBaseObject());
   		
   		/*
   		 * Scan for a match in the patterns.  If a match is found,
   		 * use the values for memo, description etc. from the pattern.
   		 */
		String text = entryData.getTextToMatch();
		matchAndFill(text, entry1, entry2, entryData.getDefaultMemo(), entryData.getDefaultDescription());
		
   		entryData.assignPropertyValues(transaction, entry1, entry2);
   		
   		return entry1;
	}
}
