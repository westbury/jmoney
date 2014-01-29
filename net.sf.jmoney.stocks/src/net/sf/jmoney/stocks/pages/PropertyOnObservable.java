package net.sf.jmoney.stocks.pages;

import org.eclipse.core.databinding.observable.IChangeListener;
import org.eclipse.core.databinding.observable.Realm;
import org.eclipse.core.databinding.observable.value.AbstractObservableValue;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.IValueChangeListener;
import org.eclipse.core.databinding.property.value.ValueProperty;

abstract class PropertyOnObservable<V> extends
		ValueProperty<StockEntryData, V> {
	
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
			final StockEntryData source) {
		/*
		 * This is a bit hacky.  It would be nice if we could simply return
		 * source.sharePrice().  Unfortunately the caller owns the returned observable
		 * and may well dispose it.  We can't dispose source.sharePrice() hence all this code.
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
			public void addValueChangeListener(IValueChangeListener<? super V> listener) {
				getObservable(source).addValueChangeListener(listener);
			}

			@Override
			public void removeValueChangeListener(IValueChangeListener<? super V> listener) {
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

	protected abstract IObservableValue<V> getObservable(StockEntryData source);

}