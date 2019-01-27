package net.sf.jmoney.importer.matcher;

import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.jface.internal.databinding.provisional.swt.UpdatingComposite;
import org.eclipse.swt.widgets.Control;

import net.sf.jmoney.importer.model.MemoPattern;

public abstract class TransactionParamMetadata {

	protected String id;
	
	protected String name;

	public TransactionParamMetadata(String id, String name) {
		this.id = id;
		this.name = name;
	}
	
	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public abstract Control createControl(UpdatingComposite parent, IObservableValue<MemoPattern> memoPattern, IObservableValue<String[]> args);

	public abstract String getResolvedValueAsString(PatternMatch match);

}
