package org.eclipse.core.internal.databinding.provisional.bind;

import org.eclipse.core.runtime.IStatus;

/**
 * @since 1.5
 * 
 * @param <T>
 */
public class TwoWayValidationBinding<T> extends TwoWayBinding<T> implements
		ITargetBinding<T> {
	private final IModelBinding<T> modelBinding;
	private final IValidator<T> validator;

	/**
	 * @param modelBinding
	 * @param validator
	 * @param pullInitialValue
	 */
	public TwoWayValidationBinding(IModelBinding<T> modelBinding,
			IValidator<T> validator, boolean pullInitialValue) {
		super(pullInitialValue);
		this.modelBinding = modelBinding;
		this.validator = validator;
	}

	/**
	 * The default behavior is to validate only when going from target to model.
	 * The error status observable is assumed to be there to show user errors.
	 * Therefore no validation is done in this method.
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
		IStatus status = validator.validate(valueOnTargetSide);
		targetBinding.setStatus(status);

		/*
		 * We pass on the value towards the model only if a warning or better.
		 * We block if an error or worse. This may or may not be the behavior
		 * expected by the users.
		 */
		if (status.getSeverity() >= IStatus.WARNING) {
			modelBinding.setModelValue(valueOnTargetSide);
		}
	}

	/**
	 * The default behavior is to validate only when going from target to model.
	 * The error status observable is assumed to be there to show user errors.
	 * Therefore no validation is done in this method.
	 */
	@Override
	public void setTargetValue(T valueOnModelSide) {
		targetBinding.setTargetValue(valueOnModelSide);
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