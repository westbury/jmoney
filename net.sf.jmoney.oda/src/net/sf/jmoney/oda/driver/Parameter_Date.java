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

class Parameter_Date extends Parameter {
	private Date value;

	public Parameter_Date(String name) {
		super(name);
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public ColumnType getColumnType() {
		return ColumnType.dateType;
	}

	@Override
	public boolean isNullable() {
		// For time being, do not allow null parameters
		return false;
	}

	@Override
	public void setDate(Date value) throws OdaException {
		this.value = value;
	}

	public Date getValue() {
		return value;
	}
}