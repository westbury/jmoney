package net.sf.jmoney.fields;

import net.sf.jmoney.model2.ExtendableObject;
import net.sf.jmoney.model2.PropertyControlFactory;
import net.sf.jmoney.model2.ScalarPropertyAccessor;

import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.internal.databinding.provisional.bind.Bind;
import org.eclipse.jface.databinding.swt.SWTObservables;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

public class NonEditableTextControlFactory<S extends ExtendableObject> extends PropertyControlFactory<S,String> {

    @Override
	public Control createPropertyControl(Composite parent, ScalarPropertyAccessor<String,S> propertyAccessor, S modelObject) {
    	return createPropertyControlInternal(parent, propertyAccessor.observe(modelObject));
    }

    @Override
	public Control createPropertyControl(Composite parent, ScalarPropertyAccessor<String,S> propertyAccessor, IObservableValue<? extends S> modelObservable) {
    	return createPropertyControlInternal(parent, propertyAccessor.observeDetail(modelObservable));
    }

    private Control createPropertyControlInternal(Composite parent, IObservableValue<String> modelStringObservable) {
		// Property is not editable
        final Label control = new Label(parent, SWT.NONE);

		Bind.oneWay(modelStringObservable)
		.to(SWTObservables.observeText(control));

		return control;
	}

	@Override
	public String formatValueForMessage(S extendableObject, ScalarPropertyAccessor<? extends String,S> propertyAccessor) {
		String value = propertyAccessor.getValue(extendableObject);
		return (value == null) ? "<blank>" : value;
	}

	@Override
	public String formatValueForTable(S extendableObject, ScalarPropertyAccessor<? extends String,S> propertyAccessor) {
		String value = propertyAccessor.getValue(extendableObject);
		return (value == null) ? "" : value;
	}

	@Override
	public String getDefaultValue() {
		return null;
	}

	@Override
	public boolean isEditable() {
		return true;
	}
}
