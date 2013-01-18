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

import org.eclipse.datatools.connectivity.oda.IConnection;
import org.eclipse.datatools.connectivity.oda.IDriver;
import org.eclipse.datatools.connectivity.oda.LogConfiguration;
import org.eclipse.datatools.connectivity.oda.OdaException;

/**
 * The implementation of the IDriver interface
 * for the JMoney datasource.
 */

public class Driver implements IDriver
{
    /*
     * @see org.eclipse.datatools.connectivity.oda.IDriver#getConnection(java.lang.String)
     */
    public IConnection getConnection( String dataSourceId )
            throws OdaException
    {
        return new Connection();
    }

    /*
     * @see org.eclipse.datatools.connectivity.oda.IDriver#getMaxConnections()
     */
    public int getMaxConnections() throws OdaException
    {
    	// Zero indicates no maximum
        return 0;
    }

    /*
     * @see org.eclipse.datatools.connectivity.oda.IDriver#setAppContext(java.lang.Object)
     */
    public void setAppContext( Object context ) throws OdaException
    {
        // do nothing; no support for pass-through application context
    }

    /*
     * @see org.eclipse.datatools.connectivity.oda.IDriver#setLogConfiguration(org.eclipse.datatools.connectivity.oda.LogConfiguration)
     */
    public void setLogConfiguration( LogConfiguration logConfig )
            throws OdaException
    {
        throw new UnsupportedOperationException();
    }
}