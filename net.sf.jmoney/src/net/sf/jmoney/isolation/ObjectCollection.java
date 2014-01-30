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

import java.util.Collections;
import java.util.Set;

import org.eclipse.core.databinding.observable.set.ObservableSet;
import org.eclipse.core.databinding.observable.set.SetDiff;
import org.eclipse.core.runtime.Assert;

/**
 * This class is used to provide access to lists of objects
 * contained in a list property of a parent object.
 *
 * @author Nigel Westbury
 */
public class ObjectCollection<E extends IModelObject> extends ObservableSet<E> {

	private IListManager<E> listManager;

	ListKey<E,?> listKey;

	// TODO what happens if the object containing this list is deleted
	// by another thread?

	private SessionChangeListener sessionListener = new SessionChangeAdapter() {

		@Override
		public void objectInserted(IModelObject newObject) {
			if (newObject.getParentListKey().equals(listKey)) {
				elementAdded((E)newObject);
			}

		}

		@Override
		public void objectRemoved(IModelObject deletedObject) {
			if (deletedObject.getParentListKey().equals(listKey)) {
				elementRemoved((E)deletedObject);
			}
		}

		@Override
		public void objectMoved(IModelObject movedObject,
				IModelObject originalParent, IModelObject newParent,
				IListPropertyAccessor originalParentListProperty,
				IListPropertyAccessor newParentListProperty) {
			if (originalParentListProperty == listKey.getListPropertyAccessor()
					&& originalParent.getObjectKey().equals(listKey.getParentKey())) {
				elementRemoved((E)movedObject);
			}
			if (newParentListProperty == listKey.getListPropertyAccessor()
					&& newParent.getObjectKey().equals(listKey.getParentKey())) {
				elementAdded((E)movedObject);
			}
		}

		private <F> void elementAdded(
				final E newObject) {
			SetDiff<E> diff = new SetDiff<E>() {
				@Override
				public Set<E> getAdditions() {
					return Collections.singleton(newObject);
				}

				@Override
				public Set<E> getRemovals() {
					return null;
				}
			};
			fireSetChange(diff);
		}

		private void elementRemoved(
				final E newObject) {
			SetDiff<E> diff = new SetDiff<E>() {
				@Override
				public Set<E> getAdditions() {
					return null;
				}

				@Override
				public Set<E> getRemovals() {
					return Collections.singleton(newObject);
				}
			};
			fireSetChange(diff);
		}
	};

	public <S extends IModelObject> ObjectCollection(IListManager<E> listManager, S parent, IListPropertyAccessor<E,S> listPropertyAccessor) {
		super(listManager, (Class<E>)listManager.getClass());
		this.listManager = listManager;
		this.listKey = new ListKey<E,S>(parent.getObjectKey(), listPropertyAccessor);

		IDataManager dataManager = listKey.getParentKey().getDataManager();

		dataManager.addChangeListenerWeakly(sessionListener);
	}

	/**
	 * This version of this method is called only by the end-user code, i.e. this method
	 * is not called when a transaction manager is committing its changes to the underlying
	 * data manager.
	 *
	 * @param <F> the class of the object being created in this list
	 * @param actualPropertySet
	 * @return
	 */
	public <F extends E> F createNewElement(IExtendablePropertySet<F> actualPropertySet) {
		final F newObject = listManager.createNewElement(actualPropertySet);

		listKey.getParentKey().getDataManager().getChangeManager().processObjectCreation(listKey, newObject);

		// Fire the event.
		listKey.getParentKey().getDataManager().fireEvent(
				new ISessionChangeFirer() {
					@Override
					public void fire(SessionChangeListener listener) {
						listener.objectInserted(newObject);
						listener.objectCreated(newObject);
					}
				});

		return newObject;
	}

	/**
	 * This version of this method is called only from within a transaction. The values of
	 * the scalar properties are passed so that:
	 *
	 * - the underlying database need only do a single insert, instead of inserting with
	 *   default values and then updating each value as they are set.
	 *
	 * - a single notification is fired, passing the object with its final property values,
	 *   rather than sending out an object with default values and then a property change
	 *   notification for each property.
	 *
	 * This may be a top level insert or a descendant of an object that was inserted in
	 * the same transaction.  We must know the difference so we can fire the objectInserted
	 * event methods correctly. We therefore need a flag to indicate this.
	 *
	 * @param isDescendentInsert true if this object is being inserted because its parent is
	 * 			being inserted in the same transaction, false if this object is being inserted
	 *          into a list that existed prior to this transaction
	 */
	public <F extends E> F createNewElement(IExtendablePropertySet<F> actualPropertySet, IValues<F> values, final boolean isDescendentInsert) {
		final F newObject = listManager.createNewElement(actualPropertySet, values);

		listKey.getParentKey().getDataManager().getChangeManager().processObjectCreation(listKey, newObject);

		return newObject;
	}

	/**
	 * Moves the given object into this collection, removing it from its
	 * current parent.
	 */
	public <F extends E> void moveElement(final F element) {
		Assert.isTrue(listKey.getParentKey().getDataManager() == element.getDataManager());

		ListKey<? super F,?> originalListKey = element.getParentListKey();

		moveIt(element, originalListKey);

		listKey.getParentKey().getDataManager().getChangeManager().processObjectMove(element, originalListKey);
	}

	private  <F extends E, S extends IModelObject> void moveIt(final F element,
			final ListKey<? super F, S> originalListKey) {
		/*
		 * Note that if the parent object is not materialized (meaning that the
		 * getObject method in the following line needs to materialize the
		 * parent) then we really don't need to do anything to remove the object
		 * from the parent's list. However, there is no API for this, and the
		 * extra code for such a small optimization is not worth it.
		 */
		ObjectCollection<? super F> originalCollection = originalListKey.getListPropertyAccessor().getElements((S)originalListKey.getParentKey().getObject());
		IListManager<? super F> originalListManager = originalCollection.listManager;

		// Move in the underlying datastore.
		listManager.moveElement(element, originalListManager);

		listManager.add(element);
		originalListManager.remove(element);
		element.replaceParentListKey(listKey);

		listKey.getParentKey().getDataManager().fireEvent(
				new ISessionChangeFirer() {
					@Override
					public void fire(SessionChangeListener listener) {
						listener.objectMoved(
								element,
								originalListKey.getParentKey().getObject(),
								listKey.getParentKey().getObject(),
								originalListKey.getListPropertyAccessor(),
								listKey.getListPropertyAccessor()
						);
					}
				});
	}

//	@Override
//	public int size() {
//		getterCalled();
//		return listManager.size();
//	}
//
//	@Override
//	public boolean isEmpty() {
//		getterCalled();
//		return listManager.isEmpty();
//	}
//
//	@Override
//	public boolean contains(Object arg0) {
//		getterCalled();
//		return listManager.contains(arg0);
//	}
//
//	@Override
//	public Iterator<E> iterator() {
//		getterCalled();
//		return listManager.iterator();
//	}
//
//	@Override
//	public Object[] toArray() {
//		getterCalled();
//		return listManager.toArray();
//	}
//
//	@Override
//	public <T> T[] toArray(T[] arg0) {
//		getterCalled();
//		return listManager.toArray(arg0);
//	}
//
//	@Override
//	public boolean add(E arg0) {
//		/*
//		 * The Collection methods that mutate the collection should not be
//		 * used.  Use instead createNewElement, deleteElement, and moveElement.
//		 */
//		throw new UnsupportedOperationException();
//	}
//
//	@Override
//	public boolean remove(Object object) {
//		/*
//		 * The Collection methods that mutate the collection should not be
//		 * used.  Use instead createNewElement, deleteElement, and moveElement.
//		 */
//		throw new UnsupportedOperationException();
//	}

	/**
	 * Removes an object from the collection. Because this collection 'owns' the
	 * object, this results in the object being deleted. This method ensures
	 * that listeners are notified as appropriate.
	 *
	 * @return true if the object was deleted, false if the object had
	 *         references to it and so the object could not be deleted
	 * @throws ReferenceViolationException
	 *             if the object cannot be deleted because there are
	 *             references to it
	 * @throws RuntimeException
	 *             if the object does not exist in the collection
	 */
	public void deleteElement(E element) throws ReferenceViolationException {
		if (element.getDataManager() != listKey.getParentKey().getDataManager()) {
    		throw new RuntimeException("Invalid call to remove.  The object passed does not belong to the data manager that is the base data manager of this collection."); //$NON-NLS-1$
		}

		/*
		 * Check that the object is in the list.  It is in this list if the parent
		 * object is the same and the list property is the same.
		 */
		if (!element.getParentListKey().equals(listKey)) {
			throw new RuntimeException("Passed object is not in the list.");
		}

		final E objectToRemove = listKey.getListPropertyAccessor().getElementPropertySet().getImplementationClass().cast(element);

		/*
		 * This must be done before the object is actually deleted.  The reason is that the change
		 * manager is responsible for the undo/redo support and so must fetch from the object
		 * all the information it needs to restore it.  It cannot do this with a disposed object.
		 */
		myProcessObjectDeletion(listKey, objectToRemove);

		/*
		 * This causes the object to be removed from the model and marked as 'disposed'.
		 */
		listManager.deleteElement(objectToRemove);

		/*
		 * Deletion events must be fired after the object has been removed from the list.
		 * That does mean listeners see a model object that is not in the model and therefore not in a valid state.
		 * (Remember model objects only exist inside the model).  However we can't fire events before the change is visible
		 * in the model because then listener code is hard to make work correctly.  (Consider, for example, ComputedValue implementations; they are just
		 * not going to work at all if the change is not visible in the model when the event is fired).
		 * 
		 * One cannot do much with an object that has been disposed, but that is okay.  They are marked as being disposed and their API
		 * enforces the limited capabilities of a disposed object.
		 */
		listKey.getParentKey().getDataManager().fireEvent(
				new ISessionChangeFirer() {
					@Override
					public void fire(SessionChangeListener listener) {
						listener.objectRemoved(objectToRemove);
					}
				}
		);
	}

	private <S extends IModelObject> void myProcessObjectDeletion(ListKey<E,S> listKey2, E objectToRemove) {
		listKey.getParentKey().getDataManager().getChangeManager().processObjectDeletion((S)listKey2.getParentKey().getObject(), listKey2.getListPropertyAccessor(), objectToRemove);
	}

//	@Override
//	public boolean containsAll(Collection<?> arg0) {
//		getterCalled();
//		return listManager.containsAll(arg0);
//	}
//
//	@Override
//	public boolean addAll(Collection<? extends E> arg0) {
//		/*
//		 * The Collection methods that mutate the collection should not be
//		 * used.  Use instead createNewElement, deleteElement, and moveElement.
//		 */
//		throw new UnsupportedOperationException();
//	}
//
//	@Override
//	public boolean removeAll(Collection<?> arg0) {
//		/*
//		 * The Collection methods that mutate the collection should not be
//		 * used.  Use instead createNewElement, deleteElement, and moveElement.
//		 */
//		throw new UnsupportedOperationException();
//	}
//
//	@Override
//	public boolean retainAll(Collection<?> arg0) {
//		/*
//		 * The Collection methods that mutate the collection should not be
//		 * used.  Use instead createNewElement, deleteElement, and moveElement.
//		 */
//		throw new UnsupportedOperationException();
//	}
//
//	@Override
//	public void clear() {
//		/*
//		 * The Collection methods that mutate the collection should not be
//		 * used.  Use instead createNewElement, deleteElement, and moveElement.
//		 */
//		throw new UnsupportedOperationException();
//	}
//
//	@Override
//	public Realm getRealm() {
//		// TODO Auto-generated method stub
//		return null;
//	}
//
//	@Override
//	protected void getterCalled() {
//		ObservableTracker.getterCalled(this);
//	}

//	@Override
//	public void addSetChangeListener(final ISetChangeListener<? super E> listener) {
//
//		// TODO Auto-generated method stub
//
//	}
//
//	@Override
//	public void removeSetChangeListener(ISetChangeListener<? super E> listener) {
//		// TODO Auto-generated method stub
//
//	}

//	@Override
//	public boolean isStale() {
//		// TODO Auto-generated method stub
//		return false;
//	}
//
//	@Override
//	public Object getElementType() {
//		return listKey.getListPropertyAccessor().getClass();
//	}
//
//	@Override
//	public Class<E> getElementClass() {
//		return (Class<E>)listKey.getListPropertyAccessor().getClass();
//	}
}
