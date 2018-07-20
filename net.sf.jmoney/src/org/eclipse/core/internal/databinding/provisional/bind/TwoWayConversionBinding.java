package org.eclipse.core.internal.databinding.provisional.bind;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

/**
 * @since 1.5
 * 
 * @param <T2>
 * @param <T1>
 */
public class TwoWayConversionBinding<T2, T1> extends TwoWayBinding<T2>
		implements ITargetBinding<T1> {
	private final IModelBinding<T1> modelBinding;
	private final IBidiConverter<T1, T2> converter;

	/**
	 * @param modelBinding
	 * @param converter
	 * @param pullInitialValue
	 */
	public TwoWayConversionBinding(IModelBinding<T1> modelBinding,
			IBidiConverter<T1, T2> converter, boolean pullInitialValue) {
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
		return converter.modelToTarget(modelValue);
	}

	/**
	 * @param valueOnTargetSide
	 */
	@Override
	public void setModelValue(T2 valueOnTargetSide) {
		// try {
		T1 valueOnModelSide = converter.targetToModel(valueOnTargetSide);
		modelBinding.setModelValue(valueOnModelSide);
		targetBinding.setStatus(Status.OK_STATUS);
		// } catch (CoreException e) {
		// targetBinding.setStatus(e.getStatus());
		// }
	}

	@Override
	public void setTargetValue(T1 valueOnModelSide) {
		T2 valueOnTargetSide = converter.modelToTarget(valueOnModelSide);
		targetBinding.setTargetValue(valueOnTargetSide);
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