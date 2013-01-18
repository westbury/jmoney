package net.sf.jmoney.model2;

import net.sf.jmoney.isolation.IObjectKey;
import net.sf.jmoney.isolation.IValues;
import net.sf.jmoney.isolation.ListKey;


public interface IExtendableObjectConstructors<E extends ExtendableObject> {
	E construct(IObjectKey objectKey, ListKey<? super E,?> parentKey);
	E construct(IObjectKey objectKey, ListKey<? super E,?> parentKey, IValues<E> values);
}

