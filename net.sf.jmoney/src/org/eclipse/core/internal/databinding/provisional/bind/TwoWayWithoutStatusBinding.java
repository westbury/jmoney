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

import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.property.value.IValueProperty;
import org.eclipse.core.runtime.CoreException;

/**
 * This is a wrapper class. The purpose is to restrict the user to use only
 * methods in the given TwoWayBinding class that are applicable when there
 * cannot be any error statuses.
 * 
 * @param <T2>
 * @since 3.3
 *
 */
public class TwoWayWithoutStatusBinding<T2> implements ITwoWayBinding<T2> {

	private TwoWayBinding<T2> underlyingBinding;

	/**
	 * @param underlyingBinding
	 */
	public TwoWayWithoutStatusBinding(TwoWayBinding<T2> underlyingBinding) {
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
	public <T3> ITwoWayBinding<T3> convert(IBidiConverter<T2, T3> converter) {
		TwoWayBinding<T3> nextBinding = underlyingBinding.convert(converter);
		return new TwoWayWithoutStatusBinding<T3>(nextBinding);
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
	public <T3> ITwoWayBinding<T3> convertWithTracking(
			IBidiConverter<T2, T3> converter) {
		TwoWayBinding<T3> nextBinding = underlyingBinding
				.convertWithTracking(converter);
		return new TwoWayWithoutStatusBinding<T3>(nextBinding);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.core.internal.databinding.provisional.bind.ITwoWayBinding#
	 * convertWithTracking(org.eclipse.core.internal.databinding.provisional.
	 * bind.IBidiWithStatusConverter)
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
	 * convertWithTracking(org.eclipse.core.internal.databinding.provisional.
	 * bind.IBidiWithExceptionConverter)
	 */
	@Override
	public <T3> ITwoWayWithStatusBinding<T3> convertWithTracking(
			final IBidiWithExceptionConverter<T2, T3> converter) {

		IBidiWithStatusConverter<T2, T3> converterWithStatus = new IBidiWithStatusConverter<T2, T3>() {

			@Override
			public T3 modelToTarget(T2 fromObject) {
				return converter.modelToTarget(fromObject);
			}

			@Override
			public IValueWithStatus<T2> targetToModel(T3 fromObject) {
				try {
					return ValueWithStatus
							.ok(converter.targetToModel(fromObject));
				} catch (CoreException e) {
					return ValueWithStatus.error(e.getLocalizedMessage());
				}
			}

		};

		TwoWayBinding<T3> nextBinding = underlyingBinding
				.convertWithTracking(converterWithStatus);
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
	 * @see
	 * org.eclipse.core.internal.databinding.provisional.bind.ITwoWayBinding#to(
	 * org.eclipse.core.databinding.observable.value.IObservableValue)
	 */
	@Override
	public void to(IObservableValue<T2> targetObservable) {
		underlyingBinding.to(targetObservable);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.core.internal.databinding.provisional.bind.ITwoWayBinding#to(
	 * org.eclipse.core.databinding.property.value.IValueProperty,
	 * java.lang.Object)
	 */
	@Override
	public <S> void to(IValueProperty<S, T2> targetProperty, S source) {
		underlyingBinding.to(targetProperty, source);

	}

}
