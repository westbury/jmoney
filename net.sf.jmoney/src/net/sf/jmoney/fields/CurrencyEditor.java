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
import net.sf.jmoney.model2.CurrencyAccount;
import net.sf.jmoney.model2.CurrencyAccountInfo;
import net.sf.jmoney.model2.ExtendableObject;
import net.sf.jmoney.model2.IPropertyControl;
import net.sf.jmoney.model2.ScalarPropertyAccessor;
import net.sf.jmoney.model2.Session;

import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * Note that this class has neither get/set methods for the value being edited
 * and no support for property change listeners.  This is
 * because objects of this class are tied to an CapitalAccount object.  
 * Changes to this
 * object are reflected by this object in the CapitalAccount class objects.  
 * Consumers who are interested in changes to the CapitalAccount class objects should
 * add themselves as listeners to the appropriate PropertyAccessor object.
 *
 * @author Nigel Westbury
 * @author Johann Gyger
 */
public class CurrencyEditor<S extends ExtendableObject> implements IPropertyControl<S> {

    private S extendableObject;

    private ScalarPropertyAccessor<Currency,S> currencyPropertyAccessor;

    private CCombo propertyControl;

    /** 
     * @param propertyAccessor the accessor for the property to be edited
     * 			by this control.  The property must be of type Currency.
     */
    public CurrencyEditor(Composite parent, ScalarPropertyAccessor<Currency,S> propertyAccessor) {
        propertyControl = new CCombo(parent, 0);
        this.currencyPropertyAccessor = propertyAccessor;

        Session session = JMoneyPlugin.getDefault().getSession();

        for (Commodity commodity: session.getCommodityCollection()) {
            if (commodity instanceof Currency) {
                propertyControl.add(commodity.getName());
            }
        }

        // Selection changes are reflected immediately in the
        // mutable account object.  This allows other properties
        // such as money amounts to listen for changes to the
        // currency and change their format to be correct for
        // the newly selected currency.

        propertyControl.addSelectionListener(new SelectionListener() {
            @Override
			public void widgetSelected(SelectionEvent e) {
                save();
            }
            @Override
			public void widgetDefaultSelected(SelectionEvent e) {
                // Should this be here?
                save();
            }
        });
    }

    /**
     * Load the control with the value from the given model object.
     */
    @Override
	public void load(S extendableObject) {
    	this.extendableObject = extendableObject;
    	
    	if (extendableObject == null) {
            propertyControl.setText(""); //$NON-NLS-1$
    	} else {
            Currency currency = currencyPropertyAccessor.getValue(extendableObject);
            // Currency should not be null, but check in case of incomplete data.
            // TODO: clean this up when we know the policy on invalid data in the
            // datastore.
            if (currency == null) {
                propertyControl.setText(""); //$NON-NLS-1$
            } else {
            	propertyControl.setText(currency.getName() == null ? "" : currency.getName()); //$NON-NLS-1$
            }

            // If the currency property being edited is the currency
            // for a CurrencyAccount and the account has entries then the currency cannot
            // be changed.  We therefore disable the control.
            // It might be that at some future time we implement
            // an extension point that allows plug-ins to veto
            // changes.  If so then this may be better implemented
            // using such an extension point.
            if (currencyPropertyAccessor == CurrencyAccountInfo.getCurrencyAccessor()) {
                CurrencyAccount currencyAccount = (CurrencyAccount) extendableObject;
                propertyControl.setEnabled(!currencyAccount.hasEntries());
            }
    	}
    	propertyControl.setEnabled(extendableObject != null);
    }

    /**
     * Save the value from the control back into the account object.
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
     * so we can assume that <code>account</code> is not null.
     */
    @Override
	public void save() {
        String currencyName = propertyControl.getText();
        for (Commodity commodity: extendableObject.getSession().getCommodityCollection()) {
            if (commodity instanceof Currency && commodity.getName().equals(currencyName)) {
            	currencyPropertyAccessor.setValue(extendableObject, (Currency) commodity);
            }
        }
    }

    /* (non-Javadoc)
     * @see net.sf.jmoney.model2.IPropertyControl#getControl()
     */
    @Override
	public Control getControl() {
        return propertyControl;
    }

}