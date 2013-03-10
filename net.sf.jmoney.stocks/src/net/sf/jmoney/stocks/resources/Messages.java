package net.sf.jmoney.stocks.resources;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "net.sf.jmoney.stocks.resources.messages"; //$NON-NLS-1$

	public static String NavigationTreeNode_stocks;
	public static String NavigationTree_stockDescription;
	public static String SecuritiesActionProvider_DeleteSecurity;

	public static String StockAccountInfo_currency;
	public static String StockAccountInfo_brokerageFirm;
	public static String StockAccountInfo_accountNumber;
	public static String StockAccountInfo_dividendAccount;
	public static String StockAccountInfo_returnOfCapitalAccount;
	public static String StockAccountInfo_withholdingTaxAccount;
	public static String StockAccountInfo_commissionAccount;
	public static String StockAccountInfo_buyCommissionRates;
	public static String StockAccountInfo_sellCommissionRates;
	public static String StockAccountInfo_tax1Name;
	public static String StockAccountInfo_tax1Account;
	public static String StockAccountInfo_tax1Rates;
	public static String StockAccountInfo_tax2Name;
	public static String StockAccountInfo_tax2Account;
	public static String StockAccountInfo_tax2Rates;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
		// Private constructor
	}

}
