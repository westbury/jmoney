package net.sf.jmoney.isolation;


public interface IValues<E extends IModelObject> {
	<V> V getScalarValue(IScalarPropertyAccessor<V,? super E> propertyAccessor);
	IObjectKey getReferencedObjectKey(IReferencePropertyAccessor<?,? super E> propertyAccessor);
	<E2 extends IModelObject> IListManager<E2> getListManager(IObjectKey listOwnerKey, IListPropertyAccessor<E2,? super E> listAccessor);
}
