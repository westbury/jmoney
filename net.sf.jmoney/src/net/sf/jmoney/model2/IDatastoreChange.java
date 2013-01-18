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

package net.sf.jmoney.model2;

/**
 * Interface to objects that control a change to the datastore.
 * 
 * All changes to the datastore must be done thru a datastore
 * change object.
 * <P>
 * There are a number of reasons why changes should be made
 * thru an IDatastoreChange object, and why plug-ins should not simply
 * call 'setter' methods, 'add' methods, and 'remove' methods.
 * <ul>
 * <li>
 * If the underlying datastore supports transactions
 * (using 'transaction' with the meaning usually assumed
 * in the database community) then each IDatastoreChange object
 * will correspond to a single transaction in the datastore.
 * </li>
 * <li>
 * By using IDatastoreChange objects, multiple changes may be
 * consolidated into more efficient updates.  For example,
 * if multiple property values are changed by calling the
 * setter methods then these can all result in a single
 * 'update' statement being sent to the database.
 * </li>
 * <li>
 * IDatastoreChange objects manage locks.  For example, consider a user
 * who opens two views that both allow the user to change the
 * properties of the same account.
 * Neither view will see changes from the other view until
 * those changes are committed.  This can
 * cause confusion and cause changes to be lost, if the
 * user switched back and forth between the two views.
 * By using IDatastoreChange objects, the second view will not
 * be able to obtain an IDatastoreChange object and must act
 * appropriately (for example, by disabling the property
 * value fields). 
 * </li>
 * <li>
 * 'Undo' and 'Redo' support.  The 'Undo'/'Redo' feature
 * keeps a list of the IDatastoreChange objects that performed changes
 * to the datastore.  Every IDatastoreChange object must implement the
 * <code>undo</code> and <code>redo</code> methods.
 * </li>
 * </ul>
 */
public interface IDatastoreChange {
	/**
	 * 
	 * @return The description of the change.  This string
	 * 		is shown to the user by the 'undo' and 'redo'
	 * 		feature.  This description must be localized.
	 */
	String getDescription();
	
	/**
	 * Commits the changes to the datastore.  Other views
	 * will see the changes, when this method is called.
	 * <P>
	 * This method also adds this object to the 'undo'/'redo'
	 * list.
	 * <P>
	 * Either <code>commit</code> or <code>rollback</code>
	 * must be called after the changes have been set in
	 * this object.  If not, locks will not be released. 
	 */
	void commit();
	
	/**
	 * Releases the locks.
	 * <P>
	 * Either <code>commit</code> or <code>rollback</code>
	 * must be called after the changes have been set in
	 * this object.  If not, locks will not be released. 
	 */
	void rollback();
	
	/**
	 * Undo the change and commit the 'undo' so that other
	 * views see that the change has been 'undone'.
	 */
	void undo();
	
	/**
	 * Redo the change and commit the change so that other
	 * views see the change again.
	 * <P>
	 * In most implementations this method will have an
	 * identical implementation to the <code>commit</code>
	 * method.
	 */
	void redo();
}
