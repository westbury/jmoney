package net.sf.jmoney.ebay;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import net.sf.jmoney.fields.IBlob;
import net.sf.jmoney.fields.IPersistentBlob;
import net.sf.jmoney.fields.PersistentBlobFromImmutableByteArray;

public class UrlBlob implements IBlob {

	private URL url;
	
	public UrlBlob(URL url) {
		this.url = url;
	}

	@Override
	public InputStream createStream() throws IOException {
	    URLConnection connection = url.openConnection();
	    return connection.getInputStream();
	}

	@Override
	public IPersistentBlob createPersistentBlob() throws IOException {
		/* We could possibly just return 'this' here, because the URL would be a remote URL on
		 * Ebay and is not likely to just disappear.
		 * However for now leave this as is because it may be helpful to catch network errors early
		 * rather than have them occur deep down in the datastore commit code.
		 */
		try (InputStream inputStream = createStream()) {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			inputStream.transferTo(baos);
			return new PersistentBlobFromImmutableByteArray(baos.toByteArray());
		}
	}

	@Override
	public void close() {
		/*
		 * The connection is closed automatically when the stream is
		 * closed so we have nothing to do here.  There is not a close method
		 * on the connection anyway.
		 */
	}
}
