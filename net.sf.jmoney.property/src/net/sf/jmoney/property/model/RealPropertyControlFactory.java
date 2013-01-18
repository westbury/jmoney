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
public abstract class RealPropertyControlFactory<P> extends PropertyControlFactory<RealProperty> implements IReferenceControlFactory<P, RealProperty> {

	public IPropertyControl createPropertyControl(Composite parent, final ScalarPropertyAccessor<RealProperty,?> propertyAccessor) {

		final RealPropertyControl<RealProperty> control = new RealPropertyControl<RealProperty>(parent, null, RealProperty.class);
		
		return new IPropertyControl<ExtendableObject>() {

			private ExtendableObject fObject;

			public Control getControl() {
				return control;
			}

			public void load(ExtendableObject object) {
		        fObject = object;
		        
		        control.setSession(object.getSession(), propertyAccessor.getClassOfValueObject());
		        control.setSecurity(object.getPropertyValue(propertyAccessor));
			}

			public void save() {
				RealProperty stock = control.getSecurity();
				fObject.setPropertyValue(propertyAccessor, stock);
			}};
	}

	public RealProperty getDefaultValue() {
		return null;
	}

	public boolean isEditable() {
		return true;
	}
}
