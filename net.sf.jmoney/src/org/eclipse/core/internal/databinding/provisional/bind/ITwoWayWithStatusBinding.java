/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/

package org.eclipse.core.internal.databinding.provisional.bind;

import java.util.function.Consumer;

import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.runtime.IStatus;

/**
 * @param <T1>
 * @since 3.3
 *
 */
public interface ITwoWayWithStatusBinding<T1> {

	/**
	 * @param converter
	 * @return an object that can chain two-way bindings
	 */
	<T2> ITwoWayWithStatusBinding<T2> convert(IBidiConverter<T1, T2> converter);

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
	<T2> ITwoWayWithStatusBinding<T2> convertWithTracking(
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
	 * @param validator
	 * @return status of the validation
	 */
	ITwoWayWithStatusBinding<T1> validate(IValidator<T1> validator);

	/**
	 * This method spins off the status to another status consumer.
	 * <P>
	 * Anything further down the chain does not have a status.
	 * 
	 * @param statusConsumer
	 * @return an instance to allow continuation of the chain of bindings
	 */
	ITwoWayBinding<T1> spinoffStatus(Consumer<IStatus> statusConsumer);

	/**
	 * This method splits off the status to the given consumer.
	 * <P>
	 * The status is still passed on further down the chain. This method allows
	 * a status to be split off and passed to, for example, a status
	 * consolidator, perhaps to show a message in a status bar of a dialog, or
	 * perhaps to determine if the 'OK' button should be enabled. As the same
	 * time the status is passed on down the chain so in can be bound to an
	 * error indicator in the final UI control.
	 * 
	 * @param statusConsumer
	 * @return an instance to allow continuation of the chain of bindings
	 */
	ITwoWayWithStatusBinding<T1> splitStatus(Consumer<IStatus> statusConsumer);

	/**
	 * When chaining together two-way binding operations, this method must be
	 * last. It binds to the target observable.
	 * <P>
	 * The target observable is not disposed when this binding is disposed.
	 * 
	 * @param targetObservable
	 * @param statusConsumer
	 */
	void to(IObservableValue<T1> targetObservable,
			Consumer<IStatus> statusConsumer);

}
