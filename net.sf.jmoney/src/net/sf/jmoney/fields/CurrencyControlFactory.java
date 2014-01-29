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

import net.sf.jmoney.JMoneyPlugin;
import net.sf.jmoney.model2.Commodity;
import net.sf.jmoney.model2.Currency;
import net.sf.jmoney.model2.ExtendableObject;
import net.sf.jmoney.model2.IReferenceControlFactory;
import net.sf.jmoney.model2.PropertyControlFactory;
import net.sf.jmoney.model2.ScalarPropertyAccessor;
import net.sf.jmoney.model2.Session;
import net.sf.jmoney.resources.Messages;

import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.internal.databinding.provisional.bind.Bind;
import org.eclipse.core.internal.databinding.provisional.bind.IBidiConverter;
import org.eclipse.jface.databinding.swt.SWTObservables;
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
	public Control createPropertyControl(Composite parent, ScalarPropertyAccessor<Currency,S> propertyAccessor, S modelObject) {
    	return createPropertyControlInternal(parent, propertyAccessor.observe(modelObject));
    }

    @Override
	public Control createPropertyControl(Composite parent, ScalarPropertyAccessor<Currency,S> propertyAccessor, IObservableValue<? extends S> modelObservable) {
    	return createPropertyControlInternal(parent, propertyAccessor.observeDetail(modelObservable));
    }

    private Control createPropertyControlInternal(Composite parent, IObservableValue<Currency> modelCurrencyObservable) {
        CCombo propertyControl = new CCombo(parent, 0);

        Session session = JMoneyPlugin.getDefault().getSession();

        // TODO should we keep the combo list synchronized with the
        // currency list?

        for (Commodity commodity: session.getCommodityCollection()) {
            if (commodity instanceof Currency) {
                propertyControl.add(commodity.getName());
            }
        }

        IBidiConverter<Currency,String> currencyToName = new IBidiConverter<Currency,String>() {
			@Override
			public String modelToTarget(Currency fromObject) {
				return fromObject == null ? null : fromObject.getName();
			}

			@Override
			public Currency targetToModel(String amountString) {
		        Session session = JMoneyPlugin.getDefault().getSession();
		        for (Commodity commodity: session.getCommodityCollection()) {
		            if (commodity instanceof Currency) {
		                if (commodity.getName().equals(amountString)) {
		                	return (Currency)commodity;
		                }
		            }
		        }
		        throw new RuntimeException("error - text from target is not a valid selection");
			}
		};

		Bind.twoWay(modelCurrencyObservable)
		.convert(currencyToName)
		.to(SWTObservables.observeSelection(propertyControl));

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