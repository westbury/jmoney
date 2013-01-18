package net.sf.jmoney.shoebox.resources;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {

	private static final String BUNDLE_NAME = "net.sf.jmoney.shoebox.resources.messages"; //$NON-NLS-1$

	public static String ShoeboxEditor_EntryDescription;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}

}
