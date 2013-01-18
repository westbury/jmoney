package net.sf.jmoney.fields;

/**
 * This interface defines methods that convert an amount to
 * and from a string representation suitable for display to
 * the user or input from the user.
 * 
 * All commodity objects can provide an implementation of this
 * interface.  This allows the format of an amount to depend on
 * the commodity to which the amount refers.  For example, if
 * the commodity were US dollars then 34125 may be formatted as
 * 341.25.  On the other hand if the commodity were a stock then
 * the same amount may be formatted as 3.41 1/4.
 * 
 * It is possible that the user may enter an amount before entering
 * the commodity to which the amount refers.  To allow this, an
 * implementation of this interface may also be provided by other
 * classes such as an account class. 
 */
public interface IAmountFormatter {

	String format(long longValue);

	long parse(String amountString);

}
