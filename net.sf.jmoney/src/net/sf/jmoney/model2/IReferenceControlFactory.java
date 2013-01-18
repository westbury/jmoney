package net.sf.jmoney.model2;

import net.sf.jmoney.isolation.IObjectKey;


/**
 * An implementation of this interface must be provided for every property that
 * is a reference to another extendable object. The implementation provides a
 * method for getting the object key from a given parent object. Code that
 * wishes to process property references in a generalized way (without knowing
 * about all the properties at compile time) can thus do so.
 * 
 * @author Nigel Westbury
 * 
 * @param <P>
 * 		the class of objects that contain this property
 * @param <V>
 *      the class of the object that is being referenced
 */
public interface IReferenceControlFactory<P, V extends ExtendableObject> extends IPropertyControlFactory<V> {
	IObjectKey getObjectKey(P parentObject);
}
