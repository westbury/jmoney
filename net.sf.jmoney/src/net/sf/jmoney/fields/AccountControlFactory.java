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

import net.sf.jmoney.model2.Account;
import net.sf.jmoney.model2.ExtendableObject;
import net.sf.jmoney.model2.IReferenceControlFactory;
import net.sf.jmoney.model2.PropertyControlFactory;
import net.sf.jmoney.model2.ScalarPropertyAccessor;
import net.sf.jmoney.model2.Session;
import net.sf.jmoney.resources.Messages;

import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.WritableValue;
import org.eclipse.core.internal.databinding.provisional.bind.Bind;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * A control factory to select an account.
 *
 * @author Nigel Westbury
 * @author Johann Gyger
 */
public abstract class AccountControlFactory<P, S extends ExtendableObject, A extends Account> extends PropertyControlFactory<S,A> implements IReferenceControlFactory<P,S,A> {

	   @Override
	public Control createPropertyControl(Composite parent,
			ScalarPropertyAccessor<A, S> propertyAccessor,
			S modelObject) {
 	WritableValue<S> observable = new WritableValue<S>();
 	observable.setValue(modelObject);
		return createPropertyControl(parent, propertyAccessor, observable);
	}

	@Override
	public Control createPropertyControl(Composite parent,
			final ScalarPropertyAccessor<A, S> propertyAccessor,
			final IObservableValue<? extends S> modelObservable) {
		
		/*
		 * We have a problem in that the account control needs to know the session so it can
		 * get the list of accounts.  It could use the default session for the active workbench window, but we want to avoid
		 * use of the default session because that is poor design.  It also means we need to convert to instances of the selected
		 * account that is in the correct data manager.  Furthermore, the list of accounts inside a transaction could be different
		 * from the list of committed accounts.
		 * 
		 * We can get the data manager from an object.  However, modelObservable may be null when this method is called.  Therefore
		 * AccountControl provides a method to get the session when the list is dropped down.  By the time the user drops down the list,
		 * a session must be available.
		 */

		final AccountControl<A> control = new AccountControl<A>(parent, propertyAccessor.getClassOfValueObject()) {
			@Override
			protected Session getSession() {
				return modelObservable.getValue() == null
						? null
								: modelObservable.getValue().getSession();
			}
		};

	    Bind.twoWay(propertyAccessor.observeDetail(modelObservable))
		.to(control.account);

		return control;
	}

	@Override
	public String formatValueForMessage(S extendableObject, ScalarPropertyAccessor<? extends A,S> propertyAccessor) {
        Account value = propertyAccessor.getValue(extendableObject);
        return value == null ? Messages.AccountControlFactory_None : value.getFullAccountName();
    }

    @Override
    public String formatValueForTable(S extendableObject, ScalarPropertyAccessor<? extends A,S> propertyAccessor) {
        Account value = propertyAccessor.getValue(extendableObject);
        return value == null ? "" : value.getFullAccountName(); //$NON-NLS-1$
    }

	@Override
	public A getDefaultValue() {
		return null;
	}

	@Override
	public boolean isEditable() {
		return true;
	}
}