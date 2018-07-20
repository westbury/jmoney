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

import net.sf.jmoney.isolation.IValues;

import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.core.databinding.observable.Realm;
import org.eclipse.core.databinding.observable.value.AbstractObservableValue;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.property.value.IValueProperty;
import org.eclipse.core.databinding.property.value.ValueProperty;


public class ExtensionPropertySet<X extends ExtensionObject,E extends ExtendableObject> extends PropertySet<X,E> {

	private ExtendablePropertySet<E> extendablePropertySet;

	/**
	 * An interface that can be used to construct implementation objects,
	 * or null if this is an abstract property set.
	 */
	IExtensionObjectConstructors<X,E> constructors;

	IValueProperty<E,X> extensionGivenExtendableObject;

	/**
	 * Constructs an extension property set object.
	 *
	 * @param classOfObject
	 * @param constructors
	 *            a required interface containing methods for constructing
	 *            implementation objects
	 */
	protected ExtensionPropertySet(Class<X> classOfObject, ExtendablePropertySet<E> extendablePropertySet, IExtensionObjectConstructors<X,E> constructors) {
		this.isExtension = true;
		this.classOfObject = classOfObject;
		this.extendablePropertySet = extendablePropertySet;
		this.constructors = constructors;

		// TODO: move outside this constructor.
		if (extendablePropertySet == null) {
			throw new MalformedPluginException("A non-null extendable property set must be passed."); //$NON-NLS-1$
		}

		extensionGivenExtendableObject = new ValueProperty<E,X>() {
			@Override
			public Class<X> getValueType() {
				return ExtensionPropertySet.this.classOfObject;
			}

			@Override
			public IObservableValue<X> observe(Realm realm, final E source) {
				return new AbstractObservableValue<X>() {

					@Override
					public Object getValueType() {
						return ExtensionPropertySet.this.classOfObject;
					}

					@Override
					protected X doGetValue() {
						return source.getExtension(ExtensionPropertySet.this, false);
					}
				};
			}
		};
	}

	@Override
	public void initProperties(String propertySetId) {
		super.initProperties(propertySetId);

		// Add to our map that maps ids to ExtensionPropertySet objects.
		allExtensionPropertySetsMap.put(propertySetId, this);

		// Add to our map that maps ids to ExtensionPropertySet objects
		// within a particular extendable object.
		extendablePropertySet.extensionPropertySets.put(propertySetId, this);
/*
		// Build the list of properties that are passed to
		// the 'new object' constructor and another list that
		// are passed to the 're-instantiating' constructor.

		constructorProperties = new Vector<PropertyAccessor>();
		defaultConstructorProperties = new Vector<PropertyAccessor>();

		// This property set is an extension.
		int parameterIndex = 0;
		for (PropertyAccessor propertyAccessor: properties) {
			constructorProperties.add(propertyAccessor);
			propertyAccessor.setIndexIntoConstructorParameters(parameterIndex++);
			if (propertyAccessor.isList()) {
				defaultConstructorProperties.add(propertyAccessor);
			}
		}
*/
	}

	public ExtendablePropertySet getExtendablePropertySet() {
		return extendablePropertySet;
	}

	/**
	 * This method should be used only by plug-ins that implement
	 * a datastore.
	 * @param constructorParameters an array of values to be passed to
	 * 		the constructor.  If an extendable object is being constructed
	 * 		then the first three elements of this array must be the
	 * 		object key, the extension map, and the parent object key.
	 *
	 * @return A newly constructed object, constructed from the given
	 * 		parameters.  This object may be an ExtendableObject or
	 * 		may be an ExtensionObject.
	 */
	public X constructImplementationObject(E extendableObject, IValues<E> values) {
		X extensionObject = constructors.construct(extendableObject, values);
		extensionObject.setPropertySet(this);
		return extensionObject;
	}

	/**
	 * This method should be used only by plug-ins that implement
	 * a datastore.
	 *
	 * @return A newly constructed object, constructed from the given
	 * 		parameters.  This object may be an ExtendableObject or
	 * 		may be an ExtensionObject.
	 */
	public X constructDefaultImplementationObject(E extendableObject) {
		X extensionObject = constructors.construct(extendableObject);
		extensionObject.setPropertySet(this);
		return extensionObject;
	}

	@Override
	protected X getImplementationObject(ExtendableObject extendableObject) {
		return extendableObject.getExtension(this, true);
	}

	@Override
	public <V> IValueProperty<E,V> createValueProperty(final ScalarPropertyAccessor<V,E> accessor) {
		/*
		 * This is a little more complex.  We need to create an IValueProperty
		 * implementation that takes as the source object the ExtendableObject
		 * but the property is actually in an extension object.
		 *
		 * By returning such an IValueProperty implementation, consumers
		 * can use databinding without needing to know whether the property is
		 * in the base object or an extension object.
		 */

		final IValueProperty<X,V> beanValueProperty = BeanProperties.value(classOfObject, accessor.localName, accessor.getClassOfValueObject());

		return new ValueProperty<E,V>() {

			@Override
			public Object getValueType() {
				/*
				 * Don't use accessor.getValueType.  That would recursively
				 * come back to this method! 
				 */
				return accessor.getClassOfValueObject();
			}

			@Override
			public V doGetValue(E source) {
				/*
				 * This implementation is a little more efficient than going through
				 * the observable.  So we overwrite the default implementation with
				 * this more efficient way of getting the value.
				 */

				X extension = source.getExtension(ExtensionPropertySet.this, false);
				Class<?> implementationClass = extendablePropertySet.getImplementationClass();

				/*
				 * If there is no extension then we return the default value.
				 */
				if (extension == null) {
					return accessor.getDefaultValue();
				}

				if (!implementationClass.isAssignableFrom(source.getClass())) {
					// TODO: We should be able to validate this at compile time using generics.
					// This would involve adding the implementation class of the containing
					// property set as a type parameter to all property accessors.
					throw new RuntimeException("Property " + accessor.getName() //$NON-NLS-1$
							+ " is implemented by " + implementationClass.getName() //$NON-NLS-1$
							+ " but is being called on an object of type " //$NON-NLS-1$
							+ getClass().getName());
				}

				return beanValueProperty.getValue(extension);
			}

			@Override
			protected void doSetValue(E source, V value) {
				/*
				 * This implementation is a little more efficient than going through
				 * the observable.  So we overwrite the default implementation with
				 * this more efficient way of setting the value.
				 */

				// Get the extension, creating one if necessary.
				X extension = source.getExtension(ExtensionPropertySet.this, true);
				beanValueProperty.setValue(extension, value);
			}

			@Override
			public IObservableValue<V> observe(Realm realm, E source) {
				/*
				 * We use a bit of a trick here.  If the extension does not
				 * exist then we create it at this time.  This allows us to
				 * simply observe the property in the extension.  This trick
				 * works because once an extension has been created the extension
				 * is never removed or replaced (only fields within the extension
				 * may change).
				 *
				 * We cannot be significantly adding to a scalability problem
				 * here because there is a fair amount of overhead in creating an
				 * observable and listening to it.
				 */
				X extension = source.getExtension(ExtensionPropertySet.this, true);
				return beanValueProperty.observe(realm, extension);
			}
		};
	}
}
