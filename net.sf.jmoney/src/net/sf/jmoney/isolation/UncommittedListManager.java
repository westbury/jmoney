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

import java.util.Vector;


/**
 * This is a special implementation of the IListManager interface. It is used
 * only in objects that have never been committed to the datastore (objects that
 * were created in this transaction). This implementation uses the Vector class
 * to keep the list of objects.
 * 
 * @author Nigel Westbury
 */
public class UncommittedListManager<E extends IModelObject> extends Vector<E> implements IListManager<E> {

	private static final long serialVersionUID = 196103020038035348L;

	private ListKey<E,?> listKey;
	private TransactionManager transactionManager;
	
	public UncommittedListManager(ListKey<E,?> listKey, TransactionManager transactionManager) {
		this.listKey = listKey;
	 	this.transactionManager = transactionManager;
	 }

	/**
	 * Create a new model object in the list represented by this object.
	 */
	@Override
	public <F extends E> F createNewElement(IExtendablePropertySet<F> propertySet) {
		UncommittedObjectKey objectKey = new UncommittedObjectKey(transactionManager);
		F newObject = propertySet.constructDefaultImplementationObject(objectKey, listKey);

		objectKey.setObject(newObject);

		add(newObject);
		
		return newObject;
	}

	/*
	 * This method is used only if a nested transaction manager is commitinsg
	 * its changes into this transaction manager.
	 */
	@Override
	public <F extends E> F createNewElement(IExtendablePropertySet<F> propertySet, IValues<F> values) {
		UncommittedObjectKey objectKey = new UncommittedObjectKey(transactionManager);
		F newObject = propertySet.constructImplementationObject(objectKey, listKey, values);

		objectKey.setObject(newObject);

		add(newObject);
		
		return newObject;
	}

	/*
	 * This method is used only if a nested transaction manager is committing
	 * its changes into this transaction manager.
	 */
	@Override
	public void deleteElement(E element) {
		boolean found = remove(element);
		if (!found) {
			throw new RuntimeException("internal error - element not in list");
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
}
