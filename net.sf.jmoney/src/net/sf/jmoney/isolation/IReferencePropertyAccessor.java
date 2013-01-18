package net.sf.jmoney.isolation;


public interface IReferencePropertyAccessor<V extends IModelObject, E extends IModelObject> extends IScalarPropertyAccessor<V,E> {

	/**
	 * Given an object (which must be of a class that contains this
	 * property), return the object key to this property.
	 *   
	 * @param object
	 * @return
	 */
	IObjectKey invokeObjectKeyField(E parentObject);

}
