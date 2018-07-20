package org.eclipse.core.internal.databinding.provisional.bind;

import org.eclipse.core.runtime.IStatus;

/**
 * @since 1.5
 * 
 * @param <F>
 */
public interface ITargetBinding<F> {
	/**
	 * @param targetValue
	 */
	void setTargetValue(F targetValue);

	/**
	 * Push the error status back to the target
	 * 
	 * @param status
	 */
	void setStatus(IStatus status);
}
