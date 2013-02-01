package net.sf.jmoney.model2;

import java.util.Collection;

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
	public abstract Session getSession();
	
	/**
	 * @param account
	 * @return
	 */
	public abstract boolean hasEntries(Account account);

	/**
	 * @param account
	 * @return
	 */
	public abstract Collection<Entry> getEntries(Account account);
}
