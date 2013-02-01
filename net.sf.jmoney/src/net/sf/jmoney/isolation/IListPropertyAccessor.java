package net.sf.jmoney.isolation;


public interface IListPropertyAccessor<E extends IModelObject, S extends IModelObject> {

	IExtendablePropertySet<E> getElementPropertySet();

	// TODO rename to getList????
	ObjectCollection<E> getElements(S parentObject);

}
