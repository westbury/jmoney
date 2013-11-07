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

import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.internal.databinding.provisional.bind.Bind;
import org.eclipse.core.internal.databinding.provisional.bind.IBidiConverter;
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

    protected VerySimpleDateFormat fDateFormat = new VerySimpleDateFormat(
            JMoneyPlugin.getDefault().getDateFormat());

    // Listen to date format changes so we keep up to date
    // Johann, 2005-07-02: Shouldn't this be done in the control?
    /*
    static {
        JMoneyPlugin.getDefault().getPreferenceStore()
                .addPropertyChangeListener(new IPropertyChangeListener() {
                    public void propertyChange(PropertyChangeEvent event) {
                        if (event.getProperty().equals("dateFormat")) {
                            fDateFormat = new VerySimpleDateFormat(JMoneyPlugin
                                    .getDefault().getDateFormat());
                        }
                    }
                });
    }
    */

    protected boolean readOnly;

    public DateControlFactory() {
        this.readOnly = false;
    }

    public DateControlFactory(boolean readOnly) {
        this.readOnly = readOnly;
    }

    @Override
	public IPropertyControl<S> createPropertyControl(Composite parent, ScalarPropertyAccessor<Date,S> propertyAccessor) {
        return new DateEditor<S>(parent, propertyAccessor);
    }

    @Override
	public Control createPropertyControl(Composite parent, ScalarPropertyAccessor<Date,S> propertyAccessor, IObservableValue<? extends S> modelObservable) {
        DateControl propertyControl = new DateControl(parent);

    	IBidiConverter<Date,String> dateToText = new IBidiConverter<Date,String>() {
			@Override
			public String modelToTarget(Date date) {
		    	if (date == null) {
		    		return ""; //$NON-NLS-1$
		    	} else {
		    		return fDateFormat.format(date);
		    	}
			}

			@Override
			public Date targetToModel(String text) {
		        try {
		        	return fDateFormat.parse(text);
		        } catch (IllegalArgumentException e) {
		        	// TODO show validation errors
		        	return null;
		        }
			}
		};

		Bind.twoWay(propertyAccessor.observeDetail(modelObservable))
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
            return "'" + fDateFormat.format(value) + "'"; //$NON-NLS-1$ //$NON-NLS-2$
        }
    }

    @Override
	public String formatValueForTable(S extendableObject,
            ScalarPropertyAccessor<? extends Date,S> propertyAccessor) {
        Date value = propertyAccessor.getValue(extendableObject);
        return fDateFormat.format(value);
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