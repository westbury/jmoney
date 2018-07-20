package org.eclipse.core.internal.databinding.provisional.bind;

import java.util.function.Consumer;

import org.eclipse.core.runtime.IStatus;

/**
 * @since 1.5
 * 
 * @param <T>
 */
public class TwoWaySplitStatusBinding<T> extends TwoWayBinding<T>
		implements ITargetBinding<T> {
	private final IModelBinding<T> modelBinding;
	private final Consumer<IStatus> statusConsumer;

	/**
	 * @param modelBinding
	 * @param statusConsumer
	 * @param pullInitialValue
	 */
	public TwoWaySplitStatusBinding(IModelBinding<T> modelBinding,
			Consumer<IStatus> statusConsumer, boolean pullInitialValue) {
		super(pullInitialValue);
		this.modelBinding = modelBinding;
		this.statusConsumer = statusConsumer;
	}

	/**
	 * 
	 * @return the value from the model
	 */
	@Override
	public T getModelValue() {
		return modelBinding.getModelValue();
	}

	/**
	 * @param valueOnTargetSide
	 */
	@Override
	public void setModelValue(T valueOnTargetSide) {
		modelBinding.setModelValue(valueOnTargetSide);
	}

	@Override
	public void setTargetValue(T valueOnModelSide) {
		targetBinding.setTargetValue(valueOnModelSide);
	}

	@Override
	public void setStatus(IStatus status) {
		/*
		 * We pass the status to both the given consumer and we also pass it on
		 * along the chain to the target.
		 */
		targetBinding.setStatus(status);
		this.statusConsumer.accept(status);
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