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
import net.sf.jmoney.resources.Messages;

import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.internal.databinding.provisional.bind.Bind;
import org.eclipse.jface.databinding.swt.SWTObservables;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;

/**
 * A control factory to edit multi line text.
 *
 * @author Nigel Westbury
 * @author Johann Gyger
 */
public class MultiTextControlFactory<S extends ExtendableObject> implements IPropertyControlFactory<S,String> {

    @Override
	public Control createPropertyControl(Composite parent, ScalarPropertyAccessor<String,S> propertyAccessor, S modelObject) {
    	return createPropertyControlInternal(parent, propertyAccessor.observe(modelObject));
    }

    @Override
	public Control createPropertyControl(Composite parent, ScalarPropertyAccessor<String,S> propertyAccessor, IObservableValue<? extends S> modelObservable) {
    	return createPropertyControlInternal(parent, propertyAccessor.observeDetail(modelObservable));
    }
    
    private Control createPropertyControlInternal(Composite parent, IObservableValue<String> modelStringObservable) {
    	Text propertyControl = new Text(parent, SWT.MULTI | SWT.WRAP);

        GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
        propertyControl.setLayoutData(gridData);

		Bind.twoWay(modelStringObservable)
		.to(SWTObservables.observeText(propertyControl, SWT.Modify));

		return propertyControl;
    }

    @Override
	public String formatValueForMessage(S extendableObject, ScalarPropertyAccessor<? extends String,S> propertyAccessor) {
        String value = propertyAccessor.getValue(extendableObject);
        if (value == null || value.length() == 0) {
            return Messages.MultiTextControlFactory_Empty;
        } else {
            return "'" + value + "'"; //$NON-NLS-1$ //$NON-NLS-2$
        }
    }

    @Override
	public String formatValueForTable(S extendableObject, ScalarPropertyAccessor<? extends String,S> propertyAccessor) {
        return propertyAccessor.getValue(extendableObject);
    }

	@Override
	public String getDefaultValue() {
		return null;
	}

	@Override
	public boolean isEditable() {
		return true;
	}

	@Override
	public Comparator<String> getComparator() {
		return null;
	}
}