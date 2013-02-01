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

}
