package net.sf.jmoney.property.model;

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
public abstract class RealPropertyControlFactory<P,S extends ExtendableObject> extends PropertyControlFactory<S,RealProperty> implements IReferenceControlFactory<P, S, RealProperty> {

	public IPropertyControl<S> createPropertyControl(Composite parent, final ScalarPropertyAccessor<RealProperty,S> propertyAccessor) {

		final RealPropertyControl<RealProperty> control = new RealPropertyControl<RealProperty>(parent, null, RealProperty.class);
		
		return new IPropertyControl<S>() {

			private S fObject;

			public Control getControl() {
				return control;
			}

			public void load(S object) {
		        fObject = object;
		        
		        control.setSession(object.getSession(), propertyAccessor.getClassOfValueObject());
		        control.setSecurity(propertyAccessor.getValue(object));
			}

			public void save() {
				RealProperty stock = control.getSecurity();
				propertyAccessor.setValue(fObject, stock);
			}};
	}

	public RealProperty getDefaultValue() {
		return null;
	}

	public boolean isEditable() {
		return true;
	}
}
