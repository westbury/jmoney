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

import org.eclipse.core.databinding.observable.Diffs;
import org.eclipse.core.databinding.observable.value.AbstractObservableValue;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;

/**
 * This class is an observable on a preference value in a single node.
 * <P>
 * This class should generally not be used directly by clients. Clients should
 * use the methods in PreferenceObservables because those methods will properly
 * scan the nodes in multiple scopes (e.g. instance scope, configuration scope,
 * default scope) to ensure the correct value is always set in the observable.
 * 
 * @param <T>
 * @since 1.7
 * 
 */
public class NodeObserver<T> extends AbstractObservableValue<T> {

	private final static String defaultString = ""; //$NON-NLS-1$

	private final IEclipsePreferences node;
	private final String key;
	private Class<T> valueClass;
	private INodeTypingMethods<T> nodeTypingMethods;

	/**
	 * @param node
	 * @param key
	 * @param valueClass
	 * @param nodeTypingMethods
	 */
	public NodeObserver(IEclipsePreferences node, final String key,
			Class<T> valueClass, final INodeTypingMethods<T> nodeTypingMethods) {
		this.node = node;
		this.key = key;
		this.valueClass = valueClass;
		this.nodeTypingMethods = nodeTypingMethods;

		node.addPreferenceChangeListener(new IPreferenceChangeListener() {

			@Override
			public void preferenceChange(PreferenceChangeEvent event) {
				if (event.getKey().equals(key)) {
					String oldValueAsString = (String) event.getOldValue();
					T oldValue = (oldValueAsString == null) ? null
							: nodeTypingMethods.convertToType(oldValueAsString);

					String newValueAsString = (String) event.getNewValue();
					T newValue = (newValueAsString == null) ? null
							: nodeTypingMethods.convertToType(newValueAsString);

					fireValueChange(Diffs.createValueDiff(oldValue, newValue));
				}

			}
		});

	}

	/**
	 * @return true if key exists in this node, false if not
	 */
	public boolean containsKey() {
		return node.get(key, defaultString) != defaultString;
	}

	/**
	 * @return value, which is assumed to be set
	 */
	public String get() {
		return node.get(key, null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.core.databinding.observable.value.AbstractObservableValue
	 * #doGetValue()
	 */
	@Override
	protected T doGetValue() {
		String value = node.get(key, defaultString);
		if (value == defaultString) {
			// Can we be sure this does not happen?
			// How do we handle this?
			throw new RuntimeException("doGetValue called when not set"); //$NON-NLS-1$
		}
		return value == null ? null : nodeTypingMethods.convertToType(value);
	}

	@Override
	public Object getValueType() {
		return valueClass;
	}

//	@Override
//	public Class<T> getValueClass() {
//		return valueClass;
//	}
}
