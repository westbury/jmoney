package net.sf.jmoney.isolation;

import net.sf.jmoney.model2.PropertySet;

public interface IListPropertyAccessor<E extends IModelObject, S extends IModelObject> {

	PropertySet getPropertySet();

	IExtendablePropertySet<E> getElementPropertySet();

	// TODO rename to getList????
	ObjectCollection<E> getElements(S parentObject);

}
