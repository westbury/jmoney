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

package net.sf.jmoney.paypal;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Comparator;

import net.sf.jmoney.model2.ExtendableObject;
import net.sf.jmoney.model2.IPropertyControl;
import net.sf.jmoney.model2.IPropertyControlFactory;
import net.sf.jmoney.model2.ScalarPropertyAccessor;

import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.WritableValue;
import org.eclipse.core.internal.databinding.provisional.bind.Bind;
import org.eclipse.core.internal.databinding.provisional.bind.IBidiConverter;
import org.eclipse.core.internal.databinding.provisional.bind.IBidiWithStatusConverter;
import org.eclipse.core.internal.databinding.provisional.bind.IValueWithStatus;
import org.eclipse.core.internal.databinding.provisional.bind.ValueWithStatus;
import org.eclipse.jface.databinding.fieldassist.ControlStatusDecoration;
import org.eclipse.jface.databinding.swt.SWTObservables;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;

/**
 * A control factory to edit date values.
 */
public class UrlControlFactory<S extends ExtendableObject> implements IPropertyControlFactory<S,URL> {

//    @Override
//	public IPropertyControl<S> createPropertyControl(Composite parent, ScalarPropertyAccessor<URL,S> propertyAccessor) {
//        return new UrlEditor<S>(parent, propertyAccessor);
//    }

	   @Override
	public Control createPropertyControl(Composite parent,
			ScalarPropertyAccessor<URL, S> propertyAccessor,
			S modelObject) {
 	WritableValue<S> observable = new WritableValue<S>();
 	observable.setValue(modelObject);
		return createPropertyControl(parent, propertyAccessor, observable);
	}


	@Override
	public Control createPropertyControl(Composite parent,
			ScalarPropertyAccessor<URL, S> propertyAccessor,
			IObservableValue<? extends S> modelObservable) {
        Text control = new Text(parent, SWT.NONE);

    	IBidiWithStatusConverter<URL,String> urlToStringConverter = new IBidiWithStatusConverter<URL,String>() {
			@Override
			public String modelToTarget(URL fromValue) {
	            return (fromValue == null) ? "" : fromValue.toExternalForm();
			}

			@Override
			public IValueWithStatus<URL> targetToModel(String text) {
				try {
					URL url = text.trim().isEmpty() ? null : new URL(text);
					return ValueWithStatus.ok(url);
				} catch (MalformedURLException e) {
					return ValueWithStatus.error(e.getLocalizedMessage());
				}
			}
		};

		ControlStatusDecoration statusDecoration = new ControlStatusDecoration(
				control, SWT.LEFT | SWT.TOP);

		Bind.twoWay(propertyAccessor.observeDetail(modelObservable))
		.convert(urlToStringConverter)
		.to(SWTObservables.observeText(control, SWT.FocusOut), statusDecoration::update);

        return control;
	}

    @Override
	public String formatValueForMessage(S extendableObject,
            ScalarPropertyAccessor<? extends URL,S> propertyAccessor) {
        URL value = propertyAccessor.getValue(extendableObject);
        if (value == null) {
            return "none";
        } else {
            return "'" + value.toExternalForm() + "'";
        }
    }

    @Override
	public String formatValueForTable(S extendableObject,
            ScalarPropertyAccessor<? extends URL,S> propertyAccessor) {
        URL value = propertyAccessor.getValue(extendableObject);
        return value.toExternalForm();
    }

	@Override
	public URL getDefaultValue() {
		return null;
	}

    @Override
	public boolean isEditable() {
        return true;
    }

	@Override
	public Comparator<URL> getComparator() {
		return new Comparator<URL>() {
			@Override
			public int compare(URL url1, URL url2) {
				return url1.toExternalForm().compareTo(url2.toExternalForm());
			}
		};
	}
}