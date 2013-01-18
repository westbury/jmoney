package net.sf.jmoney.isolation;


import org.eclipse.core.runtime.Assert;

/**
 * This class acts as a key to a list of objects.  It allows an object
 * to contain a reference to its parent without the need to instantiate
 * the parent.
 * <P>
 * If the accounts are stored in a serialized file such as an XML file then
 * all objects are instantiated anyway, but if the accounts are kept in
 * a database then we don't always instantiate objects unless they are needed.
 *
 * @param <E> the class of objects kept in the list
 */
public class ListKey<E extends IModelObject, S extends IModelObject> {
	private IObjectKey parentKey;
	private IListPropertyAccessor<E,? super S> listProperty;

	public ListKey(IObjectKey parentKey, IListPropertyAccessor<E,? super S> listProperty) {
		this.parentKey = parentKey;
		this.listProperty = listProperty;
	}

	public IObjectKey getParentKey() {
		return parentKey;
	}
	
	public IListPropertyAccessor<E,? super S> getListPropertyAccessor() {
		return listProperty;
	}
	
	@Override
	public boolean equals(Object other) {
		Assert.isTrue(other instanceof ListKey);
		ListKey otherListKey = (ListKey)other;
		return parentKey.equals(otherListKey.parentKey)
			&& listProperty == otherListKey.listProperty;
	}
}
