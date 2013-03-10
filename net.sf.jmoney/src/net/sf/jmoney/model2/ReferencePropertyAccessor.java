package net.sf.jmoney.model2;

import net.sf.jmoney.isolation.IReferencePropertyAccessor;


public abstract class ReferencePropertyAccessor<V extends ExtendableObject,E extends ExtendableObject> extends ScalarPropertyAccessor<V,E> implements IReferencePropertyAccessor<V,E> {

	public ReferencePropertyAccessor(Class<V> classOfValueObject, PropertySet<?,E> propertySet, String localName, String displayName, int weight, int minimumWidth, IPropertyControlFactory<E,V> propertyControlFactory, IPropertyDependency<E> propertyDependency) {
		super(classOfValueObject, propertySet, localName, displayName, weight, minimumWidth, propertyControlFactory, propertyDependency);
	}

}
