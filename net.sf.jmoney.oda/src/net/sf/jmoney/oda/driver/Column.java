package net.sf.jmoney.oda.driver;


public abstract class Column {
	
	String name;
	String displayName;
	Class classOfValueObject;
	boolean isNullAllowed;
	
	public Column(String name, String displayName, Class classOfValueObject, boolean isNullAllowed) {
		this.name = name;
		this.displayName = displayName;
		this.classOfValueObject = classOfValueObject;
		this.isNullAllowed = isNullAllowed;
	}

	public String getName() {
		return name;
	}

	public String getDisplayName() {
		return displayName;
	}

	public Class getClassOfValueObject() {
		return classOfValueObject;
	}

	public boolean isNullAllowed() {
		return isNullAllowed;
	}
	
	/**
	 * This method returns double
	 * (not long) for all commodity types.  It looks to the commodity object
	 * which the amount represents to see how many decimal places are maintained.
	 * 
	 * As the method for obtaining the commodity depends on the amount property,
	 * we provide only a default implementation which does no conversion.  This
	 * method should be overwritten for all amounts as otherwise ODA will return
	 * them as long values and not doubles.
	 * 
	 * @return
	 */
	abstract Object getValue();
}
