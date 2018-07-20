package org.eclipse.core.internal.databinding.provisional.bind;

/**
 * This interface is used internally by this package to manage one-way binding
 * from the model observable.
 * 
 * @since 1.5
 * @noimplement
 * @param <T>
 */
interface IOneWayModelBinding<T> {

	/**
	 * @return the value from the model side
	 */
	T getModelValue();

	/**
	 * Removes the listener from the model. This method is called when the
	 * target is disposed.
	 */
	void removeModelListener();
}
