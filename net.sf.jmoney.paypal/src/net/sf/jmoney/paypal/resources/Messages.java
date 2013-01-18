package net.sf.jmoney.paypal.resources;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "net.sf.jmoney.paypal.resources.messages"; //$NON-NLS-1$
	public static String PaypalAccountInfo_ObjectDescription;
	public static String PaypalAccountInfo_TransferBankAccount;
	public static String PaypalAccountInfo_TransferCreditCardAccount;
	public static String PaypalAccountInfo_SaleAndPurchaseAccount;
	public static String PaypalAccountInfo_PaypalFeesAccount;
	public static String PaypalAccountInfo_DonationAccount;
	public static String TransferAccountsPage_PageTitle;
	public static String TransferAccountsPage_PageMessage;
	public static String PaypalFeesPage_PageTitle;
	public static String PaypalFeesPage_PageMessage;
	public static String PaypalAccount_NewAccount;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
