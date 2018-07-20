package org.eclipse.core.internal.databinding.provisional.bind;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.databinding.observable.ChangeEvent;
import org.eclipse.core.databinding.observable.IChangeListener;
import org.eclipse.core.databinding.observable.IObservable;
import org.eclipse.core.databinding.observable.ObservableTracker;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

/**
 * An implementation of ITwoWayBinding that is similar to
 * TwoWayConversionBinding (performs a two-way conversion step as part of the
 * binding chain). However it tracks any other observables that may have been
 * used in the model-to-target conversion and re-calculates the target even when
 * the model has not changed.
 * <P>
 * The conversion is always repeated keeping the same value of the model. It is
 * assumed that the tracked observables affect the target. For example suppose a
 * time widget contains a time which is bound to a Date property in the model.
 * The time zone to use is a preference and an observable exists for the time
 * zone (which would implement IObservableValue<TimeZone>). If the user changes
 * the time zone in the preferences then the text in the time widget will change
 * to show the same time but in a different time zone. The time in the model
 * will not change when the time zone is changed. If the user edits the time in
 * the time widget then that time will be interpreted using the new time zone
 * and converted to a Date object for the model.
 * 
 * @since 1.5
 * 
 * @param <T2>
 * @param <T1>
 */
public class TwoWayTrackedConversionBinding<T2, T1> extends TwoWayBinding<T2>
		implements ITargetBinding<T1> {
	private final IModelBinding<T1> modelBinding;
	private final IBidiWithStatusConverter<T1, T2> converter;

	/**
	 * Array of observables the model-to-target conversion depends on. This
	 * field has a value of <code>null</code> if we are not currently listening.
	 */
	private IObservable[] dependencies = null;

	/**
	 * Inner class that implements interfaces that we don't want to expose as
	 * public API. Each interface could have been implemented using a separate
	 * anonymous class, but we combine them here to reduce the memory overhead
	 * and number of classes.
	 * <p>
	 * The IChangeListener stores each observable in the dependencies list. This
	 * is registered as the listener when calling ObservableTracker, to detect
	 * every observable that is used by computeValue.
	 * <p>
	 * The IChangeListener is attached to every dependency.
	 */
	private class PrivateChangeInterface implements IChangeListener {
		@Override
		public void handleChange(ChangeEvent event) {
			/*
			 * When any of the tracked observable listeners are fired, we get
			 * the value from the model (which should be unchanged) and
			 * re-convert to the target. We then push the converted value
			 * through to the target side.
			 * 
			 * The setTargetValue method will drop all listeners and
			 * re-determine the dependencies. It should not be a problem that
			 * listeners are removed and added from within a listener because
			 * listeners are always fired on a copy of the listener list taken
			 * before the first listener is fired.
			 */
			T1 valueFromModelSide = modelBinding.getModelValue();
			setTargetValue(valueFromModelSide);
		}
	}

	private IChangeListener privateChangeInterface = new PrivateChangeInterface();

	/**
	 * @param modelBinding
	 * @param converter
	 * @param pullInitialValue
	 */
	public TwoWayTrackedConversionBinding(IModelBinding<T1> modelBinding,
			IBidiWithStatusConverter<T1, T2> converter,
			boolean pullInitialValue) {
		super(pullInitialValue);
		this.modelBinding = modelBinding;
		this.converter = converter;
	}

	/**
	 * @return the value from the model, converted for use on the target side
	 */
	@Override
	public T2 getModelValue() {
		T1 modelValue = modelBinding.getModelValue();
		return convertAndTrack(modelValue);
	}

	/**
	 * @param valueOnTargetSide
	 */
	@Override
	public void setModelValue(T2 valueOnTargetSide) {
		// try {
		IValueWithStatus<T1> valueOnModelSide = converter
				.targetToModel(valueOnTargetSide);
		if (valueOnModelSide.getSeverity() == IStatus.OK) {
			modelBinding.setModelValue(valueOnModelSide.getValue());
		} else {
			targetBinding.setStatus(
					ValidationStatus.error(valueOnModelSide.getMessage()));
		}
		targetBinding.setStatus(Status.OK_STATUS);
		// } catch (CoreException e) {
		// targetBinding.setStatus(e.getStatus());
		// }
	}

	@Override
	public void setTargetValue(T1 valueOnModelSide) {
		T2 valueOnTargetSide = convertAndTrack(valueOnModelSide);
		targetBinding.setTargetValue(valueOnTargetSide);
	}

	private T2 convertAndTrack(final T1 valueOnModelSide) {
		stopListening();

		/**
		 * Hack to get back result from Runnable.
		 */
		final List<T2> valueOnTargetSide = new ArrayList<T2>();

		/*
		 * This line will do the following: Run the calculate method and, while
		 * doing so, add any observable that is touched to the dependencies
		 * list.
		 */
		Runnable privateRunnableInterface = new Runnable() {
			@Override
			public void run() {
				valueOnTargetSide
						.add(converter.modelToTarget(valueOnModelSide));
			}
		};

		IObservable[] newDependencies = ObservableTracker.runAndMonitor(
				privateRunnableInterface, privateChangeInterface, null);
		dependencies = newDependencies;

		return valueOnTargetSide.get(0);
	}

	/**
	 * Stop listening for dependency changes.
	 */
	private void stopListening() {
		if (dependencies != null) {
			for (IObservable observable : dependencies) {
				observable.removeChangeListener(privateChangeInterface);
			}
			dependencies = null;
		}
	}

	@Override
	public void setStatus(IStatus status) {
		/*
		 * The error occurred somewhere on the model side. We push this error
		 * back to the target.
		 */
		targetBinding.setStatus(status);
	}

	@Override
	public void removeModelListener() {
		/*
		 * Pass the request back to the next link in the binding chain so
		 * eventually the request gets back to the model observable.
		 */
		modelBinding.removeModelListener();
	}
}