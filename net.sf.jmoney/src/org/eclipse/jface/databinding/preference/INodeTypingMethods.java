/*******************************************************************************
 * Copyright (c) 2013 Nigel Westbury and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nigel Westbury - initial API and implementation
 ******************************************************************************/
package org.eclipse.jface.databinding.preference;

/**
 * Call-back to provide a conversion from the preference value (always reported
 * in the change event as a <code>String</code>) to the required type.
 * 
 * @param <T>
 * @since 1.7
 * 
 */
public interface INodeTypingMethods<T> {

	/**
	 * @param value
	 * @return value converted to type T
	 */
	T convertToType(String value);
}
