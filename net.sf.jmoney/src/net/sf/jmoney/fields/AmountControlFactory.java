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

import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.internal.databinding.observable.ConstantObservableValue;
import org.eclipse.core.internal.databinding.provisional.bind.Bind;
import org.eclipse.core.internal.databinding.provisional.bind.IBidiWithExceptionConverter;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.databinding.fieldassist.ControlStatusDecoration;
import org.eclipse.jface.databinding.swt.SWTObservables;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;

import net.sf.jmoney.model2.Commodity;
import net.sf.jmoney.model2.ExtendableObject;
import net.sf.jmoney.model2.PropertyControlFactory;
import net.sf.jmoney.model2.ScalarPropertyAccessor;
import net.sf.jmoney.resources.Messages;

/**
 * A control factory to edit an amount of a commodity.
 *
 * @author Nigel Westbury
 * @author Johann Gyger
 */
public abstract class AmountControlFactory<S extends ExtendableObject> extends PropertyControlFactory<S,Long> {

    @Override
	public Control createPropertyControl(Composite parent, ScalarPropertyAccessor<Long,S> propertyAccessor, S modelObject) {
    	return createPropertyControlInternal(parent, propertyAccessor.observe(modelObject), new ConstantObservableValue<S>(modelObject, null));
    }

    @Override
	public Control createPropertyControl(Composite parent, ScalarPropertyAccessor<Long,S> propertyAccessor, IObservableValue<? extends S> modelObservable) {
    	return createPropertyControlInternal(parent, propertyAccessor.observeDetail(modelObservable), modelObservable);
    }

    private Control createPropertyControlInternal(Composite parent, IObservableValue<Long> modelAmountObservable, final IObservableValue<? extends S> parentObservable) {
    	Text propertyControl = new Text(parent, SWT.TRAIL);

    	IBidiWithExceptionConverter<Long,String> amountToText = new IBidiWithExceptionConverter<Long,String>() {
			@Override
			public String modelToTarget(Long fromObject) {
				if (fromObject == null) {
					return null;
				} else {
					Commodity commodity = getCommodity(parentObservable.getValue());
					return commodity.format(fromObject.longValue());
				}
			}

			@Override
			public Long targetToModel(String amountString) throws CoreException {
				Commodity commodity = getCommodity(parentObservable.getValue());
		        if (amountString.trim().length() == 0) {
					/*
					 * The text box is empty so this maps to null. The property
					 * may be 'long' in the model and so not accept nulls.
					 * However that is the responsibility of the binding chain
					 * outside of this class to detect that and behave
					 * accordingly.
					 */
		        	return null;
		        } else {
		        	return commodity.parse(amountString);
		        }
			}
		};

		ControlStatusDecoration statusDecoration = new ControlStatusDecoration(
				propertyControl, SWT.LEFT | SWT.TOP);

		Bind.twoWay(modelAmountObservable)
		.convertWithTracking(amountToText)
		.to(SWTObservables.observeText(propertyControl, SWT.Modify), statusDecoration::update);

		Bind.bounceBack(amountToText)
		.to(SWTObservables.observeText(propertyControl, SWT.FocusOut));

		return propertyControl;
	}

    @Override
    public String formatValueForMessage(S extendableObject, ScalarPropertyAccessor<? extends Long,S> propertyAccessor) {
        Long amount = propertyAccessor.getValue(extendableObject);
        if (amount == null) {
            return Messages.AmountControlFactory_None;
        } else {
            return "'" + getCommodity(extendableObject).format(amount.longValue()) + "'"; //$NON-NLS-1$ //$NON-NLS-2$
        }
    }

    @Override
    public String formatValueForTable(S extendableObject, ScalarPropertyAccessor<? extends Long,S> propertyAccessor) {
        Long amount = propertyAccessor.getValue(extendableObject);
        if (amount == null) {
            return ""; //$NON-NLS-1$
        } else {
        	// TODO: clean this up when we have a plan for incomplete data.
        	// For time being, use the default currency for formatting if the user
        	// has not set the currency for the account.
        	Commodity commodity = getCommodity(extendableObject);
        	if (commodity == null) {
        		commodity = extendableObject.getSession().getDefaultCurrency();
        	}
            return commodity.format(amount.longValue());
        }
    }

	@Override
	public Long getDefaultValue() {
		return 0L;
	}

	@Override
	public boolean isEditable() {
		return true;
	}

	protected abstract Commodity getCommodity(S object);
}