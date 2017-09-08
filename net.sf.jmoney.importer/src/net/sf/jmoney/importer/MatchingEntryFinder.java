package net.sf.jmoney.importer;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;

import net.sf.jmoney.model2.Account;
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
	 * @param checkNumber the number of the check if this is a check payment
	 * 			and the check number is known, otherwise null
	 * @return the matching entry if one is found, otherwise null
	 */
	public Entry findMatch(Account account, long amount, Date startSearchDate) {
		Collection<Entry> possibleMatches = new ArrayList<Entry>();
		for (Entry entry : account.getEntries()) {
			if (!doNotConsiderEntryForMatch(entry)
					&& entry.getAmount() == amount) {
				Date date = entry.getTransaction().getDate();
				if (nearEnoughMatches(date, startSearchDate, entry)){
					possibleMatches.add(entry);
				}
			}
		}

		/*
		 * Find the earliest. If multiple matches have the same day then we
		 * pick an arbitrary one. Note that as we generally set an id when
		 * we match, and that id will stop the transaction being a possible
		 * match again.
		 */
		return possibleMatches.stream()
			.sorted((entry1, entry2) -> entry1.getTransaction().getDate().compareTo(entry2.getTransaction().getDate()))
					.findFirst().orElse(null);
	}

	/**
	 * 
	 * @param date
	 * @param startSearchDate the first date to search
	 * @param numberOfDays the number of days to search, including
	 * 			both the first and last day search
	 * @return
	 */
	protected boolean isDateInRange(Date date, Date startSearchDate, int numberOfDays) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(startSearchDate);
		calendar.add(Calendar.DAY_OF_MONTH, numberOfDays);
		Date endSearchDate = calendar.getTime();

		return !date.before(startSearchDate)
				&& date.before(endSearchDate);
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

	abstract protected boolean nearEnoughMatches(Date dateOfExistingTransaction, Date dateInImport, Entry entry);

}
