package net.sf.jmoney.ebay;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import net.sf.jmoney.fields.IBlob;

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
	public void close() {
		/*
		 * The connection is closed automatically when the stream is
		 * closed so we have nothing to do here.  There is not a close method
		 * on the connection anyway.
		 */
	}
}
