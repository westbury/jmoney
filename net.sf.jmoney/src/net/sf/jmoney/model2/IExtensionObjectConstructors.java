package net.sf.jmoney.model2;

import net.sf.jmoney.isolation.IValues;


public interface IExtensionObjectConstructors<X extends ExtensionObject, E extends ExtendableObject> {
	X construct(E extendedObject);
	X construct(E extendedObject, IValues<E> values);
}

