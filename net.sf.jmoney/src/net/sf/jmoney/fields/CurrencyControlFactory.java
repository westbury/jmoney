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

import java.util.ArrayList;
import java.util.List;

import net.sf.jmoney.model2.Commodity;
import net.sf.jmoney.model2.Currency;
import net.sf.jmoney.model2.ExtendableObject;
import net.sf.jmoney.model2.IReferenceControlFactory;
import net.sf.jmoney.model2.PropertyControlFactory;
import net.sf.jmoney.model2.ScalarPropertyAccessor;
import net.sf.jmoney.resources.Messages;

import org.eclipse.core.databinding.observable.list.ComputedList;
import org.eclipse.core.databinding.observable.list.IObservableList;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.internal.databinding.provisional.bind.Bind;
import org.eclipse.jface.databinding.viewers.ObservableListContentProvider;
import org.eclipse.jface.databinding.viewers.ViewersObservables;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * A control factory to select a currency.
 *
 * @author Nigel Westbury
 * @author Johann Gyger
 */
public abstract class CurrencyControlFactory<P, S extends ExtendableObject> extends PropertyControlFactory<S,Currency> implements IReferenceControlFactory<P,S,Currency> {

    @Override
	public Control createPropertyControl(Composite parent, ScalarPropertyAccessor<Currency,S> propertyAccessor, final S modelObject) {
    	IObservableList<Currency> currencyListObservable = new ComputedList<Currency>() {
			@Override
			protected List<Currency> calculate() {
				List<Currency> result = new ArrayList<Currency>();
		        for (Commodity commodity: modelObject.getSession().getCommodityCollection()) {
		            if (commodity instanceof Currency) {
		                result.add((Currency)commodity);
		            }
		        }
				return result;
			}
    	};

    	return createPropertyControlInternal(parent, propertyAccessor.observe(modelObject), currencyListObservable);
    }

    @Override
	public Control createPropertyControl(Composite parent, ScalarPropertyAccessor<Currency,S> propertyAccessor, final IObservableValue<? extends S> modelObservable) {
    	IObservableList<Currency> currencyListObservable = new ComputedList<Currency>() {
			@Override
			protected List<Currency> calculate() {
				List<Currency> result = new ArrayList<Currency>();
		        for (Commodity commodity: modelObservable.getValue().getSession().getCommodityCollection()) {
		            if (commodity instanceof Currency) {
		                result.add((Currency)commodity);
		            }
		        }
				return result;
			}
    	};

    	return createPropertyControlInternal(parent, propertyAccessor.observeDetail(modelObservable), currencyListObservable);
    }

    
    private Control createPropertyControlInternal(Composite parent, IObservableValue<Currency> modelCurrencyObservable, IObservableList<Currency> currencyList) {
        CCombo propertyControl = new CCombo(parent, SWT.NONE);
        ComboViewer viewer = new ComboViewer(propertyControl);

        viewer.setContentProvider(new ObservableListContentProvider<Currency>(Currency.class));
        viewer.setInput(currencyList);

        Bind.twoWay(modelCurrencyObservable)
        .to(ViewersObservables.observeSingleSelection(viewer, Currency.class));

		return propertyControl;
    }

    @Override
    public String formatValueForMessage(S extendableObject, ScalarPropertyAccessor<? extends Currency,S> propertyAccessor) {
        Currency value = propertyAccessor.getValue(extendableObject);
        return value == null ? Messages.CurrencyControlFactory_None : "'" + value.getName() + "'";  //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
    }

    @Override
    public String formatValueForTable(S extendableObject, ScalarPropertyAccessor<? extends Currency,S> propertyAccessor) {
        Currency value = propertyAccessor.getValue(extendableObject);
        return value == null ? "" : value.getCode(); //$NON-NLS-1$
    }

	@Override
	public Currency getDefaultValue() {
		return null;
	}

	@Override
	public boolean isEditable() {
		return true;
	}
}