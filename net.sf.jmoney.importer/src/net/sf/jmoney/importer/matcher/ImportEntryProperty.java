package net.sf.jmoney.importer.matcher;

import org.eclipse.core.databinding.observable.Realm;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.property.value.ValueProperty;
import org.eclipse.core.internal.databinding.observable.ConstantObservableValue;

public abstract class ImportEntryProperty<T extends BaseEntryData> extends ValueProperty<T,String> {
	final String id;
	
	final String label;
	
	protected ImportEntryProperty(String id, String label) {
		super();
		this.id = id;
		this.label = label;
	}

	@Override
	public Class<String> getValueClass() {
		return String.class;
	}

	@Override
	public Object getValueType() {
		return String.class;
	}

	@Override
	public IObservableValue<String> observe(Realm realm,
			T source) {
		String value = getCurrentValue(source);
		return new ConstantObservableValue<String>(value, String.class);
	}
	
	/**
	 * 
	 * @param entryData
	 * @return string to match against the pattern, which may be
	 * 			empty but cannot be null
	 */
	protected abstract String getCurrentValue(T entryData);
}

