package net.sf.jmoney.model2;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * @param <P>
 * 		the class of objects that contain this property
 */
public abstract class CommodityControlFactory<P> extends PropertyControlFactory<Commodity> implements IReferenceControlFactory<P, Commodity> {

	@Override
	public IPropertyControl createPropertyControl(Composite parent, final ScalarPropertyAccessor<Commodity,?> propertyAccessor) {

		final CommodityControl<Commodity> control = new CommodityControl<Commodity>(parent, null, Commodity.class);
		
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
		        control.setCommodity(object.getPropertyValue(propertyAccessor));
			}

			@Override
			public void save() {
				Commodity commodity = control.getCommodity();
				fObject.setPropertyValue(propertyAccessor, commodity);
			}};
	}

	@Override
	public Commodity getDefaultValue() {
		return null;
	}

	@Override
	public boolean isEditable() {
		return true;
	}
}
