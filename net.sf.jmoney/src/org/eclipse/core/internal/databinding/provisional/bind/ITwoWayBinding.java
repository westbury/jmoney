package org.eclipse.core.internal.databinding.provisional.bind;

import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.property.value.IValueProperty;

/**
 * This interface is used to chain together two-way binding.
 * <P>
 * To create a two-way binding, first create an implementation of this interface
 * from an observable on the model value, e.g.: <code>
 * Bind.twoWay(modelObservable)
 * </code> then call methods on this interface to perform conversions or other
 * supported operations. Each of these methods returns an implementation of this
 * interface so these operations can be chained together. Finally call the
 * <code>to</code> method to bind to the target observable.
 * 
 * @since 1.5
 * @param <T1>
 */
public interface ITwoWayBinding<T1> {

	/**
	 * @param converter
	 * @return an object that can chain two-way bindings
	 */
	<T2> ITwoWayBinding<T2> convert(IBidiConverter<T1, T2> converter);

	/**
	 * @param converter
	 * @return an object that can chain two-way bindings
	 */
	<T2> ITwoWayWithStatusBinding<T2> convert(
			IBidiWithStatusConverter<T1, T2> converter);

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
	 * @return an object that can chain two-way bindings
	 */
	<T2> ITwoWayBinding<T2> convertWithTracking(
			IBidiConverter<T1, T2> converter);

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
	 * @return an object that can chain two-way bindings
	 */
	<T2> ITwoWayWithStatusBinding<T2> convertWithTracking(
			IBidiWithStatusConverter<T1, T2> converter);

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
	 * @return an object that can chain two-way bindings
	 */
	<T2> ITwoWayWithStatusBinding<T2> convertWithTracking(
			IBidiWithExceptionConverter<T1, T2> converter);

	/**
	 * @param validator
	 * @return status of the validation
	 */
	ITwoWayWithStatusBinding<T1> validate(IValidator<T1> validator);

	/**
	 * When chaining together two-way binding operations, this method must be
	 * last. It binds to the target observable.
	 * <P>
	 * The target observable is not disposed when this binding is disposed.
	 * 
	 * @param targetObservable
	 */
	void to(IObservableValue<T1> targetObservable);

	/**
	 * When chaining together two-way binding operations, this method must be
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

}
