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

import java.sql.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import net.sf.jmoney.oda.Messages;

import org.eclipse.datatools.connectivity.oda.IResultSetMetaData;
import org.eclipse.datatools.connectivity.oda.OdaException;

/**
 * Class to hold the metadata for JMoney result sets. This class provides an
 * implementation of the ODA IResultSetMetaData interface.
 */
public class ResultSetMetaData implements IResultSetMetaData
{
	/*
	 * Vector of all properties in the result set, in
	 * the correct order
	 */
	Vector<Column> selectedProperties = new Vector<Column>();

	/*
	 * Vector of all column types (cached so we don't have to determine the type
	 * each time an attribute of the type is requested).
	 */
	private Vector<ColumnType> queryColumnTypes = new Vector<ColumnType>();

    /*
     * Maps column names to indexes.  Used to support the
     * versions of the methods that use column names instead
     * of the column indexes.
     * 
     * Column names are case sensitive.  Column names in this map
     * contain '.' characters.  Note that these are not allowed in ODA column
     * names so must be converted to and from '_' characters.
     */
    Map<String, Integer> columnNameToIndexMap = new HashMap<String, Integer>();

    ResultSetMetaData(IFetcher fetcher)
    {
		fetcher.buildColumnList(selectedProperties);
		
		/*
		 * Build the map so we can get a column index quickly
		 * given a column name.
		 */
		for (int i = 0; i < selectedProperties.size(); i++) {
			Column property = selectedProperties.get(i);
			columnNameToIndexMap.put(property.getName(), i);
		}
		
		for (Column property: selectedProperties) {
			queryColumnTypes.add(getColumnType(property.getClassOfValueObject()));
		}
    }

    /*
     * @see org.eclipse.datatools.connectivity.oda.IResultSetMetaData#getColumnCount()
     */
    public int getColumnCount() throws OdaException
    {
        return selectedProperties.size();
    }

    /*
     * @see org.eclipse.datatools.connectivity.oda.IResultSetMetaData#getColumnName(int)
     */
    public String getColumnName(int columnNumber) throws OdaException
    {
        validateColumnNumber(columnNumber);
        String nameWithDots = selectedProperties.get(columnNumber-1).getName();
        return nameWithDots.replace('.', '_');
    }

    /*
     * @see org.eclipse.datatools.connectivity.oda.IResultSetMetaData#getColumnLabel(int)
     */
    public String getColumnLabel(int columnNumber) throws OdaException
    {
        validateColumnNumber(columnNumber);
        return selectedProperties.get(columnNumber-1).getDisplayName();
    }

    /*
     * @see org.eclipse.datatools.connectivity.oda.IResultSetMetaData#getColumnType(int)
     */
    public int getColumnType(int columnNumber) throws OdaException
    {
        validateColumnNumber(columnNumber);
        return queryColumnTypes.get(columnNumber-1).getNativeType();
    }

    /*
     * @see org.eclipse.datatools.connectivity.oda.IResultSetMetaData#getColumnTypeName(int)
     */
    public String getColumnTypeName(int columnNumber) throws OdaException
    {
        validateColumnNumber(columnNumber);
        return queryColumnTypes.get(columnNumber-1).getNativeTypeName();
    }

    /*
     * @see org.eclipse.datatools.connectivity.oda.IResultSetMetaData#getColumnDisplayLength(int)
     */
    public int getColumnDisplayLength(int columnNumber) throws OdaException
    {
    	return 10;
//        throw new UnsupportedOperationException();
    }

    /*
     * @see org.eclipse.datatools.connectivity.oda.IResultSetMetaData#getPrecision(int)
     */
    public int getPrecision(int columnNumber) throws OdaException
    {
        return -1;
    }

    /*
     * @see org.eclipse.datatools.connectivity.oda.IResultSetMetaData#getScale(int)
     */
    public int getScale(int columnNumber) throws OdaException
    {
        return -1;
    }

    /*
     * @see org.eclipse.datatools.connectivity.oda.IResultSetMetaData#isNullable(int)
     */
    public int isNullable(int columnNumber) throws OdaException
    {
        return selectedProperties.get(columnNumber-1).isNullAllowed()
        ? columnNullable 
        		: columnNoNulls;
    }

    /**
     * Evaluate whether the value of given column number is valid.
     * @param columnNumber column number (1-based)
     * @throws OdaException if the given index value is invalid
     */
    private void validateColumnNumber(int columnNumber) throws OdaException
    {
        if (columnNumber > getColumnCount() || columnNumber < 1) {
            throw new OdaException(
                    Messages.getString("resultSetMetaData.invalidColumnIndex") + columnNumber); //$NON-NLS-1$
        }
    }

    private ColumnType getColumnType(Class<?> classOfValueObject) {
		if (classOfValueObject == String.class) {
			return ColumnType.stringType;
		} else if (classOfValueObject == Boolean.class) {
			return ColumnType.booleanType;
		} else if (classOfValueObject == Integer.class) {
			return ColumnType.integerType;
		} else if (classOfValueObject == Long.class) {
			return ColumnType.longType;
		} else if (classOfValueObject.isAssignableFrom(Date.class)) {
			return ColumnType.dateType;
		} else {
			/*
			 * All unknown classes are converted to strings
			 * using the toString method.
			 */
			return ColumnType.stringType;
		}
	}

}