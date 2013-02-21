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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import net.sf.jmoney.isolation.IObjectKey;
import net.sf.jmoney.isolation.ObjectCollection;

import org.eclipse.core.databinding.property.value.IValueProperty;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;

/**
 * A <code>PropertySet</code> contains information about a
 * property set.  A property set is a set of properties
 * that either:
 * <UL>
 * <LI>form the base set of properties in a data model object
 *     </LI>
 * <LI>or are the properties added to a data model object
 * by a derived class (such property sets are known as
 * derived property sets)
 *     </LI>
 * <LI>or are the properties added to a data model object by
 * a plug-in (such property sets are know as extension
 * property sets)
 *     </LI>
 * </UL>
 * The <code>getBasePropertySet</code> and <code>isExtension</code> methods
 * can be called to determine in which of the above three categories
 * a property set lies.
 *
 * @see <a href="propertySets.html">Property Set Documentation</a>
 * @see <a href="extendingDatamodel.html#propertySets">Property Set Documentation</a>
 * @param P the type of the implementation object, which must be
 * 		either an ExtendableObject or an ExtensionObject
 * @Param E the type of the extendable object, which will be the same
 * 		as P if this is an extendable object but will be different if
 * 		P is an extension object
 * @author Nigel Westbury
*/
public abstract class PropertySet<P,S extends ExtendableObject> {

	protected String propertySetId;

	protected Class<P> classOfObject;

	protected Vector<PropertyAccessor> properties = new Vector<PropertyAccessor>();

	/**
	 * These arrays are built on first use and then cached.
	 */
	private Vector<ScalarPropertyAccessor<?,S>> scalarProperties1 = null;
	private Vector<ListPropertyAccessor<?,S>> listProperties1 = null;

	boolean isExtension;

	static Vector<PropertySet> allPropertySets = new Vector<PropertySet>();
	static Set<String> allPropertySetIds = new HashSet<String>();

	/**
	 * Maps property set id to the property set
	 */
	protected static Map<String, ExtendablePropertySet<?>> allExtendablePropertySetsMap = new HashMap<String, ExtendablePropertySet<?>>();
	protected static Map<String, ExtensionPropertySet<?,?>> allExtensionPropertySetsMap = new HashMap<String, ExtensionPropertySet<?,?>>();

	/**
	 * Map extendable classes to property sets.
	 */
	protected static Map<Class<? extends ExtendableObject>, ExtendablePropertySet> classToPropertySetMap = new HashMap<Class<? extends ExtendableObject>, ExtendablePropertySet>();

	protected PropertySet() {
		// Add to our list of all property sets
		allPropertySets.add(this);
	}

	/**
	 * This method is called after all the properties in this property set have
	 * been set.  It completes the initialization of this object.
	 *
	 * This cannot be done in the constructor because there may be circular references
	 * between property sets, properties in those property sets, and property sets for
	 * the objects referenced by those properties.
	 *
	 * @param propertySetId
	 *
	 */
	public void initProperties(String propertySetId) {
		/*
		 * Check that the property set id is unique.
		 */
		if (allPropertySetIds.contains(propertySetId)) {
			throw new MalformedPluginException("More than one property set has an id of " + propertySetId); //$NON-NLS-1$
		}
		this.propertySetId = propertySetId;
		allPropertySetIds.add(propertySetId);
	}

	/**
	 * Loads the property sets.
	 * All property sets (both base and extensions) are added to the
	 * net.sf.jmoney.fields extension point.
	 */
	public static void init() {
		// Load the property set extensions.
		IExtensionRegistry registry = Platform.getExtensionRegistry();

		// TODO: They may be not much point in processing extendable classes before extension
		// classes.  Eclipse, I believe, will always iterate extension info from a plug-in
		// before extensions from plug-ins that depend on that plug-in, so we don't have the
		// problem of the extendable not being processed before the extension.
		// We do have other problems, however, which have required a second pass thru
		// the property sets.

		for (IConfigurationElement element: registry.getConfigurationElementsFor("net.sf.jmoney.fields")) { //$NON-NLS-1$
			if (element.getName().equals("extendable-property-set")) { //$NON-NLS-1$
				try {
					Object listener = element.createExecutableExtension("info-class"); //$NON-NLS-1$
					if (!(listener instanceof IPropertySetInfo)) {
						throw new MalformedPluginException(
								"Plug-in " + element.getContributor().getName() //$NON-NLS-1$
								+ " extends the net.sf.jmoney.fields extension point. " //$NON-NLS-1$
								+ "However, the class specified by the info-class attribute " //$NON-NLS-1$
								+ "(" + listener.getClass().getName() + ") " //$NON-NLS-1$ //$NON-NLS-2$
								+ "does not implement the IPropertySetInfo interface. " //$NON-NLS-1$
								+ "This interface must be implemented by all classes referenced " //$NON-NLS-1$
								+ "by the info-class attribute."); //$NON-NLS-1$
					}

					IPropertySetInfo pageListener = (IPropertySetInfo)listener;

					String fullPropertySetId = element.getNamespaceIdentifier();
					String id = element.getAttribute("id"); //$NON-NLS-1$
					if (id != null && id.length() != 0) {
						fullPropertySetId = fullPropertySetId + '.' + id;
					}

					String basePropertySetId = element.getAttribute("base-property-set"); //$NON-NLS-1$
					if (basePropertySetId != null && basePropertySetId.length() == 0) {
						basePropertySetId = null;
					}
					registerExtendablePropertySet(fullPropertySetId, basePropertySetId, pageListener);
				} catch (CoreException e) {
					if (e.getStatus().getException() instanceof ClassNotFoundException) {
						ClassNotFoundException e2 = (ClassNotFoundException)e.getStatus().getException();
						throw new MalformedPluginException(
								"Plug-in " + element.getContributor().getName() //$NON-NLS-1$
								+ " extends the net.sf.jmoney.fields extension point. " //$NON-NLS-1$
								+ "However, the class specified by the info-class attribute " //$NON-NLS-1$
								+ "(" + e2.getMessage() + ") " //$NON-NLS-1$ //$NON-NLS-2$
								+ "could not be found. " //$NON-NLS-1$
								+ "The info-class attribute must specify a class that implements the " //$NON-NLS-1$
								+ "IPropertySetInfo interface."); //$NON-NLS-1$
					}
					e.printStackTrace();
				}
			}
		}

		for (IConfigurationElement element: registry.getConfigurationElementsFor("net.sf.jmoney.fields")) { //$NON-NLS-1$
			if (element.getName().equals("extension-property-set")) { //$NON-NLS-1$
				try {
					Object listener = element.createExecutableExtension("info-class"); //$NON-NLS-1$
					if (listener instanceof IPropertySetInfo) {
						IPropertySetInfo pageListener = (IPropertySetInfo)listener;

						String fullPropertySetId = element.getNamespaceIdentifier();
						String id = element.getAttribute("id"); //$NON-NLS-1$
						if (id != null && id.length() != 0) {
							fullPropertySetId = fullPropertySetId + '.' + id;
						}

						String extendablePropertySetId = element.getAttribute("extendable-property-set"); //$NON-NLS-1$
						if (extendablePropertySetId != null) {
							registerExtensionPropertySet(fullPropertySetId, extendablePropertySetId, pageListener);
						} else {
							// TODO plug-in error
						}
					}
				} catch (CoreException e) {
					e.printStackTrace();
				}
			}
		}

		/*
		 * Check for property sets that have been created (because
		 * other property sets depended on them) but that have no entry
		 * in a plugin.xml file.
		 */
		for (PropertySet propertySet: PropertySet.allPropertySets) {
			if (propertySet.getId() == null) {
				throw new MalformedPluginException("The property set for " + propertySet.getImplementationClass().getName() + " has not been registered in the plugin.xml file."); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}

		/*
		 * After all property information has been registered, make a second
		 * pass through the extendable objects. In this pass we do processing of
		 * extendable property sets that requires the complete set of extension
		 * property sets to be available and complete.
		 */
		for (ExtendablePropertySet propertySet: PropertySet.getAllExtendablePropertySets()) {
			propertySet.initPropertiesPass2();
		}
	}

	/**
	 *
	 * @param propertySetId
	 * @param basePropertySetId If this property set is derived from
	 * 			another property set then the id of the base property set,
	 * 			otherwise null.
	 * @param propertySetInfo Null if property set data is found in
	 * 			the datastore but no plug-in defined a property set
	 * 			with this id.
	 * @param class1
	 * @return
	 */
	static private void registerExtendablePropertySet(final String propertySetId, final String basePropertySetId, IPropertySetInfo propertySetInfo) {

		// Set up the list of properties.
		// This is done by calling the registerProperties
		// method of the supplied interface.
		PropertySet propertySet = propertySetInfo.registerProperties();
		propertySet.initProperties(propertySetId);
	}

	/**
	 *
	 * @param propertySetId
	 * @param propertySetInfo Null if property set data is found in
	 * 			the datastore but no plug-in defined a property set
	 * 			with this id.
	 * @return
	 */
	static private void registerExtensionPropertySet(final String propertySetId, final String extendablePropertySetId, IPropertySetInfo propertySetInfo) {
		// Set up the list of properties.
		// This is done by calling the registerProperties
		// method of the supplied interface.
		PropertySet propertySet = propertySetInfo.registerProperties();
		propertySet.initProperties(propertySetId);
	}

	/**
	 * This method is called when a property set id in plugin.xml references
	 * an extendable property set.  The property set object is
	 * returned.
	 */
	static public ExtendablePropertySet getExtendablePropertySet(String propertySetId) throws PropertySetNotFoundException {
		ExtendablePropertySet propertySet = allExtendablePropertySetsMap.get(propertySetId);
		if (propertySet == null) {
			throw new PropertySetNotFoundException(propertySetId);
		}
		return propertySet;
	}

	/**
	 * This method is called when one plug-in wants to access a property
	 * in another plug-in's property set.  Callers must be able to handle
	 * the case where the requested property set is not found.  The plug-in
	 * must catch PropertySetNotFoundException and supply appropriate behavior
	 * (not an error from the user's perspective).
	 */
	static public ExtensionPropertySet getExtensionPropertySet(String propertySetId) throws PropertySetNotFoundException {
		ExtensionPropertySet propertySet = allExtensionPropertySetsMap.get(propertySetId);
		if (propertySet == null) {
			throw new PropertySetNotFoundException(propertySetId);
		}
		return propertySet;
	}


	/**
	 * This method will find the PropertySet object, given the class of an
	 * implementation object.  The given class must be an implementation of ExtendableObject
	 * (The class may not be an implementation of an extension
	 * property set).
	 * <P>
	 * This method should be called when we have an object, but we do not know
	 * exactly of what derived class the object is.  By calling this method,
	 * we can get the actual set of properties for this object.
	 * For example, if one wants to display the properties for
	 * a CapitalAccount object, then call this method to get the property
	 * set for the actual object and you will then see properties for
	 * this particular object (bank account properties if the object
	 * is a bank account, credit card account properties if the
	 * object is a credit card account and so on).
	 */
	@SuppressWarnings("unchecked")
	static public <E extends ExtendableObject> ExtendablePropertySet<E> getPropertySet(Class<E> propertySetClass) {
		return classToPropertySetMap.get(propertySetClass);
	}

	@Override
	public String toString() {
		return propertySetId;
	}

	/**
	 * @return The globally unique id of the property set.
	 */
	public String getId() {
		return propertySetId;
	}

	/**
	 * Returns the implementation class for this property set.
	 *
	 * The implementation class for a property set is a class that
	 * implements getters and setters for all the properties in
	 * the property set.  Implementation classes for property sets
	 * have a few other rules they must follow too.  For example,
	 * certain constructors must be provided and they must extend
	 * either ExtendableObject or ExtensionObject.
	 * See the documentation on property set implementation classes
	 * for further information.
	 *
	 * @see doc on implemetation classes
	 * @return the implementation class
	 */
	public Class<P> getImplementationClass() {
		return classOfObject;
	}

	/**
	 * Get the property accessor for a property in a
	 * property set.
	 *
	 * This method looks in only in the given property set
	 * (it will not look in base property sets or extension
	 * property sets).
	 *
	 * @param name The local name of the property.  This name does not
	 *          include the dotted prefix.
	 */
	public PropertyAccessor getProperty(String name) throws PropertyNotFoundException {
		for (PropertyAccessor propertyAccessor: properties) {
			if (propertyAccessor.getLocalName().equals(name)) {
				return propertyAccessor;
			}
		}
		throw new PropertyNotFoundException(propertySetId, name);
	}

	/**
	 * Gets a list of all extension property sets.
	 *
	 * @return the collection of all property sets
	 */
	static public Collection<ExtensionPropertySet<?,?>> getAllExtensionPropertySets() {
		return allExtensionPropertySetsMap.values();
	}

	/**
	 * Gets a list of all extendable property sets.
	 *
	 * @return the collection of all property sets
	 */
	static public Collection<ExtendablePropertySet<?>> getAllExtendablePropertySets() {
		return allExtendablePropertySetsMap.values();
	}

	/**
	 * @return An iterator that iterates over all properties
	 * 		in this property set, returning, for each property,
	 * 		the PropertyAccessor object for that property.
	 */
	public Collection<PropertyAccessor> getProperties1() {
		return properties;
	}

	public Collection<ScalarPropertyAccessor<?,S>> getScalarProperties1() {
		if (scalarProperties1 == null) {
			scalarProperties1 = new Vector<ScalarPropertyAccessor<?,S>>();
			for (PropertyAccessor propertyAccessor: properties) {
				if (propertyAccessor instanceof ScalarPropertyAccessor) {
					scalarProperties1.add((ScalarPropertyAccessor<?,S>)propertyAccessor);
				}
			}
		}

		return scalarProperties1;
	}

	public Collection<ListPropertyAccessor<?,S>> getListProperties1() {
		if (listProperties1 == null) {
			listProperties1 = new Vector<ListPropertyAccessor<?,S>>();
			for (PropertyAccessor propertyAccessor: properties) {
				if (propertyAccessor instanceof ListPropertyAccessor) {
					listProperties1.add((ListPropertyAccessor<?,S>)propertyAccessor);
				}
			}
		}

		return listProperties1;
	}

	/**
	 * @return
	 */
	public boolean isExtension() {
		return isExtension;
	}

	public static <E2 extends ExtendableObject> ExtendablePropertySet<E2> addBaseAbstractPropertySet(Class<E2> classOfImplementationObject, String description) {
		return new ExtendablePropertySet<E2>(classOfImplementationObject, description, null, null);
	}

	public static <E2 extends ExtendableObject> ExtendablePropertySet<E2> addBaseFinalPropertySet(Class<E2> classOfImplementationObject, String description, IExtendableObjectConstructors<E2> constructors) {
		return new ExtendablePropertySet<E2>(classOfImplementationObject, description, null, constructors);
	}

	public static <E extends ExtendableObject> ExtendablePropertySet<E> addDerivedAbstractPropertySet(Class<E> classOfImplementationObject, String description, ExtendablePropertySet<? super E> basePropertySet) {
		return new ExtendablePropertySet<E>(classOfImplementationObject, description, basePropertySet, null);
	}

	public static <E extends ExtendableObject> ExtendablePropertySet<E> addDerivedFinalPropertySet(Class<E> classOfImplementationObject, String description, ExtendablePropertySet<? super E> basePropertySet, IExtendableObjectConstructors<E> constructors) {
		return new ExtendablePropertySet<E>(classOfImplementationObject, description, basePropertySet, constructors);
	}

	public static <X extends ExtensionObject, E extends ExtendableObject> ExtensionPropertySet<X,E> addExtensionPropertySet(Class<X> classOfImplementationObject, ExtendablePropertySet<E> extendablePropertySet, IExtensionObjectConstructors<X,E> constructors) {
		return new ExtensionPropertySet<X,E>(classOfImplementationObject, extendablePropertySet, constructors);
	}

	public <V> ScalarPropertyAccessor<V,S> addProperty(String name, String displayName, Class<V> classOfValue, int weight, int minimumWidth, IPropertyControlFactory<V> propertyControlFactory, IPropertyDependency<S> propertyDependency) {
		if (propertyControlFactory == null) {
			throw new MalformedPluginException(
					"No IPropertyControlFactory object has been specified for property " + name //$NON-NLS-1$
					+ ".  This is needed even if the property is not editable.  (Though the method that gets the" + //$NON-NLS-1$
			" control may return null if the property is not editable)."); //$NON-NLS-1$
		}

		ScalarPropertyAccessor<V,S> accessor = new ScalarPropertyAccessor<V,S>(classOfValue, this, name, displayName, weight, minimumWidth, propertyControlFactory, propertyDependency);
		properties.add(accessor);
		return accessor;
	}

	public <V extends ExtendableObject> ReferencePropertyAccessor<V,S> addProperty(String name, String displayName, Class<V> classOfValue, int weight, int minimumWidth, final IReferenceControlFactory<P,V> propertyControlFactory, IPropertyDependency<S> propertyDependency) {
		if (propertyControlFactory == null) {
			throw new MalformedPluginException(
					"No IPropertyControlFactory object has been specified for property " + name //$NON-NLS-1$
					+ ".  This is needed even if the property is not editable.  (Though the method that gets the" + //$NON-NLS-1$
			" control may return null if the property is not editable)."); //$NON-NLS-1$
		}

		ReferencePropertyAccessor<V,S> accessor = new ReferencePropertyAccessor<V,S>(classOfValue, this, name, displayName, weight, minimumWidth, propertyControlFactory, propertyDependency) {
			@Override
			public IObjectKey invokeObjectKeyField(S parentObject) {
				return propertyControlFactory.getObjectKey(getImplementationObject(parentObject));
			}
		};

		properties.add(accessor);
		return accessor;
	}

	public <E2 extends ExtendableObject> ListPropertyAccessor<E2,S> addPropertyList(String name, String displayName, ExtendablePropertySet<E2> elementPropertySet, final IListGetter<P, E2> listGetter) {
		ListPropertyAccessor<E2,S> accessor = new ListPropertyAccessor<E2,S>(this, name, displayName, elementPropertySet) {
			@Override
			public ObjectCollection<E2> getElements(S parentObject) {
				return listGetter.getList(getImplementationObject(parentObject));
			}
		};

		properties.add(accessor);
		return accessor;
	}

	/**
	 * Given an extendable object, return the Java object that contains the
	 * getters and setters for this property set.
	 *
	 * If this property set is an extendable property set then this method
	 * should return the passed object as is. If this property set is an
	 * extension property set then this method should get the appropriate
	 * extension object.
	 *
	 * @param extendableObject
	 * @return
	 */
	protected abstract P getImplementationObject(S extendableObject);

	/**
	 * Given the Bean name of a property in this property set, return an
	 * IValueProperty implementation that uses as the source the extendable
	 * object (not the extension object).
	 *
	 * @param localName
	 * @return
	 */
	public abstract IValueProperty createValueProperty(ScalarPropertyAccessor accessor);
}

