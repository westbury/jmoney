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

import java.net.URL;
import java.util.Comparator;

import net.sf.jmoney.model2.ExtendableObject;
import net.sf.jmoney.model2.IPropertyControl;
import net.sf.jmoney.model2.IPropertyControlFactory;
import net.sf.jmoney.model2.ScalarPropertyAccessor;

import org.eclipse.swt.widgets.Composite;

/**
 * A control factory to edit date values.
 */
public class UrlControlFactory implements IPropertyControlFactory<URL> {

    public IPropertyControl<ExtendableObject> createPropertyControl(Composite parent, ScalarPropertyAccessor<URL,?> propertyAccessor) {
        return new UrlEditor(parent, propertyAccessor);
    }

    public String formatValueForMessage(ExtendableObject extendableObject,
            ScalarPropertyAccessor<? extends URL,?> propertyAccessor) {
        URL value = extendableObject.getPropertyValue(propertyAccessor);
        if (value == null) {
            return "none";
        } else {
            return "'" + value.toExternalForm() + "'";
        }
    }

    public String formatValueForTable(ExtendableObject extendableObject,
            ScalarPropertyAccessor<? extends URL,?> propertyAccessor) {
        URL value = extendableObject.getPropertyValue(propertyAccessor);
        return value.toExternalForm();
    }

	public URL getDefaultValue() {
		return null;
	}

    public boolean isEditable() {
        return true;
    }

	public Comparator<URL> getComparator() {
		return new Comparator<URL>() {
			public int compare(URL url1, URL url2) {
				return url1.toExternalForm().compareTo(url2.toExternalForm());
			}
		};
	}
}