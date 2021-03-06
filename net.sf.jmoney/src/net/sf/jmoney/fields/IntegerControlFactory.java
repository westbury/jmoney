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

import java.util.Comparator;

import net.sf.jmoney.model2.ExtendableObject;
import net.sf.jmoney.model2.IPropertyControlFactory;
import net.sf.jmoney.model2.ScalarPropertyAccessor;

import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.internal.databinding.provisional.bind.Bind;
import org.eclipse.core.internal.databinding.provisional.bind.IBidiConverter;
import org.eclipse.jface.databinding.swt.SWTObservables;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;

/**
 * A control factory for integer properties.
 *
 * @author Nigel Westbury
 */
public class IntegerControlFactory<S extends ExtendableObject> implements IPropertyControlFactory<S,Integer> {

    @Override
	public Control createPropertyControl(Composite parent, ScalarPropertyAccessor<Integer,S> propertyAccessor, S modelObject) {
    	return createPropertyControlInternal(parent, propertyAccessor.observe(modelObject));
    }

    @Override
	public Control createPropertyControl(Composite parent, ScalarPropertyAccessor<Integer,S> propertyAccessor, IObservableValue<? extends S> modelObservable) {
    	return createPropertyControlInternal(parent, propertyAccessor.observeDetail(modelObservable));
    }
    
    private Control createPropertyControlInternal(Composite parent, IObservableValue<Integer> modelIntegerObservable) {
	Text propertyControl = new Text(parent, SWT.NONE);

    	IBidiConverter<Integer,String> integerToStringConverter = new IBidiConverter<Integer,String>() {
			@Override
			public String modelToTarget(Integer fromValue) {
		    	if (fromValue == null) {
		    		return ""; //$NON-NLS-1$
		    	} else {
		    		return fromValue.toString();
		    	}
			}

			@Override
			public Integer targetToModel(String text) {
		        try {
		        	return Integer.parseInt(text);
		        } catch (IllegalArgumentException e) {
		        	// TODO show validation errors
		        	return null;
		        }
			}
		};

		Bind.twoWay(modelIntegerObservable)
		.convert(integerToStringConverter)
		.to(SWTObservables.observeText(propertyControl, SWT.Modify));

		return propertyControl;
    }

    @Override
    public String formatValueForMessage(S extendableObject, ScalarPropertyAccessor<? extends Integer,S> propertyAccessor) {
    	Integer value = propertyAccessor.getValue(extendableObject);
    	return value.toString();
    }

    @Override
	public String formatValueForTable(S extendableObject, ScalarPropertyAccessor<? extends Integer,S> propertyAccessor) {
 	   Integer value = propertyAccessor.getValue(extendableObject);
       return value.toString();
    }

	@Override
	public Integer getDefaultValue() {
		return 0;
	}

	@Override
	public boolean isEditable() {
		return true;
	}

	@Override
	public Comparator<Integer> getComparator() {
		return new Comparator<Integer>() {
			@Override
			public int compare(Integer value1, Integer value2) {
				return value1.compareTo(value2);
			}
		};
	}
}