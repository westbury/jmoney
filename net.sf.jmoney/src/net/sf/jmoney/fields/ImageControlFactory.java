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

import org.eclipse.core.databinding.beans.PojoProperties;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.internal.databinding.provisional.bind.Bind;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import net.sf.jmoney.model2.ExtendableObject;
import net.sf.jmoney.model2.IPropertyControlFactory;
import net.sf.jmoney.model2.ScalarPropertyAccessor;

/**
 * A control factory to edit images.  Well, we don't allow the users to actually
 * edit an image, this is an accounting program after all.  But we do allow users
 * to select images.  In practice images generally are fetched when accounting information
 * is imported from a site such as Paypal or Amazon.  Few users are going to take pictures
 * of their purchases and manually attach pictures, so this is really only here for
 * completeness.
 *
 * @author Nigel Westbury
 */
public class ImageControlFactory<S extends ExtendableObject> implements IPropertyControlFactory<S,IBlob> {

    @Override
	public Control createPropertyControl(Composite parent, ScalarPropertyAccessor<IBlob,S> propertyAccessor, S modelObject) {
    	return createPropertyControlInternal(parent, propertyAccessor.observe(modelObject));
    }

    @Override
	public Control createPropertyControl(Composite parent, ScalarPropertyAccessor<IBlob,S> propertyAccessor, IObservableValue<? extends S> modelObservable) {
    	return createPropertyControlInternal(parent, propertyAccessor.observeDetail(modelObservable));
    }

    private Control createPropertyControlInternal(Composite parent, IObservableValue<IBlob> modelBlobObservable) {
    	ImageControl propertyControl = new ImageControl(parent);

//    	IBidiConverter<IBlob,String> dateToText = new IBidiConverter<IBlob,String>() {
//			@Override
//			public String modelToTarget(IBlob date) {
//		    	if (date == null) {
//		    		return ""; //$NON-NLS-1$
//		    	} else {
//		    		return fIBlobFormat.format(date);
//		    	}
//			}
//
//			@Override
//			public IBlob targetToModel(String text) {
//		        try {
//		        	return fDateFormat.parse(text);
//		        } catch (IllegalArgumentException e) {
//		        	// TODO show validation errors
//		        	return null;
//		        }
//			}
//		};

		Bind.oneWay(modelBlobObservable)
		.to(PojoProperties.value(ImageControl.class, "blob", IBlob.class).observe(propertyControl));

		return propertyControl;
    }

    @Override
	public String formatValueForMessage(S extendableObject,
            ScalarPropertyAccessor<? extends IBlob,S> propertyAccessor) {
        // What do we do here?
        return "picture";
    }

    @Override
	public String formatValueForTable(S extendableObject,
            ScalarPropertyAccessor<? extends IBlob,S> propertyAccessor) {
        // What do we do here?
        return "picture";
    }

	@Override
	public IBlob getDefaultValue() {
		return null;
	}

    @Override
	public boolean isEditable() {
        return false;
    }

	@Override
	public Comparator<IBlob> getComparator() {
		return null;
	}
}