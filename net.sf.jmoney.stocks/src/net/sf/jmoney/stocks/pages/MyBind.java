package net.sf.jmoney.stocks.pages;

import java.math.BigDecimal;

import org.eclipse.core.databinding.observable.value.ComputedValue;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.internal.databinding.provisional.bind.IOneWayBinding;

public class MyBind {

	/**
	 * The method starts a one-way binding from the model observable.
	 * However the binding is only in effect when the <code>activeWhen</code>
	 * observable is true.  The binding will turn on and off as the <code>activeWhen</code>
	 * observable changes between <code>true</code> and <code>false</code>.
	 * <P>
	 * The use case for this is fairly slim.  An example use-case is when further bindings
	 * depend on values of previous fields.  For example the first field may be a transaction type,
	 * and a later binding is only to be active when the transaction type is 'purchase' or 'sale'.
	 * However one could easily have coded around this by making the model observable a computed value
	 * that returns a value representing 'n/a' if the transaction type is not a purchase or sale.
	 * 
	 * 
	 * @param activeWhen
	 * @param modelObservable
	 * @return
	 */
	public static <T> IOneWayBinding<T> oneWayWhenever(
			IObservableValue<Boolean> activeWhen,
			IObservableValue<T> modelObservable) {
		// TODO Auto-generated method stub
		return null;
	}

	
	/**
	 * Default binding.
	 * 
	 * This version copes with master-detail.  Default binding might be in
	 * operation on one master element but not on another master element.  If
	 * the master observable switches to 
	 */
}
