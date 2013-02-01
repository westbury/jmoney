package net.sf.jmoney.isolation;


public interface IModelObject {

	IDataManager getDataManager();

	IObjectKey getObjectKey();
	
	ListKey getParentListKey();

	/**
	 * This method allows datastore implementations to re-parent an
	 * object (move it from one list to another).
	 * <P>
	 * This method is to be used by datastore implementations only.
	 * Other plug-ins should not be calling this method.
	 * 
	 * @param listKey
	 */
	void replaceParentListKey(ListKey parentListKey);
	
// TODO remove this method.  We should have a getter for each list, and then
	// the IProperty accessor should get using Bean reflection.
//	<E2 extends IModelObject> ObjectCollection<E2> getListPropertyValue(IListPropertyAccessor<E2,?> owningListProperty);

	// rename this?
	IExtendablePropertySet getPropertySet();
}
