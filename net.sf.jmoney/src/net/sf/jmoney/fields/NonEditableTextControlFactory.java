package net.sf.jmoney.fields;

import net.sf.jmoney.model2.ExtendableObject;
import net.sf.jmoney.model2.IPropertyControl;
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
	public IPropertyControl<S> createPropertyControl(Composite parent, final ScalarPropertyAccessor<String,S> propertyAccessor) {

		// Property is not editable
        final Label control = new Label(parent, SWT.NONE);
        return new IPropertyControl<S>() {

			@Override
			public Control getControl() {
				return control;
			}

			@Override
			public void load(S object) {
				String text = propertyAccessor.getValue(object);
				if (text == null) {
					control.setText("");
				} else {
					control.setText(text);
				}
			}

			@Override
			public void save() {
				/*
				 * The property is not editable so there is nothing
				 * to do here.
				 */
			}
        };
	}

    @Override
	public Control createPropertyControl(Composite parent, ScalarPropertyAccessor<String,S> propertyAccessor, IObservableValue<? extends S> modelObservable) {
        final Label control = new Label(parent, SWT.NONE);

		Bind.oneWay(propertyAccessor.observeDetail(modelObservable))
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
