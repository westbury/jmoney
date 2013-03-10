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

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import net.sf.jmoney.JMoneyPlugin;
import net.sf.jmoney.isolation.IModelObject;
import net.sf.jmoney.isolation.IObjectKey;
import net.sf.jmoney.isolation.IReferencePropertyAccessor;
import net.sf.jmoney.isolation.ISessionChangeFirer;
import net.sf.jmoney.isolation.IValues;
import net.sf.jmoney.isolation.ListKey;
import net.sf.jmoney.isolation.SessionChangeListener;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.ui.views.properties.IPropertySource;

/**
 * This is the base class for all objects that may have extension
 * property sets added by plug-ins.  The framework supports the
 * following objects that may be extended:
 * <UL>
 * <LI>Session</LI>
 * <LI>Commodity</LI>
 * <LI>Account</LI>
 * <LI>Transaction</LI>
 * <LI>Entry</LI>
 * </UL>
 * <P>
 * Plug-ins are also able to create new classes of extendable
 * objects by deriving classes from this class.
 * <P>
 * This class contains abstract methods for which an implementation
 * must be provided.
 *
 * @author  Nigel Westbury
 */
public abstract class ExtendableObject implements IModelObject, IAdaptable {

	/**
	 * The key from which this object can be fetched from
	 * the datastore and a reference to this object obtained.
	 */
	IObjectKey objectKey;

	/**
	 * Extendable objects may have extensions containing additional data needed
	 * by the plug-ins. Plug-ins add properties to an object class by creating a
	 * property set and then adding that property set to the object class. This
	 * map will map property sets to the appropriate extension object.
	 */
	protected Map<ExtensionPropertySet<?,?>, ExtensionObject> extensions = new HashMap<ExtensionPropertySet<?,?>, ExtensionObject>();

	/**
	 * The key which contains this object's parent and also the list property
	 * which contains this object.
	 */
	protected ListKey parentKey;

	protected abstract String getExtendablePropertySetId();

	/**
	 * Constructs a new object with property values obtained from
	 * the given IValues interface.
	 *
	 * Derived classes will set their own properties from this interface,
	 * but this method is responsible for ensuring the appropriate extensions
	 * are created and passes on the IValues interface to the extension constructors.
	 */
	protected <E extends ExtendableObject> ExtendableObject(IObjectKey objectKey, ListKey parentKey, IValues<E> extensionValues) {
		this.objectKey = objectKey;
		this.parentKey = parentKey;

		/*
		 * In order to find out which extensions have non-default values, we have to read the property
		 * values from the rowset and compare against the default values given by the accessor.
		 * Note that the values may be null so we use the utility method to do the comparison.
		 */
		Collection<ExtensionPropertySet<?,?>> nonDefaultExtensions = new Vector<ExtensionPropertySet<?,?>>();
		Collection<ExtensionPropertySet<?,?>> extensionPropertySets = getPropertySet().getExtensionPropertySets();
		outerLoop: for (ExtensionPropertySet<?,?> extensionPropertySet: extensionPropertySets) {
			for (ScalarPropertyAccessor accessor: extensionPropertySet.getScalarProperties1()) {
				/*
				 * If the property is a reference then the default value is
				 * always null, otherwise compare against the default.
				 */
 				if (accessor instanceof IReferencePropertyAccessor) {
 					if (extensionValues.getReferencedObjectKey((IReferencePropertyAccessor)accessor) != null) {
						nonDefaultExtensions.add(extensionPropertySet);
						continue outerLoop;
					}
 				} else {
 					if (!JMoneyPlugin.areEqual(extensionValues.getScalarValue(accessor), accessor.getDefaultValue())) {
 						nonDefaultExtensions.add(extensionPropertySet);
 						continue outerLoop;
 					}
 				}
			}
			for (@SuppressWarnings("unused") ListPropertyAccessor accessor: extensionPropertySet.getListProperties1()) {
				// For time being, always create an extension if there is a list property in it.
				// If we test to see if the list is empty then we may be causing a query to the database,
				// and it is clearly a lot cheaper just to create the extension.
				nonDefaultExtensions.add(extensionPropertySet);
				continue outerLoop;
			}
		}

		for (ExtensionPropertySet<?,?> propertySet2: nonDefaultExtensions) {
			ExtensionPropertySet<?,E> propertySet = (ExtensionPropertySet<?,E>)propertySet2;
			ExtensionObject extensionObject = propertySet.constructImplementationObject((E)this, extensionValues);
			extensions.put(propertySet, extensionObject);
		}
	}

	/**
	 * Constructs a new object with default property values.
	 */
	protected ExtendableObject(IObjectKey objectKey, ListKey parentKey) {
		this.objectKey = objectKey;
		this.parentKey = parentKey;
	}

	/**
	 * @return The key that fetches this object.
	 */
	@Override
	public IObjectKey getObjectKey() {
		return objectKey;
	}

	/**
	 * @return
	 */
	// TODO: do we need this as well as the method below?
	public IObjectKey getParentKey() {
		return parentKey == null ? null : parentKey.getParentKey();
	}

	@Override
	public ListKey getParentListKey() {
		return parentKey;
	}

	/**
	 * @return The session containing this object
	 */
	public Session getSession() {
		// The data manager contains the session and so there is no reason
		// for the extendable objects to also contain a session field.
		// Get the session from the data manager.
		return getDataManager().getSession();
	}

	/**
	 * @return The data manager containing this object
	 */
	@Override
	public IDataManagerForAccounts getDataManager() {
		// The key must contain the data manager and so there is no reason
		// for the extendable objects to also contain a data manager field.
		// Get the data manager from the key.
		return (IDataManagerForAccounts)objectKey.getDataManager();
	}

	/**
	 * Two or more instantiated objects may represent the same object
	 * in the datastore.  Such objects should be considered
	 * the same.  Therefore this method overrides the default
	 * implementation that is based on Java identity.
	 * <P>
	 * This method also considers two objects to be the same if the
	 * other object is an extension object to an object that is
	 * the same object.
	 * <P>
	 * @return true if the two objects represent the same object
	 * 		in the datastore, false otherwise.
	 */
	// If we had an interface with the getObjectKey() method that
	// both ExtendableObject and ExtensionObject implemented, then
	// this method would be simpler.
    @Override
	public boolean equals(Object object) {
		// Two objects represent the same object if and only if
		// the keys from which they were created are the same.
		// Therefore we compare the key objects to see if they
		// both contain the same data.
		if (object instanceof ExtendableObject) {
			ExtendableObject extendableObject = (ExtendableObject)object;
			return getObjectKey().equals(extendableObject.getObjectKey());
		} else if (object instanceof ExtensionObject) {
			ExtensionObject extensionObject = (ExtensionObject)object;
			return getObjectKey().equals(extensionObject.getObjectKey());
		} else {
			return false;
		}
	}

	/**
	 * Required to support hash maps.
	 *
	 * If the datastore plug-in keeps the entire datastore in
	 * memory then the default hashCode implementation in the
	 * object key will work fine.  However, if the datastore is
	 * backed by a database then multiple instances of the same
	 * object key may exist in memory.  In such a case, a hashCode
	 * implementation must be provided for the object keys that
	 * return the same hash code for each instance of the object key.
	 */
    @Override
	public int hashCode() {
		return getObjectKey().hashCode();
	}

	// Should allow default package access and protected access
	// but not public access.  Unfortunately this cannot be done
	// so for time being allow public access.
	public <V> void processPropertyChange(final ScalarPropertyAccessor<V,?> propertyAccessor, final V oldValue, final V newValue) {
		/*
		 * If the value is an extendable object then we check that both this object and this object
		 * and the value are from the same data manager.  Mixing objects from different data
		 * managers is not allowed.
		 */
		if (newValue instanceof ExtendableObject
				&& ((ExtendableObject)newValue).getDataManager() != getDataManager()) {
			throw new RuntimeException("The object being set as the value of a property and the parent object are being managed by different data managers.  Objects cannot contain references to objects from other data managers."); //$NON-NLS-1$
		}

		if (oldValue == newValue ||
				(oldValue != null && oldValue.equals(newValue)))
					return;

		// Update the database.
		ExtendablePropertySet<?> actualPropertySet = PropertySet.getPropertySet(this.getClass());

		processPropertyChangeTyped(propertyAccessor, oldValue, newValue, actualPropertySet);
	}

	private <S extends ExtendableObject, V> void processPropertyChangeTyped(final ScalarPropertyAccessor<V,?> propertyAccessorUntyped, final V oldValue, final V newValue, ExtendablePropertySet<S> actualPropertySet) {
		final ScalarPropertyAccessor<V,? super S> propertyAccessor = (ScalarPropertyAccessor<V,? super S>) propertyAccessorUntyped;

		// Build two arrays of old and new values.
		// Ultimately we will have a layer between that does this
		// for us, also combining multiple updates to the same row
		// into a single update.  Until then, we need this code here.

		// TODO: improve performance here.
		// TODO: Do we really need this, or, now that transactional
		// processing is supported, is it unnecessary to support the
		// passing of multiple values???
		int count = actualPropertySet.getScalarProperties3().size();
		Object [] oldValues = new Object[count];
		Object [] newValues = new Object[count];

		int i = 0;
		for (ScalarPropertyAccessor<?,? super S> propertyAccessor2: actualPropertySet.getScalarProperties3()) {
			if (propertyAccessor2 == propertyAccessor) {
				oldValues[i] = oldValue;
				newValues[i] = newValue;
			} else {
				Object value = propertyAccessor2.getValue((S)this);
				oldValues[i] = value;
				newValues[i] = value;
			}
			i++;
		}
		objectKey.updateProperties(actualPropertySet, oldValues, newValues);

		// Notify the change manager.
		getDataManager().getChangeManager().processPropertyUpdate((S)this, propertyAccessor, oldValue, newValue);

		/*
		 * Fire an event for this change.
		 *
		 * This method is called only when a property change is initially made.
		 * This method is not called when a transaction is being committed and
		 * thus the change is being applied to a base transaction. Therefore we
		 * also call performFinish.
		 */
		// TODO above comment is not true.  This is called when a nested data manager
		// is committing property changes to us.
		getDataManager().fireEvent(
            	new ISessionChangeFirer() {
            		@Override
					public void fire(SessionChangeListener listener) {
            			listener.objectChanged(ExtendableObject.this, propertyAccessor, oldValue, newValue);
            			listener.performRefresh();
            		}
           		});
	}

	/**
	 * Get the extension that implements the properties needed by
	 * a given plug-in.
	 *
	 * @param alwaysReturnNonNull
	 *            If true then the return value is guaranteed to be non-null. If false
	 *            then the return value may be null, indicating that all properties in
	 *            the extension have default values.
	 */
	public <X extends ExtensionObject, E extends ExtendableObject> X getExtension(ExtensionPropertySet<X,E> propertySet, boolean alwaysReturnNonNull) {
		X extension = propertySet.classOfObject.cast(extensions.get(propertySet));

		if (extension == null && alwaysReturnNonNull) {
				extension = propertySet.constructDefaultImplementationObject((E)this);
				extensions.put(propertySet, extension);
		}

		return extension;
	}

	/**
	 * Return a list of extension that exist for this object.
	 * This is the list of extensions that have actually been
	 * created for this object, not the list of valid extensions
	 * for this object type.  If no property values have yet been set
	 * in an extension that the extension will not have been created
	 * and will thus not be returned by this method.
	 * <P>
	 * It is more efficient to use this method than to loop through
	 * all the possible extension property sets and see which ones exist
	 * in this object.
	 *
	 * @return an Iterator that returns elements of type
	 * 		<code>Map.Entry</code>.  Each Map.Entry contains a
	 * 		key of type PropertySet and a value of
	 * 		ExtensionObject.
	 */
	public Collection<ExtensionPropertySet<?,?>> getExtensions() {
		return extensions.keySet();
	}

	/**
	 * This method is called when loading data from a datastore.
	 * Therefore the method can assume that there is no prior extension
	 * in this object for the given property set id.  The results are
	 * undetermined if the extension already exists.
	 */
/*
remove this...
	protected void importExtensionString(String propertySetId, String extensionString) {
		// This is a bit of a kludge.  We need to put the object
		// into editable mode.  This ensures that a request for an
		// extension will always return a non-null extension.
		// This is necessary when setting properties here, and also
		// necessary that the code that propagates property changes
		// through the propagators get non-null extensions.
		alwaysReturnNonNullExtensions = true;

		PropertySet propertySet = PropertySet.getPropertySetCreatingIfNecessary(propertySetId, getExtendablePropertySetId());

		if (!propertySet.isExtensionClassKnown()) {
			// The plug-in that originally implemented this extension
			// is not installed.  We therefore do not know the class
			// that contains the properties.  We must not lose the
			// data in case the plug-in is installed later.
			// We therefore store the data in the map as a String.
			// If the plug-in is ever installed then the string can be
			// de-serialized to produce the correct extension object.
			extensions.put(propertySet, extensionString);
		} else {
			// Because the 'alwaysReturnNonNullExtensions' flag is set,
			// this method will always return  non-null extension.
			ExtensionObject extension = getExtension(propertySet);

			stringToExtension(extensionString, extension);
		}

		alwaysReturnNonNullExtensions = false;
	}


	protected static String extensionToString(ExtendableObject extension) {
		BeanInfo beanInfo;
		try {
			beanInfo = Introspector.getBeanInfo(extension.getClass());
		} catch (IntrospectionException e) {
			throw new MalformedPluginException("Property set extension caused introspection error");
		}

		StringBuffer buffer = new StringBuffer();
		PropertyDescriptor pd[] = beanInfo.getPropertyDescriptors();
		for (int j = 0; j < pd.length; j++) {
			String name = pd[j].getName();
			// Must have read and write method to be serialized.
			Method readMethod = pd[j].getReadMethod();
			Method writeMethod = pd[j].getWriteMethod();
			// TODO figure out a better way of finding our properties
			// than the following.
			if (readMethod != null
					&& writeMethod != null
					&& readMethod.getDeclaringClass() != ExtendableObject.class
					&& writeMethod.getDeclaringClass() != ExtendableObject.class
					&& readMethod.getDeclaringClass() != AccountExtension.class
					&& writeMethod.getDeclaringClass() != AccountExtension.class
					&& readMethod.getDeclaringClass() != EntryExtension.class
					&& writeMethod.getDeclaringClass() != EntryExtension.class) {
				Object value;
				try {
					value = readMethod.invoke(extension, (Object [])null);
				} catch (IllegalAccessException e) {
					throw new MalformedPluginException("Property set extension caused introspection error");
					//                      throw new MalformedPluginException("Method 'getEntryExtensionClass' in '" + pluginBean.getClass().getName() + "' must be public.");
				} catch (InvocationTargetException e) {
					// Plugin error
					throw new RuntimeException("bad error");
				}
				buffer.append('<');
				buffer.append(name);
				buffer.append('>');
				buffer.append(value);
				buffer.append("</");
				buffer.append(name);
				buffer.append('>');
			}
		}
		return buffer.toString();
	}

	protected static void stringToExtension(String s, ExtensionObject extension) {
		ByteArrayInputStream bin = new ByteArrayInputStream(s.getBytes());


		SAXParserFactory factory = SAXParserFactory.newInstance();
		try {
			SAXParser saxParser = factory.newSAXParser();
			HandlerForExtensions handler = new HandlerForExtensions(extension);
			saxParser.parse(bin, handler);
		}
		catch (ParserConfigurationException e) {
			throw new RuntimeException("Serious XML parser configuration error");
		}
		catch (SAXException se) {
			throw new RuntimeException("SAX exception error");
		}
		catch (IOException ioe) {
			throw new RuntimeException("IO internal exception error");
		}

		try {
			bin.close();
		}
		catch (IOException e) {
			throw new RuntimeException("internal error");
		}
	}

	private static class HandlerForExtensions extends DefaultHandler {

		ExtensionObject extension;

		BeanInfo beanInfo;

		Method writeMethod = null;

		HandlerForExtensions(ExtensionObject extension) {
			this.extension = extension;

			try {
				beanInfo = Introspector.getBeanInfo(extension.getClass());
			} catch (IntrospectionException e) {
				throw new MalformedPluginException("Property set extension caused introspection error");
			}
		}

		/**
		 * Receive notification of the start of an element.
		 *
		 * <p>See if there is a setter for this element name.  If there is
		 * then set the setter.  Otherwise set the setter to null to indicate
		 * that any character data should be ignored.
		 * </p>
		 * @param name The element type name.
		 * @param attributes The specified or defaulted attributes.
		 * @exception org.xml.sax.SAXException Any SAX exception, possibly
		 *            wrapping another exception.
		 * @see org.xml.sax.ContentHandler#startElement
		 * /
		public void startElement(String uri, String localName,
				String qName, Attributes attributes)
		throws SAXException {
			String propertyName = qName;

			PropertyDescriptor pd[] = beanInfo.getPropertyDescriptors();
			for (int j = 0; j < pd.length; j++) {
				String name = pd[j].getName();
				if (name.equals(propertyName)) {
					// Must have write method in the extension class.
					Method writeMethod = pd[j].getWriteMethod();
					// TODO: clean up
					if (writeMethod != null
							&& writeMethod.getDeclaringClass() != ExtendableObject.class
							&& writeMethod.getDeclaringClass() != AccountExtension.class
							&& writeMethod.getDeclaringClass() != EntryExtension.class) {
						this.writeMethod = writeMethod;
					}
					break;
				}
			}
		}


		/**
		 * Receive notification of the end of an element.
		 *
		 * <p>Set the setter back to null.
		 * </p>
		 * @param name The element type name.
		 * @param attributes The specified or defaulted attributes.
		 * @exception org.xml.sax.SAXException Any SAX exception, possibly
		 *            wrapping another exception.
		 * @see org.xml.sax.ContentHandler#endElement
		 * /
		public void endElement(String uri, String localName, String qName)
		throws SAXException {
			writeMethod = null;
		}


		/**
		 * Receive notification of character data inside an element.
		 *
		 * <p>If a setter method is set then the character data is passed
		 * to the setter.  Otherwise the character data is dropped.
		 * </p>
		 * @param ch The characters.
		 * @param start The start position in the character array.
		 * @param length The number of characters to use from the
		 *               character array.
		 * @exception org.xml.sax.SAXException Any SAX exception, possibly
		 *            wrapping another exception.
		 * @see org.xml.sax.ContentHandler#characters
		 * /
		public void characters(char ch[], int start, int length)
		throws SAXException {
			if (writeMethod != null) {
				Class type = writeMethod.getParameterTypes()[0];
				Object value = null;

				// TODO: change this.  Find a constructor from string.
				if (type.equals(int.class)) {
					String s = new String(ch, start, length);
					value = new Integer(s);
				} else if (type.equals(String.class)) {
					value = new String(ch, start, length);
				} else if (type.equals(char.class)) {
					value = new Character(ch[start]);
				} else {
					throw new RuntimeException("unsupported type");
				}

				try {
					writeMethod.invoke(extension, new Object[] { value });
				} catch (IllegalAccessException e) {
					throw new MalformedPluginException("Property set extension caused introspection error");
					//                      throw new MalformedPluginException("Method 'getEntryExtensionClass' in '" + pluginBean.getClass().getName() + "' must be public.");
				} catch (InvocationTargetException e) {
					// Plugin error
					throw new RuntimeException("bad error");
				}
			}
		}
	}
*/
	/**
	 * This method is used to enable other classes in the package to
	 * access protected fields in the extendable objects.
	 *
	 * @param theObjectKeyField
	 * @return
	 */
	Object getProtectedFieldValue(Field theObjectKeyField) {
    	try {
    		return theObjectKeyField.get(this);
    	} catch (IllegalArgumentException e) {
    		throw new RuntimeException("internal error", e); //$NON-NLS-1$
    	} catch (IllegalAccessException e) {
    		e.printStackTrace();
    		// TODO: check the protection earlier and raise MalformedPlugin
    		throw new RuntimeException("internal error - field protection problem"); //$NON-NLS-1$
    	}
	}

	/**
	 * This method allows datastore implementations to re-parent an
	 * object (move it from one list to another).
	 * <P>
	 * This method is to be used by datastore implementations only.
	 * Other plug-ins should not be calling this method.
	 *
	 * @param listKey
	 */
	// TODO figure out how to make this not public
	@Override
	public void replaceParentListKey(ListKey listKey) {
		this.parentKey = listKey;
	}

    @Override
	public Object getAdapter(Class adapter) {
    	if (adapter == IPropertySource.class) {
    		ExtendablePropertySet<?> propertySet = PropertySet.getPropertySet(this.getClass());
    		return ExtendableObjectPropertySource.construct(propertySet, this);
    	}
        return null;
     }

    @Override
    public ExtendablePropertySet getPropertySet() {
    	return PropertySet.getPropertySet(this.getClass());
    }
}
