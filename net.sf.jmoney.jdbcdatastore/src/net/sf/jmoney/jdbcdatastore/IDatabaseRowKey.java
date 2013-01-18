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

package net.sf.jmoney.jdbcdatastore;

import net.sf.jmoney.isolation.IObjectKey;

/**
 * An interface which all object keys in this plug-in implement.
 * 
 * JMoney specifies that object keys must implement the IObjectKey
 * interface.  This plug-in has a further requirement for methods
 * that must be supported by the object keys, hence this interface
 * which extends the IObjectKey interface.
 *
 * @author Nigel Westbury
 */
public interface IDatabaseRowKey extends IObjectKey {
	/**
	 * 
	 * @return The integer id of the row in the database table 
	 * 			from which this object is read.  The id is
	 * 			the value of the "_ID" column.
	 */
	int getRowId();
}
