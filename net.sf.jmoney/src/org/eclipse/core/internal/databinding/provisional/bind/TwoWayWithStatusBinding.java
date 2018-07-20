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
 * This is a wrapper class. The purpose is to restrict the user to use only
 * methods in the given TwoWayBinding class that are applicable when there may
 * be error statuses that must be shown to the user.
 * 
 * @param <T2>
 * @since 3.3
 *
 */
public class TwoWayWithStatusBinding<T2>
		implements ITwoWayWithStatusBinding<T2> {

	private TwoWayBinding<T2> underlyingBinding;

	/**
	 * @param underlyingBinding
	 */
	public TwoWayWithStatusBinding(TwoWayBinding<T2> underlyingBinding) {
		this.underlyingBinding = underlyingBinding;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.core.internal.databinding.provisional.bind.ITwoWayBinding#
	 * convert(org.eclipse.core.internal.databinding.provisional.bind.
	 * IBidiConverter)
	 */
	@Override
	public <T3> ITwoWayWithStatusBinding<T3> convert(
			IBidiConverter<T2, T3> converter) {
		TwoWayBinding<T3> nextBinding = underlyingBinding.convert(converter);
		return new TwoWayWithStatusBinding<T3>(nextBinding);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.core.internal.databinding.provisional.bind.ITwoWayBinding#
	 * convert(org.eclipse.core.internal.databinding.provisional.bind.
	 * IBidiWithStatusConverter)
	 */
	@Override
	public <T3> ITwoWayWithStatusBinding<T3> convert(
			IBidiWithStatusConverter<T2, T3> converter) {
		TwoWayBinding<T3> nextBinding = underlyingBinding.convert(converter);
		return new TwoWayWithStatusBinding<T3>(nextBinding);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.core.internal.databinding.provisional.bind.ITwoWayBinding#
	 * convertWithTracking(org.eclipse.core.internal.databinding.provisional.
	 * bind.IBidiConverter)
	 */
	@Override
	public <T3> ITwoWayWithStatusBinding<T3> convertWithTracking(
			IBidiConverter<T2, T3> converter) {
		TwoWayBinding<T3> nextBinding = underlyingBinding
				.convertWithTracking(converter);
		return new TwoWayWithStatusBinding<T3>(nextBinding);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.core.internal.databinding.provisional.bind.ITwoWayBinding#
	 * convertWithTracking(org.eclipse.core.internal.databinding.provisional.
	 * bind.IBidiConverter)
	 */
	@Override
	public <T3> ITwoWayWithStatusBinding<T3> convertWithTracking(
			IBidiWithStatusConverter<T2, T3> converter) {
		TwoWayBinding<T3> nextBinding = underlyingBinding
				.convertWithTracking(converter);
		return new TwoWayWithStatusBinding<T3>(nextBinding);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.core.internal.databinding.provisional.bind.ITwoWayBinding#
	 * validate(org.eclipse.core.databinding.validation.IValidator)
	 */
	@Override
	public ITwoWayWithStatusBinding<T2> validate(IValidator<T2> validator) {
		TwoWayBinding<T2> nextBinding = underlyingBinding.validate(validator);
		return new TwoWayWithStatusBinding<T2>(nextBinding);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.internal.databinding.provisional.bind.
	 * ITwoWayWithStatusBinding#spinoffStatus(java.util.function.Consumer)
	 */
	@Override
	public ITwoWayBinding<T2> spinoffStatus(Consumer<IStatus> statusConsumer) {
		TwoWayBinding<T2> nextBinding = underlyingBinding
				.spinoffStatus(statusConsumer);
		return new TwoWayWithoutStatusBinding<T2>(nextBinding);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.internal.databinding.provisional.bind.
	 * ITwoWayWithStatusBinding#splitStatus(java.util.function.Consumer)
	 */
	@Override
	public ITwoWayWithStatusBinding<T2> splitStatus(
			Consumer<IStatus> statusConsumer) {
		TwoWayBinding<T2> nextBinding = underlyingBinding
				.splitStatus(statusConsumer);
		return new TwoWayWithStatusBinding<T2>(nextBinding);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.internal.databinding.provisional.bind.
	 * ITwoWayWithStatusBinding#to(org.eclipse.core.databinding.observable.value
	 * .IObservableValue, java.util.function.Consumer)
	 */
	@Override
	public void to(IObservableValue<T2> targetObservable,
			Consumer<IStatus> statusConsumer) {
		underlyingBinding.to(targetObservable, statusConsumer);
	}

}
