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

package net.sf.jmoney.importer.model;

import java.util.Collections;
import java.util.List;

import net.sf.jmoney.importer.wizards.IAccountImportWizard;
import net.sf.jmoney.model2.CapitalAccount;
import net.sf.jmoney.model2.ExtendableObject;
import net.sf.jmoney.model2.PropertyControlFactory;
import net.sf.jmoney.model2.ScalarPropertyAccessor;
import net.sf.jmoney.resources.Messages;

import org.eclipse.core.databinding.observable.list.ComputedList;
import org.eclipse.core.databinding.observable.list.IObservableList;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.internal.databinding.provisional.bind.Bind;
import org.eclipse.core.internal.databinding.provisional.bind.IBidiConverter;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.databinding.viewers.ObservableListContentProvider;
import org.eclipse.jface.databinding.viewers.ViewersObservables;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * A control factory to select a transaction type.
 *
 * @author Nigel Westbury
 */
public class TransactionTypeControlFactory<P, S extends ExtendableObject> extends PropertyControlFactory<S,String> {

//	public List<TransactionType> allTransactionTypes = new ArrayList<TransactionType>();
	
	TransactionTypeControlFactory() {
		// TODO should not be passing null here.  Must use actual account type.
//		allTransactionTypes = getTransactionTypes(null);
	}
	
    @Override
	public Control createPropertyControl(Composite parent, ScalarPropertyAccessor<String,S> propertyAccessor, final S modelObject) {
    	IObservableList<TransactionType> transTypeListObservable = new ComputedList<TransactionType>() {
			@Override
			protected List<TransactionType> calculate() {
				// TODO pass correct account.  A null value is currently accepted but should not be.
				return getTransactionTypes(null);
			}
    	};

    	return createPropertyControlInternal(parent, propertyAccessor.observe(modelObject), transTypeListObservable);
    }

    @Override
	public Control createPropertyControl(Composite parent, ScalarPropertyAccessor<String,S> propertyAccessor, final IObservableValue<? extends S> modelObservable) {
    	IObservableList<TransactionType> transTypeListObservable = new ComputedList<TransactionType>() {
			@Override
			protected List<TransactionType> calculate() {
				/*
				 * The model object may be null if the control is created before input
				 * is set.
				 */
				// TODO pass correct account.  A null value is currently accepted but should not be.
				
				return getTransactionTypes(null);
			}
    	};

    	return createPropertyControlInternal(parent, propertyAccessor.observeDetail(modelObservable), transTypeListObservable);
    }

    
    private Control createPropertyControlInternal(Composite parent, IObservableValue<String> modelTransTypeIdObservable, final IObservableList<TransactionType> transTypeList) {
        CCombo propertyControl = new CCombo(parent, SWT.NONE);
        ComboViewer viewer = new ComboViewer(propertyControl);

        viewer.setContentProvider(new ObservableListContentProvider<TransactionType>(TransactionType.class));
        viewer.setInput(transTypeList);

        Bind.twoWay(modelTransTypeIdObservable)
        .convert(new IBidiConverter<String,TransactionType>() {

			@Override
			public TransactionType modelToTarget(String transactionTypeId) {
				return lookupTransactionType(transactionTypeId, transTypeList);
			}

			@Override
			public String targetToModel(TransactionType transactionType)
					throws CoreException {
				return transactionType.getId();
			}
		})
        .to(ViewersObservables.observeSingleSelection(viewer, TransactionType.class));

		return propertyControl;
    }

    private TransactionType lookupTransactionType(String transactionTypeId, List<TransactionType> transTypeList) {
		for (TransactionType transType : transTypeList) {
			if (transType.getId().equals(transactionTypeId)) {
				return transType;
			}
		}
		return null;
	}

	@Override
    public String formatValueForMessage(S extendableObject, ScalarPropertyAccessor<? extends String,S> propertyAccessor) {
        String transactionTypeId = propertyAccessor.getValue(extendableObject);
        MemoPattern pattern = (MemoPattern)extendableObject;
        CapitalAccount account = (CapitalAccount)pattern.getParentKey().getObject();
        TransactionType value = lookupTransactionType(transactionTypeId, getTransactionTypes(account));
        return value == null ? Messages.CurrencyControlFactory_None : "'" + value.getLabel() + "'";  //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
    }

    @Override
    public String formatValueForTable(S extendableObject, ScalarPropertyAccessor<? extends String,S> propertyAccessor) {
        String transactionTypeId = propertyAccessor.getValue(extendableObject);
        MemoPattern pattern = (MemoPattern)extendableObject;
        CapitalAccount account = (CapitalAccount)pattern.getParentKey().getObject();
        TransactionType value = lookupTransactionType(transactionTypeId, getTransactionTypes(account));
        return value == null ? "" : value.getLabel(); //$NON-NLS-1$
    }

    /**
     * This is a general implementation that has no context.  The actual supported transaction
     * types depend on context.   So we must include all possible transaction types here, regardless
     * of whether they are supported only by OFX or only by custom CSV import.
     * 
     * @param account
     * @return
     */
	private List<TransactionType> getTransactionTypes(CapitalAccount account) {
		
//		IConfigurationElement wizardElement = findWizard(account);

		// Merge with code in CsvImportToAccountAssociations???
		String importDataExtensionId = ImportAccountInfo.getImportDataExtensionIdAccessor().observe(account).getValue();
		IConfigurationElement matchingElement = null;
		if (importDataExtensionId != null) {
			// Find the wizard by reading the registry.
			IAccountImportWizard wizard = null;
			IExtensionRegistry registry = Platform.getExtensionRegistry();
			for (IConfigurationElement element: registry.getConfigurationElementsFor("net.sf.jmoney.importer.importdata")) { //$NON-NLS-1$ $NON-NLS-2$
				if (element.getName().equals("import-format") //$NON-NLS-1$
						&& element.getAttribute("id").equals(importDataExtensionId)) { //$NON-NLS-1$
					matchingElement = element;
					break;
				}
			}
		}
		
		if (matchingElement != null) {
			try {
				Object executableExtension = matchingElement.createExecutableExtension("class"); //$NON-NLS-1$
				IAccountImportWizard wizard = (IAccountImportWizard)executableExtension;
				return wizard.getApplicableTransactionTypes();
			} catch (CoreException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		// No CSV wizard found, so return those for OFX.
		return Collections.<TransactionType>singletonList(new TransactionTypeBasic());
	}

	@Override
	public String getDefaultValue() {
		return null;
	}

	@Override
	public boolean isEditable() {
		return true;
	}

}