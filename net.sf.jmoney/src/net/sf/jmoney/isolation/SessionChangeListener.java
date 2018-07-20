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

import java.util.EventListener;


/**
 * Listener interface for changes to the accounting data.
 *
 * @author  Nigel Westbury
 */
public interface SessionChangeListener extends EventListener {
	/**
	 * An object has been added to the datastore.
	 * <P>
	 * If an object with child objects is added to the datastore as a single
	 * transaction then this method is called only for the object itself and not
	 * for the child objects. For example, if a Transaction object and its list
	 * of Entry objects is added then this method will be called only for the
	 * Transaction object, but if another Entry object is added in a later
	 * transaction then this method will be called for that Entry object.
	 * 
	 * All the descendents of the inserted object will already be created when
	 * this method is called.
	 * 
	 * If the newly inserted object contains a reference to another object that
	 * is also created in this transaction, and if that object has not already been
	 * created (i.e. objectInserted has not yet been fired for the referenced object)
	 * then the value of the reference when this method is called will be null.
	 * A later objectChanged event will be fired after the referenced object has been
	 * created and the correct reference being set in this object.  This makes things
	 * a little easier for some listeners because the listener does not have to worry about seeing
	 * a reference to an object that has not been processed in the objectInserted or
	 * objectCreated methods.  It does mean the listener must be able to deal with the case
	 * where a reference is null even though a null reference is not valid, but that is
	 * an easier case with which to deal.
	 * 
	 * Listeners should put code in this method to avoid complications that can
	 * arise if objects and their children are added piecemeal.
	 * 
	 * @param newObject
	 */
	void objectInserted(IModelObject newObject);

	/**
	 * An object has been added to the datastore.
	 * <P>
	 * If an object with child objects is added to the datastore then this
	 * method is called for the inserted object and all its descendent objects.
	 * For example, if a Transaction object and its list
	 * of Entry objects is added then this method will be called for the Transaction
	 * object and for each Entry object.
	 * 
	 * Listeners should put code in this method if it needs to process new objects in
	 * the same way regardless of how the object was added.
	 * 
	 * @param newObject
	 */
	void objectCreated(IModelObject newObject);

	/**
	 * An object has been deleted.
	 * 
	 * If an object with child objects is deleted from the datastore as a single
	 * transaction then this method is called only for the object itself and not
	 * for the child objects. For example, if a Transaction object is deleted
	 * then this method will be called only for the Transaction object and not
	 * for the Entry objects in the Transaction object. If, however, an entry is
	 * deleted from a transaction with split entries then this method will be
	 * called for that Entry object.
	 * 
	 * Listeners should put code in this method to avoid complications that can
	 * arise if objects and there children are removed piecemeal.
	 * 
	 * @param deletedObject
	 */
    void objectRemoved(IModelObject deletedObject);

	/**
	 * An object has been deleted.
	 * 
	 * If an object with child objects is deleted from the datastore then this method is called for the object itself and
	 * for all the descendent objects. For example, if a Transaction object is deleted
	 * then this method will be called for the Transaction object and
	 * for the Entry objects in the Transaction object.
	 * 
	 * Listeners should put code in this method if it needs to process deleted objects in
	 * the same way regardless of how the object was deleted.
	 * 
	 * @param deletedObject
	 */
    void objectDestroyed(IModelObject deletedObject);

    /**
	 * A scalar property in an object has been changed.
	 */
    void objectChanged(IModelObject changedObject, IScalarPropertyAccessor changedProperty, Object oldValue, Object newValue);

    /**
	 * An object has been moved from one list property to another list property.
	 * The contents of the object, including the contents of any list properties in it, remain
	 * intact.  References to the object also remain intact.
	 */
    void objectMoved(IModelObject movedObject, IModelObject originalParent, IModelObject newParent, IListPropertyAccessor<?,?> originalParentListProperty, IListPropertyAccessor<?,?> newParentListProperty);

	/**
	 * This method is called after a transaction has completed firing notifications
	 * during the committing of a transaction.  
	 * 
	 * A listener may 'batch up' changes in the other methods and then update the view and/or data
	 * in a single pass in this method.  Listeners could make all updates in the other methods and provide
	 * an empty implementation of this method, or listeners could even ignore all the other methods and
	 * do a complete refresh of a view and/or data when this method is called.
	 * 
	 * If changes are made by other plug-ins outside a transaction then this method is called after each change.
	 * Listeners can thus rely on this method being called in a timely manner.
	 */
	void performRefresh();

}