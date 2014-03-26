/*******************************************************************************
 * Copyright (c) 2014 Nigel Westbury and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nigel Westbury - initial API and implementation
 *******************************************************************************/
package net.sf.jmoney.stocks.pages;

import org.eclipse.core.databinding.observable.IChangeListener;
import org.eclipse.core.databinding.observable.Realm;
import org.eclipse.core.databinding.observable.value.AbstractObservableValue;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.IValueChangeListener;
import org.eclipse.core.databinding.property.value.ValueProperty;

/**
 * This class is here because we have some model objects that have methods that
 * return observables.  That's really great if you want to bind to that property
 * of the model object.  The problem is when you want to do a master-detail on
 * to the property or if you want a IValueProperty for providing labels in a table.
 * This class can be used to create a Property object that can
 * then be used in these situations.
 * 
 * @param <S> type of the source object
 * @param <V> type of the value, being the generic type of the IObservableValue
 * 				that is the observable property value
 * @author Nigel Westbury
 */
abstract class PropertyOnObservable<S, V> extends
		ValueProperty<S, V> {
	
	private Class<V> valueClass;
	
	PropertyOnObservable(Class<V> valueClass) {
		this.valueClass = valueClass;
	}
	
	@Override
	public Object getValueType() {
		return valueClass;
	}

	@Override
	public IObservableValue<V> observe(Realm realm,
			final S source) {
		/*
		 * This is a bit hacky.  It would be nice if we could simply return
		 * the observable directly.  Unfortunately the caller owns the returned observable
		 * and may well dispose it.  We don't own this observable hence all this code.
		 */
		return new AbstractObservableValue<V>() {

			@Override
			public Object getValueType() {
				return valueClass;
			}

			@Override
			protected V doGetValue() {
				return getObservable(source).getValue();
			}

			@Override
			protected void doSetValue(V value) {
				getObservable(source).setValue(value);
			}

			@Override
			public void addValueChangeListener(IValueChangeListener<V> listener) {
				getObservable(source).addValueChangeListener(listener);
			}

			@Override
			public void removeValueChangeListener(IValueChangeListener<V> listener) {
				getObservable(source).removeValueChangeListener(listener);
			}

			@Override
			public void addChangeListener(IChangeListener listener) {
				getObservable(source).addChangeListener(listener);
			}

			@Override
			public void removeChangeListener(IChangeListener listener) {
				getObservable(source).removeChangeListener(listener);
			}
		};
	}

	protected abstract IObservableValue<V> getObservable(S source);

}