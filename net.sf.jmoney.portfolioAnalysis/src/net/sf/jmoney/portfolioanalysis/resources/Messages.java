package net.sf.jmoney.portfolioanalysis.resources;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "net.sf.jmoney.portfolioanalysis.resources.messages"; //$NON-NLS-1$
	public static String Entry_portfolioName;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
