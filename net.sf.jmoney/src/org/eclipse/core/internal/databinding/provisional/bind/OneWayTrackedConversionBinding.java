package org.eclipse.core.internal.databinding.provisional.bind;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.databinding.observable.ChangeEvent;
import org.eclipse.core.databinding.observable.IChangeListener;
import org.eclipse.core.databinding.observable.IObservable;
import org.eclipse.core.databinding.observable.ObservableTracker;

/**
 * An implementation of IOneWayBinding that is similar to
 * OneWayConversionBinding (performs a one-way conversion step as part of the
 * binding chain). However it tracks any other observables that may have been
 * used in the model-to-target conversion and re-calculates the target even when
 * the model has not changed.
 * <P>
 * 
 * @since 1.5
 * 
 * @param <T2>
 * @param <T1>
 */
public class OneWayTrackedConversionBinding<T2, T1> extends OneWayBinding<T2>
		implements ITargetBinding<T1> {
	private final IOneWayModelBinding<T1> modelBinding;
	private final IConverter<T1, T2> converter;

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
	 */
	public OneWayTrackedConversionBinding(IOneWayModelBinding<T1> modelBinding,
			IConverter<T1, T2> converter) {
		this.modelBinding = modelBinding;
		this.converter = converter;
	}

	@Override
	public T2 getModelValue() {
		T1 modelValue = modelBinding.getModelValue();
		return convertAndTrack(modelValue);
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
				valueOnTargetSide.add(converter.convert(valueOnModelSide));
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
	public void removeModelListener() {
		/*
		 * Pass the request back to the next link in the binding chain so
		 * eventually the request gets back to the model observable.
		 */
		modelBinding.removeModelListener();
	}
}