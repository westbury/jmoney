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

package net.sf.jmoney.qif;

import net.sf.jmoney.fields.TextControlFactory;
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

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

/**
 * Add extra properties to the Entry objects to support QIF import
 * and export.  The plug-in in which this class appears provides support
 * to import and export QIF files.  However not all properties supported
 * by QIF files are included in the base set of JMoney properties.
 * We do not want to drop any data when importing a QIF file because,
 * even though the JMoney framework does not know about all the
 * properties, plug-ins may be able to make use of the properties.
 * This class adds all the properties supported by QIF that are
 * not base JMoney properties.
 * <P>
 * The data can be accessed by a plug-in in one of three ways:
 * <LI>A plug-in can depend on this plug-in.  That plug-in can then
 * 		access the properties in this class.</LI>
 * <LI>A propagator plug-in can progagate property values between this
 * 		class and properties in other Entry extension property set 
 * 		classes.
 * 		This approach should be taken if using a plug-in that was
 * 		developed without any knowledge of this plug-in.</LI>
 * <LI>Even if no other plug-in accesses a property imported by QIF
 * 		import, the property value will be maintained for as long as
 * 		the entry is not deleted and will be written out if a QIF
 * 		export is performed.</LI>
 *
 * @author Nigel Westbury
 */
public class QIFEntryInfo implements IPropertySetInfo {

	private static ExtensionPropertySet<QIFEntry,Entry> propertySet = PropertySet.addExtensionPropertySet(QIFEntry.class, EntryInfo.getPropertySet(), new IExtensionObjectConstructors<QIFEntry,Entry>() {

		@Override
		public QIFEntry construct(Entry extendedObject) {
			return new QIFEntry(extendedObject);
		}

		@Override
		public QIFEntry construct(Entry extendedObject, IValues<Entry> values) {
			return new QIFEntry(
					extendedObject, 
					values.getScalarValue(getReconcilingStateAccessor()),
					values.getScalarValue(getAddressAccessor()) 
			);
		}
	});
	
	private static ScalarPropertyAccessor<Character,Entry> reconcilingStateAccessor;
	private static ScalarPropertyAccessor<String,Entry> addressAccessor;
	
	@Override
	public PropertySet registerProperties() {
		IPropertyControlFactory<Entry,String> textControlFactory = new TextControlFactory<Entry>();
		IPropertyControlFactory<Entry,Character> stateControlFactory = new PropertyControlFactory<Entry,Character>() {

			@Override
			public IPropertyControl<Entry> createPropertyControl(Composite parent, final ScalarPropertyAccessor<Character,Entry> propertyAccessor) {
				// This property is not editable???
				final Label control = new Label(parent, SWT.NONE);
				
		    	return new IPropertyControl<Entry>() {
					@Override
					public Control getControl() {
						return control;
					}
					@Override
					public void load(Entry object) {
						control.setText(formatValueForTable(object, propertyAccessor));
					}
					@Override
					public void save() {
						// Not editable so nothing to do
					}
				};
			}

			@Override
			public String formatValueForMessage(Entry extendableObject, ScalarPropertyAccessor<? extends Character,Entry> propertyAccessor) {
				return "'" + propertyAccessor.getValue(extendableObject).toString() + "'";
			}

			@Override
			public Character getDefaultValue() {
				return ' ';
			}

			@Override
			public boolean isEditable() {
				return false;
			}
		};
		
		reconcilingStateAccessor = propertySet.addProperty("reconcilingState", "Reconciled", Character.class, 1, 30, stateControlFactory, null);
		addressAccessor = propertySet.addProperty("address", "Address", String.class, 3, 100, textControlFactory, null);
		
		return propertySet;
	}

	/**
	 * @return
	 */
	public static ExtensionPropertySet<QIFEntry,Entry> getPropertySet() {
		return propertySet;
	}

	/**
	 * @return
	 */
	public static ScalarPropertyAccessor<Character,Entry> getReconcilingStateAccessor() {
		return reconcilingStateAccessor;
	}	

	/**
	 * @return
	 */
	public static ScalarPropertyAccessor<String,Entry> getAddressAccessor() {
		return addressAccessor;
	}	
}
