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

import java.io.Reader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

import net.sf.jmoney.JMoneyPlugin;
import net.sf.jmoney.oda.Messages;

import org.eclipse.datatools.connectivity.oda.IConnection;
import org.eclipse.datatools.connectivity.oda.IParameterMetaData;
import org.eclipse.datatools.connectivity.oda.IQuery;
import org.eclipse.datatools.connectivity.oda.IResultSet;
import org.eclipse.datatools.connectivity.oda.IResultSetMetaData;
import org.eclipse.datatools.connectivity.oda.OdaException;
import org.eclipse.datatools.connectivity.oda.SortSpec;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.XMLMemento;

/**
 * Flat file data provider's implementation of the ODA IQuery interface. It
 * supports single result set and no input parameters.
 */
public class Query implements IQuery
{
	// The max number of rows that can be read
	private int maxRows = 0;

	// The Connection instance associated with the Query.
	private IConnection connection = null;

	/*
	 * The object that fetches the rows in the result set.
	 */
	private IFetcher fetcher = null;
	
	/*
	 * The meta data of the query's result set. It is available only after a
	 * query is prepared.
	 */
	private ResultSetMetaData resultSetMetaData = null;

	/*
	 * The meta data of the query's parameters. It is available only after a
	 * query is prepared.
	 */
	private ParameterMetaData parameterMetaData = null;

    /*
     * Maps parameter names to indexes.  Used to support the
     * versions of the methods that use parameter names instead
     * of the parameter indexes.
     * 
     * Parameter names are case sensitive.
     */
    Map<String, Integer> parameterNameToIndexMap = new HashMap<String, Integer>();

	/**
	 * Constructor
	 * 
	 * @param homeDir
	 *            the directory in which the data files reside
	 * @param host
	 *            The connection which creates this query
	 * @param charSet
	 *            The character set used to decode the file
	 * @throws OdaException
	 */
	Query(IConnection connection) throws OdaException
	{
		if (connection == null) {
			throw new OdaException(Messages.getString( "common.connectionArgumentIsNull")); //$NON-NLS-1$
		}
		
		this.connection = connection;
	}

	/*
	 * @see org.eclipse.datatools.connectivity.oda.IQuery#prepare(java.lang.String)
	 */
	public void prepare(String queryText) throws OdaException
	{
		validateOpenConnection();
		
		// Is this test necessary and complete?
		if (queryText == null || queryText.length( ) == 0) {
			throw new OdaException(Messages.getString("query.commandIsEmpty")); //$NON-NLS-1$
		}

		fetcher = parseQueryText(queryText);
		
		resultSetMetaData = new ResultSetMetaData(fetcher);
		parameterMetaData = new ParameterMetaData(fetcher);
		
		/*
		 * Build the map so we can get a column index quickly
		 * given a column name.
		 */
		for (int i = 0; i < parameterMetaData.getParameterCount(); i++) {
			String parameterName = parameterMetaData.parameters.get(i).getName();
			parameterNameToIndexMap.put(parameterName, i);
		}
	}

	private IFetcher parseQueryText(String queryText) throws OdaException {
        Reader reader = new StringReader(queryText);
		IMemento memento;
        try {
			memento = XMLMemento.createReadRoot(reader);
		} catch (WorkbenchException e) {
			/*
			 * As we don't know the likely causes of this, pass on the
			 * exception.
			 */
			throw new OdaException(e);
		}
		
		IMemento tableMemento;
		
		tableMemento = memento.getChild("listProperty");
		if (tableMemento != null) {
			return new ListFetcher(tableMemento);
		} 

		tableMemento = memento.getChild("entriesInAccount");
		if (tableMemento != null) {
			return new EntriesInAccountFetcher(tableMemento);
		} 

		tableMemento = memento.getChild("parameter");
		if (tableMemento != null) {
			return new ParameterFetcher(tableMemento);
		} 

		/*
		 * A null memento indicates the session object
		 */
		return new SessionFetcher();
	}
	
	/*
	 * @see org.eclipse.datatools.connectivity.oda.IQuery#setAppContext(java.lang.Object)
	 */
	public void setAppContext(Object context) throws OdaException
	{
		// do nothing; no support for pass-through application context
	}

	/*
	 * @see org.eclipse.datatools.connectivity.oda.IQuery#setProperty(java.lang.String,
	 *      java.lang.String)
	 */
	public void setProperty(String name, String value) throws OdaException
	{
		throw new UnsupportedOperationException( );
	}

	/*
	 * @see org.eclipse.datatools.connectivity.oda.IQuery#close()
	 */
	public void close() throws OdaException
	{
		// Nothing to close
	}

	/*
	 * @see org.eclipse.datatools.connectivity.oda.IQuery#setMaxRows(int)
	 */
	public void setMaxRows(int maxRows) throws OdaException
	{
		this.maxRows = maxRows;
	}

	/*
	 * @see org.eclipse.datatools.connectivity.oda.IQuery#getMaxRows()
	 */
	public int getMaxRows() throws OdaException
	{
		return maxRows;
	}

	/*
	 * @see org.eclipse.datatools.connectivity.oda.IQuery#getMetaData()
	 */
	public IResultSetMetaData getMetaData() throws OdaException
	{
		return resultSetMetaData;
	}

	/*
	 * @see org.eclipse.datatools.connectivity.oda.IQuery#executeQuery()
	 */
	public IResultSet executeQuery() throws OdaException
	{
		if (JMoneyPlugin.getDefault().getSession() == null) {
			throw new OdaException("You must open a JMoney accounting session before you can fetch a result set.");
		}
		
		/*
		 * Note that the consumer must finish with the result set
		 * before calling this method again to get another result
		 * set.  I don't know if this requirement is within the
		 * ODA specification, but if this causes a problem then
		 * we cannot re-use the IFetcher objects.
		 */
		fetcher.reset();
		return new ResultSet(fetcher, resultSetMetaData);
	}

	/*
	 * @see org.eclipse.datatools.connectivity.oda.IQuery#setInt(java.lang.String,
	 *      int)
	 */
	public void setInt(String parameterName, int value) throws OdaException
	{
        int parameterIndex = findInParameter(parameterName) - 1;
        setInt(parameterIndex, value);
	}

	/*
	 * @see org.eclipse.datatools.connectivity.oda.IQuery#setInt(int, int)
	 */
	public void setInt(int parameterId, int value) throws OdaException
	{
		throw new UnsupportedOperationException();
	}

	/*
	 * @see org.eclipse.datatools.connectivity.oda.IQuery#setDouble(java.lang.String,
	 *      double)
	 */
	public void setDouble(String parameterName, double value)
			throws OdaException
	{
        int parameterIndex = findInParameter(parameterName) - 1;
        setDouble(parameterIndex, value);
	}

	/*
	 * @see org.eclipse.datatools.connectivity.oda.IQuery#setDouble(int, double)
	 */
	public void setDouble(int parameterId, double value) throws OdaException
	{
		throw new UnsupportedOperationException( );
	}

	/*
	 * @see org.eclipse.datatools.connectivity.oda.IQuery#setBigDecimal(java.lang.String,
	 *      java.math.BigDecimal)
	 */
	public void setBigDecimal(String parameterName, BigDecimal value)
			throws OdaException
	{
        int parameterIndex = findInParameter(parameterName) - 1;
        setBigDecimal(parameterIndex, value);
	}

	/*
	 * @see org.eclipse.datatools.connectivity.oda.IQuery#setBigDecimal(int,
	 *      java.math.BigDecimal)
	 */
	public void setBigDecimal(int parameterId, BigDecimal value)
			throws OdaException
	{
		throw new UnsupportedOperationException();
	}

	/*
	 * @see org.eclipse.datatools.connectivity.oda.IQuery#setString(java.lang.String,
	 *      java.lang.String)
	 */
	public void setString(String parameterName, String value)
			throws OdaException
	{
        int parameterIndex = findInParameter(parameterName) - 1;
        setString(parameterIndex, value);
	}

	/*
	 * @see org.eclipse.datatools.connectivity.oda.IQuery#setString(int,
	 *      java.lang.String)
	 */
	public void setString(int parameterId, String value) throws OdaException
	{
		Parameter paramData = parameterMetaData.parameters.get(parameterId - 1);
		paramData.setString(value);
	}

	/*
	 * @see org.eclipse.datatools.connectivity.oda.IQuery#setDate(java.lang.String,
	 *      java.sql.Date)
	 */
	public void setDate(String parameterName, Date value) throws OdaException
	{
        int parameterIndex = findInParameter(parameterName) - 1;
        setDate(parameterIndex, value);
	}

	/*
	 * @see org.eclipse.datatools.connectivity.oda.IQuery#setDate(int,
	 *      java.sql.Date)
	 */
	public void setDate(int parameterId, Date value) throws OdaException
	{
		Parameter paramData = parameterMetaData.parameters.get(parameterId - 1);
		paramData.setDate(value);
	}

	/*
	 * @see org.eclipse.datatools.connectivity.oda.IQuery#setTime(java.lang.String,
	 *      java.sql.Time)
	 */
	public void setTime(String parameterName, Time value) throws OdaException
	{
		throw new UnsupportedOperationException();
	}

	/*
	 * @see org.eclipse.datatools.connectivity.oda.IQuery#setTime(int,
	 *      java.sql.Time)
	 */
	public void setTime(int parameterId, Time value) throws OdaException
	{
		throw new UnsupportedOperationException();
	}

	/*
	 * @see org.eclipse.datatools.connectivity.oda.IQuery#setTimestamp(java.lang.String,
	 *      java.sql.Timestamp)
	 */
	public void setTimestamp(String parameterName, Timestamp value)
			throws OdaException
	{
		throw new UnsupportedOperationException();
	}

	/*
	 * @see org.eclipse.datatools.connectivity.oda.IQuery#setTimestamp(int,
	 *      java.sql.Timestamp)
	 */
	public void setTimestamp(int parameterId, Timestamp value)
			throws OdaException
	{
		throw new UnsupportedOperationException();
	}

	public void setBoolean(String parameterName, boolean value) throws OdaException {
        int parameterIndex = findInParameter(parameterName) - 1;
        setBoolean(parameterIndex, value);
	}

	public void setBoolean(int parameterId, boolean value) throws OdaException {
		throw new UnsupportedOperationException();
	}

	public void setNull(String parameterName) throws OdaException {
        int parameterIndex = findInParameter(parameterName) - 1;
        setNull(parameterIndex);
	}

	public void setNull(int parameterId) throws OdaException {
		throw new UnsupportedOperationException();
	}

	/*
	 * @see org.eclipse.datatools.connectivity.oda.IQuery#findInParameter(java.lang.String)
	 */
	public int findInParameter(String parameterName) throws OdaException
	{
        Integer parameterIndex = parameterNameToIndexMap.get(parameterName);
        if (parameterIndex == null) {
        	throw new OdaException( Messages
                .getString( "query_PARAMETER_NOT_FOUND" ) + parameterName ); //$NON-NLS-1$
        }
        
        return parameterIndex + 1;
	}

	/*
	 * @see org.eclipse.datatools.connectivity.oda.IQuery#getParameterMetaData()
	 */
	public IParameterMetaData getParameterMetaData() throws OdaException
	{
		return parameterMetaData;
	}

	/*
	 * @see org.eclipse.datatools.connectivity.oda.IQuery#clearInParameters()
	 */
	public void clearInParameters() throws OdaException
	{
		throw new UnsupportedOperationException();
	}

	/*
	 * @see org.eclipse.datatools.connectivity.oda.IQuery#setSortSpec(org.eclipse.datatools.connectivity.oda.SortSpec)
	 */
	public void setSortSpec(SortSpec sortBy) throws OdaException
	{
		throw new UnsupportedOperationException();
	}

	/*
	 * @see org.eclipse.datatools.connectivity.oda.IQuery#getSortSpec()
	 */
	public SortSpec getSortSpec() throws OdaException
	{
		throw new UnsupportedOperationException();
	}

	/**
	 * Validate whether the query's connection is open.
	 * 
	 * @throws OdaException
	 *             if connection is not open yet
	 */
	private void validateOpenConnection() throws OdaException
	{
		if (connection.isOpen( ) == false) {
			throw new OdaException( Messages.getString("common.connectionNotOpen")); //$NON-NLS-1$
		}
	}
}
