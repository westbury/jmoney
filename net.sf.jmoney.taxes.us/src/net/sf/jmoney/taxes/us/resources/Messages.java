package net.sf.jmoney.taxes.us.resources;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "net.sf.jmoney.taxes.us.resources.Messages"; //$NON-NLS-1$
	
	public static String Dialog_OverwriteExistingFile;
	public static String Dialog_FileExists;
	
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
