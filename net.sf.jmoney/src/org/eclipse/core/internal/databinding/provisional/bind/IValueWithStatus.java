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

/**
 * @param <T>
 * @since 3.3
 *
 */
public interface IValueWithStatus<T> {

	/**
	 * @return the value
	 */
	T getValue();

	/**
	 * Returns the message describing the outcome. The message is localized to
	 * the current locale.
	 *
	 * @return a localized message
	 */
	String getMessage();

	/**
	 * Returns the severity. The severities are as follows (in descending
	 * order):
	 * <ul>
	 * <li><code>CANCEL</code> - cancelation occurred</li>
	 * <li><code>ERROR</code> - a serious error (most severe)</li>
	 * <li><code>WARNING</code> - a warning (less severe)</li>
	 * <li><code>INFO</code> - an informational ("fyi") message (least severe)
	 * </li>
	 * <li><code>OK</code> - everything is just fine</li>
	 * </ul>
	 * <p>
	 * The severity of a multi-status is defined to be the maximum severity of
	 * any of its children, or <code>OK</code> if it has no children.
	 * </p>
	 *
	 * @return the severity: one of <code>OK</code>, <code>ERROR</code>,
	 *         <code>INFO</code>, <code>WARNING</code>, or <code>CANCEL</code>
	 */
	int getSeverity();
}
