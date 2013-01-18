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

import java.util.Iterator;
import java.util.Vector;

import net.sf.jmoney.model2.ExtendableObject;
import net.sf.jmoney.model2.ExtendablePropertySet;
import net.sf.jmoney.model2.PropertySet;
import net.sf.jmoney.model2.PropertySetNotFoundException;
import net.sf.jmoney.model2.ScalarPropertyAccessor;

import org.eclipse.datatools.connectivity.oda.OdaException;
import org.eclipse.ui.IMemento;

/**
 * This class implements a fetcher that fetches a value
 * that has been passed as a report parameter.
 * 
 * Currently all report parameters are scalar values,
 * so objects of this class will always return a single
 * row.  However, BIRT/ODA is planned to support complex
 * parameters which may themselves be datasets.  If such
 * complex parameters are supported then objects of this
 * class may return multiple rows.
 * 
 * @author Nigel Westbury
 *
 */
public class ParameterFetcher implements IFetcher {

	private Parameter_Object parameterData;
	
	private Vector<Column> columnProperties = new Vector<Column>();
	
	private Iterator<? extends ExtendableObject> iterator = null;

	ExtendableObject parameterObject = null;
	
	public ParameterFetcher(IMemento memento) throws OdaException {
		String propertySetId = memento.getString("propertySetId");
		if (propertySetId == null) {
			throw new OdaException("no propertySetId");
		}

		try {
			ExtendablePropertySet<?> propertySet = PropertySet.getExtendablePropertySet(propertySetId);
			parameterData = new Parameter_Object(propertySet, ColumnType.stringType);
		} catch (PropertySetNotFoundException e) {
			throw new OdaException("Parameter specified but property set " + propertySetId + " not found.");
		}
		
/* Just include all properties for time being		
		for (IMemento childMemento: memento.getChildren("column")) {
			String id = childMemento.getString("name");
			ScalarPropertyAccessor property = parentPropertySet.getScalarProperty(id);
			columnProperties.add(property);
		}
*/
		for (final ScalarPropertyAccessor<?,?> property: parameterData.getPropertySet().getScalarProperties3()) {
			columnProperties.add(new Column(property.getName(), property.getDisplayName(), property.getClassOfValueObject(), property.isNullAllowed()) {
				@Override
				Object getValue() {
					return parameterObject.getPropertyValue(property);
				}
			});
		}
	}
	
	public void reset() {
		iterator = parameterData.getValue().iterator();
	}
	
	public boolean next() {
		if (iterator.hasNext()) {
			parameterObject = iterator.next();
			return true;
		} else {
			return false;
		}
	}

	public ExtendableObject getCurrentObject() {
		return parameterObject;
	}

	public void buildColumnList(Vector<Column> selectedProperties) {
		for (Column property: this.columnProperties) {
			selectedProperties.add(property);
		}
	}

	public ExtendablePropertySet getPropertySet() {
		return parameterData.getPropertySet();
	}

	public void buildParameterList(Vector<Parameter> parameters) {
		parameters.add(parameterData);
	}
}
