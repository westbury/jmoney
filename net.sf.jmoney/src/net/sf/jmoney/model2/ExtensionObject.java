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

import net.sf.jmoney.isolation.DataManager;
import net.sf.jmoney.isolation.IObjectKey;


/**
 * This class is the base class from which all classes that implement
 * extension property sets should be derived.

 * @author Nigel Westbury
 */
public abstract class ExtensionObject {
	protected ExtendableObject baseObject;
	protected PropertySet propertySet;
	
	public ExtensionObject(ExtendableObject extendedObject) {
		this.baseObject = extendedObject;
	}

	void setPropertySet(PropertySet propertySet) {
		this.propertySet = propertySet;
	}

	public IObjectKey getObjectKey() {
    	return baseObject.getObjectKey();
	}
	
	public Session getSession() {
    	return baseObject.getSession();
	}
	
	public DataManager getDataManager() {
    	return baseObject.getDataManager();
	}
	
	/**
	 * Two or more instantiated objects may represent the same object
	 * in the datastore.  Such objects should be considered
	 * the same.  Therefore this method overrides the default
	 * implementation that is based on Java identity.
	 * <P>
	 * This method also considers two objects to be the same if either
	 * or both of the objects are extension objects and the underlying
	 * objects are the same.
	 * <P>
	 * @return true if the two objects represent the same object
	 * 		in the datastore, false otherwise.
	 */
    // TODO: Is this method really correct?
    // If so, we need a hashcode also.
    @Override	
	public boolean equals(Object object) {
    	return baseObject.equals(object);
	}
	
	public <X extends ExtensionObject> X getExtension(ExtensionPropertySet<X,?> propertySet, boolean alwaysReturnNonNull) {
    	return baseObject.getExtension(propertySet, alwaysReturnNonNull);
    }

	@Deprecated
    public <V> V getPropertyValue(ScalarPropertyAccessor<V,?> propertyAccessor) {
        return baseObject.getPropertyValue(propertyAccessor);
    }
    
    public <V> void setPropertyValue(ScalarPropertyAccessor<V,?> propertyAccessor, V value) {
    	baseObject.setPropertyValue(propertyAccessor, value);
    }

	protected <V> void processPropertyChange(ScalarPropertyAccessor<V,?> propertyAccessor, V oldValue, V newValue) {
		baseObject.processPropertyChange(propertyAccessor, oldValue, newValue);
	}

	/**
	 * This method is used to enable other classes in the package to
	 * access protected fields in the extension objects.
	 * 
	 * @param theObjectKeyField
	 * @return
	 */
	Object getProtectedFieldValue(Field theObjectKeyField) {
    	try {
    		return theObjectKeyField.get(this);
    	} catch (IllegalArgumentException e) {
    		e.printStackTrace();
    		throw new RuntimeException("internal error"); //$NON-NLS-1$
    	} catch (IllegalAccessException e) {
    		e.printStackTrace();
    		// TODO: check the protection earlier and raise MalformedPlugin
    		throw new RuntimeException("internal error - field protection problem"); //$NON-NLS-1$
    	}
	}
}
