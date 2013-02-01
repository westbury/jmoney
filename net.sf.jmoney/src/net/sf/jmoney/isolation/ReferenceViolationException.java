package net.sf.jmoney.isolation;


/**
 * This exception is throw when at attempt is made to delete an
 * object but there are references to the object.
 * <P>
 * If the data-store is backed by a database, the implementation may
 * attempt the delete in the database and catch the foreign key constraint
 * violation exception thrown by the database, re-throwing this exception.
 * If the data-store is serialized to a file then the implementation should
 * check itself for references.
 */
public class ReferenceViolationException extends Exception {
	private static final long serialVersionUID = 5464554045939430785L;

	private IExtendablePropertySet<?> propertySet;
	private String sqlErrorMessage;
	
	/**
	 * 
	 * @param propertySet
	 *            the property set associated with the table which contained the
	 *            row which was referenced
	 * @param sqlErrorMessage
	 *            the error message returned by the database which hopefully
	 *            will contain a clue as to which foreign key constraint was
	 *            violated
	 */
	public ReferenceViolationException(IExtendablePropertySet<?> propertySet, String sqlErrorMessage) {
		this.propertySet = propertySet;
		this.sqlErrorMessage = sqlErrorMessage;
	}

	public IExtendablePropertySet<?> getPropertySet() {
		return propertySet;
	}

	public String getSqlErrorMessage() {
		return sqlErrorMessage;
	}
}
