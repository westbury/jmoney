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

import java.util.Comparator;
import java.util.Date;

import net.sf.jmoney.JMoneyPlugin;
import net.sf.jmoney.VerySimpleDateFormat;
import net.sf.jmoney.model2.ExtendableObject;
import net.sf.jmoney.model2.IPropertyControl;
import net.sf.jmoney.model2.IPropertyControlFactory;
import net.sf.jmoney.model2.ScalarPropertyAccessor;
import net.sf.jmoney.resources.Messages;

import org.eclipse.core.databinding.observable.value.ComputedValue;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.internal.databinding.Activator;
import org.eclipse.core.internal.databinding.provisional.bind.Bind;
import org.eclipse.core.internal.databinding.provisional.bind.IBidiConverter;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.databinding.preference.PreferenceObservables;
import org.eclipse.jface.databinding.swt.SWTObservables;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * A control factory to edit date values.
 *
 * @author Johann Gyger
 */
public class DateControlFactory<S extends ExtendableObject> implements IPropertyControlFactory<S,Date> {

    private static IObservableValue<VerySimpleDateFormat> dateFormat = null;

    private static IObservableValue<VerySimpleDateFormat> observeDateFormat() {
    	if (dateFormat == null) {
    		final IObservableValue<String> dateFormatString = PreferenceObservables.observe(
    				new IScopeContext [] { InstanceScope.INSTANCE, ConfigurationScope.INSTANCE, DefaultScope.INSTANCE }, 
    				JMoneyPlugin.PLUGIN_ID).observe("dateFormat", "MMM/yy.d");

    	    dateFormat = new ComputedValue<VerySimpleDateFormat>() {
    			@Override
    			protected VerySimpleDateFormat calculate() {
    		    	return new VerySimpleDateFormat(dateFormatString.getValue());
    			}
    	    };
    	    
    	}
    	return dateFormat;
    }
    
    protected boolean readOnly;

    public DateControlFactory() {
        this.readOnly = false;
    }

    public DateControlFactory(boolean readOnly) {
        this.readOnly = readOnly;
    }

    @Override
	public Control createPropertyControl(Composite parent, ScalarPropertyAccessor<Date,S> propertyAccessor, S modelObject) {
    	return createPropertyControlInternal(parent, propertyAccessor.observe(modelObject));
    }

    @Override
	public Control createPropertyControl(Composite parent, ScalarPropertyAccessor<Date,S> propertyAccessor, IObservableValue<? extends S> modelObservable) {
    	return createPropertyControlInternal(parent, propertyAccessor.observeDetail(modelObservable));
    }
    
	private Control createPropertyControlInternal(Composite parent, IObservableValue<Date> modelDateObservable) {
        DateControl propertyControl = new DateControl(parent);

    	IBidiConverter<Date,String> dateToText = new IBidiConverter<Date,String>() {
			@Override
			public String modelToTarget(Date date) {
		    	if (date == null) {
		    		return ""; //$NON-NLS-1$
		    	} else {
		    		return observeDateFormat().getValue().format(date);
		    	}
			}

			@Override
			public Date targetToModel(String text) throws CoreException {
		        try {
		        	return observeDateFormat().getValue().parse(text);
		        } catch (IllegalArgumentException e) {
					IStatus status = new Status(Status.ERROR, JMoneyPlugin.PLUGIN_ID, "'" + text + "' is not a valid date.");
					throw new CoreException(status);
		        }
			}
		};

		Bind.twoWay(modelDateObservable)
		.convertWithTracking(dateToText)
		.to(SWTObservables.observeText(propertyControl.textControl, SWT.Modify));

		Bind.bounceBack(dateToText)
		.to(SWTObservables.observeText(propertyControl.textControl, SWT.FocusOut));

		return propertyControl;
    }

    @Override
	public String formatValueForMessage(S extendableObject,
            ScalarPropertyAccessor<? extends Date,S> propertyAccessor) {
        Date value = propertyAccessor.getValue(extendableObject);
        if (value == null) {
            return Messages.DateControlFactory_None;
        } else {
            return "'" + observeDateFormat().getValue().format(value) + "'"; //$NON-NLS-1$ //$NON-NLS-2$
        }
    }

    @Override
	public String formatValueForTable(S extendableObject,
            ScalarPropertyAccessor<? extends Date,S> propertyAccessor) {
        Date value = propertyAccessor.getValue(extendableObject);
        return observeDateFormat().getValue().format(value);
    }

	@Override
	public Date getDefaultValue() {
		return null;
	}

    @Override
	public boolean isEditable() {
        return !readOnly;
    }

	@Override
	public Comparator<Date> getComparator() {
		return new Comparator<Date>() {
			@Override
			public int compare(Date date1, Date date2) {
				return date1.compareTo(date2);
			}
		};
	}
}