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

import java.util.Date;

import net.sf.jmoney.isolation.IModelObject;
import net.sf.jmoney.isolation.IScalarPropertyAccessor;
import net.sf.jmoney.isolation.SessionChangeAdapter;
import net.sf.jmoney.isolation.SessionChangeListener;
import net.sf.jmoney.model2.ExtendableObject;
import net.sf.jmoney.model2.IPropertyControl;
import net.sf.jmoney.model2.ScalarPropertyAccessor;

import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * Editor class for date properties. These dates are formatted according to the
 * selected format in the preferences.
 *
 * @author Johann Gyger
 */
public class DateEditor implements IPropertyControl<ExtendableObject> {

    protected ScalarPropertyAccessor<Date,?> propertyAccessor;
    protected DateControl propertyControl;
    protected ExtendableObject extendableObject;

    private SessionChangeListener dateChangeListener = new SessionChangeAdapter() {
		@Override
		public void objectChanged(IModelObject changedObject, IScalarPropertyAccessor changedProperty, Object oldValue, Object newValue) {
			if (changedObject.equals(extendableObject) && changedProperty == propertyAccessor) {
	            Date date = extendableObject.getPropertyValue(propertyAccessor);
	            propertyControl.setDate(date);
			}
		}
	};
	
	/**
     * Create a new date editor.
     */
    public DateEditor(final Composite parent, ScalarPropertyAccessor<Date,?> propertyAccessor) {
        this.propertyAccessor = propertyAccessor;
        
		Font font = parent.getFont();

		propertyControl = new DateControl(parent);
		propertyControl.setFont(font);
		
		propertyControl.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
		    	if (extendableObject != null) {
		    		extendableObject.getDataManager().removeChangeListener(dateChangeListener);
		    	}
			}
		});
	}

    /* (non-Javadoc)
     * @see net.sf.jmoney.model2.IPropertyControl#load(net.sf.jmoney.model2.ExtendableObject)
     */
    @Override
	public void load(ExtendableObject object) {
    	if (extendableObject != null) {
    		extendableObject.getDataManager().removeChangeListener(dateChangeListener);
    	}
    		
    	this.extendableObject = object;
    	if (object == null) {
    		propertyControl.setDate(null);
    	} else {
            Date d = object.getPropertyValue(propertyAccessor);
    		propertyControl.setDate(d);

    		// Listen for changes to the session data.
    		extendableObject.getDataManager().addChangeListener(dateChangeListener);
    	}
    	
    	propertyControl.setEnabled(object != null);
    }

    /* (non-Javadoc)
     * @see net.sf.jmoney.model2.IPropertyControl#save()
     */
    @Override
	public void save() {
        extendableObject.setPropertyValue(propertyAccessor, propertyControl.getDate());
    }

    /* (non-Javadoc)
     * @see net.sf.jmoney.model2.IPropertyControl#getControl()
     */
    @Override
	public Control getControl() {
        return propertyControl;
    }
}