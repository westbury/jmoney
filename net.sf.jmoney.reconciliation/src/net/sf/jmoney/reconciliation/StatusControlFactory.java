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

package net.sf.jmoney.reconciliation;

import java.util.Comparator;

import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.IPropertyControl;
import net.sf.jmoney.model2.IPropertyControlFactory;
import net.sf.jmoney.model2.ScalarPropertyAccessor;

import org.eclipse.core.databinding.bind.Bind;
import org.eclipse.core.databinding.observable.value.AbstractObservableValue;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * A control factory to select the uncleared/pending/reconciled status
 * of an entry.
 *
 * @author Nigel Westbury
 */
public class StatusControlFactory implements IPropertyControlFactory<Entry,Integer> {

	/**
	 * Localized descriptions of the reconciled/cleared status
	 */
	static String[] statusText = new String[] {
			ReconciliationPlugin.getResourceString("Entry.uncleared"),
			ReconciliationPlugin.getResourceString("Entry.reconciling"),
			ReconciliationPlugin.getResourceString("Entry.cleared"),
	};

	/**
	 * Single letter indicators to be used in tables
	 */
	static String[] shortStatusText = new String[] {
			ReconciliationPlugin.getResourceString("Entry.unclearedShort"),
			ReconciliationPlugin.getResourceString("Entry.reconcilingShort"),
			ReconciliationPlugin.getResourceString("Entry.clearedShort"),
	};

    @Override
	public IPropertyControl createPropertyControl(Composite parent, ScalarPropertyAccessor<Integer,Entry> propertyAccessor) {
        return new StatusEditor(parent, propertyAccessor, statusText);
    }

	@Override
	public Control createPropertyControl(Composite parent,
			ScalarPropertyAccessor<Integer, Entry> propertyAccessor,
			IObservableValue<? extends Entry> modelObservable) {
        final CCombo propertyControl = new CCombo(parent, 0);

        propertyControl.setItems(statusText);

//    	IBidiConverter<Integer,String> integerToStringConverter = new IBidiConverter<Integer,String>() {
//			@Override
//			public String modelToTarget(Integer fromValue) {
//		    	if (fromValue == null) {
//		    		return ""; //$NON-NLS-1$
//		    	} else {
//		    		return fromValue.toString();
//		    	}
//			}
//
//			@Override
//			public Integer targetToModel(String text) {
//		        try {
//		        	return Integer.parseInt(text);
//		        } catch (IllegalArgumentException e) {
//		        	// TODO show validation errors
//		        	return null;
//		        }
//			}
//		};

		IObservableValue<Integer> target = new AbstractObservableValue<Integer>() {

			@Override
			public Object getValueType() {
				return Integer.class;
			}

			@Override
			protected Integer doGetValue() {
				return propertyControl.getSelectionIndex();
			}

			@Override
			protected void doSetValue(Integer index) {
				propertyControl.select(index);
			}
		};

		Bind.twoWay(propertyAccessor.observeDetail(modelObservable))
//		.convert(integerToStringConverter)
		.to(target);

		return propertyControl;
	}

    @Override
	public String formatValueForMessage(Entry extendableObject, ScalarPropertyAccessor<? extends Integer,Entry> propertyAccessor) {
        int status = propertyAccessor.getValue(extendableObject);
        return statusText[status];
    }

    @Override
	public String formatValueForTable(Entry extendableObject, ScalarPropertyAccessor<? extends Integer,Entry> propertyAccessor) {
        int status = propertyAccessor.getValue(extendableObject);
        return shortStatusText[status];
    }

	@Override
	public Integer getDefaultValue() {
		// By default, unreconciled
		return ReconciliationEntry.UNCLEARED;
	}

	@Override
	public boolean isEditable() {
		return true;
	}

	@Override
	public Comparator<Integer> getComparator() {
		return new Comparator<Integer> () {
			@Override
			public int compare(Integer status1, Integer status2) {
				return status1 - status2;
			}
		};
	}
}