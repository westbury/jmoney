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

import net.sf.jmoney.fields.NonEditableTextControlFactory;
import net.sf.jmoney.isolation.IValues;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.EntryInfo;
import net.sf.jmoney.model2.ExtensionPropertySet;
import net.sf.jmoney.model2.IExtensionObjectConstructors;
import net.sf.jmoney.model2.IPropertyControl;
import net.sf.jmoney.model2.IPropertyControlFactory;
import net.sf.jmoney.model2.IPropertySetInfo;
import net.sf.jmoney.model2.PropertyControlFactory;
import net.sf.jmoney.model2.PropertySet;
import net.sf.jmoney.model2.ScalarPropertyAccessor;

import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.internal.databinding.provisional.bind.Bind;
import org.eclipse.core.internal.databinding.provisional.bind.IBidiConverter;
import org.eclipse.jface.databinding.swt.SWTObservables;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;

/**
 * Provides the metadata for the extra properties added to each
 * entry by this plug-in.
 *
 * @author Nigel Westbury
 */
public class ReconciliationEntryInfo implements IPropertySetInfo {

	private static ExtensionPropertySet<ReconciliationEntry,Entry> propertySet = PropertySet.addExtensionPropertySet(ReconciliationEntry.class, EntryInfo.getPropertySet(), new IExtensionObjectConstructors<ReconciliationEntry,Entry>() {

		@Override
		public ReconciliationEntry construct(Entry extendedObject) {
			return new ReconciliationEntry(extendedObject);
		}

		@Override
		public ReconciliationEntry construct(Entry extendedObject, IValues<Entry> values) {
			Integer a = values.getScalarValue(getStatusAccessor());
			BankStatement b = values.getScalarValue(getStatementAccessor());
			String c = values.getScalarValue(getUniqueIdAccessor());
			return new ReconciliationEntry(
					extendedObject,
					a,
					b,
					c
			);
		}
	});

	private static ScalarPropertyAccessor<Integer,Entry> statusAccessor = null;
	private static ScalarPropertyAccessor<BankStatement,Entry> statementAccessor = null;
	private static ScalarPropertyAccessor<String,Entry> uniqueIdAccessor = null;

	@Override
	public PropertySet registerProperties() {

		IPropertyControlFactory<Entry,BankStatement> statementControlFactory = new PropertyControlFactory<Entry,BankStatement>() {
//			@Override
//			public IPropertyControl<Entry> createPropertyControl(Composite parent, final ScalarPropertyAccessor<BankStatement,Entry> propertyAccessor) {
//		        final Text control = new Text(parent, SWT.NONE);
//		        return new IPropertyControl<Entry>() {
//
//		        	private Entry object;
//
//					@Override
//					public Control getControl() {
//						return control;
//					}
//
//					@Override
//					public void load(Entry object) {
//						this.object = object;
//						BankStatement statement = propertyAccessor.getValue(object);
//						if (statement == null) {
//							control.setText("");
//						} else {
//							control.setText(statement.toString());
//						}
//					}
//
//					@Override
//					public void save() {
//						// TODO: make this more robust.
//						// And the control is better if a CCombo.
//						String text = control.getText();
//						BankStatement value;
//						if (text.trim().isEmpty()) {
//							value = null;
//						} else {
//							value = new BankStatement(text);
//						}
//						propertyAccessor.setValue(object, value);
//					}
//		        };
//			}

			@Override
			public Control createPropertyControl(
					Composite parent,
					ScalarPropertyAccessor<BankStatement, Entry> propertyAccessor,
					IObservableValue<? extends Entry> modelObservable) {
		        final Text propertyControl = new Text(parent, SWT.NONE);

		        IBidiConverter<BankStatement,String> integerToStringConverter = new IBidiConverter<BankStatement,String>() {
					@Override
					public String modelToTarget(BankStatement fromValue) {
				    	if (fromValue == null) {
				    		return ""; //$NON-NLS-1$
				    	} else {
				    		return fromValue.toString();
				    	}
					}

					@Override
					public BankStatement targetToModel(String text) {
						// TODO: make this more robust.
						// And the control is better if a CCombo.
						BankStatement value;
						if (text.trim().isEmpty()) {
							value = null;
						} else {
							value = new BankStatement(text);
						}
						return value;
					}
				};

				Bind.twoWay(propertyAccessor.observeDetail(modelObservable))
				.convert(integerToStringConverter)
				.to(SWTObservables.observeText(propertyControl, SWT.Modify));

				return propertyControl;
			}

			@Override
			public String formatValueForMessage(Entry extendableObject, ScalarPropertyAccessor<? extends BankStatement,Entry> propertyAccessor) {
				BankStatement statement = propertyAccessor.getValue(extendableObject);
				if (statement == null) {
					return "unreconciled";
				} else {
					return statement.toString();
				}
			}

			@Override
			public String formatValueForTable(Entry extendableObject, ScalarPropertyAccessor<? extends BankStatement,Entry> propertyAccessor) {
				BankStatement statement = propertyAccessor.getValue(extendableObject);
				if (statement == null) {
					return "unreconciled";
				} else {
					return statement.toString();
				}
			}

			@Override
			public BankStatement getDefaultValue() {
				// By default, not on any statement (unreconciled)
				return null;
			}

			@Override
			public boolean isEditable() {
				return true;
			}
		};

		// TODO: correct localized text:
		statusAccessor    = propertySet.addProperty("status", ReconciliationPlugin.getResourceString("Entry.statusShort"), Integer.class, 1, 30, new StatusControlFactory(), null);
		statementAccessor = propertySet.addProperty("statement", ReconciliationPlugin.getResourceString("Entry.statementShort"), BankStatement.class, 1, 80, statementControlFactory, null);
		uniqueIdAccessor  = propertySet.addProperty("uniqueId", ReconciliationPlugin.getResourceString("Entry.uniqueIdShort"), String.class, 1, 80, new NonEditableTextControlFactory(), null);

		return propertySet;
	}

	/**
	 * @return
	 */
	public static ExtensionPropertySet<ReconciliationEntry,Entry> getPropertySet() {
		return propertySet;
	}

	/**
	 * @return
	 */
	public static ScalarPropertyAccessor<Integer,Entry> getStatusAccessor() {
		return statusAccessor;
	}

	/**
	 * @return
	 */
	public static ScalarPropertyAccessor<BankStatement,Entry> getStatementAccessor() {
		return statementAccessor;
	}

	/**
	 * @return
	 */
	public static ScalarPropertyAccessor<String,Entry> getUniqueIdAccessor() {
		return uniqueIdAccessor;
	}
}
