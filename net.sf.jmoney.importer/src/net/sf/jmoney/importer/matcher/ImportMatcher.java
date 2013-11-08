package net.sf.jmoney.importer.matcher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;

import net.sf.jmoney.importer.MatchingEntryFinder;
import net.sf.jmoney.importer.model.MemoPattern;
import net.sf.jmoney.importer.model.PatternMatcherAccount;
import net.sf.jmoney.importer.model.ReconciliationEntryInfo;
import net.sf.jmoney.model2.Commodity;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.Session;
import net.sf.jmoney.model2.Transaction;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.PlatformUI;

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

   				if (entry1 != null && pattern.getMemo() != null) {
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

   				/*
   				 * Before setting the account, check that if a default
   				 * account was previously set then the currency is the
   				 * same.  The amount will end up being just plain wrong
   				 * if we change the currency.
   				 */
   				if (entry2.getAccount() != null) {
   					Commodity currencyBefore = entry2.getCommodity();
   	           		entry2.setAccount(pattern.getAccount());
   					Commodity currencyAfter = entry2.getCommodity();
   					if (currencyBefore != currencyAfter) {
   						MessageDialog.openError(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), "Problem Transaction", "Currency is being changed by pattern match.");
   						throw new RuntimeException("currency change on pattern match");
   					}
   				}
           		entry2.setAccount(pattern.getAccount());

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
		String text = entryData.getTextToMatch();
		matchAndFill(text, entry1, entry2, entryData.getDefaultMemo(), entryData.getDefaultDescription());

   		return entry1;
	}
}
