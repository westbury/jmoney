package org.eclipse.core.internal.databinding.provisional.bind;


/**
 * @since 1.5
 * 
 * @param <T2>
 * @param <T1>
 */
public class OneWayConversionBinding<T2, T1> extends OneWayBinding<T2>
		implements ITargetBinding<T1> {
	private final IOneWayModelBinding<T1> modelBinding;
	private final IConverter<T1, T2> converter;

	/**
	 * @param modelBinding
	 * @param converter
	 */
	public OneWayConversionBinding(IOneWayModelBinding<T1> modelBinding,
			IConverter<T1, T2> converter) {
		this.modelBinding = modelBinding;
		this.converter = converter;
	}

	@Override
	public T2 getModelValue() {
		T1 modelValue = modelBinding.getModelValue();
		return converter.convert(modelValue);
	}

	@Override
	public void setTargetValue(T1 valueOnModelSide) {
		T2 valueOnTargetSide = converter.convert(valueOnModelSide);
		targetBinding.setTargetValue(valueOnTargetSide);
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