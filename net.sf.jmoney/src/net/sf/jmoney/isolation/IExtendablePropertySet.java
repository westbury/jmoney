package net.sf.jmoney.isolation;

import java.util.Collection;

import net.sf.jmoney.model2.ListPropertyAccessor;
import net.sf.jmoney.model2.ScalarPropertyAccessor;

public interface IExtendablePropertySet<E extends IModelObject> {

	Class<E> getImplementationClass();

	Collection<ScalarPropertyAccessor<?,? super E>> getScalarProperties3();

	Collection<ListPropertyAccessor<?,? super E>> getListProperties3();

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
