package org.eclipse.core.internal.databinding.provisional.bind;

import java.util.function.Consumer;

import org.eclipse.core.databinding.observable.DisposeEvent;
import org.eclipse.core.databinding.observable.IDisposeListener;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.IValueChangeListener;
import org.eclipse.core.databinding.observable.value.ValueChangeEvent;
import org.eclipse.core.databinding.property.value.IValueProperty;
import org.eclipse.core.runtime.IStatus;

/**
 * @since 1.5
 * 
 * @param <T2>
 */
public abstract class TwoWayBinding<T2> implements IModelBinding<T2> {

	/**
	 * <code>true</code> if the target observable bound by the <code>to</code>
	 * method is to be initially set to the value from the model side,
	 * <code>false</code> if its value is to be set only when changes are pushed
	 * from the target side
	 */
	protected boolean pullInitialValue;

	protected ITargetBinding<T2> targetBinding;

	/**
	 * @param pullInitialValue
	 *            <code>true</code> if the initial value from the model is to be
	 *            set into the target, <code>false</code> if the target must not
	 *            be set until the model value changes
	 */
	public TwoWayBinding(boolean pullInitialValue) {
		this.pullInitialValue = pullInitialValue;
	}

	/**
	 * @param converter
	 * @return interface appropriate for chaining
	 */
	public <T3> TwoWayBinding<T3> convert(
			final IBidiConverter<T2, T3> converter) {
		if (targetBinding != null) {
			throw new IllegalStateException(
					"When chaining together a binding, you cannot chain more than one target."); //$NON-NLS-1$
		}

		TwoWayConversionBinding<T3, T2> nextBinding = new TwoWayConversionBinding<T3, T2>(
				this, converter, pullInitialValue);
		targetBinding = nextBinding;
		return nextBinding;
	}

	/**
	 * @param converter
	 * @return interface appropriate for chaining
	 */
	public <T3> TwoWayBinding<T3> convert(
			final IBidiWithStatusConverter<T2, T3> converter) {
		if (targetBinding != null) {
			throw new IllegalStateException(
					"When chaining together a binding, you cannot chain more than one target."); //$NON-NLS-1$
		}

		TwoWayWithStatusConversionBinding<T3, T2> nextBinding = new TwoWayWithStatusConversionBinding<T3, T2>(
				this, converter, pullInitialValue);
		targetBinding = nextBinding;
		return nextBinding;
	}

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
	public <T3> TwoWayBinding<T3> convertWithTracking(
			final IBidiConverter<T2, T3> converter) {
		if (targetBinding != null) {
			throw new RuntimeException(
					"When chaining together a binding, you cannot chain more than one target."); //$NON-NLS-1$
		}

		IBidiWithStatusConverter<T2, T3> converter2 = new IBidiWithStatusConverter<T2, T3>() {
			@Override
			public T3 modelToTarget(T2 fromObject) {
				return converter.modelToTarget(fromObject);
			}

			@Override
			public IValueWithStatus<T2> targetToModel(T3 fromObject) {
				T2 value = converter.targetToModel(fromObject);
				return ValueWithStatus.ok(value);
			}
		};

		TwoWayTrackedConversionBinding<T3, T2> nextBinding = new TwoWayTrackedConversionBinding<T3, T2>(
				this, converter2, pullInitialValue);
		targetBinding = nextBinding;
		return nextBinding;
	}

	/**
	 * @param converter
	 * @return interface for chaining
	 */
	public <T3> TwoWayBinding<T3> convertWithTracking(
			final IBidiWithStatusConverter<T2, T3> converter) {
		if (targetBinding != null) {
			throw new RuntimeException(
					"When chaining together a binding, you cannot chain more than one target."); //$NON-NLS-1$
		}

		TwoWayTrackedConversionBinding<T3, T2> nextBinding = new TwoWayTrackedConversionBinding<T3, T2>(
				this, converter, pullInitialValue);
		targetBinding = nextBinding;
		return nextBinding;
	}

	/**
	 * @param validator
	 * @return an interface that can chain two-way bindings
	 */
	public TwoWayBinding<T2> validate(final IValidator<T2> validator) {
		if (targetBinding != null) {
			throw new UnsupportedOperationException(
					"When chaining together a binding, you cannot chain more than one target."); //$NON-NLS-1$
		}

		TwoWayValidationBinding<T2> nextBinding = new TwoWayValidationBinding<T2>(
				this, validator, pullInitialValue);
		targetBinding = nextBinding;
		return nextBinding;
	}

	/**
	 * @param statusConsumer
	 * @return an interface that can chain two-way bindings
	 */
	public TwoWayBinding<T2> spinoffStatus(Consumer<IStatus> statusConsumer) {
		if (targetBinding != null) {
			throw new UnsupportedOperationException(
					"When chaining together a binding, you cannot chain more than one target."); //$NON-NLS-1$
		}

		TwoWaySpinoffStatusBinding<T2> nextBinding = new TwoWaySpinoffStatusBinding<T2>(
				this, statusConsumer, pullInitialValue);
		targetBinding = nextBinding;
		return nextBinding;
	}

	/**
	 * @param statusConsumer
	 * @return an interface that can chain two-way bindings
	 */
	public TwoWayBinding<T2> splitStatus(Consumer<IStatus> statusConsumer) {
		if (targetBinding != null) {
			throw new UnsupportedOperationException(
					"When chaining together a binding, you cannot chain more than one target."); //$NON-NLS-1$
		}

		TwoWaySplitStatusBinding<T2> nextBinding = new TwoWaySplitStatusBinding<T2>(
				this, statusConsumer, pullInitialValue);
		targetBinding = nextBinding;
		return nextBinding;
	}

	/**
	 * @param targetObservable
	 */
	public void to(final IObservableValue<T2> targetObservable) {
		to(targetObservable, null);
	}

	/**
	 * @param targetProperty
	 * @param source
	 */
	public <S> void to(IValueProperty<S, T2> targetProperty, S source) {
		IObservableValue<T2> targetObservable = targetProperty.observe(source);
		to(targetObservable);
		// TODO dispose observable if binding is disposed
	}

	/**
	 * We have finally made it to the target observable.
	 * 
	 * Initially set the target observable to the current value from the model
	 * (if the pullInitialValue flag is set which it will be in most cases).
	 * 
	 * @param targetObservable
	 * @param statusConsumer
	 */
	public void to(final IObservableValue<T2> targetObservable,
			final Consumer<IStatus> statusConsumer) {
		if (pullInitialValue) {
			targetObservable.setValue(getModelValue());
		}

		final boolean[] isChanging = new boolean[] { false };

		/*
		 * The target binding contains a method that is called whenever a new
		 * value comes from the model side. We simply set a target binding that
		 * sets that value into the target observable.
		 */
		targetBinding = new ITargetBinding<T2>() {
			@Override
			public void setTargetValue(T2 targetValue) {
				try {
					isChanging[0] = true;
					targetObservable.setValue(targetValue);
				} finally {
					isChanging[0] = false;
				}
			}

			@Override
			public void setStatus(IStatus status) {
				/*
				 * If there is a target for the status, set it. Otherwise drop
				 * it.
				 */
				if (statusConsumer != null) {
					statusConsumer.accept(status);
				}
			}
		};

		/*
		 * Listen for changes originating from the target observable, and send
		 * those back through to the model side.
		 */
		targetObservable.addValueChangeListener(new IValueChangeListener<T2>() {
			@Override
			public void handleValueChange(ValueChangeEvent<? extends T2> event) {
				if (!isChanging[0]) {
					setModelValue(event.diff.getNewValue());
				}
			}
		});

		/*
		 * If the target is disposed, be sure to remove the listener from the
		 * model.
		 */
		targetObservable.addDisposeListener(new IDisposeListener() {
			@Override
			public void handleDispose(DisposeEvent event) {
				removeModelListener();
			}
		});
	}
}
