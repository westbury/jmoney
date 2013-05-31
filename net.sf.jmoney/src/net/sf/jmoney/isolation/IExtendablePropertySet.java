package net.sf.jmoney.isolation;

import java.util.Collection;

public interface IExtendablePropertySet<E extends IModelObject> {

	Class<E> getImplementationClass();

	Collection<? extends IScalarPropertyAccessor<?,? super E>> getScalarProperties3();

	Collection<? extends IListPropertyAccessor<?,? super E>> getListProperties3();

	E constructImplementationObject(IObjectKey objectKey, ListKey<? super E,?> parentKey, IValues<E> values);

	/**
	 * This method should be used only by plug-ins that implement
	 * a datastore.
	 * 
	 * @return A newly constructed object, constructed from the given
	 * 		parameters.  This object may be an ExtendableObject or
	 * 		may be an ExtensionObject.
	 */
	E constructDefaultImplementationObject(IObjectKey objectKey, ListKey<? super E, ?> parentKey);
	
	IExtendablePropertySet<? extends E> getActualPropertySet(Class<? extends E> classOfObject);

	IExtendablePropertySet<? super E> getBasePropertySet();

	/**
	 * It is often useful to have an array of property values
	 * of an extendable object.  This array contains all scalar
	 * properties in the extendable object, including extension
	 * properties and properties from any base property sets.
	 * <P>
	 * In these arrays, the properties (including extension properties)
	 * from the base property sets are put first in the array.
	 * This means a given property will always be at the same index
	 * in the array regardless of the actual derived property set.
	 * <P>
	 * This index is guaranteed to match the order in which
	 * properties are returned by the PropertySet.getPropertyIterator_Scalar3().
	 * i.e. if this method returns n then in every case where the
	 * collection returned by getPropertyIterator_Scalar3 contains this property,
	 * this property will be returned as the (n+1)'th element in the collection.
	 * 
	 * @return the index of this property in the list of scalar
	 * 			properties for the class.  This method returns zero
	 * 			for the first scalar property returned by
	 * 			PropertySet.getPropertyIterator3() and so on. 
	 */
	int getIndexIntoScalarProperties(IScalarPropertyAccessor<?,?> property);

}
