/*
*
*  JMoney - A Personal Finance Manager
*  Copyright (c) 2007 Nigel Westbury <westbury@users.sourceforge.net>
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

package net.sf.jmoney.oda.driver;

import java.util.Vector;

import net.sf.jmoney.model2.ExtendableObject;
import net.sf.jmoney.model2.ExtendablePropertySet;

public interface IFetcher {

	/**
	 * Resets to a position before the first row.
	 * 
	 * Parameter data is updated, and new iterators are obtained
	 * that iterate over the data in its current state.  This method
	 * must be called  before a query is executed.
	 */
	void reset();
	
	/**
	 * Move to the next row.
	 * 
	 * @return true if there was another row, false if the cursor is
	 * 			now positioned after the last row
	 */
	boolean next();

	/**
	 * The results of this method are undefined if the
	 * cursor is positioned either before the first row
	 * or after the last row.
	 * 
	 * @return the object for the current row
	 */
	ExtendableObject getCurrentObject();
	
	/**
	 * Adds all properties in the rowset to the end
	 * of the given vector.
	 * 
	 * @param selectedProperties
	 */
	void buildColumnList(Vector<Column> selectedProperties);

	/**
	 * Adds all parameters used by the fetcher to the end
	 * of the given vector.
	 * 
	 * @param parameters
	 */
	void buildParameterList(Vector<Parameter> parameters);
	
	/**
	 * Gets the property set for the type of objects that are returned in this
	 * data set.
	 */
	ExtendablePropertySet getPropertySet();
	
}
