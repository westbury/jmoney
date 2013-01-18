package net.sf.jmoney.reconciliation.resources;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "net.sf.jmoney.reconciliation.resources.messages"; //$NON-NLS-1$
	public static String ReconciliationAccountInfo_Import;
	public static String ReconciliationAccountInfo_Patterns;
	public static String ReconciliationAccountInfo_DefaultCategory;
	public static String StatementSection_EntryDescription;
	public static String UnreconciledSection_EntryDescription;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
