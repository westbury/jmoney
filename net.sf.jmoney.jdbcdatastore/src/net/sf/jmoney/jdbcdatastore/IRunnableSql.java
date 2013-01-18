package net.sf.jmoney.jdbcdatastore;

import java.sql.Connection;
import java.sql.SQLException;

public interface IRunnableSql<T> {
	T execute(Connection connection) throws SQLException;
}
