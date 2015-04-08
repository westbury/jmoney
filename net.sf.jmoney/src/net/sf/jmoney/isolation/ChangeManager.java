/*
 *
 *  JMoney - A Personal Finance Manager
 *  Copyright (c) 2004 Nigel Westbury <westbury@users.sourceforge.net>
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


import java.util.ArrayList;
import java.util.List;

/**
 * Keeps track of changes made to the model. This is done to enable the
 * undo/redo feature.
 * <P>
 * As changes are undone and redone, the id of each object may change. For
 * example, in the serializeddatastore plug-in, the id of each object is a
 * reference to the object itself, i.e. the java identity, so the identity of
 * objects will not be the same when the object is re-created. If the datastore
 * is a database, for example in the jdbcdatastore plug-in, the id is
 * automatically generated as a value of a unique column. The database may
 * decide to re-use the id of a deleted row.
 * <P>
 * Because of these issues, this class never stores ids of objects that have
 * been deleted. When an object is deleted, all old values that reference the
 * object are replaced with references to the delete entry. This allows the data
 * to be re-created correctly by the undo method.
 * 
 * @author Nigel Westbury
 */
public class ChangeManager {

	/**
	 * When we delete an object, we know that nothing in the
	 * datastore references it.  However, there may be old
	 * values that referenced it.  It is important that these
	 * old values are updated to reference this deleted object.
	 * Otherwise, if the object is re-created with a different
	 * id then those old values cannot be restored correctly.
	 */
	private class KeyProxy {
		IObjectKey key;

		KeyProxy(IObjectKey key) {
			this.key = key;
		}
	}

	/*
	 * If there are no references to the KeyProxy then this means there are no changes that
	 * need the key proxy to undo the change.  The entry can be removed from the map.
	 * Thus we use a map with weak value references.
	 */
	private WeakValuedMap<IObjectKey, KeyProxy> keyProxyMap = new WeakValuedMap<IObjectKey, KeyProxy>();

	private UndoableChange currentUndoableChange = null;

	public class UndoableChange {
		/**
		 * Vector of ChangeEntry objects.  Changes are added to
		 * this vector in order.  If changes are undone, they must
		 * be undone in reverse order, starting at the end of this
		 * vector.
		 */
		private List<ChangeEntry> changes = new ArrayList<ChangeEntry>();

		/**
		 * Submit a series of updates, which have been stored,
		 * to the datastore.  These updates carry out the reverse
		 * of the updates stored.
		 */
		public void undoChanges() {
			// Undo the changes in reverse order.
			for (int i = changes.size() - 1; i >= 0; i--) {
				ChangeEntry changeEntry = changes.get(i);
				changeEntry.undo();
			}
		}

		/**
		 * @param newChangeEntry
		 */
		void addChange(ChangeEntry newChangeEntry) {
			changes.add(newChangeEntry);
		}
	}

	/**
	 * Base class for all objects that represent a component of
	 * a change.  Derived classes represent property changes,
	 * insertion of new objects, and deletion of objects.
	 * 
	 * These objects have only a constructor and the <code>undo</code> method.
	 * Once the <code>undo</code> is called the object is dead.
	 */
	abstract class ChangeEntry {
		abstract void undo();
	}

	/**
	 * A ChangeEntry object for an update to a scalar property (excluding
	 * scalar properties that are references to model objects).
	 *
	 * @param <S> type of the object that is the parent object for the property,
	 * 			though the actual type of the source object may be a class that
	 * 			extends S
	 * @param <V>
	 */
	class ChangeEntry_UpdateScalar<S extends IModelObject, V> extends ChangeEntry {
		private KeyProxy objectKeyProxy;
		private IScalarPropertyAccessor<V,? super S> propertyAccessor;
		private V oldValue = null;

		ChangeEntry_UpdateScalar(KeyProxy objectKeyProxy,
				IScalarPropertyAccessor<V,? super S> propertyAccessor, V oldValue) {
			this.objectKeyProxy = objectKeyProxy;
			this.propertyAccessor = propertyAccessor;
			this.oldValue = oldValue;
		}

		@Override
		void undo() {
			S object = (S)objectKeyProxy.key.getObject(); // efficient???
			propertyAccessor.setValue(object, oldValue);
		}
	}

	/**
	 * A ChangeEntry object for an update to a scalar property that is a
	 * reference to an model object.
	 *
	 * @param <E>
	 */
	// TODO: E should be bounded to classes that extend IModelObject.
	// However, this method does not currently make use of such bounding,
	// and to do that we would have to push back seperate methods for
	// reference properties and other scalar properties.
	class ChangeEntry_UpdateReference<S extends IModelObject, E> extends ChangeEntry {
		private KeyProxy objectKeyProxy;
		private IScalarPropertyAccessor<E,? super S> propertyAccessor;
		private KeyProxy oldValueProxy = null;

		ChangeEntry_UpdateReference(KeyProxy objectKeyProxy,
				IScalarPropertyAccessor<E,? super S> propertyAccessor,
				KeyProxy oldValueProxy) {
			this.objectKeyProxy = objectKeyProxy;
			this.propertyAccessor = propertyAccessor;
			this.oldValueProxy = oldValueProxy;
		}

		@Override
		void undo() {
			S object = (S)objectKeyProxy.key.getObject(); // efficient???
			// If IObjectKey had a type parameter, we would not need
			// this cast.
			propertyAccessor.setValue(object, propertyAccessor
					.getClassOfValueObject()
					.cast(oldValueProxy.key.getObject()));
		}
	}

	class ChangeEntry_Insert<E extends IModelObject, S extends IModelObject> extends ChangeEntry {
		private KeyProxy parentKeyProxy;
		private IListPropertyAccessor<E,? super S> owningListProperty;
		private KeyProxy objectKeyProxy;

		ChangeEntry_Insert(KeyProxy parentKeyProxy,
				IListPropertyAccessor<E,? super S> owningListProperty,
				KeyProxy objectKeyProxy) {
			this.parentKeyProxy = parentKeyProxy;
			this.owningListProperty = owningListProperty;
			this.objectKeyProxy = objectKeyProxy;
		}

		@Override
		void undo() {
			// Delete the object.
			E object = (E)objectKeyProxy.key.getObject(); // efficient???
			S parent = (S)parentKeyProxy.key.getObject();

			// Delete the object from the datastore.
			try {
				owningListProperty.getElements(parent).deleteElement(object);
			} catch (ReferenceViolationException e) {
				/*
				 * This should not happen because we are going back to a state that we
				 * were in before.  The object was created, and now we are un-doing that create,
				 * we are deleting the object.  It can't possibly have references to it.
				 */
				throw new RuntimeException("Internal error", e);
			}
		}
	}

	/**
	 * @param <E> the type of the object being deleted
	 */
	class ChangeEntry_Delete<E extends IModelObject, S extends IModelObject> extends ChangeEntry {
		// TODO if we make this a map instead of an array then we don't need
		// to maintain all that index stuff.
		private Object[] oldValues;
//		private Collection<ExtensionPropertySet<?,?>> nonDefaultExtensions;
		
		private KeyProxy parentKeyProxy;
		private IListPropertyAccessor<E,S> owningListProperty;
		private KeyProxy objectKeyProxy;
		private IExtendablePropertySet<? extends E> actualPropertySet;

		ChangeEntry_Delete(KeyProxy parentKeyProxy,
				IListPropertyAccessor<E,S> owningListProperty, E oldObject) {
			this.parentKeyProxy = parentKeyProxy;
			this.owningListProperty = owningListProperty;

			this.objectKeyProxy = getKeyProxy(oldObject.getObjectKey());
			this.actualPropertySet = owningListProperty.getElementPropertySet()
					.getActualPropertySet(
							(Class<? extends E>) oldObject.getClass());

			/*
			 * Save all the property values from the deleted object. We need
			 * these to re-create the object if this change is undone.
			 */
			saveDeletedValues(actualPropertySet, oldObject);
		}
		
		private <E2 extends E> void saveDeletedValues(IExtendablePropertySet<E2> actualPropertySet, E oldObject1) {
			E2 oldObject = (E2)oldObject1;
			
			/*
			 * Save all the property values from the deleted object. We need
			 * these to re-create the object if this change is undone.
			 */
//			nonDefaultExtensions = oldObject.getExtensions();

			int count = actualPropertySet.getScalarProperties3().size();
			oldValues = new Object[count];
			int index = 0;
			for (IScalarPropertyAccessor<?,? super E2> propertyAccessor : actualPropertySet
					.getScalarProperties3()) {
				if (index != actualPropertySet.getIndexIntoScalarProperties(propertyAccessor)) {
					throw new RuntimeException("index mismatch"); //$NON-NLS-1$
				}

				Object value = propertyAccessor.getValue(oldObject);
				if (value instanceof IModelObject) {
					/*
					 * We can't store model objects or even the object keys
					 * because those may not remain valid (the referenced object may
					 * be deleted). We store instead a KeyProxy. If the referenced
					 * object is later deleted, then un-deleted using an undo
					 * operation, then this change is also undone, the key proxy
					 * will give us the new object key for the referenced object.
					 */
					IObjectKey objectKey = ((IModelObject) value)
							.getObjectKey();
					oldValues[index++] = getKeyProxy(objectKey);
				} else {
					oldValues[index++] = value;
				}
			}
		}

		@Override
		void undo() {
			/* Create the object in the datastore.
			 * However, we must first convert the key proxies back to keys before passing
			 * on to the constructor.
			 */
			IValues oldValues2 = new IValues<E>() {

				@Override
				public <V> V getScalarValue(IScalarPropertyAccessor<V,? super E> propertyAccessor) {
					return propertyAccessor.getClassOfValueObject().cast(oldValues[actualPropertySet.getIndexIntoScalarProperties(propertyAccessor)]);
				}

				@Override
				public IObjectKey getReferencedObjectKey(
						IReferencePropertyAccessor<? extends IModelObject,? super E> propertyAccessor) {
					KeyProxy keyProxy = (KeyProxy)oldValues[actualPropertySet.getIndexIntoScalarProperties(propertyAccessor)];
					return keyProxy == null ? null : keyProxy.key;
				}

				@Override
				public <E2 extends IModelObject> IListManager<E2> getListManager(
						IObjectKey listOwnerKey,
						IListPropertyAccessor<E2,? super E> listAccessor) {
					return listOwnerKey.constructListManager((IListPropertyAccessor<E2,? super E>)listAccessor);
				}
			};
			
			S parent = (S)parentKeyProxy.key.getObject();
			IModelObject object = owningListProperty.getElements(parent).createNewElement(actualPropertySet,
					oldValues2, false);

			/*
			 * Set the new object key back into the proxy. This ensures that
			 * earlier changes to this object will be undone in this object. We
			 * must also add to our map so that if further changes are made that
			 * reference this object key, they will be using the same proxy.
			 */
			if (objectKeyProxy.key != null) {
				throw new RuntimeException("internal error - key proxy error"); //$NON-NLS-1$
			}
			objectKeyProxy.key = object.getObjectKey();
			keyProxyMap.put(objectKeyProxy.key, objectKeyProxy);
		}
	}

	/**
	 * A ChangeEntry object for a move of an object from one list to another.
	 */
	class ChangeEntry_Move<E extends IModelObject, S extends IModelObject> extends ChangeEntry {
		private E movedObject;
		private KeyProxy originalParentKeyProxy;
		private IListPropertyAccessor<? super E,? super S> originalListProperty;

		ChangeEntry_Move(E movedObject,
				KeyProxy originalParentKeyProxy,
				IListPropertyAccessor<? super E,? super S> originalListProperty) {
			this.movedObject = movedObject;
			this.originalParentKeyProxy = originalParentKeyProxy;
			this.originalListProperty = originalListProperty;
		}

		@Override
		void undo() {
			S originalParent = (S)originalParentKeyProxy.key.getObject(); // efficient???
			originalListProperty.getElements(originalParent).moveElement(movedObject);
		}
	}

	private KeyProxy getKeyProxy(IObjectKey objectKey) {
		if (objectKey != null) {
			KeyProxy keyProxy = keyProxyMap.get(objectKey);
			if (keyProxy == null) {
				keyProxy = new KeyProxy(objectKey);
				keyProxyMap.put(objectKey, keyProxy);
			}
			return keyProxy;
		} else {
			return null;
		}
	}

	private void addUndoableChangeEntry(ChangeEntry changeEntry) {
		if (currentUndoableChange != null) {
			currentUndoableChange.addChange(changeEntry);
		}
		
		/*
		 * If changes are made while currentUndoableChange is set to null then
		 * the changes are not undoable. This is supported but is not common. It
		 * is typically used for very large transactions such as imports of
		 * entire databases by the copier plug-in.
		 */
		// TODO: We should really clear out the change history as
		// prior changes are not likely to be undoable after this
		// change has been applied. 
	}

	/**
	 * The property may be any property in the passed object.
	 * The property may be defined in the actual class or
	 * any super classes which the class extends.  The property
	 * may also be a property in any extension class which extends
	 * the class of this object or which extends any super class
	 * of the class of this object.
	 */
	public <S extends IModelObject, V> void processPropertyUpdate(S object,
			IScalarPropertyAccessor<V,? super S> propertyAccessor, V oldValue, V newValue) {

		/*
		 * If the property value is a model object then we need special processing to
		 * ensure everything works correctly.  The keys are replaced by proxy keys so all
		 * will work correctly.  
		 * 
		 * The interesting case is when a change is made to remove the
		 * one and only reference to an object and then another change is made to delete that
		 * object.  Now undo both changes.  The deleted object will be re-created and the undo
		 * of the reference change should now reference the re-created object even if the id of
		 * the object assigned by the database is different.
		 */
		if (IModelObject.class.isAssignableFrom(propertyAccessor.getClassOfValueObject())) {
			IObjectKey oldObjectKey = oldValue == null ? null : ((IModelObject)oldValue).getObjectKey();
			ChangeEntry newChangeEntry = new ChangeEntry_UpdateReference<S,V>(
					getKeyProxy(object.getObjectKey()), propertyAccessor, getKeyProxy(oldObjectKey));
			addUndoableChangeEntry(newChangeEntry);
		} else {
			ChangeEntry newChangeEntry = new ChangeEntry_UpdateScalar<S,V>(
					getKeyProxy(object.getObjectKey()), propertyAccessor,
					oldValue);

			addUndoableChangeEntry(newChangeEntry);
		}
	}

	public <E extends IModelObject, S extends IModelObject> void processObjectCreation(ListKey<E,S> owningListKey, E newObject) {

		ChangeEntry newChangeEntry = new ChangeEntry_Insert<E,S>(
				getKeyProxy(owningListKey.getParentKey()),
				owningListKey.getListPropertyAccessor(), 
				getKeyProxy(newObject.getObjectKey()));

		addUndoableChangeEntry(newChangeEntry);
	}

	/**
	 * Processes the deletion of an object. This involves adding the property
	 * values to the change list so that the deletion can be undone.
	 * <P>
	 * Also we must call this method recursively on any objects contained in any
	 * list properties in the object. This is because this object 'owns' such
	 * objects, and so those objects will also be deleted and must be restored
	 * if this operation is undone.
	 * 
	 * @param <E>
	 * @param parent
	 * @param owningListProperty
	 * @param oldObject
	 */
	public <E extends IModelObject, S extends IModelObject> void processObjectDeletion(
			S parent,
			IListPropertyAccessor<E,S> owningListProperty, E oldObject) {

		/*
		 * We must also process objects owned by this object in a recursive
		 * manner. Otherwise, undoing the deletion of an object will not restore
		 * any objects owned by that object.
		 */
		IExtendablePropertySet<E> propertySet = oldObject.getPropertySet();
		for (IListPropertyAccessor<?,? super E> subList : propertySet.getListProperties3()) {
			processObjectListDeletion(oldObject, subList);
		}

		ChangeEntry_Delete<E,S> newChangeEntry = new ChangeEntry_Delete<E,S>(
				getKeyProxy(parent.getObjectKey()), owningListProperty,
				oldObject);

		/*
		 * The actual key is no longer valid, so we remove the proxy from the
		 * map that maps object keys to proxies. For safety we also set this to
		 * null.
		 * 
		 * Note that the proxy itself still exists.  If this deletion is later
		 * undone then the object is re-inserted and will be given a new object
		 * key by the underlying datastore.  That new object key will then be set in
		 * the proxy and the proxy will be added back to the map with the new
		 * object key.   
		 */

		// Remove from the map.
		keyProxyMap.remove(newChangeEntry.objectKeyProxy.key);

		// This line may not be needed, as the key should never
		// be accessed if the proxy represents a key that currently
		// does not exist in the datastore.  This line is here for
		// safety only.
		newChangeEntry.objectKeyProxy.key = null;

		addUndoableChangeEntry(newChangeEntry);
	}

	/**
	 * Processes the moving of an object. This involves adding the original
	 * parent object and parent list to the change list so that the move
	 * can be undone.
	 * 
	 * @param <E>
	 * @param parent
	 * @param owningListProperty
	 * @param oldObject
	 */
	public <E extends IModelObject, S extends IModelObject> void processObjectMove(
			E movedObject,
			ListKey<? super E, S> originalParentListKey) {

		ChangeEntry_Move<E,S> newChangeEntry = new ChangeEntry_Move<E,S>(
				movedObject,
				getKeyProxy(originalParentListKey.getParentKey()), 
				originalParentListKey.getListPropertyAccessor());

		addUndoableChangeEntry(newChangeEntry);
	}

	/**
	 * Helper function to process the deletion of all objects in a list
	 * property.
	 * 
	 * @param <E>
	 * @param parent the object containing the list
	 * @param listProperty the property accessor for the list
	 */
	private <E extends IModelObject, S extends IModelObject> void processObjectListDeletion(S parent, IListPropertyAccessor<E,? super S> listProperty) {
		for (E childObject : listProperty.getElements(parent)) {
			processObjectDeletion(parent, listProperty, childObject);
		}
	}

	public void setUndoableChange() {
		currentUndoableChange = new UndoableChange();
	}

	public UndoableChange takeUndoableChange() {
		UndoableChange result = currentUndoableChange;
		currentUndoableChange = null;
		return result;
	}
}
