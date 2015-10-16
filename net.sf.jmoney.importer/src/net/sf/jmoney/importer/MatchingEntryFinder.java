package net.sf.jmoney.importer;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;

import net.sf.jmoney.model2.CapitalAccount;
import net.sf.jmoney.model2.Entry;

public abstract class MatchingEntryFinder {

	/**
	 * It is probably more efficient if the caller calls this method with
	 * an account that is outside a transaction.  The returned entry would
	 * then also be outside a transaction.  For that reason this method does not update the matched entry.
	 * It is the caller's responsibility to update the matched entry with any extra
	 * data from the import.
	 * <P>
	 * An entry auto-matches if:
	 * <UL>
	 *  <LI>The amount exactly matches</LI>
	 *  <LI>The entry has no unique id set</LI>
	 *  <LI>If a check number is specified in the existing entry then
	 * it must match a check number in the import (but if no check
	 * number is in the existing entry, that is ok)</LI>
	 * <LI>The date must be either exactly equal,
	 * or it can be up to 10 days in the future but it can only be
	 * in the future if there is a check number match. This allows,
	 * say, a check to match that is likely not going to appear till
	 * a few days later.</LI>
	 * <UL>
	 * <P>
	 * or it can be up to 1 day in the future but only if there
	 * are no other entries that match. This restriction prevents a
	 * false match when there are lots of charges for the same
	 * amount very close together (e.g. consider a cup of coffee
	 * charged every day or two)
	 * 
	 * @param account
	 * @param amount
	 * @param startSearchDate
	 * @param numberOfDays the number of days following the given postedDate
	 * 		to look for the entry (this value is ignored when a check number
	 * 		is present in which case 20 days are always allowed)
	 * @param checkNumber
	 * @return the matching entry if one is found, otherwise null
	 */
	public Entry findMatch(CapitalAccount account, long amount, Date startSearchDate, int numberOfDays, String checkNumber) {
		Collection<Entry> possibleMatches = new ArrayList<Entry>();
		for (Entry entry : account.getEntries()) {
			if (!doNotConsiderEntryForMatch(entry)
					&& entry.getAmount() == amount) {
				Date date = entry.getTransaction().getDate();
				if (entry.getCheck() == null) {
					if (date.equals(startSearchDate)) {
						possibleMatches.add(entry);

						/*
						 * Date exactly matched - so we can quit
						 * searching for other matches. (If user entered
						 * multiple entries with same check number then
						 * the user will not be surprised to see an
						 * arbitrary one being used for the match).
						 */
						break;
					} else {
						Calendar calendar = Calendar.getInstance();
						calendar.setTime(startSearchDate);
						calendar.add(Calendar.DAY_OF_MONTH, numberOfDays);
						Date endSearchDate = calendar.getTime();

						if ((checkNumber == null || checkNumber.length() == 0) 
								&& !date.before(startSearchDate)
										&& !date.after(endSearchDate)) {
							// Auto-reconcile
							possibleMatches.add(entry);
						}
					}
				} else {
					// A check number is present
					Calendar twentyDaysLater = Calendar.getInstance();
					twentyDaysLater.setTime(date);
					twentyDaysLater.add(Calendar.DAY_OF_MONTH, 20);

					if (entry.getCheck().equals(checkNumber)
							&& (startSearchDate.equals(date)
									|| startSearchDate.after(date))
									&& startSearchDate.before(twentyDaysLater.getTime())) {
						// Auto-reconcile
						possibleMatches.add(entry);

						/*
						 * Check number matched - so we can quit
						 * searching for other matches. (If user entered
						 * multiple entries with same check number then
						 * the user will not be surprised to see an
						 * arbitrary one being used for the match).
						 */
						break;
					}
				}
			}
		}

		if (possibleMatches.size() == 1) {
			return possibleMatches.iterator().next();
		} else {
			return null;
		}
	}

	/**
	 * This method indicates if the given entry has already been matched to an
	 * imported entry.
	 * <P>
	 * Note that if the same entry is imported using two different importers
	 * then one importer will not be aware that the entry has been matched using
	 * the other importer. For example, if an entry is imported using QIF format
	 * and then later the same entry is imported using OFX format then this
	 * method may not detect that. This is not a serious problem, it just means
	 * matching on the second format will fall back to match on other fields
	 * such as the date and amount, and there is a risk of a duplicate entry.
	 * 
	 * @param entry
	 * @return true if the given entry is already marked as being matched to an
	 *         imported entry using this same importer
	 */
	abstract protected boolean doNotConsiderEntryForMatch(Entry entry);
}
