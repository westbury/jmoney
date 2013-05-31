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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.sf.jmoney.JMoneyPlugin;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.operations.IOperationHistory;
import org.eclipse.core.commands.operations.IUndoContext;
import org.eclipse.core.commands.operations.IUndoableOperation;
import org.eclipse.core.commands.operations.UndoContext;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

/**
 * A transaction manager must be set before the datastore can be modified.
 * An exception will be throw if an attempt is made to modify the datastore
 * (setting a property, or creating or deleting an object) when
 * no transaction manager is set in the session.
 * <P>
 * Changes to the datastore are stored in the transaction manager.  They
 * are not applied to the underlying datastore until the transaction is
 * committed.  Read accesses (property getters and queries) are passed on
 * to any transaction manager which will modify the results to reflect
 * changes stored in the transaction.
 *
 * @author Nigel Westbury
 */
public abstract class TransactionManager extends AbstractDataManager {
	private IDataManager baseDataManager;

	/**
	 * Maps IObjectKey to Map, where IObjectKey is the key in the comitted
	 * datastore and each Map maps PropertyAccessor to the value of that
	 * property. Every object that has been modified by this transaction manager
	 * (a scalar property in the object has been changed) will have an entry in
	 * this map. The map keys are objects from the datastore and will contain
	 * the property values that are currently committed in the datastore. The
	 * map values are objects that contain details of the changes (changed
	 * scalar property values, or an indication that the object has been
	 * deleted).
	 * <P>
	 * If a value of a property is a reference to another object then an
	 * UncommittedObjectKey is stored as the value. By doing this, the
	 * referenced object does not need to be materialized unless necessary.
	 * <P>
	 * Deleted objects will also have an entry in this map. If an object
	 * contains list properties and the object is deleted then all the objects
	 * in the lists will also be added to this map with a ModifiedObject that
	 * has a 'deleted' indication. This is necessary because this list is used
	 * to determine if an object has been deleted, to ensure that we do not
	 * attempt to modify a property value in an object that has in fact been
	 * deleted.
	 */
	// TODO: Now we have the allObjects map, this map can be removed.  The code
	// should be significantly simplified now that we don't have to re-apply
	// the changes each time an object is requested.
	// (Of course, the complications now need to be added to the listener code
	// that needs to keep the objects in allObjects map up to date
	// and tell our listeners.
	protected Map<IObjectKey, ModifiedObject> modifiedObjects = new HashMap<IObjectKey, ModifiedObject>();

	/**
	 * Every model object that exists in the base data manager and that has
	 * already had a copy created in this data manager will be put in this map.
	 * This is required because all DataManager objects must guarantee that
	 * there is only ever a single instance of an object in existence.
	 * <P>
	 * Objects that were created in this transaction (i.e. do not exist in the
	 * base data manager) are not in this map. There is no risk of having two
	 * instances of the same such object because all such objects created by
	 * this class are distinct objects.
	 * <P>
	 * The object key is the key in the committed datastore. Only objects that
	 * exist in the underlying datastore are in this map, and it is just easier
	 * to use the committed key than to use an uncommitted key.
	 */
	Map<IObjectKey, IModelObject> allObjects = new HashMap<IObjectKey, IModelObject>();

	/**
	 * Every list that has been modified by this transaction manager
	 * (objects added to the list or objects removed from the list)
	 * will have an entry in this set.  This enables the transaction
	 * manager to easily find the added and deleted objects when committing
	 * the changes.
	 */
	public Set<DeltaListManager> modifiedLists = new HashSet<DeltaListManager>();

	/**
	 * true if a nested transaction is in the process of applying its
	 * changes to our data
	 */
	// TODO: use this flag to ensure that performRefresh is called correctly.
	@SuppressWarnings("unused")
	private boolean insideTransaction = false;

	/**
	 * This listener is used to listen for changes in the base session. By
	 * listening for such changes, we ensure that our view of the session is
	 * kept up to date with changes.
	 * <P>
	 * Note that we must keep a reference to this listener for as long as this
	 * transaction manager exists. The reason for this is that the listener is
	 * kept as a weak reference by the base transaction manager. We don't want
	 * it to be garbage collected as long as this transaction manager exists.
	 */
	private MySessionChangeListener baseSessionChangeListener = new MySessionChangeListener();

	private IUndoContext undoContext = new UndoContext();

    /**
	 * Construct a transaction manager for use with the given session.
	 * The transaction manager does not become the active transaction
	 * manager for the session until it is specifically set as the
	 * active transaction manager.  By separating the construction of
	 * the transaction manager from the activating of the transaction manager,
	 * a transaction manager can be created and listeners can be set up to
	 * listen to session changes within the transaction manager during an
	 * initialization stage even though the transaction is not made active
	 * until changes are made.
	 *
	 * @param session the session object from the committed datastore
	 */
	public TransactionManager(IDataManager baseDataManager) {
		this.baseDataManager = baseDataManager;

		/*
		 * Listen for changes to the base data. Note that a weak reference is
		 * maintained to this listener so we don't have to worry about removing
		 * the listener, which is just as well because this transaction manager
		 * may be left for the garbage collector without knowing when it is no
		 * longer being used.
		 */
		baseDataManager.addChangeListenerWeakly(baseSessionChangeListener);
	}

	/**
	 * Indicate whether there are any uncommitted changes being held
	 * by this transaction manager.  This method is useful when the user
	 * does something like selecting another transaction or closing a
	 * dialog box and it is not clear whether the user wants to commit
	 * or to cancel changes.
	 *
	 * @return true if property values have been changed or objects
	 * 			have been created or deleted within the context of this
	 * 			transaction manager since the last commit
	 */
	public boolean hasChanges() {
		return !modifiedObjects.isEmpty()
		|| !modifiedLists.isEmpty();
	}

	/**
     * Given an instance of an object in the datastore
     * (i.e. committed), obtain a copy of the object that
     * is in the version of the datastore managed by this
     * transaction manager.
     * <P>
     * Updates to property values in the returned object will
     * not be applied to the datastore until the changes held
     * by this transaction manager are committed to the datastore.
     *
     * @param an object that exists in the datastore (committed)
     * @return a copy of the given object, being an uncommitted version
     * 			of the object in this transaction, or null if the
     * 			given object has been deleted in this transaction
     */
    public <E extends IModelObject> E getCopyInTransaction(final E committedObject) {
    	/*
		 * As a convenience to the caller, this method accepts null objects. If
		 * a reference is null in the committed version of the data then it
		 * should of course be null in the uncommitted version of the data.
		 */
    	if (committedObject == null) {
    		return null;
    	}

    	if (committedObject.getDataManager() != baseDataManager) {
    		throw new RuntimeException("Invalid call to getCopyInTransaction.  The object passed must belong to the data manager that is the base data manager of this transaction manager."); //$NON-NLS-1$
    	}

    	/*
		 * First look in our map to see if this object has already been modified
		 * within the context of this transaction manager. If it has, return the
		 * modified version. Also check to see if the object has been deleted
		 * within the context of this transaction manager. If it has then we
		 * raise an error.
		 *
		 * Both these situations are not likely to happen because usually one
		 * object is copied into the transaction manager and all other objects
		 * are obtained by traversing from that object. However, it is good to
		 * check.
		 */
		IModelObject objectInTransaction = allObjects.get(committedObject.getObjectKey());
		if (objectInTransaction != null) {
			// TODO: decide if and how we check for deleted objects.
//			if (objectInTransaction.isDeleted()) {
//				throw new RuntimeException("Attempt to get copy of object, but that object has been deleted in the transaction");
//			}
			return (E)committedObject.getClass().cast(objectInTransaction);
		}

		IExtendablePropertySet<E> propertySet = committedObject.getPropertySet();

		ListKey<? super E,?> committedListKey = committedObject.getParentListKey();
		ListKey<? super E,?> listKey;
		if (committedListKey == null) {
			listKey = null;
		} else {
			IObjectKey committedParentKey = committedListKey.getParentKey();
			UncommittedObjectKey parentKey = new UncommittedObjectKey(this, committedParentKey);
			listKey = new ListKey(parentKey, committedListKey.getListPropertyAccessor());
		}

		final UncommittedObjectKey key = new UncommittedObjectKey(this, committedObject.getObjectKey());

		/*
		 * If the object has been modified by this transaction, it will be in
		 * the allObjects map (which contains all objects ever returned by this
		 * transaction)
		 */

		IValues<E> values = new IValues<E>() {
			@Override
			public IObjectKey getReferencedObjectKey(IReferencePropertyAccessor<?,? super E> propertyAccessor) {
				IObjectKey committedObjectKey = propertyAccessor.invokeObjectKeyField(committedObject);
				return (committedObjectKey == null)
				? null
						: new UncommittedObjectKey(TransactionManager.this, committedObjectKey);
			}

			@Override
			public <V> V getScalarValue(IScalarPropertyAccessor<V,? super E> propertyAccessor) {
				return propertyAccessor.getValue(committedObject);
			}

			@Override
			public <E2 extends IModelObject> IListManager<E2> getListManager(IObjectKey listOwnerKey, IListPropertyAccessor<E2,? super E> listAccessor) {
				return new DeltaListManager<E2,E>(TransactionManager.this, committedObject, key, listAccessor);
			}
		};

		// We can now create the object.
    	E copyInTransaction = (E)committedObject.getClass().cast(propertySet.constructImplementationObject(key, listKey, values));

    	/*
    	 * Now we have created a version of this object that is valid in this datastore,
    	 * put it in the map so that we can guarantee that we always return the same
    	 * instance in future.
    	 */
    	allObjects.put(committedObject.getObjectKey(), copyInTransaction);

    	// We do not copy lists owned by the object at this time.
    	// If a list is iterated, we must return copies in this transaction.
    	// Originals may never be materialized.

    	// If a list is iterated multiple times, a new set
    	// is instantiated.  This is consistent with list processing
    	// when objects are stored in database - do not cache the
    	// list because it must then be kept up to date and the
    	// list may no longer be needed, plus lists are not in general
    	// iterated more than once because the consumer should keep its
    	// own data up to date using listeners.

    	// This is ok because the API contract states
    	// that if the user gets the same object
    	// twice then the user must listen for changes and refresh
    	// the other objects.  This is something the user has to
    	// do anyway because objects could be out of date due to
    	// changes to the committed version.

    	// Therefore, a list is really a delta from the committed list.

    	// How does the delta get the committed list?
    	// We could pass an iterator here, but that is a once
    	// off.  We must pass a dynamic collection (a collection
    	// that is a view of the committed items in the list)

    	return copyInTransaction;
    }

	/**
	 * Apply the changes that are stored in this transaction manager.
	 * <P>
	 * When changes are committed, they are seen in the datastore
	 * and also in the version of the datastore as seen through other
	 * transaction managers.
	 * <P>
	 * All datastore listeners and all listeners which are listening
	 * for changes ......
	 *
	 */
	public void commit() {
		/*
		 * First we remove our listener.  Although this is a 'weak' listener
		 * and so will be removed by the garbage collector we want to
		 * specifically remove it before a commit so we don't get back our
		 * own changes or other changes after this transaction is dead.
		 */
		baseDataManager.removeChangeListener(baseSessionChangeListener);

		baseDataManager.startTransaction();

		// Add all the new objects, but set references to other
		// new objects to null because the other new object may
		// not have yet been added to the database and thus no
		// reference can be set to the other object.
		for (DeltaListManager<?,?> modifiedList: modifiedLists) {
			commitObjectsInList(modifiedList);
		}

		// Update all the updated objects
		for (Map.Entry<IObjectKey, ModifiedObject> mapEntry: modifiedObjects.entrySet()) {
			IObjectKey committedKey = mapEntry.getKey();
			ModifiedObject newValuesMap = mapEntry.getValue();

			if (!newValuesMap.isDeleted()) {
				Map<IScalarPropertyAccessor, Object> propertyMap = newValuesMap.getMap();
/* Actually this does not work.  We must go through the setters because we are 'outside' the datastore.
 * The values would not otherwise be set in the objects themselves.

				IModelObject committedObject = committedKey.getObject();
				PropertySet<?> actualPropertySet = PropertySet.getPropertySet(committedObject.getClass());

				int count = actualPropertySet.getScalarProperties3().size();
				Object [] newValues = new Object[count];
				Object [] oldValues = new Object[count];

				// TODO: It may be better if we save the data from the committed
				// object early on, before any changes.  This really depends on
				// how we decide to cope with conflicts between the committed and
				// this data manager.
				int index = 0;
				for (ScalarPropertyAccessor<?,?> accessor: actualPropertySet.getScalarProperties3()) {
					Object value = committedObject.getPropertyValue(accessor);
					if (value instanceof IModelObject) {
						IModelObject referencedObject = (IModelObject)value;
						oldValues[index] = referencedObject.getObjectKey();
					} else {
						oldValues[index] = value;
					}

					if (propertyMap.containsKey(accessor)) {
						Object newValue = propertyMap.get(accessor);
						if (newValue instanceof UncommittedObjectKey) {
							UncommittedObjectKey referencedKey = (UncommittedObjectKey)newValue;
							newValues[index] = referencedKey.getCommittedObjectKey();
						} else {
							newValues[index] = value;
						}
					} else {
						// No change in the property value
						newValues[index] = oldValues[index];
					}

					index++;
				}
*/



				for (Map.Entry<IScalarPropertyAccessor, Object> mapEntry2: propertyMap.entrySet()) {
					IScalarPropertyAccessor<?,?> accessor = mapEntry2.getKey();
					Object newValue = mapEntry2.getValue();

					// TODO: If we create a method in IObjectKey for updating properties
					// then we don't have to instantiate the committed object here.
					// The advantages are small, however, because the key is likely to
					// need to read the old properties from the database before setting
					// the new property values.
					IModelObject committedObject = committedKey.getObject();

					if (newValue instanceof UncommittedObjectKey) {
						UncommittedObjectKey referencedKey = (UncommittedObjectKey)newValue;
						// TODO: We should not have to instantiate an instance of the
						// referenced object in the committed datastore.  However, there
						// is no method to set the key as the value.
						// We should add such a mechanism.
						newValue = referencedKey.getCommittedObjectKey().getObject();
					}
					setProperty(committedObject, accessor, newValue);
				}
			}
		}

		/*
		 * Delete all object marked for deletion. This is a two-step process.
		 * The first step involves iterating over all the objects marked for
		 * deletion and seeing if they contain any references to other objects
		 * that are also marked for deletion. If any such references are found,
		 * the reference is set to null. The second step involves actually
		 * deleting the objects. The reason why we must do this two-step process
		 * is that there may be circular references between objects marked for
		 * deletion, and the underlying database may raise a reference constraint
		 * violation if an object is deleted while other objects contain
		 * references to it.
		 *
		 * This code is not perfect and needs more work to make it perfect.
		 * Firstly, there may be a problem if the underlying database does not
		 * allow null values for a particular reference. In that case, we should
		 * ignore the failure to set the value to null. We set what we can to
		 * null and then go on to delete all the values. Secondly, we may have
		 * to make multiple passes while attempting to delete all the objects
		 * because if one contains a non-nullable reference to the other then it
		 * must be deleted first. It should be theoretically possible to delete
		 * the objects, because the database got into this state in the first
		 * case, and we can remove the objects by reversing the steps taken to
		 * get to this state.
		 */

		/*
		 * Step 1: Update all the objects marked for deletion, setting any
		 * references to other objects also marked for deletion to be null
		 * references.
		 */
		for (Map.Entry<IObjectKey, ModifiedObject> mapEntry: modifiedObjects.entrySet()) {
			IObjectKey committedKey = mapEntry.getKey();
			ModifiedObject newValues = mapEntry.getValue();

			if (newValues.isDeleted()) {
				IModelObject deletedObject = committedKey.getObject();

				IExtendablePropertySet<?> propertySet = deletedObject.getPropertySet();
				setValues(propertySet, deletedObject);
			}
		}

		/*
		 * Step 2: Delete the deleted objects
		 */
		for (DeltaListManager<?,?> modifiedList: modifiedLists) {
			deleteObjectsInList(modifiedList);
		}

		baseDataManager.commitTransaction();

		// Clear out the changes in the object. These changes are the
		// delta between the datastore and the uncommitted view.
		// Now that the changes have been committed, these changes
		// must be cleared.

		// TODO: Is this loop needed if the outer list is being cleared anyway?
		for (DeltaListManager<?,?> modifiedList: modifiedLists) {
			modifiedList.addedObjects.clear();
			modifiedList.deletedObjects.clear();
		}
		modifiedLists.clear();
		modifiedObjects.clear();
	}

	private <S extends IModelObject> void setValues(IExtendablePropertySet<S> propertySet,
			IModelObject deletedObjectUntyped) {
		S deletedObject = (S)deletedObjectUntyped;

		for (IScalarPropertyAccessor<?,? super S> accessor: propertySet.getScalarProperties3()) {
			Object value = accessor.getValue(deletedObject);
			if (value instanceof IModelObject) {
				IModelObject referencedObject = (IModelObject)value;
				IObjectKey committedReferencedKey = referencedObject.getObjectKey();
				ModifiedObject referencedNewValues = modifiedObjects.get(committedReferencedKey);
				if (referencedNewValues != null && referencedNewValues.isDeleted()) {
					// This is a reference to an object that is marked for deletion.
					// Set the reference to null
					accessor.setValue(deletedObject, null);
				}
			}
		}
	}

	private <E extends IModelObject, S extends IModelObject> void deleteObjectsInList(DeltaListManager<E,S> modifiedList) {
		S parent = modifiedList.committedParent;

		for (IObjectKey objectKeyToDelete: modifiedList.getDeletedObjects()) {
			E objectToDelete = (E)objectKeyToDelete.getObject();
			fireDestroyEvents(objectToDelete);
			try {
				modifiedList.listAccessor.getElements(parent).deleteElement(objectToDelete);
			} catch (ReferenceViolationException e) {
				/*
				 * We are trying to delete something that has references to it.
				 *
				 * We really need to pass back this error so it can be handled by the code that started this
				 * commit in a user-friendly way.  However that means every commit needs to catch this exception
				 * which is a lot of places.  We therefore convert it to a runtime exception here.  Unfortunately
				 * this means the users don't get a friendly message.
				 *
				 * Ideally we should perhaps check for references when changes are made within the transaction.
				 * However that would be a lot of work.
				 */
				throw new RuntimeException("Attempt to delete an object that has references", e);
			}
		}
	}

	private <S extends IModelObject> void fireDestroyEvents(final S objectToDelete) {
		IExtendablePropertySet<S> propertySet = objectToDelete.getPropertySet();

		for (IListPropertyAccessor<?,? super S> accessor : propertySet.getListProperties3()) {
			for (IModelObject childObject : accessor.getElements(objectToDelete)) {
				fireDestroyEvents(childObject);
			}
		}

		/*
		 * Fire the event to indicate that an object has been destroyed.
		 */
		baseDataManager.fireEvent(
				new ISessionChangeFirer() {
					@Override
					public void fire(SessionChangeListener listener) {
						listener.objectDestroyed(objectToDelete);
					}
				}
		);
	}

	/**
	 * This method does the same as the commit() method but the changes
	 * may be undone and redone.  This support is available with no
	 * coding needed by the caller other than to pass the label to be
	 * used to describe the operation.
	 *
	 * @param label the label to be used to describe this operation
	 * 			for undo/redo purposes
	 */
	public void commit(String label) {
		IUndoContext undoContext = baseDataManager.getUndoContext();
		IOperationHistory history = JMoneyPlugin.getDefault().getWorkbench().getOperationSupport().getOperationHistory();

		IUndoableOperation operation = new AbstractDataOperation(baseDataManager.getChangeManager(), label) {
			@Override
			public IStatus execute() throws ExecutionException {
				commit();
				return Status.OK_STATUS;
			}
		};

		operation.addContext(undoContext);
		try {
			history.execute(operation, null, null);
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private <S extends IModelObject, V> void setProperty(IModelObject committedObject, IScalarPropertyAccessor<V,S> accessor, Object newValue) {
		accessor.setValue((S)committedObject, accessor.getClassOfValueObject().cast(newValue));
	}

	/**
	 * Add a new uncommitted object to the committed datastore.
	 *
	 * All objects in any list properties in the object are also added, hence
	 * this method is recursive.
	 *
	 * @param newObject the uncommitted version of the new object
	 * @param parent the committed version of the parent into which this new
	 * 			object is to be inserted
	 * @param listAccessor the list into which this new object is to be
	 * 			inserted
	 * @return the committed version of the object
	 */
	private <E extends IModelObject, S extends IModelObject> E commitNewObject(final E newObject, S parent, IListPropertyAccessor<? super E,? super S> listAccessor, boolean isDescendentInsert) {
		IExtendablePropertySet<E> actualPropertySet = (IExtendablePropertySet<E>)listAccessor.getElementPropertySet().getActualPropertySet((Class<E>)newObject.getClass());

		/**
		 * Holds references to new objects that have never been committed, so
		 * that such references can be set later after all the new objects have
		 * been committed.
		 */
		final ModifiedObject deferredReferences = new ModifiedObject();

		IValues values = new IValues<E>() {
			@Override
			public IObjectKey getReferencedObjectKey(IReferencePropertyAccessor<?, ? super E> propertyAccessor) {
				// TODO remove unnecessary cast when generics added
				IModelObject referencedObject = propertyAccessor.getValue(newObject);
				if (referencedObject == null) {
					return null;
				}
				UncommittedObjectKey uncommittedKey = (UncommittedObjectKey)referencedObject.getObjectKey();
				IObjectKey committedKey = uncommittedKey.getCommittedObjectKey();
				if (committedKey != null) {
					return committedKey;
				} else {
					/*
					 * The property value is a reference to an object that may
					 * not have yet been committed to the datastore. Such values
					 * cannot be set in the datastore. We therefore set the
					 * property to null for the time being but add it to our
					 * list of properties to be set later.
					 */
					deferredReferences.put(propertyAccessor, uncommittedKey);
					return null;
				}
			}

			@Override
			public <V> V getScalarValue(IScalarPropertyAccessor<V,? super E> propertyAccessor) {
				return propertyAccessor.getValue(newObject);
			}

			@Override
			public <E2 extends IModelObject> IListManager<E2> getListManager(IObjectKey listOwnerKey, IListPropertyAccessor<E2,? super E> listAccessor) {
				/*
				 * Create an empty list manager. The implementation depends on
				 * the data manager, thus we request a list manager from the
				 * object key.
				 */
				return listOwnerKey.constructListManager(listAccessor);
			}
		};

		// Create the object with the appropriate property values
		ObjectCollection<? super E> propertyValues = listAccessor.getElements(parent);
		final E newCommittedObject = (E)propertyValues.createNewElement(actualPropertySet, values, isDescendentInsert);

		// Update the uncommitted object key to indicate that there is now a committed
		// version of the object in the datastore
		((UncommittedObjectKey)newObject.getObjectKey()).setCommittedObject(newCommittedObject);

		/*
		 * Fire the event to indicate that a new object has been created. Note
		 * that the objectCreated event, as opposed to the objectInserted event,
		 * is fired as soon as any object is created and before it's children
		 * are created. A later call to objectCreated will be made for each
		 * descendant.
		 */
		parent.getDataManager().fireEvent(
				new ISessionChangeFirer() {
					@Override
					public void fire(SessionChangeListener listener) {
						listener.objectCreated(newCommittedObject);
					}
				}
		);

		// Commit all the child objects in the list properties.
		for (IListPropertyAccessor<?,? super E> subListAccessor: actualPropertySet.getListProperties3()) {
			commitChildren(newObject, newCommittedObject, subListAccessor);
		}

		// If there are property changes that must be applied later, add the property
		// change map to the list
		if (!deferredReferences.isEmpty()) {
			modifiedObjects.put(newCommittedObject.getObjectKey(), deferredReferences);
		}

		return newCommittedObject;
	}


	private <E extends IModelObject, S extends IModelObject> void commitChildren(S newObject, S newCommittedObject, IListPropertyAccessor<E,? super S> subListAccessor) {
		for (E childObject: subListAccessor.getElements(newObject)) {
			commitNewObject(childObject, newCommittedObject, subListAccessor, true);
		}
	}

	private <E extends IModelObject, S extends IModelObject> void commitObjectsInList(DeltaListManager<E,S> modifiedList) {
		S parent = modifiedList.committedParent;

		for (IModelObject newUntypedObject: modifiedList.getAddedObjects()) {
			E newObject = modifiedList.listAccessor.getElementPropertySet().getImplementationClass().cast(newUntypedObject);

			final IModelObject newCommittedObject = commitNewObject(newObject, parent, modifiedList.listAccessor, false);

			/*
			 * Now we can fire the notifications of newly inserted objects. This
			 * is done now after all the descendants of the object have been
			 * created.
			 *
			 * Note that if this object references other objects that are added
			 * in this same transaction but that have not yet been added then
			 * the reference will be null. A later objectChanged event will be
			 * fired when the reference is later set to the correct value.
			 */
			parent.getDataManager().fireEvent(
					new ISessionChangeFirer() {
						@Override
						public void fire(SessionChangeListener listener) {
							listener.objectInserted(newCommittedObject);
						}
					}
			);
		}
	}

	class DeletedObject<E extends IModelObject, S extends IModelObject> {
		private S parent;
		private IListPropertyAccessor<E,S> owningListProperty;

		DeletedObject(S parent, IListPropertyAccessor<E,S> owningListProperty) {
			this.parent = parent;
			this.owningListProperty = owningListProperty;
		}

		void deleteObject(IModelObject object) {
			owningListProperty.getElements(parent).remove(object);
		}
	}

	/**
	 * This method is called when a nested transaction manager is about to apply its
	 * changes to our data.
	 */
	@Override
	public void startTransaction() {
		/*
		 * This method is called only when transactions are nested. The nested
		 * transaction will call this method before it applies the changes to
		 * this transaction.
		 */
		insideTransaction = true;
	}

	/**
	 * This method is called when a nested transaction manager has completed
	 * making changes to our data.
	 */
    @Override
	public void commitTransaction() {
		/*
		 * This method is called only when transaction are nested.
		 * The nested transaction will call this method after it has
		 * applied the changes to this transaction.
		 *
		 * Changes are applied to this object's data as the changes are
		 * made and there is nothing we need do to 'commit' the changes.
		 * However, we do need to fire the performRefresh event method
		 * at this time.  This event notifies our listeners that a batch
		 * of changes has completed and now is a good time to refresh
		 * views.
		 */
		fireEvent(
				new ISessionChangeFirer() {
					@Override
					public void fire(SessionChangeListener listener) {
						listener.performRefresh();
					}
				});

		insideTransaction = false;
	}

	/**
	 * This class contains the methods that merge changes
	 * from the base data into the data for this object,
	 * and also tell our listeners of such changes.
	 */
	private class MySessionChangeListener implements SessionChangeListener {

		@Override
		public void objectInserted(IModelObject newObject) {
			/*
			 * The object may contain references to objects
			 * that have been deleted in this view.
			 *
			 * In such a situation, the object deleted in this view
			 * could be first 'undeleted'.  If the undeleted object references
			 * other objects that were deleted by this view then those
			 * objects are in turn undeleted in a recursive manner.
			 *
			 * A simpler approach may be to ignore the new object until
			 * the transaction is committed.  At that time, the commit fails
			 * with a conflict exception.
			 */
			// TODO Implement this method
		}

		@Override
		public void objectCreated(IModelObject newObject) {
			// TODO Auto-generated method stub

		}

		@Override
		public void objectChanged(IModelObject changedObject, IScalarPropertyAccessor changedProperty, Object oldValue, Object newValue) {
			/*
			 * The property may have been changed to reference an
			 * object that has been deleted in this view.
			 *
			 * In such a situation, the object could be
			 * first 'undeleted' in this view.  If the undeleted object references
			 * other objects that were deleted by this view then those
			 * objects are in turn undeleted in a recursive manner.
			 *
			 * A simpler approach may be to ignore the new object until
			 * the transaction is committed.  At that time, the commit fails
			 * with a conflict exception.
			 *
			 * We also need to consider what happens if this property change made
			 * another property inapplicable, and that other property was changed
			 * in this view, or if this property has been made inapplicable as
			 * a result of a change in this view of another property.
			 * All sorts of possibilities to consider.
			 */

			// TODO For the time being, this does, but it is imperfect.
			IModelObject modelObject = getCopyInTransaction(changedObject);
			if (newValue instanceof IModelObject) {
				newValue = getCopyInTransaction((IModelObject)newValue);
			}
			changedProperty.setValue(modelObject, newValue);
		}

		@Override
		public void objectRemoved(IModelObject deletedObject) {
			/*
			 * If an object is deleted from the base data then we
			 * know there are no references to the object from the
			 * base data.  However, it is possible that a reference
			 * was created to this object in this version of the data.
			 *
			 * If that is the case then we could just not remove the object
			 * from this view of the data.  The object is removed from
			 * this view of the data only if all references to it are
			 * removed from this view.  If references still remain when
			 * this view is committed then a conflict error occurs.
			 * The user should be told that the object no longer exists
			 * and the user must remove references to it before attempting
			 * again to commit.
			 *
			 * A simpler approach may be to ignore the deletion of the object until
			 * the transaction is committed.  At that time, the commit fails
			 * with a conflict exception.
			 */
			// TODO Implement this method
		}

		@Override
		public void objectDestroyed(IModelObject deletedObject) {
			// TODO Auto-generated method stub

		}

		@Override
		public void performRefresh() {
			// TODO Auto-generated method stub

		}

		@Override
		public void objectMoved(IModelObject movedObject,
				IModelObject originalParent, IModelObject newParent,
				IListPropertyAccessor originalParentListProperty,
				IListPropertyAccessor newParentListProperty) {
			/*
			 * If an object was moved and the move conflicts with outstanding changes
			 * held by this transaction then we have problems.
			 * However, with the current JMoney feature set, this is unlikely
			 * to happen, so just ignore.  Views working off transactions will not
			 * see the move.  When the transaction commits, the changes should
			 * not conflict with the move.
			 */
		}
	}

	@Override
	public IUndoContext getUndoContext() {
		return undoContext;
	}
}
