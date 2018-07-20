/*******************************************************************************
 * Copyright (c) 2005, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.internal.databinding.provisional.bind;

/**
 * A one-way converter.
 * 
 * @param <S>
 *            type of source value
 * @param <T>
 *            type of converted value
 * @noextend This interface is not intended to be extended by clients.
 * @noimplement This interface is not intended to be implemented by clients.
 *              Clients should subclass {@link Converter}.
 * @since 1.0
 * 
 */
public interface IConverter<S, T> {

	/**
	 * Returns the result of the conversion of the given object.
	 * 
	 * @param fromObject
	 *            the object to convert, of type {@link #getFromType()}
	 * @return the converted object, of type {@link #getToType()}
	 */
	public T convert(S fromObject);
}
