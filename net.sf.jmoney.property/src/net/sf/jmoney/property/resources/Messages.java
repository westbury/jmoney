package net.sf.jmoney.property.resources;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "net.sf.jmoney.property.resources.messages"; //$NON-NLS-1$

	public static String NavigationTreeNode_stocks;
	public static String NavigationTree_realPropertyDescription;
	public static String SecuritiesActionProvider_DeleteSecurity;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
		// Private constructor
	}

}
