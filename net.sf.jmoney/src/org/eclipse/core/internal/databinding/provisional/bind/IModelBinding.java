package org.eclipse.core.internal.databinding.provisional.bind;

/**
 * This interface is used internally by this package to manage two-way binding
 * to the model observable.
 * 
 * @since 1.5
 * @noimplement
 * @param <T>
 */
interface IModelBinding<T> {

	/**
	 * @return the model from the model side
	 */
	T getModelValue();

	/**
	 * @param newValue
	 */
	void setModelValue(T newValue);

	/**
	 * Removes the listener from the model. This method is called when the
	 * target is disposed.
	 */
	void removeModelListener();
}
