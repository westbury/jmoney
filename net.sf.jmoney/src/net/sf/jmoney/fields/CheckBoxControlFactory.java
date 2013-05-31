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
import net.sf.jmoney.model2.IPropertyControl;
import net.sf.jmoney.model2.IPropertyControlFactory;
import net.sf.jmoney.model2.ScalarPropertyAccessor;

import org.eclipse.core.databinding.bind.Bind;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.jface.databinding.swt.SWTObservables;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * A control factory to edit boolean values using a check box.
 *
 * @author Nigel Westbury
 */
public class CheckBoxControlFactory<S extends ExtendableObject> implements IPropertyControlFactory<S,Boolean> {

    public CheckBoxControlFactory() {
    }

    @Override
	public IPropertyControl<S> createPropertyControl(Composite parent, ScalarPropertyAccessor<Boolean,S> propertyAccessor) {
  		return new CheckMarkEditor<S>(parent, propertyAccessor);
    }

    @Override
	public Control createPropertyControl(Composite parent, ScalarPropertyAccessor<Boolean,S> propertyAccessor, IObservableValue<? extends S> modelObservable) {
    	Button button = new Button(parent, SWT.CHECK);
  		Bind.twoWay(propertyAccessor.observeDetail(modelObservable)).to(SWTObservables.observeSelection(button));
  		return button;
    }

    @Override
	public String formatValueForMessage(S extendableObject, ScalarPropertyAccessor<? extends Boolean,S> propertyAccessor) {
        Boolean value = propertyAccessor.getValue(extendableObject);
        if (value == null) {
            return "N/A"; //$NON-NLS-1$
        } else {
            return value.toString();
        }
    }

    @Override
	public String formatValueForTable(S extendableObject, ScalarPropertyAccessor<? extends Boolean,S> propertyAccessor) {
        Boolean value = propertyAccessor.getValue(extendableObject);
        if (value == null) {
            return ""; //$NON-NLS-1$
        } else {
            return value.toString();
        }
    }

	@Override
	public Boolean getDefaultValue() {
		return false;
	}

	@Override
	public boolean isEditable() {
		return true;
	}

	@Override
	public Comparator<Boolean> getComparator() {
		return new Comparator<Boolean>() {
			@Override
			public int compare(Boolean flag1, Boolean flag2) {
				return flag1.compareTo(flag2);
			}
		};
	}
}