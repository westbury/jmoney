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