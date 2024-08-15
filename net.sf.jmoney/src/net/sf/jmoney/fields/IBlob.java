package net.sf.jmoney.fields;

import java.io.IOException;
import java.io.InputStream;

/**
 * A blob that has been read from the datastore. The content is not read unless it
 * is needed. This means we are not reading hundreds of images that we never need.
 * However the downside is that we can't just set the blob object into another extendable
 * object. The reason is that by the time the object needs to be written to the datastore,
 * the original blob may already have been deleted from the datastore. We deal with this by
 * creating a persistent blob object (which has the content copied into an internal byte array)
 * before setting the image into another extendable object.
 * <P>
 * In order to enforce this at compile time, we have two interfaces, IBlobReader and IBlobWriter.
 * IBlobReader is valid only for as long as the image still exists in its original location.
 *
 * @author Nigel Westbury
 *
 */
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
	 * This method returns this blob but is an implementation that is guaranteed
	 * to have an indefinite lifetime. This is useful when copying an image from an entry in the datastore
	 * and we want to be sure the image is not deleted from under us.
	 * @return
	 * @throws IOException
	 */
	IPersistentBlob createPersistentBlob() throws IOException;
	
	/**
	 * This must be called when the input stream has been completed.
	 * This allows the implementation to release resources that cannot
	 * be released before the input stream has been read and that would
	 * not otherwise be released.
	 */
	void close();
}
