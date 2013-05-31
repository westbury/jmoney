package net.sf.jmoney.isolation;


import org.eclipse.core.databinding.property.value.IValueProperty;

// TODO we should be able to get rid of this interface when we can get
// the correctly parameterized class of V from IValueProperty
public interface IScalarPropertyAccessor<V, E extends IModelObject> extends IValueProperty<E,V> {

	Class<V> getClassOfValueObject();

}
