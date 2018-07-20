package org.eclipse.core.internal.databinding.provisional.bind;

import java.util.function.Consumer;

import org.eclipse.core.databinding.observable.DisposeEvent;
import org.eclipse.core.databinding.observable.IDisposeListener;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.property.value.IValueProperty;
import org.eclipse.core.runtime.IStatus;

/**
 * @since 1.5
 * 
 * @param <T2>
 */
public abstract class OneWayBinding<T2>
		implements IOneWayBinding<T2>, IOneWayModelBinding<T2> {

	protected ITargetBinding<T2> targetBinding;

	@Override
	public <T3> IOneWayBinding<T3> convert(final IConverter<T2, T3> converter) {
		if (targetBinding != null) {
			throw new RuntimeException(
					"When chaining together a binding, you cannot chain more than one target."); //$NON-NLS-1$
		}

		OneWayConversionBinding<T3, T2> nextBinding = new OneWayConversionBinding<T3, T2>(
				this, converter);
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
	 * @return an object that can chain one-way bindings
	 */
	@Override
	public <T3> IOneWayBinding<T3> convertWithTracking(
			final IConverter<T2, T3> converter) {
		if (targetBinding != null) {
			throw new RuntimeException(
					"When chaining together a binding, you cannot chain more than one target."); //$NON-NLS-1$
		}

		OneWayTrackedConversionBinding<T3, T2> nextBinding = new OneWayTrackedConversionBinding<T3, T2>(
				this, converter);
		targetBinding = nextBinding;
		return nextBinding;
	}

	/**
	 * This method creates a one-way binding from model to target that stops if
	 * someone else edits the target.
	 * <P>
	 * This method is used to provide a default value. The default value is a
	 * one-way binding, typically a one-way binding from a ComputedValue. This
	 * default value is the receiver of this method. The target may be either an
	 * observable on the model or an observable on a UI control. (If you have
	 * two-way binding between the model and the UI control then you can bind a
	 * default value to either but you will typically have fewer conversions to
	 * do if you bind to the model).
	 * <P>
	 * The binding on to the target is a two-way binding. The default value is
	 * passed on to the target until such time that the target is changed by
	 * someone other than ourselves. At that point the binding stops and changes
	 * in the default value are no longer set into the target.
	 * <P>
	 * An example use is as follows: There is a field in which the user can
	 * enter the price of an item. There is another field in which the user can
	 * enter the sales tax for the sale of the item. We want the sales tax field
	 * to automatically calculate to be 5% of the price. If the user edits the
	 * price then the sales tax field should change too. The user may edit the
	 * sales tax field. If the user does this then the sales tax field will no
	 * longer be updated as the price changes.
	 */
	@Override
	public ITwoWayBinding<T2> untilTargetChanges() {
		if (targetBinding != null) {
			throw new RuntimeException(
					"When chaining together a binding, you cannot chain more than one target."); //$NON-NLS-1$
		}

		DefaultValueBinding<T2> nextBinding = new DefaultValueBinding<T2>(this);

		targetBinding = nextBinding;
		return new TwoWayWithoutStatusBinding<T2>(nextBinding);
	}

	@Override
	public void to(final IObservableValue<T2> targetObservable) {
		/*
		 * We have finally made it to the target observable.
		 * 
		 * Initially set the target observable to the current value from the
		 * model.
		 */
		targetObservable.setValue(getModelValue());

		/*
		 * The target binding contains a method that is called whenever a new
		 * value comes from the model side. We simply set a target binding that
		 * sets that value into the target observable.
		 */
		targetBinding = new ITargetBinding<T2>() {
			@Override
			public void setTargetValue(T2 targetValue) {
				targetObservable.setValue(targetValue);
			}

			@Override
			public void setStatus(IStatus status) {
				/*
				 * Generally there are no status values sent from the model to
				 * the target because the model is generally valid. However
				 * there may be cases when error or warning statuses come from
				 * the model. For example when using JSR-303 validations the
				 * validation is done on the value in the model object. In any
				 * case, there is no status observable provided by the user so
				 * we drop it.
				 */
			}
		};

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

	@Override
	public <S> void to(IValueProperty<S, T2> targetProperty, S source) {
		IObservableValue<T2> targetObservable = targetProperty.observe(source);
		to(targetObservable);
		// TODO dispose observable if binding is disposed
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.core.internal.databinding.provisional.bind.IOneWayBinding#to(
	 * java.util.function.Consumer)
	 */
	@Override
	public void to(final Consumer<T2> targetConsumer) {
		/*
		 * We have finally made it to the target consumer.
		 * 
		 * Initially set the target to the current value from the model.
		 */
		targetConsumer.accept(getModelValue());

		/*
		 * The target binding contains a method that is called whenever a new
		 * value comes from the model side. We simply set a target binding that
		 * sets that value into the target observable.
		 */
		targetBinding = new ITargetBinding<T2>() {
			@Override
			public void setTargetValue(T2 targetValue) {
				targetConsumer.accept(targetValue);
			}

			@Override
			public void setStatus(IStatus status) {
				/*
				 * Generally there are no status values sent from the model to
				 * the target because the model is generally valid. However
				 * there may be cases when error or warning statuses come from
				 * the model. For example when using JSR-303 validations the
				 * validation is done on the value in the model object. In any
				 * case, there is no status observable provided by the user so
				 * we drop it.
				 */
			}
		};
	}

	/**
	 * @param status
	 */
	public void setStatus(IStatus status) {
		targetBinding.setStatus(status);
	}
}
