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

import org.eclipse.swt.widgets.Control;

/**
 * Interface into a control that edits a property value.
 * <P>
 * All registered properties must include an implementation
 * of the <code>IPropertyControlFactory</code> interface.  This interface
 * contains the createPropertyControl method which is called
 * to create a <code>Control</code> that can edit the property.
 * The <code>Control</code> is wrapped in an <code>IPropertyControl</code>
 * implementation that handles the movement of data between the
 * data object and the control. 
 * <P>
 * It is essential that all controls have a common interface
 * because the framework may have to deal with columns of data
 * that were contributed by another plug-in. 
 * 
 * @param T the type of the data object that contains this property
 * @see IPropertyControlFactory
 * @see org.eclipse.swt.widgets.Control
 * @author Nigel Westbury
 * @author Johann Gyger
 */
public interface IPropertyControl<T> {

    /**
     * This method gives access to the underlying control.
     * Do not use this method to get the control for the
     * purposes of getting and setting property values
     * to and from the control.  Instead use the methods in
     * this interface.  Use this method to get the control
     * for the purpose of adding focus listeners and other
     * such functionality. 
     * 
     * @return The underlying control.
     */
    Control getControl();

    /**
     * Bind the control to the given object.
     * 
     * All property controls are constructed for a particular property,
     * so only the extendable object can be changed once the control has
     * been created.
     * 
     * Null values can be passed to this method, in which case the control
     * is disabled.  The exact way of disabling the control depends on where
     * this control is used.  In some places it may just need to be disabled.
     * In the entries tables, the control should be made invisible so the user
     * sees only the background color.  Therefore it is up to the caller to take
     * appropriate action on the control when null is passed.
     * 
     * @param object the object that contains the value of
     * the property, or null if the control in not to be bound
     */
    void load(T object);

    /**
     * This method takes the data in the control and sets it
     * into the datastore.  This method must be called before 
     * a control is destroyed, otherwise data entered by the user
     * may not be saved.
     * <P>
     * Some controls will save data in response to user edits.
     * Combo boxes typically do this.  Other controls, such as
     * text boxes, do not.
     */
    void save();
}