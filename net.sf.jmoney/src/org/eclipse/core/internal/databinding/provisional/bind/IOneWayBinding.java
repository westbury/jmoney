package org.eclipse.core.internal.databinding.provisional.bind;

import java.util.function.Consumer;

import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.property.value.IValueProperty;

/**
 * This interface is used to chain together one-way binding.
 * <P>
 * To create a one-way binding, first create an implementation of this interface
 * from an observable on the model value, e.g.: <code>
 * Bind.oneWay(modelObservable)
 * </code> then call methods on this interface to perform conversions or other
 * supported operations. Each of these methods returns an implementation of this
 * interface so these operations can be chained together. Finally call the
 * <code>to</code> method to bind to the target observable.
 * 
 * @since 1.5
 * @param <T1>
 */
public interface IOneWayBinding<T1> {

	/**
	 * @param converter
	 * @return the value converted to the type expected by the next part of the
	 *         binding chain
	 */
	<T2> IOneWayBinding<T2> convert(IConverter<T1, T2> converter);

	/**
	 * This method is similar to <code>convert</code>. However if any
	 * observables are read during the conversion then listeners are added to
	 * these observables and the conversion is done again.
	 * <P>
	 * The conversion is always repeated keeping the same value of the model. It
	 * is assumed that the tracked observables affect the target. For example
	 * suppose a time widget contains a time which is bound to a Date property
	 * in the model. The time zone to use is a preference and an observable
	 * exists for the time zone (which would implement IObservableValue
	 * <TimeZone>). If the user changes the time zone in the preferences then
	 * the text in the time widget will change to show the same time but in a
	 * different time zone. The time in the model will not change when the time
	 * zone is changed. If the user edits the time in the time widget then that
	 * time will be interpreted using the new time zone and converted to a Date
	 * object for the model.
	 * 
	 * @param converter
	 * @return an object that can chain one-way bindings
	 */
	<T2> IOneWayBinding<T2> convertWithTracking(IConverter<T1, T2> converter);

	/**
	 * When chaining together one-way binding operations, this method must be
	 * last. It binds to the target.
	 * <P>
	 * The target observable is not disposed when this binding is disposed.
	 * 
	 * @param targetObservable
	 */
	void to(IObservableValue<T1> targetObservable);

	/**
	 * When chaining together one-way binding operations, this method must be
	 * last. It binds to the target.
	 * 
	 * This is a convenience method that creates an observable for the target
	 * from the given property and source. The observable will be disposed when
	 * the binding is disposed.
	 * 
	 * @param targetProperty
	 * @param source
	 */
	<S> void to(IValueProperty<S, T1> targetProperty, S source);

	/**
	 * @param targetObservable
	 */
	void to(Consumer<T1> targetObservable);

	/**
	 * This method is used to create a one-way binding from the model to the
	 * target but the binding stops if something else changes the target.
	 * <P>
	 * A use case is when a default value is provided in a UI control. For
	 * example suppose you have two fields, 'amount' and 'sales tax'. When the
	 * user enters an amount, the requirement is that the 'sales tax' field is
	 * completed based on the amount using a given tax rate. If the user edits
	 * the amount, the sales tax amount changes accordingly. However once the
	 * user edits the sales tax field then changes to the amount field no longer
	 * affect the sales tax field.
	 * 
	 * @return an object that can chain two-way bindings (although this is a
	 *         one-way binding, the binding onwards to the target must be
	 *         two-way so that we know when the user changes the target)
	 */
	ITwoWayBinding<T1> untilTargetChanges();
}
