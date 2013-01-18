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

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;

import net.sf.jmoney.oda.Messages;

import org.eclipse.datatools.connectivity.oda.IBlob;
import org.eclipse.datatools.connectivity.oda.IClob;
import org.eclipse.datatools.connectivity.oda.IResultSet;
import org.eclipse.datatools.connectivity.oda.IResultSetMetaData;
import org.eclipse.datatools.connectivity.oda.OdaException;

/**
 * Class to hold JMoney result sets. These result sets provide an implementation
 * of the ODA IResultSet interface.
 */

public class ResultSet implements IResultSet
{
    private IFetcher fetcher;

    private ResultSetMetaData resultSetMetaData = null;

    private int maxRows = 0;
    
    /*
	 * The zero based row index. Initialize to -1 so the first increment puts it
	 * on the first row.
	 */
    private int rowIndex = -1; 

    /**
     * Flag to indicate if the last value fetched was null.
     * This is needed because not all values can be returned
     * as null values.  For example, integers are returned
     * as zero values if null.
     */
    private boolean wasNull = false;

    /**
     * Constructor
     * @param fetcher a two-dimension array which holds the data extracted from a
     *            flat file.
     * @param rsmd the metadata of sData
     */
    ResultSet(IFetcher fetcher, ResultSetMetaData rsmd)
    {
        this.fetcher = fetcher;
        this.resultSetMetaData = rsmd;
    }

    /*
     * @see org.eclipse.datatools.connectivity.oda.IResultSet#getMetaData()
     */
    public IResultSetMetaData getMetaData() throws OdaException
    {
        return this.resultSetMetaData;
    }

    /*
     * @see org.eclipse.datatools.connectivity.oda.IResultSet#close()
     */
    public void close() throws OdaException
    {
        // Nothing to do on a close.
    }

    /*
     * @see org.eclipse.datatools.connectivity.oda.IResultSet#setMaxRows(int)
     */
    public void setMaxRows(int maxRows) throws OdaException
    {
        this.maxRows = maxRows;
    }

    /*
     * @see org.eclipse.datatools.connectivity.oda.IResultSet#next()
     */
    public boolean next() throws OdaException
    {
    	if (maxRows > 0
    			&& rowIndex >= maxRows - 1) {
            rowIndex = -1;
            return false;
        }
    		
        if(!fetcher.next()) {
            rowIndex = -1;
            return false;
        }

        rowIndex++;
        return true;
    }

    /*
     * @see org.eclipse.datatools.connectivity.oda.IResultSet#getRow()
     */
    public int getRow() throws OdaException
    {
        validateCursorState();
        return this.rowIndex;
    }

    /*
     * @see org.eclipse.datatools.connectivity.oda.IResultSet#getString(int)
     */
    public String getString(int columnNumber) throws OdaException
    {
    	validateCursorState();
    	Object result = resultSetMetaData.selectedProperties.get(columnNumber-1).getValue();
    	if (result == null) {
    		wasNull = true;
    		return null;
    	} else {
    		wasNull = false;
    		return result.toString();
    	}
    }

    /*
     * @see org.eclipse.datatools.connectivity.oda.IResultSet#getString(java.lang.String)
     */
    public String getString(String columnName) throws OdaException
    {
        validateCursorState();
        int columnNumber = findColumn(columnName);
        return getString(columnNumber);
    }

    /*
     * @see org.eclipse.datatools.connectivity.oda.IResultSet#getInt(int)
     */
    public int getInt(int columnNumber) throws OdaException
    {
    	validateCursorState();
    	Object result = resultSetMetaData.selectedProperties.get(columnNumber-1).getValue();
    	if (result == null) {
    		wasNull = true;
    		return 0;
    	} else {
    		wasNull = false;
    		if (result instanceof Number) {
    			return ((Number)result).intValue();
    		} else {
    			return 0;
    		}
    	}
    }

    /*
     * @see org.eclipse.datatools.connectivity.oda.IResultSet#getInt(java.lang.String)
     */
    public int getInt(String columnName) throws OdaException
    {
        validateCursorState();
        int columnNumber = findColumn(columnName);
        return getInt(columnNumber);
    }

    /*
     * @see org.eclipse.datatools.connectivity.oda.IResultSet#getDouble(int)
     */
    public double getDouble(int columnNumber) throws OdaException
    {
    	validateCursorState();
    	Object result = resultSetMetaData.selectedProperties.get(columnNumber-1).getValue();
    	if (result == null) {
    		wasNull = true;
    		return 0;
    	} else {
    		wasNull = false;
    		
    		if (result instanceof Number) {
    			return ((Number)result).doubleValue();
    		} else {
    			return 0;
    		}
    	}
    }

    /*
     * @see org.eclipse.datatools.connectivity.oda.IResultSet#getDouble(java.lang.String)
     */
    public double getDouble(String columnName) throws OdaException
    {
        validateCursorState();
        int columnNumber = findColumn(columnName);
        return getDouble(columnNumber);
    }

    /*
     * @see org.eclipse.datatools.connectivity.oda.IResultSet#getBigDecimal(int)
     */
    public BigDecimal getBigDecimal(int columnNumber) throws OdaException
    {
    	validateCursorState();
    	Object result = resultSetMetaData.selectedProperties.get(columnNumber-1).getValue();
    	if (result == null) {
    		wasNull = true;
    		return null;
    	} else {
    		wasNull = false;
            try {
                return new BigDecimal(result.toString());
            } catch (NumberFormatException e) {
                this.wasNull = true;
                return null;
            }
    	}
    }

    /*
     * @see org.eclipse.datatools.connectivity.oda.IResultSet#getBigDecimal(java.lang.String)
     */
    public BigDecimal getBigDecimal(String columnName) throws OdaException
    {
        validateCursorState();
        int columnNumber = findColumn(columnName);
        return getBigDecimal(columnNumber);
    }

    /*
     * @see org.eclipse.datatools.connectivity.oda.IResultSet#getDate(int)
     */
    public Date getDate(int columnNumber) throws OdaException
    {
    	validateCursorState();
    	Object result = resultSetMetaData.selectedProperties.get(columnNumber-1).getValue();
    	if (result == null) {
    		wasNull = true;
    		return null;
    	} else {
    		wasNull = false;
    		if (result instanceof Date) {
    			return (Date)result;
    		} else {
    			throw new OdaException("Property is not a date");
    		}
    	}
    }

    /*
     * @see org.eclipse.datatools.connectivity.oda.IResultSet#getDate(java.lang.String)
     */
    public Date getDate(String columnName) throws OdaException
    {
        validateCursorState();
        int columnNumber = findColumn(columnName);
        return getDate(columnNumber);
    }

    /*
     * @see org.eclipse.datatools.connectivity.oda.IResultSet#getTime(int)
     */
    public Time getTime(int columnNumber) throws OdaException
    {
        throw new UnsupportedOperationException();
    }

    /*
     * @see org.eclipse.datatools.connectivity.oda.IResultSet#getTime(java.lang.String)
     */
    public Time getTime( String columnName ) throws OdaException
    {
        throw new UnsupportedOperationException();
    }

    /*
     * @see org.eclipse.datatools.connectivity.oda.IResultSet#getTimestamp(int)
     */
    public Timestamp getTimestamp(int columnNumber) throws OdaException
    {
        throw new UnsupportedOperationException();
    }

    /*
     * @see org.eclipse.datatools.connectivity.oda.IResultSet#getTimestamp(java.lang.String)
     */
    public Timestamp getTimestamp(String columnName) throws OdaException
    {
        throw new UnsupportedOperationException();
    }

    /*
     * @see org.eclipse.datatools.connectivity.oda.IResultSet#getBlob(int)
     */
    public IBlob getBlob(int columnNumber) throws OdaException
    {
        throw new UnsupportedOperationException();
    }

    /*
     * @see org.eclipse.datatools.connectivity.oda.IResultSet#getBlob(java.lang.String)
     */
    public IBlob getBlob(String columnName) throws OdaException
    {
        throw new UnsupportedOperationException();
    }

    /*
     * @see org.eclipse.datatools.connectivity.oda.IResultSet#getClob(int)
     */
    public IClob getClob(int columnNumber) throws OdaException
    {
        throw new UnsupportedOperationException();
    }

    /*
     * @see org.eclipse.datatools.connectivity.oda.IResultSet#getClob(java.lang.String)
     */
    public IClob getClob(String columnName) throws OdaException
    {
        throw new UnsupportedOperationException();
    }

	public boolean getBoolean(int columnNumber) throws OdaException {
    	validateCursorState();
    	Object result = resultSetMetaData.selectedProperties.get(columnNumber-1).getValue();
    	if (result == null) {
    		wasNull = true;
    		return false;
    	} else {
    		wasNull = false;
    		if (result instanceof Boolean) {
    			return (Boolean)result;
    		} else if (result instanceof Number) {
    			return ((Number)result).intValue() != 0;
    		} else {
    			return false;
    		}
    	}
	}

	public boolean getBoolean(String columnName) throws OdaException {
        validateCursorState();
        int columnNumber = findColumn(columnName);
        return getBoolean(columnNumber);
	}

    /*
     * @see org.eclipse.datatools.connectivity.oda.IResultSet#wasNull()
     */
    public boolean wasNull() throws OdaException
    {
        return this.wasNull;
    }

    /*
     * @see org.eclipse.datatools.connectivity.oda.IResultSet#findColumn(java.lang.String)
     */
    public int findColumn(String columnName) throws OdaException
    {
        String nameWithDots = columnName.replace('_', '.');

        Integer columnIndex = resultSetMetaData.columnNameToIndexMap.get(nameWithDots);
        if (columnIndex == null) {
        	throw new OdaException( Messages
                .getString("resultSet.columnNotFound") + columnName); //$NON-NLS-1$
        }
        
        return columnIndex + 1;
    }

    /**
     * Validate whether the cursor has been initialized and at a valid row.
     * @throws OdaException if the cursor is not initialized
     */
    private void validateCursorState() throws OdaException
    {
        if (rowIndex < 0) {
            throw new OdaException(
            		Messages.getString("resultSet.cursorNotInitialized")); //$NON-NLS-1$
        }
    }
}