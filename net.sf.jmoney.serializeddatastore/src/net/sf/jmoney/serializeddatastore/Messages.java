package net.sf.jmoney.serializeddatastore;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "net.sf.jmoney.serializeddatastore.messages"; //$NON-NLS-1$
	public static String Account_FullCategoryName;
	public static String Currency_DecimalProblem;
	public static String Entry_ClearedName;
	public static String Entry_ClearedStatus;
	public static String Entry_ReconcilingName;
	public static String Entry_ReconcilingStatus;
	public static String Entry_UnclearedName;
	public static String Entry_UnclearedStatus;
	public static String JMoneyXmlFormat_DeserializedMessage;
	public static String JMoneyXmlFormat_OpeningFile;
	public static String JMoneyXmlFormat_ReadErrorMessage;
	public static String JMoneyXmlFormat_ReadErrorTitle;
	public static String JMoneyXmlFormat_SavingFile;
	public static String JMoneyXmlFormat_WriteErrorMessage;
	public static String JMoneyXmlFormat_WriteErrorTitle;
	public static String OpenSessionAction_ErrorTitle;
	public static String OpenSessionHandler_OpenSessionFailed;
	public static String RootCategory_CategoryName;
	public static String SerializedDatastorePlugin_MessageMenu;
	public static String SerializedDatastorePlugin_MessageNoSession;
	public static String SerializedDatastorePlugin_SaveProblem;
	public static String SessionManager_UnknownFileExtension;
	public static String SessionManager_FileNotDefined;
	public static String SessionManager_InvalidFileName;
	public static String SessionManager_OverwriteExistingFile;
	public static String SessionManager_OverwriteTitle;
	public static String SessionManager_SaveQuestion;
	public static String SessionManager_SaveTitle;
	public static String SplitCategory_Name;
	public static String TransferCategory_Name;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
