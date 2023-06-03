/*
*
*  JMoney - A Personal Finance Manager
*  Copyright (c) 2010 Nigel Westbury <westbury@users.sourceforge.net>
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

package net.sf.jmoney.ofx.model;

import java.util.HashMap;
import java.util.Map;

import net.sf.jmoney.isolation.IValues;
import net.sf.jmoney.model2.CapitalAccount;
import net.sf.jmoney.model2.CapitalAccountInfo;
import net.sf.jmoney.model2.ExtensionPropertySet;
import net.sf.jmoney.model2.IExtensionObjectConstructors;
import net.sf.jmoney.model2.IPropertyControlFactory;
import net.sf.jmoney.model2.IPropertySetInfo;
import net.sf.jmoney.model2.PropertyControlFactory;
import net.sf.jmoney.model2.PropertySet;
import net.sf.jmoney.model2.ScalarPropertyAccessor;

import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.internal.databinding.provisional.bind.Bind;
import org.eclipse.core.internal.databinding.provisional.bind.IBidiConverter;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.databinding.swt.SWTObservables;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * Provides the metadata for the extra properties added to each
 * currency account by this plug-in.
 *
 * @author Nigel Westbury
 */
public class OfxAccountInfo implements IPropertySetInfo {

	private static ExtensionPropertySet<OfxAccount,CapitalAccount> propertySet = PropertySet.addExtensionPropertySet(OfxAccount.class, CapitalAccountInfo.getPropertySet(), new IExtensionObjectConstructors<OfxAccount,CapitalAccount>() {

		@Override
		public OfxAccount construct(CapitalAccount extendedObject) {
			return new OfxAccount(extendedObject);
		}

		@Override
		public OfxAccount construct(CapitalAccount extendedObject, IValues<CapitalAccount> values) {
			return new OfxAccount(
					extendedObject,
					values.getScalarValue(getImportDataExtensionIdAccessor())
			);
		}
	});

	private static ScalarPropertyAccessor<String,CapitalAccount> importDataExtensionIdAccessor = null;

	@Override
	public PropertySet<OfxAccount,CapitalAccount> registerProperties() {

		IPropertyControlFactory<CapitalAccount,String> importDataControlFactory = new PropertyControlFactory<CapitalAccount,String>() {
//			@Override
//			public IPropertyControl<CapitalAccount> createPropertyControl(Composite parent,
//					ScalarPropertyAccessor<String,CapitalAccount> propertyAccessor) {
//
//				final List<String> ids = new ArrayList<String>();
//				final Combo control = new Combo(parent, SWT.READ_ONLY);
//
//				ids.add(null);
//				control.add("none");
//
//				IExtensionRegistry registry = Platform.getExtensionRegistry();
//				for (IConfigurationElement element: registry.getConfigurationElementsFor("net.sf.jmoney.importer.importdata")) { //$NON-NLS-1$
//					if (element.getName().equals("import-format")) { //$NON-NLS-1$
//						String label = element.getAttribute("label"); //$NON-NLS-1$
//						String id = element.getAttribute("id"); //$NON-NLS-1$
//						ids.add(id);
//						control.add(label);
//					}
//				}
//
//				control.setVisibleItemCount(15);
//
//				return new IPropertyControl<CapitalAccount>() {
//
//					private CapitalAccount account;
//
//					@Override
//					public Control getControl() {
//						return control;
//					}
//
//					@Override
//					public void load(CapitalAccount account) {
//						this.account = account;
//
//						String importFormatId = getImportDataExtensionIdAccessor().getValue(account);
//						int index = ids.indexOf(importFormatId);
//						control.select(index);
//					}
//
//					@Override
//					public void save() {
//						String importFormatId = ids.get(control.getSelectionIndex());
//						getImportDataExtensionIdAccessor().setValue(account, importFormatId);
//					}};
//			}

			@Override
			public Control createPropertyControl(
					Composite parent,
					ScalarPropertyAccessor<String, CapitalAccount> propertyAccessor,
					IObservableValue<? extends CapitalAccount> modelObservable) {

				final Map<String,String> labels = new HashMap<String,String>();
				final Map<String,String> ids = new HashMap<String,String>();
				final Combo control = new Combo(parent, SWT.READ_ONLY);

				ids.put("none", null);
				control.add("none");

				IExtensionRegistry registry = Platform.getExtensionRegistry();
				for (IConfigurationElement element: registry.getConfigurationElementsFor("net.sf.jmoney.importer.importdata")) { //$NON-NLS-1$
					if (element.getName().equals("import-format")) { //$NON-NLS-1$
						String label = element.getAttribute("label"); //$NON-NLS-1$
						String id = element.getAttribute("id"); //$NON-NLS-1$
						labels.put(id,label);
						ids.put(label,id);
						control.add(label);
					}
				}

				control.setVisibleItemCount(15);


		    	IBidiConverter<String,String> idToLabelConverter = new IBidiConverter<String,String>() {
					@Override
					public String modelToTarget(String fromValue) {
				    	if (fromValue == null) {
				    		return "none"; //$NON-NLS-1$
				    	} else {
				    		return labels.get(fromValue);
				    	}
					}

					@Override
					public String targetToModel(String text) {
				        return ids.get(text);
					}
				};

				Bind.twoWay(propertyAccessor.observeDetail(modelObservable))
				.convert(idToLabelConverter)
				.to(SWTObservables.observeSelection(control));

				return control;
			}

			@Override
			public String getDefaultValue() {
				// By default, no extension is set
				return null;
			}

			@Override
			public boolean isEditable() {
				return true;
			}
		};

		importDataExtensionIdAccessor = propertySet.addProperty("importDataExtensionId", "Table Structure", String.class, 1, 5, importDataControlFactory, null);

		return propertySet;
	}

	/**
	 * @return
	 */
	public static ExtensionPropertySet<OfxAccount,CapitalAccount> getPropertySet() {
		return propertySet;
	}

	/**
	 * @return
	 */
	public static ScalarPropertyAccessor<String,CapitalAccount> getImportDataExtensionIdAccessor() {
		return importDataExtensionIdAccessor;
	}
}
