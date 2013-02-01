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

import net.sf.jmoney.JMoneyPlugin;

import org.eclipse.core.runtime.Assert;

/**
 * This class provides the IObjectKey implementation for objects
 * that have not yet been committed to the datastore.
 * <P>
 * There are different types of uncommitted objects.
 * <UL>
 * <LI>Objects that have been created within this transaction but
 * 		not yet committed to the datastore
 * 		</LI>
 * <LI>Objects that already existed in the datastore but where an
 * 		instance has been instantiated within the context of this
 * 		transaction.  There may or may not be changes to scalar
 * 		properties made within the transaction
 * 		</LI>
 * <LI>Objects that have been created within this transaction and
 * 		have been committed to the datastore.  There may or may not
 * 		be changes to scalar properties made since the object was
 * 		last committed to the datastore
 * 		</LI>
 * </UL>
 * The last two categories are indistinguishable.  That is, it is
 * not possible to tell whether an object existed in the datastore
 * before the transaction manager was created and objects that were
 * added and committed by this transaction manager.
 * 
 * @author Nigel Westbury 
 */
public class UncommittedObjectKey implements IObjectKey {
	private TransactionManager transactionManager;
	
	// One or other of the following two fields will be set
	// to a non-null value, depending on whether the object
	// exists in the committed datastore.
	
	// If a newly created object is committed then
	// modelObject is set to null and any requests for
	// the object will then be carried out by instantiating
	// the committed object and applying any changes to
	// the scalar properties before instantiating the version
	// of the object within the transaction.  This is necessary
	// because changes made outside of the transaction manager
	// must be visible.
	
	private IObjectKey committedObjectKey = null;
	private IModelObject modelObject = null;
	
	/**
	 * This constructor is used to create an object key for an object that
	 * is newly created by this transaction manager.  No version of this
	 * object exists in the datastore.
	 * 
	 * @param committedReferencingObject
	 * @param referencingProperty
	 */
	public UncommittedObjectKey(TransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	/**
	 * This constructor is used to create an object key for an object that is
	 * being returned by a getter for a property referencing another model
	 * object or is being returned by a list property iterator or is being
	 * created from a given object in the datastore. In all these cases way, the
	 * object key from the datastore is available.
	 * 
	 * @param transactionManager
	 * @param committedObjectKey
	 */
	public UncommittedObjectKey(TransactionManager transactionManager, IObjectKey committedObjectKey) {
		Assert.isNotNull(committedObjectKey);
		this.transactionManager = transactionManager;
		this.committedObjectKey = committedObjectKey;
	}

	// TODO: make this default protection
	// Called only when this object constructed with the single parameter constructor.
	public void setObject(IModelObject modelObject) {
		Assert.isNotNull(modelObject);
		this.modelObject = modelObject;
	}

    @Override	
	public boolean equals(Object object) {
		if (object instanceof UncommittedObjectKey) {
			UncommittedObjectKey otherKey = (UncommittedObjectKey)object; 
			
			if (this.transactionManager != otherKey.transactionManager) {
				return false;
			}

			if (this.modelObject != null && otherKey.modelObject != null) {
				// These are new objects, never previously committed to the datastore,
				// so there can be only one instance and we can use the default
				// compare.
				return (this == otherKey);
			} else if (this.modelObject != null || otherKey.modelObject != null) {
				// One is new, one is not, so cannot be equal
				return false;
			} else {
				return JMoneyPlugin.areEqual(this.committedObjectKey, otherKey.committedObjectKey);
			}
		} else {
			return false;
		}
	}
	
    @Override	
	public int hashCode() {
		if (modelObject != null) {
			// These are new objects, never previously committed to the datastore,
			// so there can be only one instance and we can use the default
			// hashCode.
			return super.hashCode();
		} else if (committedObjectKey != null) {
			return committedObjectKey.hashCode();
		} else {
			throw new RuntimeException("bad case"); //$NON-NLS-1$
		}
	}
			
	@Override
	public IModelObject getObject() {
		// TODO: Perhaps this class should be split into two classes.
		// We then would not need this 'switch' construct.
		if (committedObjectKey != null) {
			return transactionManager.getCopyInTransaction(committedObjectKey.getObject());
		} else if (modelObject != null) {
			return modelObject;
		} else {
			throw new RuntimeException("bad case"); //$NON-NLS-1$
		}
	}

	@Override
	public void updateProperties(IExtendablePropertySet<?> actualPropertySet, Object[] oldValues, Object[] newValues) {
		/*
		 * If this object is a new object, never committed to the datastore,
		 * then we have nothing to do. However, if this object has been
		 * committed to the datastore then we must add the property changes to a
		 * map maintained by the transaction manager. This has two purposes: If
		 * other users request the object within the context of this transaction
		 * manager, then the changes can be picked up from the map and seen by
		 * the user. Secondly, the map is iterated to get the property updates
		 * that must be committed at commit time.
		 */
		
		if (committedObjectKey != null) {
			ModifiedObject modifiedObject = transactionManager.modifiedObjects.get(committedObjectKey);
			if (modifiedObject == null) {
				modifiedObject = new ModifiedObject();
				transactionManager.modifiedObjects.put(committedObjectKey, modifiedObject);
			}

			int i = 0;
			for (IScalarPropertyAccessor<?,?> propertyAccessor: actualPropertySet.getScalarProperties3()) {
				// If values are different, put in the map
				if (!JMoneyPlugin.areEqual(oldValues[i], newValues[i])) {
					if (newValues[i] instanceof IModelObject) {
						// propertyChangeMap must contain the UncommittedObjectKey,
						// not the uncommitted object itself which is passed to this
						// method.
						modifiedObject.put(propertyAccessor, ((IModelObject)newValues[i]).getObjectKey());
					} else {
						modifiedObject.put(propertyAccessor, newValues[i]);
					}
				}
				i++;
			}
		}
	}

	@Override
	public AbstractDataManager getDataManager() {
		// This method is only called to get optimized datastore interfaces
		// from the session manager adapter, and the transaction manager provides
		// the implementation for this when the data is uncommitted.
		return transactionManager;
	}

	/**
	 * Indicate if this object has ever been committed to the datastore.
	 * 
	 * All objects managed by a transaction manager have a key of the
	 * UncommittedObjectKey.  However, some of those objects will have
	 * been newly created by the transaction manager and others will be
	 * copies of objects that are committed in the datastore (objects in
	 * the latter class may be either objects that already existed before
	 * this transaction manager was created, or objects created by this
	 * transaction manager and already committed to the datastore).
	 * 
	 * @return true if this object was newly created by this transaction manager
	 * 			and not yet committed to the datastore,
	 * 			false if this object is a version of an object in the committed
	 * 			datastore
	 */
	public boolean isNewObject() {
		return committedObjectKey == null;
	}

	/**
	 * Return the key to the committed version of the object.
	 * 
	 * @return the key to the version of the object from the committed
	 *         datastore, or null if this object has never been committed to the
	 *         datastore
	 */
	public IObjectKey getCommittedObjectKey() {
		if (modelObject != null) {
			// This is a new object created in this transaction
			return null;
		} else {
			Assert.isNotNull(committedObjectKey);
			return committedObjectKey;
		}
	}

	/**
	 * Converts this object key from a 'never committed' state
	 * to a state which indicates that there is a committed version
	 * of this object in the datastore.
	 * <P>
	 * <code>isNewMethod</code> must return true before this method is called and
	 * will return false after this method is called.
	 * 
	 * @param committedObject
	 */
	public void setCommittedObject(IModelObject committedObject) {
		Assert.isTrue(committedObjectKey == null);
		Assert.isNotNull(modelObject);
		
		committedObjectKey = committedObject.getObjectKey();
		
		// When there is a version of the object in the committed
		// datastore, we do not cache the object.  This is important
		// because once the object is committed, others can change it
		// and so we can not longer keep a cached version.
		modelObject = null;
	}

	@Override
	public <E extends IModelObject, S extends IModelObject> IListManager<E> constructListManager(IListPropertyAccessor<E,S> listAccessor) {
		return new UncommittedListManager<E>(new ListKey<E,S>(this, listAccessor), transactionManager);
	}
}
