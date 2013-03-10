/*
 *
 *  JMoney - A Personal Finance Manager
 *  Copyright (c) 2004 Johann Gyger <jgyger@users.sf.net>
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

package net.sf.jmoney.fields;

import net.sf.jmoney.model2.ExtendableObject;
import net.sf.jmoney.model2.IPropertyControl;
import net.sf.jmoney.model2.ScalarPropertyAccessor;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * Editor class for image properties.
 */
public class ImageEditor<S extends ExtendableObject> implements IPropertyControl<S> {

    protected ScalarPropertyAccessor<IBlob,S> propertyAccessor;
    protected ImageControl propertyControl;
    protected ExtendableObject extendableObject;

	/**
     * Create a new date editor.
     */
    public ImageEditor(final Composite parent, ScalarPropertyAccessor<IBlob,S> propertyAccessor) {
        this.propertyAccessor = propertyAccessor;
        
		propertyControl = new ImageControl(parent);
	}

    /* (non-Javadoc)
     * @see net.sf.jmoney.model2.IPropertyControl#load(net.sf.jmoney.model2.ExtendableObject)
     */
    @Override
    public void load(S object) {
    	this.extendableObject = object;
    	if (object == null) {
    		propertyControl.setBlob(null);
    	} else {
            IBlob blob = propertyAccessor.getValue(object);
    		propertyControl.setBlob(blob);
    	}
    	
    	propertyControl.setEnabled(object != null);
    }

    /* (non-Javadoc)
     * @see net.sf.jmoney.model2.IPropertyControl#save()
     */
    @Override
	public void save() {
        // Images are not editable in the control.
    }

    /* (non-Javadoc)
     * @see net.sf.jmoney.model2.IPropertyControl#getControl()
     */
    @Override
    public Control getControl() {
        return propertyControl;
    }
}