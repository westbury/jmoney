package org.eclipse.core.internal.databinding.provisional.bind;

import org.eclipse.core.runtime.IStatus;

/**
 * @since 1.5
 * 
 * @param <T1>
 */
public class DefaultValueBinding<T1> extends TwoWayBinding<T1> implements
		ITargetBinding<T1> {

	private final IOneWayModelBinding<T1> modelBinding;

	private boolean stopped = false;

	/**
	 * @param modelBinding
	 */
	public DefaultValueBinding(IOneWayModelBinding<T1> modelBinding) {
		super(true);
		this.modelBinding = modelBinding;
	}

	@Override
	public T1 getModelValue() {
		return modelBinding.getModelValue();
	}

	@Override
	public void setTargetValue(T1 valueOnModelSide) {
		targetBinding.setTargetValue(valueOnModelSide);
	}

	@Override
	public void setStatus(IStatus status) {
		/*
		 * Generally there are no status values sent from the model to the
		 * target because the model is generally valid. However there may be
		 * cases when error or warning statuses come from the model. For example
		 * when using JSR-303 validations the validation is done on the value in
		 * the model object. In any case we just pass it on.
		 */
		targetBinding.setStatus(status);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.core.databinding.bind.IModelBinding#setModelValue(java.lang
	 * .Object)
	 */
	@Override
	public void setModelValue(T1 newValue) {
		/*
		 * The target has changed so stop this binding. The target will continue
		 * to notify us of changes for as long as it exists so we need to set a
		 * flag to indicate that this binding is in a stopped state.
		 */
		if (!stopped) {
			stopped = true;
			modelBinding.removeModelListener();
		}
	}

	@Override
	public void removeModelListener() {
		/*
		 * Pass the request back to the next link in the binding chain so
		 * eventually the request gets back to the model observable.
		 * 
		 * Note that if we are in a 'stopped' state then the listener has
		 * already been removed from the model and we should not attempt to
		 * remove it again.
		 */
		if (!stopped) {
			modelBinding.removeModelListener();
		}
	}
}