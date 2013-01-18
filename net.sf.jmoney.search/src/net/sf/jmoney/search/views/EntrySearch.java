package net.sf.jmoney.search.views;

import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.Date;
import java.util.List;

import net.sf.jmoney.JMoneyPlugin;
import net.sf.jmoney.VerySimpleDateFormat;
import net.sf.jmoney.model2.Currency;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.EntryInfo;
import net.sf.jmoney.model2.Session;
import net.sf.jmoney.model2.TransactionInfo;
import net.sf.jmoney.search.IEntrySearch;
import net.sf.jmoney.sqldirect.SQLEntriesStatement;
import net.sf.jmoney.sqldirect.SqlDirectFactory;

import org.eclipse.jface.resource.ImageDescriptor;

public class EntrySearch implements IEntrySearch {
	private String fSingularLabel;
	private String fPluralLabelPattern;
	private ImageDescriptor fImageDescriptor;
	private List<Entry> entries;

	private Date startDate;
	private Date endDate;
	private Long amount;
	private String memo;
	
    // TODO Listen to date format changes?
    private VerySimpleDateFormat dateFormat = new VerySimpleDateFormat(JMoneyPlugin.getDefault().getDateFormat());

	public EntrySearch(Date startDate, Date endDate, Long amount, String memo) {
		this.startDate = startDate;
		this.endDate = endDate;
		this.amount = amount;
		this.memo = memo;

		fImageDescriptor = null; // TODO: make an icon

		String separator = "";
		StringBuffer description = new StringBuffer();
		if (startDate != null) {
			description.append("starting on " + dateFormat.format(startDate));
			separator = ", ";
		}

		if (endDate != null) {
			description.append(separator);
			description.append("ending on " + dateFormat.format(endDate));
			separator = ", ";
		}

		if (amount != null) {
			description.append(separator);
			// TODO: Should really make user select currency in the search box
			// and use that.
        	Currency currency = JMoneyPlugin.getDefault().getSession().getDefaultCurrency(); 
			description.append("amount " + currency.format(amount));
			separator = ", ";
		}

		if (memo != null) {
			description.append(separator);
			description.append("memo contains '" + memo + "'");
			separator = ", ";
		}

		fSingularLabel = description + " - 1 match";
		fPluralLabelPattern = description + " - {0} matches";
	}

	/**
	 * Returns the full description of the search.
	 * The description set by the client where
	 * {0} will be replaced by the match count.
	 * @return the full description
	 */
	String getFullDescription() {
		if (entries.size() == 1)
			return fSingularLabel;

		// Replace "{0}" with the match count.
		return MessageFormat.format(fPluralLabelPattern, entries.size());
	}

	/**
	 * Returns a short description of the search.
	 * Cuts off after 30 characters and adds ...
	 * The description set by the client where
	 * {0} will be replaced by the match count.
	 * @return the short description
	 */
	String getShortDescription() {
		String text= getFullDescription();
		int separatorPos= text.indexOf(" - "); //$NON-NLS-1$
		if (separatorPos < 1)
			return text.substring(0, Math.min(50, text.length())) + "..."; // use first 50 characters //$NON-NLS-1$
		if (separatorPos < 30)
			return text;	// don't cut
		if (text.charAt(0) == '"')
			return text.substring(0, Math.min(30, text.length())) + "...\" - " + text.substring(Math.min(separatorPos + 3, text.length())); //$NON-NLS-1$
		return text.substring(0, Math.min(30, text.length())) + "... - " + text.substring(Math.min(separatorPos + 3, text.length())); //$NON-NLS-1$
	}
	/** 
	 * Image used when search is displayed in a list 
	 * @return the image descriptor
	 */
	public ImageDescriptor getImageDescriptor() {
		return fImageDescriptor;
	}

	public List<Entry> getEntries() {
		return entries;
	}

	public void executeSearch() throws SearchException {
		/*
		 * This plug-in has an optional dependency on both
		 * the JDBC and the serialized XML plug-ins.
		 * (Should really be an optional dependency on two packages,
		 * in case others decide to write different storage plug-ins).
		 * 
		 * It may be that both plug-ins are present.  We look at
		 * the session to see which plug-in is supporting the session.
		 */

		Session session = JMoneyPlugin.getDefault().getSession();
		
		/*
		 * We should be able to get the package needed for SQL
		 * direct access.  The datastore plug-in is not compliant
		 * if we can't.
		 */
		try {
			String entryTableName = EntryInfo.getPropertySet().getId().replace('.', '_');
			String transactionTableName = TransactionInfo.getPropertySet().getId().replace('.', '_');
			
			StringBuffer whereClause = new StringBuffer();
			String separator = "";
			
			if (startDate != null) {
				whereClause.append(separator);
				whereClause.append("\"date\" >= ? ");
				separator = "and ";
			}
			
			if (endDate != null) {
				whereClause.append(separator);
				whereClause.append("\"date\" <= ? ");
				separator = "and ";
			}
			
			if (amount != null) {
				whereClause.append(separator);
				whereClause.append("abs(\"amount\") = ? ");
				separator = "and ";
			}
			
			if (memo != null) {
				whereClause.append(separator);
				whereClause.append("\"memo\" like ? ");
				separator = "and ";
			}
			
			if (whereClause.length() == 0) {
				throw new SearchException("Query Failed.  No restriction has been entered.  You must have at least one restriction.", null);
			}
			
			String sql = "SELECT * FROM " + entryTableName +
				" join " + transactionTableName + " ON net_sf_jmoney_transaction.\"_ID\" = net_sf_jmoney_entry.\"net_sf_jmoney_transaction_entry\"" +
				" WHERE " + whereClause + "order by \"date\"";
			
			SQLEntriesStatement statement = SqlDirectFactory.getEntriesStatement(session, sql);

			int index = 1;
			if (startDate != null) {
				statement.setDate(index++, new java.sql.Date(startDate.getTime()));
			}
			if (endDate != null) {
				statement.setDate(index++, new java.sql.Date(endDate.getTime()));
			}
			if (amount != null) {
				statement.setLong(index++, amount);
			}
			if (memo != null) {
				statement.setString(index++, "%" + memo + "%");
			}
			
			entries = statement.execute();
		} catch (SQLException e) {
			throw new SearchException("An SQL Exception has occured.", e);
		} catch (NoClassDefFoundError e) {
			/*
			 * Search the long way, which is the correct way if the entire
			 * session has been read into memory (such as is done by the
			 * Serialized datastore plug-in), and is the best we can do if some
			 * other storage mechanism.
			 */
			throw new SearchException("Query Failed.  This query has been implemented only for SQL databases.  You are not using an SQL database.  Please implement this feature and try again.", null);
		}
	}
	
	@Override
	public boolean equals(Object other) {
		if (!(other instanceof EntrySearch)) {
			return false;
		}
		
		EntrySearch otherSearch = (EntrySearch)other;
		return equals(startDate, otherSearch.startDate)
			&& equals(endDate, otherSearch.endDate)
			&& equals(amount, otherSearch.amount)
			&& equals(memo, otherSearch.memo);
	}
	
	@Override
	public int hashCode() {
		int result = 0;
		if (startDate != null) {
			result += startDate.hashCode();
		}
		if (endDate != null) {
			result += endDate.hashCode();
		}
		if (amount != null) {
			result += amount.hashCode();
		}
		if (memo != null) {
			result += memo.hashCode();
		}
		return result;
	}
	
	private boolean equals(Object object1, Object object2) {
		if (object1 == null) {
			return (object2 == null);
		} else {
			return object1.equals(object2);
		}
	}

	@Override
	public String getLabel() {
		return getShortDescription();
	}

	@Override
	public String getTooltip() {
		return getFullDescription();
	}

	@Override
	public boolean isQueryRunning() {
		// TODO: Should we just remove this?
		return false;
	}
}

