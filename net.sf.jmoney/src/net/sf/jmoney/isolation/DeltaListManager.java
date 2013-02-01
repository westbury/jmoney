/*
*
*  JMoney - A Personal Finance Manager
*  Copyright (c) 2005 Nigel Westbury <westbury@users.sourceforge.net>
*
*
*  This program is free software; you can redistribute it and/or modify
*  it under the terms of the GNU General Public License as published by
*  the Free Software Foundation; either version 2 of the License, or
*  (at your option) any later version.
*
*  This program is distributed in the hope that it will be useful,
*  but WITHOUT ANY WARRANTY; without even the implied warranty of
*  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*  GNU General Public License for more details.
*
*  You should have received a copy of the GNU General Public License
*  along with this program; if not, write to the Free Software
*  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
*
*/

package net.sf.jmoney.isolation;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;


/**
 * This is a special implementation of the IListManager interface. It is used
 * only in objects that are copies in a transaction manager of objects that have
 * been committed to the datastore.
 * <P>
 * This class implements the Collection methods by looking at the differences
 * (objects added and objects deleted) between the committed list and the list
 * in this transaction. These differences are applied to the collection of
 * committed objects returned by the underlying datastore.
 * <P>
 * This class keeps lists of inserted and deleted objects. It is ok for this
 * object to maintain these lists because all data managers must guarantee that
 * only a single instance of the same object is returned, and as
 * DeltaListManager objects are held by model objects, we can be sure that
 * only a single instance of this object will exist for a given list.
 * 
 * @author Nigel Westbury
 */
public class DeltaListManager<E extends IModelObject, S extends IModelObject> extends AbstractCollection<E> implements IListManager<E> {

	private TransactionManager transactionManager;
	
	S committedParent;

	IObjectKey uncommittedParentKey;

	public IListPropertyAccessor<E,? super S> listAccessor;

	/**
	 * The modified list.
	 * Null indicates no modification yet to this list.
	 */
	/**
	 * The uncommitted versions of the objects that have been added
	 */
	LinkedList<E> addedObjects = new LinkedList<E>();
	
	/**
	 * The keys to the committed versions of the objects that have been deleted
	 */
	LinkedList<IObjectKey> deletedObjects = new LinkedList<IObjectKey>();
	

	/**
	 * The committed list, set by the constructor
	 */
	private ObjectCollection<E> committedList;

	/**
	 * @param committedParent the object containing the list property.  This
	 * 			object must be an uncommitted object 
	 * @param uncommittedParentKey the key to the object that contains this list
	 * 			delta.  This object may not yet have been constructed, so this
	 * 			key is for reference and not for instantiating the object.  However,
	 * 			the committed object may be obtained from it.
	 * @param propertyAccessor the list property
	 */
	public DeltaListManager(TransactionManager transactionManager, S committedParent, UncommittedObjectKey uncommittedParentKey, IListPropertyAccessor<E,? super S> listProperty) {
		this.transactionManager = transactionManager;
		this.committedParent = committedParent;
		this.uncommittedParentKey = uncommittedParentKey;
		this.listAccessor = listProperty;
		this.committedList = listProperty.getElements(committedParent);
	}

	/**
	 * Create a new model object in the list represented by this object.
	 * <P>
	 * This method does not create the object in the underlying committed list,
	 * because if it did that then other views would see the object before it is
	 * committed. Instead this method adds the object to a list maintained by
	 * the transaction manager. When the consumer iterates over the list, the objects in the
	 * 'added' list are appended to the items returned by the underlying
	 * committed list.
	 */
	@Override
	public <F extends E> F createNewElement(IExtendablePropertySet<F> propertySet) {
		UncommittedObjectKey objectKey = new UncommittedObjectKey(transactionManager);
		F element = propertySet.constructDefaultImplementationObject(objectKey, new ListKey<E,S>(uncommittedParentKey, listAccessor));
		
		objectKey.setObject(element);
		
		addedObjects.add(element);
		
		/*
		 * Ensure this list is in the list of lists that have been
		 * modified within this transaction manager.
		 */
		transactionManager.modifiedLists.add(this);
		
		return element;
	}

	/**
	 * Create a new model object in the list represented by this object.
	 * This version of this method takes an array of values of the properties in
	 * the object.
	 * <P>
	 * This method does not create the object in the underlying committed list,
	 * because if it did that then other views would see the object before it is
	 * committed. Instead this method adds the object to a list maintained by
	 * the transaction manager. When the consumer iterates over the list, the
	 * objects in the 'added' list are appended to the items returned by the
	 * underlying committed list.
	 * <P>
	 * This method is used only if transactions are nested. The API between the
	 * model and the application does not support a method for creating an
	 * object and setting the property values in a single call. That can only be
	 * done when using a transaction manager.
	 */
	@Override
	public <F extends E> F createNewElement(IExtendablePropertySet<F> propertySet, IValues<F> values) {
		UncommittedObjectKey objectKey = new UncommittedObjectKey(transactionManager);

		// We can now create the object.
		F newObject = propertySet.constructImplementationObject(objectKey, new ListKey<E,S>(uncommittedParentKey, listAccessor), values);
		
		objectKey.setObject(newObject);

		addedObjects.add(newObject);
		
		/*
		 * Ensure this list is in the list of lists that have been
		 * modified within this transaction manager.
		 */
		transactionManager.modifiedLists.add(this);
		
		return newObject;
	}

	/**
	 * ReferenceViolationException is never thrown by this method.  It will be thrown
	 * when an attempt is made to commit the transaction.
	 */
	@Override
	public void deleteElement(E element) throws ReferenceViolationException {
		boolean isRemoved;
		
		UncommittedObjectKey uncommittedKey = (UncommittedObjectKey)element.getObjectKey();
		if (uncommittedKey.isNewObject()) {
			isRemoved = addedObjects.remove(element);
		} else {
			if (deletedObjects.contains(uncommittedKey.getCommittedObjectKey())) {
				isRemoved = false;
			} else {
			deletedObjects.add(uncommittedKey.getCommittedObjectKey());
			
			// TODO: following return value may not be correct.
			// However, it is expensive to see if the object
			// exists in the original list, so assume it does.
			isRemoved = true;
			}
		}
		
		
		if (!isRemoved) {
			throw new RuntimeException("Element not in list");
		}
		
		/*
		 * Ensure this object is in the transaction manager's list of lists
		 * that have changes.
		 */
		transactionManager.modifiedLists.add(this);

		/*
		 * Add this object to the map so that it is indicated as having been
		 * deleted. (But if we are deleting an object that was created in
		 * this same transaction and has never been committed then we must
		 * not add to the list).
		 */
		// TODO: This code may not be necessary.  It is probably better to flag the object itself
		// when an object is deleted.
		IObjectKey committedObjectKey = ((UncommittedObjectKey)element.getObjectKey()).getCommittedObjectKey();
		if (committedObjectKey != null) {
			ModifiedObject modifiedObject = transactionManager.modifiedObjects.get(committedObjectKey);
			if (modifiedObject == null) {
				modifiedObject = new ModifiedObject();
				transactionManager.modifiedObjects.put(committedObjectKey, modifiedObject);
			}
			modifiedObject.setDeleted();
		}
	}

	@Override
	public <F extends E> void moveElement(F element, IListManager<? super F> originalList) {
		/*
		 * It is fairly complex to implement this inside a transaction.
		 * Therefore we do not support this.
		 */ 
		throw new RuntimeException("Not implemented."); //$NON-NLS-1$
	}
	
    @Override	
	public int size() {
		// This method is called, for example when getting the number of entries
		// in a transaction.
		
		return committedList.size()
			+ addedObjects.size() 
			- deletedObjects.size(); 
	}

    @Override	
	public Iterator<E> iterator() {
		Iterator<E> committedListIterator = committedList.iterator();

		/*
		 * Even if there were no changes to the list, we cannot simply return
		 * committedListIterator because that returns materializations of the
		 * objects that are outside of the transaction. We must return objects
		 * that are versions inside the transaction.
		 */
		return new DeltaListIterator<E>(transactionManager, committedListIterator, addedObjects, deletedObjects);
	}

    @Override	
	public boolean contains(Object object) {
		IObjectKey committedObjectKey = ((UncommittedObjectKey)((IModelObject)object).getObjectKey()).getCommittedObjectKey();

		if (addedObjects.contains(object)) {
				return true; 
			} else if (deletedObjects.contains(committedObjectKey)) {
				return false;
			}

		// The object has neither been added or removed by us, so
		// pass the request on the the underlying datastore.
		return committedList.contains(committedObjectKey.getObject());
	}

    @Override	
	public boolean add(E object) {
    	/*
    	 * This method is used only when an object is moved from one list
    	 * to another.  Moving an object is not supported when inside a
    	 * transaction because it is too complicated.
    	 */
		throw new RuntimeException("Not implemented."); //$NON-NLS-1$
	}

    @Override	
	public boolean remove(Object object) {
    	/*
    	 * This method is used only when an object is moved from one list
    	 * to another.  Moving an object is not supported when inside a
    	 * transaction because it is too complicated.
    	 */
		throw new RuntimeException("Not implemented."); //$NON-NLS-1$
	}

	/**
	 * Return the collection of objects in the list that do not exist in the
	 * committed datastore but which are being added by this transaction.
	 * 
	 * @return collection of elements of type IModelObject, being the
	 *         uncommitted versions of the objects being added
	 */
	public Collection<E> getAddedObjects() {
		return addedObjects;
	}

	/**
	 * Return the collection of objects in the list that exist in the committed
	 * datastore but which are being deleted by this transaction.
	 * 
	 * @return a collection of elements of type IObjectKey, being the committed
	 *         keys of the objects being deleted
	 */
	Collection<IObjectKey> getDeletedObjects() {
		return deletedObjects;
	}
}
