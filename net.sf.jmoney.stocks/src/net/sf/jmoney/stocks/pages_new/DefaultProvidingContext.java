package net.sf.jmoney.stocks.pages_new;

import org.eclipse.core.databinding.observable.ObservableTracker;
import org.eclipse.core.databinding.observable.list.WritableList;
import org.eclipse.core.databinding.observable.value.IObservableValue;

public class DefaultProvidingContext {

	private WritableList<DefaultValueBinding> bindings;

	public DefaultProvidingContext() {
		ObservableTracker.setIgnore(true);
		try {
			bindings = new WritableList<DefaultValueBinding>();
		} finally {
			ObservableTracker.setIgnore(false);
		}
	}

	public <T> DefaultValueBinding<T> bindDefault(IObservableValue<T> target, IObservableValue<T> defaultValue) {
		DefaultValueBinding<T> result = new DefaultValueBinding<T>(target,
				defaultValue);
		result.init(this);
		return result;
	}

	void addBinding(DefaultValueBinding binding) {
		bindings.add(binding);
	}

	/**
	 * Removes the given binding.
	 * 
	 * @param binding
	 * @return <code>true</code> if was associated with the context,
	 *         <code>false</code> if not
	 */
	public boolean removeBinding(DefaultValueBinding binding) {
		return bindings.remove(binding);
	}

		/**
		 * Disposes of this data binding context and all bindings and validation
		 * status providers that were added to this context. This method must be
		 * called in the {@link #getValidationRealm() validation realm}.
		 */
	public void dispose() {
		DefaultValueBinding[] bindingArray = bindings.toArray(new DefaultValueBinding[bindings.size()]);
			for (int i = 0; i < bindingArray.length; i++) {
				bindingArray[i].dispose();
			}
		}
}
