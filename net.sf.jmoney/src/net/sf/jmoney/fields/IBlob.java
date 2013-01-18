package net.sf.jmoney.fields;

import java.io.IOException;
import java.io.InputStream;

public interface IBlob {

	/**
	 * A new stream is created on each call of this method. It is the caller's
	 * responsibility to both close this stream and also call 'close' on this
	 * interface (after the input stream is closed) to close any other
	 * resources.
	 * 
	 * @return
	 * @throws IOException
	 */
	InputStream createStream() throws IOException;

	/**
	 * This must be called when the input stream has been completed.
	 * This allows the implementation to release resources that cannot
	 * be released before the input stream has been read and that would
	 * not otherwise be released.
	 */
	void close();
}
