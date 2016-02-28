package net.sf.jmoney.model2;

import java.util.Collection;
import java.util.Date;
import java.util.List;

import net.sf.jmoney.isolation.IDataManager;

public interface IDataManagerForAccounts extends IDataManager {

	/**
	 * Returns the session object. Objects often need access to the session
	 * object. For example, the CapitalAccount constructor sets the currency to
	 * the default currency for the session if a null currency is passed.
	 * <P>
	 * WARNING: Be very careful about the use of this method. The session will
	 * not be set until it is constructed. Therefore any code that may be called
	 * during the initial construction of a session may not call this method.
	 * This includes extendable object constructors that are being called to
	 * construct an object store from a datastore. It does not include the
	 * constructor when adding a new object. As these are the same constructors,
	 * this is very confusing.
	 * 
	 * @return the session object
	 */
	Session getSession();
	
	/**
	 * @param account
	 * @return
	 */
	boolean hasEntries(Account account);

	/**
	 * @param account
	 * @return
	 */
	Collection<Entry> getEntries(Account account);
	
	/**
	 * Gets the entries in an efficient manner.
	 * 
	 * This is intended to be efficient.  If objects are not materialized from the datastore
	 * unless needed then it would be inefficient to materialize every entry during the
	 * search.  For example, if the datastore is a SQL database then an appropriate SQL statement
	 * should be executed.  In order to support this the implementation is dependent on the
	 * datastore.
	 * @param memo 
	 * @param amount 
	 * @param endDate 
	 * @param startDate 
	 *  
	 * @return
	 */
	List<Entry> getEntries(Date startDate, Date endDate, Long amount, String memo);
}
