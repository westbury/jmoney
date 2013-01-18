package net.sf.jmoney.qif;

import net.sf.jmoney.model2.Account;
import net.sf.jmoney.model2.Session;
import net.sf.jmoney.qif.parser.QifFile;
import net.sf.jmoney.qif.parser.QifImportException;

public interface IQifImporter {

	/**
	 * When the user imports a QIF file, the file is first parsed into
	 * an object that contains the complete contents of the QIF file.
	 * This object is then passed to all extensions that know how to import
	 * a part of the QIF file.
	 * <P>
	 * For example, if a plug-in is written to implement memorized transactions
	 * then that plug-in should provide an implementation of this interface
	 * that imports the memorized transactions.
	 * <P>
	 * A string is then returned to indicate what was imported.  For example, the
	 * memorized transaction plug-in may return something like "6 memorized transactions".
	 * The messages from all extensions are combined into a list and used to confirm
	 * to the user what was imported.
	 * 
	 * @param qifFile
	 * @param session
	 * @param account the selected account or null if no single account was selected,
	 *      may be null but then the import will only succeed if the QIF file contains
	 *      account information before each transaction list
	 * @return a string describing what was imported, or null if
	 * 		nothing was in the file that could be imported by this
	 * 		implementation.
	 */
	String importData(QifFile qifFile, Session session, Account account) throws QifImportException;
}
