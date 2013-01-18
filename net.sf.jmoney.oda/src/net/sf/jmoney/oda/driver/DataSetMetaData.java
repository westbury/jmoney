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

import net.sf.jmoney.Constants;
import net.sf.jmoney.oda.Messages;

import org.eclipse.datatools.connectivity.oda.IConnection;
import org.eclipse.datatools.connectivity.oda.IDataSetMetaData;
import org.eclipse.datatools.connectivity.oda.IResultSet;
import org.eclipse.datatools.connectivity.oda.OdaException;

/**
 * This class provides the implementation of the IDataSetMetaData interface.
 */
public class DataSetMetaData implements IDataSetMetaData
{
    private IConnection connection;

    public DataSetMetaData(IConnection connection)
    {
        this.connection = connection;
    }

    /*
     * @see org.eclipse.datatools.connectivity.oda.IDataSetMetaData#getConnection()
     */
    public IConnection getConnection() throws OdaException
    {
        return connection;
    }

    /*
     * @see org.eclipse.datatools.connectivity.oda.IDataSetMetaData#getDataSourceObjects(java.lang.String,
     *      java.lang.String, java.lang.String, java.lang.String)
     */
    public IResultSet getDataSourceObjects(String catalog, String schema,
            String object, String version) throws OdaException
    {
        throw new UnsupportedOperationException();
    }

    /*
     * @see org.eclipse.datatools.connectivity.oda.IDataSetMetaData#getDataSourceMajorVersion()
     */
    public int getDataSourceMajorVersion() throws OdaException
    {
        throw new UnsupportedOperationException();
    }

    /*
     * @see org.eclipse.datatools.connectivity.oda.IDataSetMetaData#getDataSourceMinorVersion()
     */
    public int getDataSourceMinorVersion() throws OdaException
    {
        throw new UnsupportedOperationException();
    }

    /*
     * @see org.eclipse.datatools.connectivity.oda.IDataSetMetaData#getDataSourceProductName()
     */
    public String getDataSourceProductName() throws OdaException
    {
        return Messages.getString("dataSetMetaData.productName"); //$NON-NLS-1$
    }

    /*
     * @see org.eclipse.datatools.connectivity.oda.IDataSetMetaData#getDataSourceProductVersion()
     */
    public String getDataSourceProductVersion() throws OdaException
    {
        return Constants.PRODUCT_VERSION;
    }

    /*
     * @see org.eclipse.datatools.connectivity.oda.IDataSetMetaData#getSQLStateType()
     */
    public int getSQLStateType() throws OdaException
    {
        throw new UnsupportedOperationException();
    }

    /*
     * @see org.eclipse.datatools.connectivity.oda.IDataSetMetaData#supportsMultipleOpenResults()
     */
    public boolean supportsMultipleOpenResults() throws OdaException
    {
        return false;
    }

    /*
     * @see org.eclipse.datatools.connectivity.oda.IDataSetMetaData#supportsMultipleResultSets()
     */
    public boolean supportsMultipleResultSets() throws OdaException
    {
        return false;
    }

    /*
     * @see org.eclipse.datatools.connectivity.oda.IDataSetMetaData#supportsNamedResultSets()
     */
    public boolean supportsNamedResultSets() throws OdaException
    {
        return false;
    }

    /*
     * @see org.eclipse.datatools.connectivity.oda.IDataSetMetaData#supportsNamedParameters()
     */
    public boolean supportsNamedParameters() throws OdaException
    {
        return true;
    }

    /*
     * @see org.eclipse.datatools.connectivity.oda.IDataSetMetaData#supportsInParameters()
     */
    public boolean supportsInParameters() throws OdaException
    {
        return true;
    }

    /*
     * @see org.eclipse.datatools.connectivity.oda.IDataSetMetaData#supportsOutParameters()
     */
    public boolean supportsOutParameters() throws OdaException
    {
        return false;
    }

    /*
     * @see org.eclipse.datatools.connectivity.oda.IDataSetMetaData#getSortMode()
     */
    public int getSortMode()
    {
        return sortModeNone;
    }

}
