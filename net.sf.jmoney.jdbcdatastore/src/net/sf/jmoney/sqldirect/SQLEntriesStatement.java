package net.sf.jmoney.sqldirect;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.sf.jmoney.jdbcdatastore.SessionManager;
import net.sf.jmoney.jdbcdatastore.UncachedObjectIterator;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.EntryInfo;

public class SQLEntriesStatement {

	private PreparedStatement statement;
	private SessionManager sessionManager;
	
	public SQLEntriesStatement(PreparedStatement statement, SessionManager sessionManager) {
		this.statement = statement;
		this.sessionManager = sessionManager;
	}

	public List<Entry> execute() throws SQLException {
		List<Entry> elements = new ArrayList<Entry>(); 

		ResultSet resultSet = statement.executeQuery();

		Iterator<Entry> iter = new UncachedObjectIterator<Entry>(resultSet, EntryInfo.getPropertySet(), null, sessionManager);
		while (iter.hasNext()) {
			elements.add(iter.next());
		}

		return elements;
	}

	public void setInt(int index, int value) throws SQLException {
		statement.setInt(index, value);
	}

	public void setLong(int index, long value) throws SQLException {
		statement.setLong(index, value);
	}

	public void setDate(int index, java.sql.Date value) throws SQLException {
		statement.setDate(index, value);
	}

	public void setString(int index, String value) throws SQLException {
		statement.setString(index, value);
	}
}
