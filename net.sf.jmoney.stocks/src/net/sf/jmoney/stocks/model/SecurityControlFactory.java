package net.sf.jmoney.stocks.model;

import net.sf.jmoney.model2.ExtendableObject;
import net.sf.jmoney.model2.IReferenceControlFactory;
import net.sf.jmoney.model2.PropertyControlFactory;
import net.sf.jmoney.model2.ScalarPropertyAccessor;
import net.sf.jmoney.model2.Session;

import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.internal.databinding.provisional.bind.Bind;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * @param <P>
 * 		the class of objects that contain this property
 */
public abstract class SecurityControlFactory<P, S extends ExtendableObject> extends PropertyControlFactory<S, Security> implements IReferenceControlFactory<P, S, Security> {

	@Override
	public Control createPropertyControl(Composite parent, final ScalarPropertyAccessor<Security,S> propertyAccessor, final IObservableValue<? extends S> modelObservable) {
		SecurityControl<Security> control = new SecurityControl<Security>(parent, SecurityInfo.getPropertySet()) {
			@Override
			protected Session getSession() {
				return modelObservable.getValue().getSession();
			}
		};

		Bind.twoWay(propertyAccessor, modelObservable)
		.to(control.commodity);

		return control;
	}

	@Override
	public Security getDefaultValue() {
		return null;
	}

	@Override
	public boolean isEditable() {
		return true;
	}
}
