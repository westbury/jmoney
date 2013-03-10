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

import net.sf.jmoney.isolation.IModelObject;
import net.sf.jmoney.isolation.IScalarPropertyAccessor;
import net.sf.jmoney.isolation.SessionChangeAdapter;
import net.sf.jmoney.isolation.SessionChangeListener;
import net.sf.jmoney.model2.Account;
import net.sf.jmoney.model2.ExtendableObject;
import net.sf.jmoney.model2.IPropertyControl;
import net.sf.jmoney.model2.ScalarPropertyAccessor;

import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * Note that this class has neither get/set methods for the value being edited
 * and no support for property change listeners.  This is
 * because objects of this class are tied to an Account object.  
 * Changes to this
 * object are reflected by this object in the Account class objects.  
 * Consumers who are interested in changes to the Account class objects should
 * add themselves as listeners to the appropriate PropertyAccessor object.
 *
 * @author Nigel Westbury
 * @author Johann Gyger
 */
public class AccountEditor<S extends ExtendableObject, A extends Account> implements IPropertyControl<S> {

    private S extendableObject;

    private ScalarPropertyAccessor<A,S> accountPropertyAccessor;

    private AccountControl<A> propertyControl;

    private SessionChangeListener amountChangeListener = new SessionChangeAdapter() {
		@Override
		public void objectChanged(IModelObject changedObject, IScalarPropertyAccessor changedProperty, Object oldValue, Object newValue) {
			if (changedObject.equals(extendableObject) && changedProperty == accountPropertyAccessor) {
		    	A account = accountPropertyAccessor.getValue(extendableObject);
		        propertyControl.setAccount(account);
			}
		}

//		public <V, E extends ExtendableObject> void objectChanged2(E changedObject, ScalarPropertyAccessor<V,E> changedProperty, V oldValue, V newValue) {
//			if (changedObject.equals(extendableObject) && changedProperty == accountPropertyAccessor) {
//				changedProperty.typeIfGivenValue(extendableObject, value);
//		    	A account = (A)accountPropertyAccessor.getValue(extendableObject);
//		        propertyControl.setAccount(account);
//			}
//		}
	};
	
    /** 
     * @param propertyAccessor the accessor for the property to be edited
     * 			by this control.  The property must be of type Account.
     * 			If the property is of a type derived from Account
     * 			(e.g. BankAccount) then the list of accounts presented to
     * 			the user for selection will be restricted to accounts
     * 			of the appropriate type.
     * @param session the session whose accounts are listed in the combo box
     */
    public AccountEditor(Composite parent, ScalarPropertyAccessor<A,S> propertyAccessor) {
        propertyControl = new AccountControl<A>(parent, null, propertyAccessor.getClassOfValueObject());
        this.accountPropertyAccessor = propertyAccessor;

        /*
		 * Selection changes are reflected immediately in the account object.
		 * This allows other properties to enable/disable themselves if they
		 * become applicable/inapplicable as a result of the account change.
		 * 
		 * More obscure, but an account change may result in the change of the
		 * currency of the amounts in an entry and thus the amount controls can
		 * re-format the amounts for the new currency.
		 */

        propertyControl.addSelectionListener(new SelectionListener() {
        	@Override
			public void widgetSelected(SelectionEvent e) {
        		save(); 
        	} 
        	@Override
			public void widgetDefaultSelected(SelectionEvent e) { 
        		/*
        		 * Although users would normally select an account
        		 * using a single click, a double click causes a
        		 * change in the selection and so to be consistent we
        		 * should propagate the change at this time.
        		 */
        		save(); 
        	}
        });
    	
    	propertyControl.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
		    	if (extendableObject != null) {
		    		extendableObject.getDataManager().removeChangeListener(amountChangeListener);
		    	}
			}
		});
    }
    
    /**
     * Load the control with the value from the given account.
     */
    @Override
	public void load(S object) {
    	if (extendableObject != null) {
    		extendableObject.getDataManager().removeChangeListener(amountChangeListener);
    	}
    	
    	extendableObject = object;

    	if (object != null) {
    		/*
    		 * We must listen to the model for changes in the value
    		 * of this property.
    		 */
    		object.getDataManager().addChangeListener(amountChangeListener);

    		propertyControl.setSession(object.getSession(), accountPropertyAccessor.getClassOfValueObject());

    		A account = accountPropertyAccessor.getValue(object);
    		propertyControl.setAccount(account);
    	} else {
    		// Disable the control.  How this is done exactly depends on where the
    		// control is located.  So we leave it to the caller.
    	}
    }

    /**
     * Save the value from the control back into the object.
     *
     * Editors may update the property on a regular basis, not just when
     * the framework calls the <code>save</code> method.  However, the only time
     * that editors must update the property is when the framework calls this method.
     *
     * In this implementation we save the value back into the entry when the selection
     * is changed.  This causes the change to be seen in other views as soon as the
     * user changes the selection.
     *
     * The framework should never call this method when no account is selected
     * so we can assume that <code>extendableObject</code> is not null.
     */
    @Override
	public void save() {
        A account = propertyControl.getAccount();
        accountPropertyAccessor.setValue(extendableObject, account);
    }

    /* (non-Javadoc)
     * @see net.sf.jmoney.model2.IPropertyControl#getControl()
     */
    @Override
	public Control getControl() {
        return propertyControl;
    }

}