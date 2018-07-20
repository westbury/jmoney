/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/

package org.eclipse.core.internal.databinding.provisional.bind;

import org.eclipse.core.runtime.IStatus;

/**
 * @param <T>
 * @since 3.3
 *
 */
public class ValueWithStatus<T> implements IValueWithStatus<T> {

	private int severity;

	private T value;

	private String message;

	/**
	 * @param severity
	 * @param value
	 * @param message
	 */
	public ValueWithStatus(int severity, T value, String message) {
		this.severity = severity;
		this.value = value;
		this.message = message;
	}

	/**
	 * @param value
	 * @return a value with no associated status message
	 */
	public static <T> IValueWithStatus<T> ok(T value) {
		return new ValueWithStatus<T>(IStatus.OK, value, null);
	}

	/**
	 * @param message
	 * @return a value with an error message and no value
	 */
	public static <T> IValueWithStatus<T> error(String message) {
		return new ValueWithStatus<T>(IStatus.ERROR, null, message);
	}

	/*
	 * (Intentionally not javadoc'd) Implements the corresponding method on
	 * <code>IValueWithStatus</code>.
	 */
	@Override
	public T getValue() {
		return value;
	}

	/*
	 * (Intentionally not javadoc'd) Implements the corresponding method on
	 * <code>IValueWithStatus</code>.
	 */
	@Override
	public String getMessage() {
		return message;
	}

	/*
	 * (Intentionally not javadoc'd) Implements the corresponding method on
	 * <code>IValueWithStatus</code>.
	 */
	@Override
	public int getSeverity() {
		return severity;
	}

}
