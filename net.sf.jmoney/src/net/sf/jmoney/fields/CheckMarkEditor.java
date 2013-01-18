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

package net.sf.jmoney.fields;

import net.sf.jmoney.model2.ExtendableObject;
import net.sf.jmoney.model2.IPropertyControl;
import net.sf.jmoney.model2.ScalarPropertyAccessor;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * Editor class for boolean values that are to be edited using a check box.
 *
 * @author Nigel Westbury
 */
public class CheckMarkEditor implements IPropertyControl<ExtendableObject> {

    protected ScalarPropertyAccessor<Boolean,?> propertyAccessor;
    protected Button propertyControl;
    protected ExtendableObject extendableObject;

    /**
     * Create a new date editor.
     */
    public CheckMarkEditor(Composite parent, ScalarPropertyAccessor<Boolean,?> propertyAccessor) {
        propertyControl = new Button(parent, SWT.CHECK);
        this.propertyAccessor = propertyAccessor;

        // Selection changes are reflected immediately in the
        // datastore.  This allows controls for other properties,
        // which may depend on this property, to be immediately made
        // visible or invisible.

        propertyControl.addSelectionListener(new SelectionAdapter() {
			@Override
            public void widgetSelected(SelectionEvent e) {
                save();
            }
        });
    }

    /* (non-Javadoc)
     * @see net.sf.jmoney.model2.IPropertyControl#load(net.sf.jmoney.model2.ExtendableObject)
     */
    @Override
	public void load(ExtendableObject object) {
    	this.extendableObject = object;
    	if (object == null) {
            propertyControl.setSelection(false);
    	} else {
            Boolean value = object.getPropertyValue(propertyAccessor);
            propertyControl.setSelection(value.booleanValue());
    	}
    	propertyControl.setEnabled(object != null);
    }

    /* (non-Javadoc)
     * @see net.sf.jmoney.model2.IPropertyControl#save()
     */
    @Override
	public void save() {
        boolean value = propertyControl.getSelection();
        extendableObject.setPropertyValue(propertyAccessor, new Boolean(value));
    }

    /* (non-Javadoc)
     * @see net.sf.jmoney.model2.IPropertyControl#getControl()
     */
    @Override
	public Control getControl() {
        return propertyControl;
    }

}