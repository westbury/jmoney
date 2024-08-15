package net.sf.jmoney.jdbcdatastore;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Blob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import net.sf.jmoney.fields.IBlob;
import net.sf.jmoney.fields.IPersistentBlob;
import net.sf.jmoney.fields.PersistentBlobFromImmutableByteArray;
import net.sf.jmoney.isolation.IExtendablePropertySet;
import net.sf.jmoney.isolation.IModelObject;
import net.sf.jmoney.isolation.IScalarPropertyAccessor;
import net.sf.jmoney.model2.ExtendablePropertySet;
import net.sf.jmoney.model2.ScalarPropertyAccessor;

/**
 * This class implements IBlob in the case where the blob comes from the
 * database.
 * <P>
 * A simple implementation would take the Blob returned from the database in a
 * result set, save the blob, and call getBinaryStream on it if the blob's data
 * is requested. However this won't work because JDBC (at least in the Derby
 * implementation) does not allow data from a blob to be read once the statement
 * has been closed. Another approach would be to copy the content of the blob to
 * a ByteArrayOutputStream in the constructor, then create an input stream from
 * that when the blob's data is requested. The problem with that approach though
 * is that is the vast majority of cases blob data is never requested, so we
 * would be fetching and storing vast amounts of blob data for no reason. This
 * implementation therefore reads the blob from the database by executing a
 * statement at the time the blob's content is requested.
 * <P>
 * However the blob must still exist in the database when we read the blob. This can cause
 * problems when an image is moved from one entry to another. The entry containing the source image
 * may have already been deleted when the image is written to the new entry. So we need to convert this
 * to an IBlobWriter (which holds the blob internally in a byte array) before starting to
 * commit the changes. The framework will enforce this by refusing to write a blob if it has not been persisted.
 * 
 */
public class BlobFromDatabase implements IBlob {

	private SessionManager sessionManager;
	private IExtendablePropertySet<?> propertySet;
	private IDatabaseRowKey objectKey;
	private IScalarPropertyAccessor<IBlob, ?> propertyAccessor;
	
	private PreparedStatement stmt = null; 

	public <E extends IModelObject> BlobFromDatabase(SessionManager sessionManager,
			IExtendablePropertySet<E> propertySet, IDatabaseRowKey objectKey,
			IScalarPropertyAccessor<IBlob, E> propertyAccessor) {
		this.sessionManager = sessionManager;
		this.propertySet = propertySet;
		this.objectKey = objectKey;
		this.propertyAccessor = propertyAccessor;
	}

	@Override
	public InputStream createStream() {
		try {
			String tableName = ((ExtendablePropertySet<?>)propertySet).getId().replace('.', '_');
			String columnName = '"' + sessionManager.getColumnName((ScalarPropertyAccessor)propertyAccessor) + '"';
			String sql = new StringBuffer()
			.append("SELECT ")
			.append(columnName)
			.append(" FROM ")
			.append(tableName)
			.append(" WHERE \"_ID\"=?")
			.toString();

			System.out.println(sql + " : " + objectKey.getRowId());
			stmt = sessionManager.getConnection().prepareStatement(sql);
			stmt.setInt(1, objectKey.getRowId());
			ResultSet rs = stmt.executeQuery();

			boolean rowExists = rs.next();
			if (!rowExists) {
				throw new RuntimeException("blob error");
			}

			Blob blob = rs.getBlob(1);
			return blob.getBinaryStream();
		} catch (SQLException e) {
			throw new RuntimeException("Unable to read JDBC blob", e);
		}
	}

	@Override
	public IPersistentBlob createPersistentBlob() throws IOException {
		try (InputStream inputStream = createStream()) {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			inputStream.transferTo(baos);
			return new PersistentBlobFromImmutableByteArray(baos.toByteArray());
		}
	}

	@Override
	public void close() {
		try {
			stmt.close();
		} catch (SQLException e) {
			// Ignore if we can't close
			e.printStackTrace();
		}
	}
}
