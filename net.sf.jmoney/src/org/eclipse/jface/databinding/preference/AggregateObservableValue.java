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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.databinding.observable.Diffs;
import org.eclipse.core.databinding.observable.value.AbstractObservableValue;
import org.eclipse.core.databinding.observable.value.IValueChangeListener;
import org.eclipse.core.databinding.observable.value.ValueChangeEvent;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;

/**
 * The class aggregates the observable values in each scope into a single
 * preference observable. The resultant observable follows the normal preference
 * rules for searching scopes for a value.
 * 
 * @param <T>
 * @since 1.7
 * 
 */
public abstract class AggregateObservableValue<T> extends
		AbstractObservableValue<T> implements INodeTypingMethods<T> {

	/**
	 * List of observable values, each observable being the value of the
	 * preference in a single scope.
	 */
	private List<NodeObserver<T>> observablePreferenceValues;

	private Class<T> valueClass;

	private boolean updating = false;

	private T currentValue;

	private T defaultValue;

	/**
	 * The number of elements in <code>observableValues</code> that have
	 * listeners, specifically, if <code>numberWithListeners</code> contains 3
	 * then indexes 0, 1, and 2 will have listeners, whereas indexes 3 onwards
	 * will not have listeners.
	 * <P>
	 * We only listen for changes to scopes if a change to that scope would
	 * affect the value. That means we add a listener to all scopes checked and
	 * in which no value was found and also in the scope in which we found a
	 * value.
	 */
	private int numberWithListeners = 0;

	private IValueChangeListener<T> listener = new IValueChangeListener<T>() {
		@Override
		public void handleValueChange(ValueChangeEvent<? extends T> event) {
			removeAllListeners();

			if (!updating) {
				fireValueChange(Diffs.createValueDiff(currentValue,
						doGetValue()));
			}
		}
	};

	/**
	 * @param nodes
	 * @param path
	 * @param valueClass
	 * @param defaultValue
	 */
	public AggregateObservableValue(List<IEclipsePreferences> nodes,
			String path, Class<T> valueClass, T defaultValue) {
		this.valueClass = valueClass;
		this.valueClass = valueClass;
		this.defaultValue = defaultValue;

		if (path.charAt(0) == IPath.SEPARATOR) {
			throw new IllegalArgumentException(
					"path cannot be absolute, it is relative to each of the root nodes for each scope"); //$NON-NLS-1$
		}

		/*
		 * Split the path, if any, from the key.
		 */
		int indexOfLastSeparator = -1;
		int index = path.indexOf(IPath.SEPARATOR);
		while (index != -1) {
			indexOfLastSeparator = index;
			index = path.indexOf(IPath.SEPARATOR, indexOfLastSeparator + 1);
		}

		String pathPart;
		final String keyPart;
		if (indexOfLastSeparator == -1) {
			pathPart = null;
			keyPart = path;
		} else {
			pathPart = path.substring(0, indexOfLastSeparator);
			keyPart = path.substring(indexOfLastSeparator + 1);
		}

		/*
		 * Try each node in our scope list in turn. If we fail to get a value
		 * then we have to listen to the appropriate event that would change
		 * that. For example, if we are following a path and we don't find the
		 * next child in the path then we must listen to the parent node so we
		 * know if the child has been added.
		 */

		/*
		 * List of nodes, starting with the first tried, and ending with the
		 * last.
		 */
		observablePreferenceValues = new ArrayList<NodeObserver<T>>();

		// Fill it
		for (IEclipsePreferences scopeRootNode : nodes) {
			final IEclipsePreferences scopeActualNode = pathPart == null ? scopeRootNode
					: (IEclipsePreferences) scopeRootNode.node(pathPart);

			observablePreferenceValues.add(new NodeObserver<T>(scopeActualNode,
					keyPart, valueClass, this));
		}

		// Is this necessary? This causes listeners to be added before the value
		// has been fetched.
		doGetValue();
	}

	@Override
	public void doSetValue(T value) {
		T oldValue = doGetValue();
		try {
			updating = true;

			/*
			 * What do we do if the value is null? Can we set a null value in
			 * the preferences? If not and we have to treat this as removing a
			 * value then the observable contract is violated because the value
			 * would actually become a value set in a backup scope. For this
			 * reason, we make this a read-only observable.
			 */

		} finally {
			updating = false;
		}
		doGetValue();
		fireValueChange(Diffs.createValueDiff(oldValue, value));
	}

	@Override
	public T doGetValue() {
		/*
		 * Remove the listeners first. This most likely will have been done
		 * already if the preferences changed and fired our preference change
		 * listener. However the user may fetch the value multiple times without
		 * the preferences changing, and it is easiest just to remove the
		 * listeners first and let them be added back.
		 */
		removeAllListeners();

		for (NodeObserver<T> observableValue : observablePreferenceValues) {
			// We are looking at this node so we need to listen to this node
			observableValue.addValueChangeListener(listener);
			numberWithListeners++;

			if (observableValue.containsKey()) {
				return observableValue.getValue();
			}
		}
		return defaultValue;
	}

	@Override
	public Object getValueType() {
		return valueClass;
	}

	public Class<T> getValueClass() {
		return valueClass;
	}

	@Override
	public synchronized void dispose() {
		removeAllListeners();
		super.dispose();
	}

	private void removeAllListeners() {
		for (int i = 0; i < numberWithListeners; i++) {
			observablePreferenceValues.get(i).removeValueChangeListener(
					listener);
		}
		numberWithListeners = 0;
	}

}
