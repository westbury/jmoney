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

import java.util.Date;

import org.eclipse.datatools.connectivity.oda.OdaException;

abstract class Parameter {
	protected String name;

	public Parameter(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	abstract public ColumnType getColumnType();

	abstract public boolean isNullable();
	
	// At least one of the following should be overridden:
	
	public void setString(String value) throws OdaException {
		throw new OdaException("String values not supported for parameter " + name);
	}
	
	public void setDate(Date value) throws OdaException {
		throw new OdaException("String values not supported for parameter " + name);
	}
	
}