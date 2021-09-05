package net.sf.jmoney.stocks.pages;

import org.eclipse.core.databinding.observable.Diffs;
import org.eclipse.core.databinding.observable.value.AbstractObservableValue;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.IValueChangeListener;
import org.eclipse.core.databinding.observable.value.ValueChangeEvent;
import org.eclipse.core.databinding.observable.value.ValueDiff;

public class RetargetableObservable<T> extends AbstractObservableValue<T> {
	// Listener to the computed value, the computed value that gives us the appropriate observable
	IValueChangeListener<IObservableValue<T>> listener = new IValueChangeListener<IObservableValue<T>>() {

		@Override
		public void handleValueChange(ValueChangeEvent<? extends IObservableValue<T>> event) {
			IObservableValue<T> oldObservable = event.diff.getOldValue();
			if (oldObservable != null) {
				oldObservable.removeValueChangeListener(listener2);
			}
			IObservableValue<T> newObservable = event.diff.getNewValue();
			if (newObservable != null) {
				newObservable.addValueChangeListener(listener2);
			}
		}
	};
	// Listener to the appropriate observable
	IValueChangeListener<T> listener2 = new IValueChangeListener<T>() {

		@Override
		public void handleValueChange(ValueChangeEvent<? extends T> event) {
			ValueDiff<T> diff = Diffs.createValueDiff(event.diff.getOldValue(), event.diff.getNewValue());
			fireChange(diff);
		}
	};
	
	private IObservableValue<IObservableValue<T>> computedObservable;
	private Class<T> valueType;

	public RetargetableObservable(Class<T> valueType, IObservableValue<IObservableValue<T>> computedObservable) {
		this.valueType = valueType;
		this.computedObservable = computedObservable;
	}

	private void fireChange(ValueDiff<T> diff) {
		this.fireValueChange(diff);
	}

	@Override
	public Object getValueType() {
		return valueType;
	}

	@Override
	protected T doGetValue() {
		IObservableValue<T> targetObservable = computedObservable.getValue();
		if (targetObservable != null) {
			return targetObservable.getValue();
		}
		return null;
	}

	@Override
	protected void doSetValue(T value) {
		IObservableValue<T> targetObservable = computedObservable.getValue();
		if (targetObservable != null) {
			targetObservable.setValue(value);
		}
	}

	@Override
	protected void firstListenerAdded() {
		computedObservable.addValueChangeListener(listener);
	}

	@Override
	protected void lastListenerRemoved() {
		computedObservable.removeValueChangeListener(listener);
	}
}