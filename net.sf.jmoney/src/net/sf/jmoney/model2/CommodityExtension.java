/*
 *
 *  JMoney - A Personal Finance Manager
 *  Copyright (c) 2006 Nigel Westbury <westbury@users.sourceforge.net>
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
 * This is a helper class that makes it a little easier for a plug-in to extend
 * the Commodity object.
 * <P>
 * To add fields and methods to a Commodity object, one may derive a class
 * from CommodityExtension. This mechanism allows multiple extensions to a
 * Commodity object to be added and maintained at runtime.
 * <P>
 * All extensions to Commodity objects implement the same methods that are
 * in the Commodity object. This is for convenience so the consumer can get
 * a single object that supports both the original Commodity methods and
 * the extension methods. All Commodity methods are passed on to the
 * Commodity object.
 * 
 * @author Nigel Westbury
 */
public abstract class CommodityExtension extends ExtensionObject {
    
    public CommodityExtension(ExtendableObject extendedObject) {
    	super(extendedObject);
    }

	public String getName() {
		return getBaseObject().getName();
	}

	public void setName(String name) {
		getBaseObject().setName(name);
	}

	// This does some casting - perhaps this is not needed
	// if generics are used????
	public Commodity getBaseObject() {
		return (Commodity)baseObject;
	}
}
