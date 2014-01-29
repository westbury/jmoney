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

package net.sf.jmoney.model2;

import java.util.Comparator;

import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * All properties that can be edited by the user must, of course, have
 * controls that can be used for such purpose.  So that the framework
 * can create a control for a property, the property metadata contains
 * a control factory.  The control factories must implement this
 * interface.
 *
 * @param V the type of the value that can be edited by the controls
 * 			produced by this factory
 */
public interface IPropertyControlFactory<S extends ExtendableObject, V> {
	/**
	 * Create a control that edits the property.
	 * <P>
	 * The PropertyAccessor object is not known when the factory
	 * is created so we require that it is passed as a parameter
	 * when a control is created.

	 * @return An interface to the class that wraps the
	 * 			control.
	 */
//	IPropertyControl<S> createPropertyControl(Composite parent, ScalarPropertyAccessor<V,S> propertyAccessor);

	/**
	 * Create a control that edits the property.
	 * <P>
	 * The PropertyAccessor object is not known when the factory
	 * is created so we require that it is passed as a parameter
	 * when a control is created.

	 * @return An interface to the class that wraps the
	 * 			control.
	 */
//	Control createPropertyControl(Composite parent, IObservableValue<V> modelObservable);

	Control createPropertyControl(Composite parent,
			ScalarPropertyAccessor<V, S> propertyAccessor,
			S modelObject);

	Control createPropertyControl(Composite parent,
			ScalarPropertyAccessor<V, S> propertyAccessor,
			IObservableValue<? extends S> modelObservable);

	/**
	 * Format the value of a property so it can be embedded into a
	 * message.
	 *
	 * The returned value must look sensible when embedded in a message.
	 * Therefore null values and empty values must return non-empty
	 * text such as "none" or "empty".  Text values should be placed in
	 * quotes unless sure that only a single word will be returned that
	 * would be readable without quotes.
	 *
	 * @return The value of the property formatted as appropriate.
	 */
	String formatValueForMessage(S extendableObject, ScalarPropertyAccessor<? extends V,S> propertyAccessor);

	/**
	 * Format the value of a property as appropriate for displaying in a
	 * table.
	 *
	 * The returned value will be displayed in a table or some similar
	 * view.  Null and empty values should be returned as empty strings.
	 * Text values should not be quoted.
	 *
	 * @return The value of the property formatted as appropriate.
	 */
	String formatValueForTable(S extendableObject, ScalarPropertyAccessor<? extends V,S> propertyAccessor);

	/**
	 * Indicates if the property is editable.  If the property
	 * is editable then the <code>createPropertyControl</code>
	 * method must create and return a valid property.  If the
	 * property is not editable then the <code>createPropertyControl</code>
	 * method will never be called by the framework.
	 * <P>
	 * Most properties will be editable.  However some properties,
	 * such as the creation date for each entry, cannot be edited
	 * by the user.  The rest of this interface must still be implemented
	 * so that the values can be formatted correctly for displaying
	 * to the user.
	 *
	 * @return true if a control is provided to allow the user to
	 * 			edit the property, false if the user cannot edit
	 * 			the property
	 */
	// TODO: Determine if this method should be removed.  If a property
	// is not to be edited then a non-editable control, such as a Label,
	// can be created as the editing control.
	boolean isEditable();

	/**
	 * The default value for a property is suitable for uses such
	 * as:
	 *
	 * - setting the default columnn value in a database
	 * - providing values when the value is missing from an
	 * 		XML file
	 *
	 * It is expected that this value is constant (the same value
	 * is always returned for a given property).  The results will
	 * be unpredicable if this is not the case.
	 *
	 * @return the default value to use for this property, which may
	 * 		be null if the property is of a nullable type
	 */
	V getDefaultValue();

	/**
	 * Many views allow sorting based on property values.  This method
	 * allows the comparator to be used for sorting to be specified.
	 *
	 * @return a comparator if sorting is to be allowed, or null if sorting
	 * 		based on this property is not to be allowed
	 */
	Comparator<V> getComparator();
}
