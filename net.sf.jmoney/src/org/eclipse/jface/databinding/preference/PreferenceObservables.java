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
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IScopeContext;

/**
 * An instance of PreferenceObservables represents a list of scopes together
 * with a qualifier. When observing a preference with a given key, these scopes
 * are searched in order for the value according to the normal preference rules.
 * <P>
 * If the scopes are fairly fixed then you can pass a list of a list of scopes
 * to the factory method. For more flexible support you can pass an observable
 * list of nodes to the factory. The latter might be useful if, say, you want to
 * observe a value of a key which may be defined in the project scope. The
 * current project may change from time to time and you want the observable
 * value of the key to track the currently selected project, like a
 * master-detail value. In this case you would need to create an observable list
 * of nodes where one of the elements is the project node which changes as the
 * selected project changes.
 * <P>
 * These observables are all one-way (read-only). Their primary purpose is for
 * enabling the UI to update as preferences are changed. For two-way binding
 * inside a preference page, see <code>PreferencePageSupport</code>.
 * 
 * @since 1.7
 * 
 */
public class PreferenceObservables {

	static public PreferenceObservables observe(List<IScopeContext> scopes,
			String qualifier) {
		return new PreferenceObservables(scopes, qualifier);
	}

	static public PreferenceObservables observe(IScopeContext[] scopes,
			String qualifier) {
		return new PreferenceObservables(Arrays.asList(scopes), qualifier);
	}

	private List<IEclipsePreferences> nodes;

	/**
	 * @param scopes
	 * @param qualifier
	 */
	private PreferenceObservables(List<IScopeContext> scopes, String qualifier) {
		/*
		 * Fetch the list of nodes for the given scopes.
		 * 
		 * The node for a given scope may not exist in which case no node is
		 * included in the resulting list. We must listen for node additions and
		 * removals and add and remove nodes from the resultant list as
		 * appropriate.
		 */
		nodes = new ArrayList<IEclipsePreferences>();
		for (IScopeContext scope : scopes) {
			IEclipsePreferences node = scope.getNode(qualifier);
			nodes.add(node);
		}
	}

	/**
	 * The key may consist of a path down the node tree with each component
	 * separated by '/'. For example, if the key is "jdbcproviders/mysql/driver"
	 * then the preference value is determined by looking in the node for each
	 * scope, looking for a node called 'jdbcproviders" then looking in that for
	 * a node called "mysql", then looking in that for a node called "driver".
	 * 
	 * @param path
	 * @param defaultValue
	 * @return observable value
	 */
	public IObservableValue<String> observe(String path,
			final String defaultValue) {
		return new AggregateObservableValue<String>(nodes, path, String.class,
				defaultValue) {
			@Override
			public String convertToType(String value) {
				// It's easy to convert a String to a String
				return value;
			}
		};
	}

	/**
	 * Observe a preference value that is of type Boolean.
	 * 
	 * @param path
	 *            the key which may include a path down the node tree with each
	 *            component separated by '/'
	 * @param defaultValue
	 * @return observable value
	 */
	public IObservableValue<Boolean> observe(String path, Boolean defaultValue) {
		return new AggregateObservableValue<Boolean>(nodes, path,
				Boolean.class, defaultValue) {
			@Override
			public Boolean convertToType(String value) {
				return value == null ? null : Boolean.valueOf(value);
			}
		};
	}
}
