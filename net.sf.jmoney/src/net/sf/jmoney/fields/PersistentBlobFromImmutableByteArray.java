package net.sf.jmoney.fields;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * This is a helper class. It helps IBlob implementations implement {@link IBlob#createPersistentBlob() createPersistentBlob}.
 */
public class PersistentBlobFromImmutableByteArray implements IPersistentBlob {

	byte[] byteArray;
	
	public PersistentBlobFromImmutableByteArray(byte[] byteArray) {
		this.byteArray = byteArray;
	}

	@Override
	public InputStream createStream() {
		return new ByteArrayInputStream(byteArray);
	}

	@Override
	public void close() {
		// Nothing to close
	}

	@Override
	public IPersistentBlob createPersistentBlob() throws IOException {
		return this;
	}

}
