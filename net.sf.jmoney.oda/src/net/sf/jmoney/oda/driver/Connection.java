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

import java.util.Properties;

import net.sf.jmoney.oda.Messages;

import org.eclipse.datatools.connectivity.oda.IConnection;
import org.eclipse.datatools.connectivity.oda.IDataSetMetaData;
import org.eclipse.datatools.connectivity.oda.IQuery;
import org.eclipse.datatools.connectivity.oda.OdaException;

/**
 * Implementation of the ODA IConnection interface.
 * 
 * This is required by ODA but does not really do anything in the case of a
 * connection to the JMoney datastore.
 */
public class Connection implements IConnection
{
	private boolean isOpen = false;

	/*
     * @see org.eclipse.datatools.connectivity.oda.IConnection#open(java.util.Properties)
     */
    public void open(Properties connProperties) throws OdaException
    {
        if (connProperties == null) {
            throw new OdaException(Messages
                    .getString("connection.connectionPropertiesMissing")); //$NON-NLS-1$
        }

        // If there were properties, we would set fields from the properties here.
        
        isOpen = true;
    }

    /*
     * @see org.eclipse.datatools.connectivity.oda.IConnection#setAppContext(java.lang.Object)
     */
    public void setAppContext(Object context) throws OdaException
    {
        // do nothing; no support for pass-through application context
    }

    /*
     * @see org.eclipse.datatools.connectivity.oda.IConnection#close()
     */
    public void close() throws OdaException
    {
        isOpen = false;
    }

    /*
     * @see org.eclipse.datatools.connectivity.oda.IConnection#isOpen()
     */
    public boolean isOpen() throws OdaException
    {
        return isOpen;
    }

    /*
     * @see org.eclipse.datatools.connectivity.oda.IConnection#getMetaData(java.lang.String)
     */
    public IDataSetMetaData getMetaData(String dataSetType)
            throws OdaException
    {
        return new DataSetMetaData(this);
    }

    /*
     * @see org.eclipse.datatools.connectivity.oda.IConnection#newQuery(java.lang.String)
     */
    public IQuery newQuery(String dataSourceType) throws OdaException
    {
        if(!isOpen()) {
            throw new OdaException(
            		Messages.getString("common.connectionNotOpen")); //$NON-NLS-1$
        }

        return new Query(this);
    }

    /*
     * @see org.eclipse.datatools.connectivity.oda.IConnection#commit()
     */
    public void commit() throws OdaException
    {
        throw new UnsupportedOperationException();
    }

    /*
     * @see org.eclipse.datatools.connectivity.oda.IConnection#rollback()
     */
    public void rollback() throws OdaException
    {
        throw new UnsupportedOperationException();
    }

    /* 
     * @see org.eclipse.datatools.connectivity.oda.IConnection#getMaxQueries()
     */
    public int getMaxQueries() throws OdaException
    {
    	// Zero indicates no limit
        return 0;
    }
}
