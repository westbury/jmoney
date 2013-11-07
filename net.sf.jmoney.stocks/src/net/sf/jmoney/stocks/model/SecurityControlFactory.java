package net.sf.jmoney.stocks.model;

import net.sf.jmoney.model2.ExtendableObject;
import net.sf.jmoney.model2.IPropertyControl;
import net.sf.jmoney.model2.IReferenceControlFactory;
import net.sf.jmoney.model2.PropertyControlFactory;
import net.sf.jmoney.model2.ScalarPropertyAccessor;

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
	public IPropertyControl<S> createPropertyControl(Composite parent, final ScalarPropertyAccessor<Security,S> propertyAccessor) {

		final SecurityControl<Security> control = new SecurityControl<Security>(parent, null, Security.class);

		return new IPropertyControl<S>() {

			private S fObject;

			@Override
			public Control getControl() {
				return control;
			}

			@Override
			public void load(S object) {
				fObject = object;

				control.setSession(object.getSession(), propertyAccessor.getClassOfValueObject());
				control.setSecurity(propertyAccessor.getValue(object));
			}

			@Override
			public void save() {
				Security stock = control.getSecurity();
				propertyAccessor.setValue(fObject, stock);
			}};
	}

	@Override
	public Control createPropertyControl(Composite parent, final ScalarPropertyAccessor<Security,S> propertyAccessor, IObservableValue<? extends S> modelObservable) {
		SecurityControl<Security> control = new SecurityControl<Security>(parent, null, Security.class);

		Bind.twoWay(propertyAccessor.observeDetail(modelObservable))
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
