package net.sf.jmoney.stocks.model;

import net.sf.jmoney.model2.ExtendableObject;
import net.sf.jmoney.model2.IPropertyControl;
import net.sf.jmoney.model2.IReferenceControlFactory;
import net.sf.jmoney.model2.PropertyControlFactory;
import net.sf.jmoney.model2.ScalarPropertyAccessor;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * @param <P>
 * 		the class of objects that contain this property
 */
public abstract class SecurityControlFactory<P> extends PropertyControlFactory<Security> implements IReferenceControlFactory<P, Security> {

	@Override
	public IPropertyControl createPropertyControl(Composite parent, final ScalarPropertyAccessor<Security,?> propertyAccessor) {

		final SecurityControl<Security> control = new SecurityControl<Security>(parent, null, Security.class);

		return new IPropertyControl<ExtendableObject>() {

			private ExtendableObject fObject;

			@Override
			public Control getControl() {
				return control;
			}

			@Override
			public void load(ExtendableObject object) {
				fObject = object;

				control.setSession(object.getSession(), propertyAccessor.getClassOfValueObject());
				control.setSecurity(object.getPropertyValue(propertyAccessor));
			}

			@Override
			public void save() {
				Security stock = control.getSecurity();
				fObject.setPropertyValue(propertyAccessor, stock);
			}};
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
