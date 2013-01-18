package net.sf.jmoney.sqldirect;

import java.sql.SQLException;

import net.sf.jmoney.jdbcdatastore.SessionManager;
import net.sf.jmoney.model2.Session;

public class SqlDirectFactory {

	public static SQLEntriesStatement getEntriesStatement(Session session, String sql) throws SQLException {
		/* There should only be one plug-in started at a time that implements
		 * a SQL datastore and that provides this package.  We should therefore
		 * be able to guarantee that the session is provided by this plug-in.
		 */
		SessionManager sessionManager = (SessionManager)session.getDataManager();

		return new SQLEntriesStatement(sessionManager.getConnection().prepareStatement(sql), sessionManager);
	}

}
