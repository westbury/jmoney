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

/**
* This class contains information about a property.  The property may be in the base
* bookkeeping class or in an extension bookkeeping class.
*
* This object contains the metadata.
*
* Some properties may be allowed to take null values and
* some may not.  The determination is made by the JMoney framework
* by looking at the type returned by the getter method.
* <P>
* The following properties may take null values:
* <UL>
* <LI>All properties that reference extendable objects</LI>
* <LI>All properties of type String, Date or other such simple class</LI>
* <LI>All properties where the type is one of the classes representing intrinsic types,
* 			such as Integer, Long</LI>
* </UL>
* The following properties may not take null values:
* <UL>
* <LI>All properties where the type is an intrinsic type, such as int, long</LI>
* </UL>
* 
* @author  Nigel Westbury
*/
public abstract class PropertyAccessor {
   
   protected PropertySet<?,?> propertySet;
   
   protected String localName;    
   
   protected String displayName;
   
	/**
	 * Index into the list of parameters passed to the constructor.
	 * Zero indicates that this property is passed as the first
	 * parameter to the constructor.
	 * 
	 */
   protected int indexIntoConstructorParameters = -1;
	
   public PropertyAccessor(PropertySet propertySet, String localName, String displayName) {
	   this.propertySet = propertySet;
	   this.localName = localName;
	   this.displayName = displayName;
   }
   
   /**
    * Create a property accessor for a list property.
    * 
	 * @param set
	 * @param name
	 * @param listItemClass
	 * @param displayName
	 * @param propertyDependency
	 */
   public PropertyAccessor(PropertySet propertySet, String localName, String displayName, Class<? extends ExtendableObject> listItemClass) {
	   this.propertySet = propertySet;
	   this.localName = localName;
	   this.displayName = displayName;
   }

   /**
    * Returns the property set which contains this property.
    */
	// TODO: Check all the uses of this.  Some of the uses require
	// that for extension property sets, the property set being
	// extended should be returned.  This saves the caller from having
	// to test the property set.
	public PropertySet getPropertySet() {
       return propertySet;
   }
	
   /**
    * Returns the extendable property set which contains this property.
    * 
    * If the property is in an extendable property set then this
    * method returns the same value as <code>getPropertySet()</code>.
    * If the property is in an extension property set then
    * the property set being extended is returned.
    */
   // TODO: Consider removing this method.  It is not used.
/*	
	public PropertySet getExtendablePropertySet() {
		if (propertySet.isExtension()) {
			return propertySet.getExtendablePropertySet();
		} else {
			return propertySet;
		}
   }
*/   
   /**
    * Returns the PropertySet for the values of this property.
    * This property must contain a value or values that are
    * extendable objects. 
    */
/* not used.   
   public PropertySet getValuePropertySet() {
		for (Iterator iter = PropertySet.getPropertySetIterator(); iter.hasNext(); ) {
			PropertySet propertySet = (PropertySet)iter.next();
			if (!propertySet.isExtension()) {
				if (propertySet.getImplementationClass() == propertyClass) {
					return propertySet;
				}
			}
		}
		
       throw new RuntimeException("No property set found for extendable class object" + propertyClass.getName() + ".");
   }
*/   
   /**
    * Return a name for this property.
    *
    * This name is used by the framework for persisting information about the property
    * in configuration files etc.  For example, if the user sorts a column based on a
    * property then that information can be stored in a configuration file so that the
    * data is sorted on the column the next time the user loads the view.
    */
   public String getName() {
       // We must uniquify the name, so prepend the property set id.
   	// TODO: this does not uniquify the name because a property may
   	// exist with the same name as both, say, an account property
   	// and an entry property.  We should probably add the base
   	// class name here too.
       return propertySet.toString() + "." + localName; //$NON-NLS-1$
   }
   
   /**
	 * The local name of the property is just the last part of the name, after
	 * the last dot. This will be unique within an extension but may not be
	 * unique across all plugins or even across extensions to different types of
	 * bookkeeping objects (entries, categories, transactions, or commodities)
	 * within a plug-in.
	 */
   public String getLocalName() {
       return localName;
   }
   
   /**
    * A short description that is suitable as a column header when this
    * property is displayed in a table.
    */
   public String getDisplayName() {
       return displayName;
   }
   
   /**
    * Indicates if the property is a list of intrinsic values or objects.
    */
   public abstract boolean isList();
   
   /**
    * Indicates if the property is a single intrinsic value or object
    * (not a list of values)
    */
   public boolean isScalar() {
       return !isList();
   }
   
	/**
	 * @return the index into the constructor parameters, where
	 * 		an index of zero indicates that the property is the
	 * 		first parameter to the constructor.  An index of -1
	 * 		indicates that the property is not passed to the
	 * 		constructor (the property value is redundant and the
	 * 		object can be fully re-constructed from the other
	 * 		properties).
	 */
	public int getIndexIntoConstructorParameters() {
		return indexIntoConstructorParameters;
	}

	/**
	 * 
	 * @param indexIntoConstructorParameters
	 */
	// TODO: This method should be accessible only from within the package. 
	public void setIndexIntoConstructorParameters(int indexIntoConstructorParameters) {
		this.indexIntoConstructorParameters = indexIntoConstructorParameters;
	}
}
