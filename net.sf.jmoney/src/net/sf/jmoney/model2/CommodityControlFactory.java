package net.sf.jmoney.model2;


import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.internal.databinding.provisional.bind.Bind;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * @param <P>
 * 		the class of objects that contain this property
 */
public abstract class CommodityControlFactory<S extends ExtendableObject, P> extends PropertyControlFactory<S,Commodity> implements IReferenceControlFactory<P, S, Commodity> {

//	@Override
//	public IPropertyControl<S> createPropertyControl(Composite parent, final ScalarPropertyAccessor<Commodity,S> propertyAccessor) {
//		final CommodityControl<Commodity> control = new CommodityControl<Commodity>(parent, null, Commodity.class);
//
//		return new IPropertyControl<S>() {
//
//			private S fObject;
//
//			@Override
//			public Control getControl() {
//				return control;
//			}
//
//			@Override
//			public void load(S object) {
//		        fObject = object;
//
//		        control.setSession(object.getSession(), propertyAccessor.getClassOfValueObject());
//		        control.setCommodity(propertyAccessor.getValue(object));
//			}
//
//			@Override
//			public void save() {
//				Commodity commodity = control.getCommodity();
//				propertyAccessor.setValue(fObject, commodity);
//			}
//		};
//	}

	@Override
	public Control createPropertyControl(Composite parent, final ScalarPropertyAccessor<Commodity,S> propertyAccessor, IObservableValue<? extends S> modelObservable) {
		CommodityControl<Commodity> control = new CommodityControl<Commodity>(parent, null, Commodity.class);

		Bind.twoWay(propertyAccessor.observeDetail(modelObservable))
		.to(control.commodity);

		return control;
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
