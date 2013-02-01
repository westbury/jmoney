/*
 * 
 *  JMoney - A Personal Finance Manager
 *  Copyright (c) 2009 Nigel Westbury <westbury@users.sourceforge.net>
 * 
 * 
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 */

package net.sf.jmoney.ofx;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import net.sf.jmoney.importer.MatchingEntryFinder;
import net.sf.jmoney.importer.matcher.EntryData;
import net.sf.jmoney.importer.matcher.ImportMatcher;
import net.sf.jmoney.importer.matcher.PatternMatchingDialog;
import net.sf.jmoney.importer.model.PatternMatcherAccount;
import net.sf.jmoney.importer.model.PatternMatcherAccountInfo;
import net.sf.jmoney.isolation.TransactionManager;
import net.sf.jmoney.model2.Account;
import net.sf.jmoney.model2.BankAccount;
import net.sf.jmoney.model2.Commodity;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.IDataManagerForAccounts;
import net.sf.jmoney.model2.ScalarPropertyAccessor;
import net.sf.jmoney.model2.Session;
import net.sf.jmoney.model2.Session.NoAccountFoundException;
import net.sf.jmoney.model2.Session.SeveralAccountsFoundException;
import net.sf.jmoney.model2.Transaction;
import net.sf.jmoney.model2.TransactionManagerForAccounts;
import net.sf.jmoney.ofx.model.OfxEntryInfo;
import net.sf.jmoney.ofx.parser.SimpleDOMParser;
import net.sf.jmoney.ofx.parser.SimpleElement;
import net.sf.jmoney.ofx.parser.TagNotFoundException;
import net.sf.jmoney.stocks.model.SecurityInfo;
import net.sf.jmoney.stocks.model.Stock;
import net.sf.jmoney.stocks.model.StockAccount;
import net.sf.jmoney.stocks.model.StockEntry;
import net.sf.jmoney.stocks.model.StockEntryInfo;
import net.sf.jmoney.stocks.model.StockInfo;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.IWorkbenchWindow;

public class OfxImporter {

	public static class OfxEntryData extends EntryData {

		public String fitid;

	}

	private IWorkbenchWindow window;
	
	public OfxImporter(IWorkbenchWindow window) {
		this.window = window;
	}

	/**
	 * 
	 * @param file
	 * @return true if all entries have either been imported or were not imported because they
	 * 			had already been imported, false if any entries in the file were not imported
	 * 			and have never previously been imported
	 */
	public boolean importFile(File file) {
		IDataManagerForAccounts sessionManager = (IDataManagerForAccounts)window.getActivePage().getInput();
		if (sessionManager == null) {
			MessageDialog waitDialog = new MessageDialog(
					window.getShell(),
					"Disabled Action Selected",
					null, // accept the default window icon
					"You cannot import data into an accounting session unless you have a session open.  You must first open a session or create a new session.",
					MessageDialog.ERROR,
					new String[] { IDialogConstants.OK_LABEL }, 0);
			waitDialog.open();
			return false;
		}

		BufferedReader buffer = null;
		try {
			/*
			 * Create a transaction to be used to import the entries.  This allows the entries to
			 * be more efficiently written to the back-end datastore and it also groups
			 * the entire import as a single change for undo/redo purposes.
			 */
			TransactionManagerForAccounts transactionManager = new TransactionManagerForAccounts(sessionManager);

			buffer = new BufferedReader(new FileReader(file));

			SimpleDOMParser parser = new SimpleDOMParser();
			SimpleElement rootElement = null;
			rootElement = parser.parse(buffer);
			
//			FileWriter fw = new FileWriter(new File("c:\\xml.xml"));
//			String xml = rootElement.toXMLString(0);
//			fw.append(xml);
//			fw.close();

			Session session = transactionManager.getSession();

			Session sessionOutsideTransaction = sessionManager.getSession();

			SimpleElement statementResultElement = rootElement.getDescendant("BANKMSGSRSV1", "STMTTRNRS", "STMTRS");
			if (statementResultElement != null) {
				boolean doImport = importBankStatement(transactionManager, rootElement, session,
						sessionOutsideTransaction, statementResultElement, false);
				if (!doImport) {
					return false;
				}
			} else {
				statementResultElement = rootElement.getDescendant("CREDITCARDMSGSRSV1", "CCSTMTTRNRS", "CCSTMTRS");
				if (statementResultElement != null) {
					boolean doImport = importBankStatement(transactionManager, rootElement, session,
							sessionOutsideTransaction, statementResultElement, true);
					if (!doImport) {
						return false;
					}
				} else {
					statementResultElement = rootElement.getDescendant("INVSTMTMSGSRSV1", "INVSTMTTRNRS", "INVSTMTRS");
					if (statementResultElement != null) {
						importStockStatement(transactionManager, rootElement, session,
								sessionOutsideTransaction, statementResultElement);
					} else {
						MessageDialog.openWarning(window.getShell(), "OFX file not imported", 
								MessageFormat.format(
										"{0} did not contain expected nodes for either a bank or a stock account.", 
										file.getName()));
						return false;
					}
				}
			}

			/*
			 * All entries have been imported and all the properties
			 * have been set and should be in a valid state, so we
			 * can now commit the imported entries to the datastore.
			 */
			if (transactionManager.hasChanges()) {
				String transactionDescription = MessageFormat.format("Import {0}", file.getName());
				transactionManager.commit(transactionDescription);									

				StringBuffer combined = new StringBuffer();
				combined.append(file.getName());
				combined.append(" was successfully imported. ");
				MessageDialog.openInformation(window.getShell(), "OFX file imported", combined.toString());
			} else {
				MessageDialog.openWarning(window.getShell(), "OFX file not imported", 
						MessageFormat.format(
								"{0} was not imported because all the data in it had already been imported.", 
								file.getName()));
			}
		} catch (IOException e) {
			MessageDialog.openError(window.getShell(), "Unable to read OFX file", e.getLocalizedMessage());
			return false;
		} catch (TagNotFoundException e) {
			MessageDialog.openError(window.getShell(), "Unable to read OFX file", e.getLocalizedMessage());
			return false;
		} finally {
			if (buffer != null) {
				try {
					buffer.close();
				} catch (IOException e) {
					// ignore if we can't close it
				}
			}
		}
		
		return true;
	}

	/**
	 * 
	 * @param transactionManager
	 * @param rootElement
	 * @param session
	 * @param sessionOutsideTransaction
	 * @param statementResultElement
	 * @param isCreditCard
	 * @return true if all the entries were imported, false if entries were not imported
	 * @throws TagNotFoundException
	 */
	// TODO change first parameter to be IDataManagerForAccounts and quit all this in and out
	// of transaction stuff.
	private boolean importBankStatement(TransactionManagerForAccounts transactionManager,
			SimpleElement rootElement, Session session,
			Session sessionOutsideTransaction,
			SimpleElement statementResultElement,
			boolean isCreditCard) throws TagNotFoundException {
		
		SimpleElement accountFromElement = 
				statementResultElement.getDescendant(isCreditCard ? "CCACCTFROM" : "BANKACCTFROM");
		String accountNumber = accountFromElement.getString("ACCTID");
		
		BankAccount account = null;
		BankAccount accountOutsideTransaction = null;
		for (Account eachAccount : sessionOutsideTransaction.getAccountCollection()) {
			if (eachAccount instanceof BankAccount) {
				BankAccount bankAccount= (BankAccount)eachAccount;
				if (accountNumber.equals(bankAccount.getAccountNumber())) {
					accountOutsideTransaction = bankAccount;
					account = transactionManager.getCopyInTransaction(accountOutsideTransaction);
				}
			}
		}
		
		if (account == null) {
			MessageDialog.openError(
					window.getShell(),
					"No Matching Account Found",
					"The OFX file contains data for bank account number " + accountNumber + ".  However no bank account exists with such an account number.  You probably need to set the account number property for the appropriate account.");
			return false;
		}

		/*
		 * If the OFX file specifies the currency for its entries, check
		 * that the currency matches the currency configured in JMoney for
		 * the account.
		 */
		SimpleElement currencyElement = rootElement.findElement("CURDEF");
		if (currencyElement != null) {
			String currencyCode = currencyElement.getTrimmedText();
			if (!account.getCurrency().getCode().equals(currencyCode)) {
				MessageDialog.openError(
						window.getShell(),
						"Currency Mismatch",
						MessageFormat.format(
								"A currency mismatch prevents the import.  The OFX file indicates it contains entries in {0} but the {2} account uses {1}.",
								currencyCode,
								account.getCurrency().getCode(),
								account.getName()
						)
				);
				return false;
			}
		}
		
		/*
		 * Get the set of ids that have already been imported
		 */
		Set<String> fitIds = new HashSet<String>();
		for (Entry entry : accountOutsideTransaction.getEntries()) {
			String fitId = OfxEntryInfo.getFitidAccessor().getValue(entry);
			if (fitId != null) {
				fitIds.add(fitId);
			}
		}
		
		SimpleElement transListElement = statementResultElement.getDescendant("BANKTRANLIST");

		Collection<OfxEntryData> importedEntries = new ArrayList<OfxEntryData>();

		/**
		 * A 'matcher' that will match if an entry has not already been matched
		 * to an imported entry and if the entry appears to match based on date,
		 * amount, and check number.
		 */
		MatchingEntryFinder matchFinder = new MatchingEntryFinder() {
			@Override
			protected boolean alreadyMatched(Entry entry) {
				return OfxEntryInfo.getFitidAccessor().getValue(entry) != null;
			}
		};
		
		for (SimpleElement transactionElement : transListElement.getChildElements()) {
			if (transactionElement.getTagName().equals("DTSTART")) {
				// ignore
			} else if (transactionElement.getTagName().equals("DTEND")) {
				// ignore
			} else if (transactionElement.getTagName().equals("STMTTRN")) {
				SimpleElement stmtTrnElement = transactionElement;

				Date postedDate = stmtTrnElement.getDate("DTPOSTED");
				Date transactionDate = postedDate;
				long amount = stmtTrnElement.getAmount("TRNAMT");
				String fitid = stmtTrnElement.getString("FITID");
				
				String checkNumber = stmtTrnElement.getString("CHECKNUM");
				if (checkNumber != null) {
//						checkNumber = checkNumber.trim(); // Is this needed???
					// QFX (or at least hsabank.com) sets CHECKNUM to zero even though not a check.
					// This is probably a bug at HSA Bank, but we ignore check numbers of zero.
					if (checkNumber.equals("0")) {
						checkNumber = null;
					}
				}

				// Hack - Citibank is hardcoded here
				String memo = null;
				if (account.getBank() != null && account.getBank().equals("Citibank UK")) {
					String name = stmtTrnElement.getString("NAME");
					if (name.equals("PURCHASE")) {
						memo = stmtTrnElement.getString("MEMO");
						Pattern compiledPattern;
						try {
							compiledPattern = Pattern.compile("(.*[^\\s])\\s*TRANS  (\\d\\d.\\d\\d.\\d\\d)");
							DateFormat df = new SimpleDateFormat("dd/MM/yy");
				   			Matcher m = compiledPattern.matcher(memo);
				   			System.out.println(compiledPattern + ", " + memo);
				   			if (m.matches()) {
				   				String transDateString = m.group(2);
				   				transactionDate = df.parse(transDateString);
				   				
				   				// If all succeeds, replace memo with part inside first brackets
				   				// (i.e. remove the transaction date from the memo)
				   				memo = m.group(1);
				   			}
						} catch (PatternSyntaxException e) {
							compiledPattern = null;
						} catch (ParseException e) {
							// Bad date so just ignore it.
						}
					} else if (name.startsWith("GBP CHEQUE")) {
						// Naughty Citibank don't use the proper field for the cheque number
						// Extract it from the "NAME" field.
						Pattern compiledPattern;
						try {
							compiledPattern = Pattern.compile("GBP CHEQUE\\s*(\\d\\d\\d\\d\\d\\d)");
				   			Matcher m = compiledPattern.matcher(name);
				   			if (m.matches()) {
				   				checkNumber = m.group(1);
				   			}
						} catch (PatternSyntaxException e) {
							compiledPattern = null;
						}
					} else {
						memo = stmtTrnElement.getString("MEMO");
					}
				} else if (account.getBank() != null && account.getBank().equals("Nationwide")) {
					String name = stmtTrnElement.getString("NAME");
//					if (name.startsWith("DIRECTDEBIT: ")) {
//						memo = 
//					}
					memo = name;
				} else {
					memo = stmtTrnElement.getString("NAME");
				}
				
				if (fitIds.contains(fitid)) {
					// This transaction has been previously imported.
					// We ignore it.
					continue;
				}

				/*
				 * First we try auto-matching.
				 * 
				 * If we have an auto-match then we don't have to create a new
				 * transaction at all. We just update a few properties in the
				 * existing entry.
				 */
				Entry match = matchFinder.findMatch(accountOutsideTransaction, amount, transactionDate, checkNumber);
				if (match != null) {
					Entry entryInTrans = transactionManager.getCopyInTransaction(match);
					entryInTrans.setValuta(postedDate);
					entryInTrans.setCheck(checkNumber);
					OfxEntryInfo.getFitidAccessor().setValue(entryInTrans, fitid);
				} else {
					/*
					 * No existing entry matches, either on FITID or by matching dates and amounts,
					 * so we need to create a new transaction.
					 */
					OfxEntryData entryData = new OfxEntryData();
					entryData.amount = amount;
					entryData.check = checkNumber;
					entryData.valueDate = transactionDate;
					entryData.clearedDate = postedDate;
					entryData.setMemo(memo);
					entryData.setPayee(memo);  // Does OFX have payee field?
					entryData.fitid = fitid;
					
					importedEntries.add(entryData);
				}
			} else {
				System.out.println("Unknown element ignored: " + transactionElement.getTagName());
				String elementXml = transactionElement.toXMLString(0);
				System.out.println(elementXml);
			}
		}
		
		/*
		 * Import the entries using the matcher dialog
		 */
		
		PatternMatcherAccount matcherAccount = account.getExtension(PatternMatcherAccountInfo.getPropertySet(), true);
		
		Dialog dialog = new PatternMatchingDialog(window.getShell(), matcherAccount, importedEntries);
		if (dialog.open() == Dialog.OK) {
			ImportMatcher matcher = new ImportMatcher(account.getExtension(PatternMatcherAccountInfo.getPropertySet(), true));

			for (OfxEntryData entryData: importedEntries) {
				Entry entry = matcher.process(entryData, transactionManager.getSession());
				OfxEntryInfo.getFitidAccessor().setValue(entry, entryData.fitid);
			}
			
			return true;
		} else {
			return false;
		}
	}

	private void importStockStatement(TransactionManager transactionManager,
			SimpleElement rootElement, Session session,
			Session sessionOutsideTransaction,
			SimpleElement statementResultElement) throws TagNotFoundException {
		SimpleElement accountFromElement = statementResultElement.getDescendant("INVACCTFROM");
		String accountNumber = accountFromElement.getString("ACCTID");
		
		StockAccount account = null;
		StockAccount accountOutsideTransaction = null;
		for (Account eachAccount : sessionOutsideTransaction.getAccountCollection()) {
			if (eachAccount instanceof StockAccount) {
				StockAccount stockAccount= (StockAccount)eachAccount;
				if (accountNumber.equals(stockAccount.getAccountNumber())) {
					accountOutsideTransaction = stockAccount;
					account = transactionManager.getCopyInTransaction(accountOutsideTransaction);
				}
			}
		}
		
		if (account == null) {
			MessageDialog.openError(
					window.getShell(),
					"No Matching Account Found",
					"The OFX file contains data for brokerage account number " + accountNumber + ".  However no stock account exists with such an account number.  You probably need to set the account number property for the appropriate account.");
			return;
		}

		StockAccount worthlessStockAccountOutsideTransaction;
		try {
			worthlessStockAccountOutsideTransaction = (StockAccount)sessionOutsideTransaction.getAccountByShortName("worthless stock and options");
		} catch (SeveralAccountsFoundException e) {
			throw new RuntimeException(e);
		} catch (NoAccountFoundException e) {
			throw new RuntimeException(e);
		}
		StockAccount worthlessStockAccount = transactionManager.getCopyInTransaction(worthlessStockAccountOutsideTransaction);

		/*
		 * If the OFX file specifies the currency for its entries, check
		 * that the currency matches the currency configured in JMoney for
		 * the account.
		 */
		SimpleElement currencyElement = rootElement.findElement("CURDEF");
		if (currencyElement != null) {
			String currencyCode = currencyElement.getTrimmedText();
			if (!account.getCurrency().getCode().equals(currencyCode)) {
				MessageDialog.openError(
						window.getShell(),
						"Currency Mismatch",
						MessageFormat.format(
								"A currency mismatch prevents the import.  The OFX file indicates it contains entries in {0} but the {2} account uses {1}.",
								currencyCode,
								account.getCurrency().getCode(),
								account.getName()
						)
				);
				return;
			}
		}
		
		if (account.getDividendAccount() == null) {
			MessageDialog.openError(
					window.getShell(),
					"Account Not Configured",
					"The " + account.getName() + " account does not have an account set to hold the dividend payments.  Select the " + account.getName() + " from the Navigator view, then open the properties view and select a dividend account.");
			return;
		}
		
		if (account.getCommissionAccount() == null) {
			MessageDialog.openError(
					window.getShell(),
					"Account Not Configured",
					"The " + account.getName() + " account does not have an account set to hold the commission amounts.  Select the " + account.getName() + " from the Navigator view, then open the properties view and select a commission account.");
			return;
		}

		/*
		 * The tax 1 account is used for anything marked as 'fees'.
		 */
		if (account.getTax1Account() == null) {
			MessageDialog.openError(
					window.getShell(),
					"Account Not Configured",
					"The " + account.getName() + " account does not have an account set to hold the tax 1 payments.  Select the " + account.getName() + " from the Navigator view, then open the properties view and select a tax 1 account.");
			return;
		}
		
		/*
		 * We update our security list before importing the transactions.
		 * 
		 * We could do this afterwards and things would work fairly well.  The transaction
		 * import would create a stock object for each CUSIP, giving it a default name
		 * which is basically the CUSIP.  The the actual name and ticker symbol will be filled
		 * in when the securities list is imported.
		 * 
		 * However, when importing transactions, financial institutions often put the stock name
		 * or ticker symbol in the memo field.  This results in a duplication of information and
		 * also makes it harder to perform pattern matching on the memo field.  We therefore
		 * replace these with '<stock name>', '<ticker>', etc.  To do that, we need to know the
		 * stock name and ticker when we import the transactions.  Hence, to save a second pass
		 * through the transactions, we import the securities first. 
		 */
		SimpleElement secList = rootElement.getDescendant("SECLISTMSGSRSV1", "SECLIST");
		for (SimpleElement securityElement : secList.getChildElements()) {
			if (securityElement.getTagName().equals("STOCKINFO")
					|| securityElement.getTagName().equals("MFINFO")) {
				SimpleElement secInfoElement = securityElement.findElement("SECINFO");
				SimpleElement secIdElement = securityElement.findElement("SECID");

				String name = toTitleCase(secInfoElement.getString("SECNAME"));
				String symbol = secInfoElement.getString("TICKER");

				Stock stock = findStock(session, secIdElement);
				
				String defaultName = secIdElement.getString("UNIQUEIDTYPE") + ": " + secIdElement.getString("UNIQUEID");
				if (stock.getName().equals(defaultName)) {
					stock.setName(name);
				}
				
				if (stock.getSymbol() == null) {
					stock.setSymbol(symbol);
				}
			} else {
				System.out.println("unknown element in SECLIST");
				String elementXml = securityElement.toXMLString(0);
				System.out.println(elementXml);
			}
		}

		/*
		 * Get the set of ids that have already been imported
		 */
		Set<String> fitIds = new HashSet<String>();
		for (Entry entry : accountOutsideTransaction.getEntries()) {
			String fitId = OfxEntryInfo.getFitidAccessor().getValue(entry);
			if (fitId != null) {
				fitIds.add(fitId);
			}
		}
		
		SimpleElement transListElement = statementResultElement.getDescendant("INVTRANLIST");

		ImportMatcher matcher = new ImportMatcher(account.getExtension(PatternMatcherAccountInfo.getPropertySet(), true));
		
		for (SimpleElement transactionElement : transListElement.getChildElements()) {
			if (transactionElement.getTagName().equals("DTSTART")) {
				// ignore
			} else if (transactionElement.getTagName().equals("DTEND")) {
				// ignore
			} else if (transactionElement.getTagName().equals("INVBANKTRAN")) {

				SimpleElement stmtTrnElement = transactionElement.findElement("STMTTRN");

				Date postedDate = stmtTrnElement.getDate("DTPOSTED");
				long amount = stmtTrnElement.getAmount("TRNAMT");
				String fitid = stmtTrnElement.getString("FITID");
				String memo = stmtTrnElement.getString("MEMO");
				
				String checkNumber = stmtTrnElement.getString("CHECKNUM");
				if (checkNumber != null) {
//						checkNumber = checkNumber.trim(); // Is this needed???
					// QFX (or at least hsabank.com) sets CHECKNUM to zero even though not a check.
					// This is probably a bug at HSA Bank, but we ignore check numbers of zero.
					if (checkNumber.equals("0")) {
						checkNumber = null;
					}
				}

				if (fitIds.contains(fitid)) {
					// This transaction has been previously imported.
					// We ignore it.
					continue;
				}

				
				/*
				 * First we try auto-matching.
				 * 
				 * If we have an auto-match then we don't have to create a new
				 * transaction at all. We just update a few properties in the
				 * existing entry.
				 * 
				 */
				MatchingEntryFinder matchFinder = new MatchingEntryFinder() {
					@Override
					protected boolean alreadyMatched(Entry entry) {
						return OfxEntryInfo.getFitidAccessor().getValue(entry) != null;
					}
				};
				Entry matchedEntryOutsideTransaction = matchFinder.findMatch(accountOutsideTransaction, amount, postedDate, checkNumber);
				if (matchedEntryOutsideTransaction != null) {
					Entry matchedEntry = transactionManager.getCopyInTransaction(matchedEntryOutsideTransaction);
					matchedEntry.setValuta(postedDate);
					matchedEntry.setCheck(checkNumber);
					OfxEntryInfo.getFitidAccessor().setValue(matchedEntry, fitid);
					continue;
				}

				/*
				 * No existing entry matches, either on FITID or by matching dates and amounts,
				 * so we need to create a new transaction.
				 */
				Transaction transaction = session.createTransaction();

				Entry firstEntry = transaction.createEntry();
				firstEntry.setAccount(account);

				OfxEntryInfo.getFitidAccessor().setValue(firstEntry, fitid);

				transaction.setDate(postedDate);
				firstEntry.setValuta(postedDate);
				firstEntry.setAmount(amount);

				Entry otherEntry = transaction.createEntry();
				otherEntry.setAmount(-amount);
				
		   		/*
		   		 * Scan for a match in the patterns.  If a match is found,
		   		 * use the values for memo, description etc. from the pattern.
		   		 */
				String trnType = stmtTrnElement.getString("TRNTYPE");
				String textToMatch = MessageFormat.format(
						"TRNTYPE={0}\nMEMO={1}",
						trnType,
						memo);
				String defaultDescription = MessageFormat.format(
						"{0}: {1}",
						trnType.toLowerCase(),
						toTitleCase(memo));
				matcher.matchAndFill(textToMatch, firstEntry, otherEntry, toTitleCase(memo), defaultDescription);
			} else {
				// Assume a stock transaction

				SimpleElement invTransElement = transactionElement.findElement("INVTRAN");
				if (invTransElement == null) {
					String elementXml = transactionElement.toXMLString(0);
					System.out.println(elementXml);
					throw new RuntimeException("missing INVTRAN");
				}

				String fitid = invTransElement.getString("FITID");
				Date tradeDate = invTransElement.getDate("DTTRADE");
				Date settleDate = invTransElement.getDate("DTSETTLE");
				String memo = invTransElement.getString("MEMO");

				if (fitIds.contains(fitid)) {
					// This transaction has been previously imported.
					// We ignore it.
					continue;
				}

				// Create a new transaction
				Transaction transaction = session.createTransaction();

				Entry firstEntry = transaction.createEntry();
				firstEntry.setAccount(account);

				OfxEntryInfo.getFitidAccessor().setValue(firstEntry, fitid);

				SimpleElement secIdElement = transactionElement.findElement("SECID");
				Stock stock = findStock(session, secIdElement);

				/*
				 * When importing transactions, financial institutions often put
				 * the stock name or ticker symbol in the memo field. This
				 * results in a duplication of information and also makes it
				 * harder to perform pattern matching on the memo field. We
				 * therefore replace these with '<stock name>', '<ticker>', etc.
				 */
//					memo = memo.replace(stock.getName().toUpperCase(), "<stock name>");
				memo = memo.replace(stock.getName().toUpperCase(), "");
				if (stock.getSymbol() != null) {
					memo.replace(stock.getSymbol(), "<ticker>");
				}
				if (stock.getCusip() != null) {
					memo.replace(stock.getCusip(), "<CUSIP>");
				}
				
				transaction.setDate(tradeDate);
				firstEntry.setValuta(settleDate);

				/*
				 * TOTAL applies to all transaction types except JRNLSEC, CLOSUREOPT and TRANSFER.  JRNLSEC
				 * transaction types are used, for example, when one stock is replaced
				 * by another due to a re-org or something.  CLOSUREOPT is used when options expire worthless, TRANSFER is used when stock is moved
				 * in or out of the account so there is no currency amount involved.  We mustn't attempt to fetch
				 * the total because there isn't one and we would get an exception.
				 */
				long total = 0;
				if (!transactionElement.getTagName().startsWith("JRNLSEC")
						&& !transactionElement.getTagName().startsWith("CLOSUREOPT")
						&& !transactionElement.getTagName().startsWith("TRANSFER")) {
					total = transactionElement.getAmount("TOTAL");
					firstEntry.setAmount(total);
				} else {
					System.out.println("here");
				}

				firstEntry.setMemo(memo);

				if (transactionElement.getTagName().startsWith("BUY")
						|| transactionElement.getTagName().startsWith("SELL")) {

					String units = transactionElement.getString("UNITS");

					// TODO check that the unit price matches the price that would be
					// calculated from the other values?
					String unitPrice = transactionElement.getString("UNITPRICE");

					long commission = transactionElement.getAmount("COMMISSION", 0);
					if (commission != 0) {
						StockEntry commissionEntry = transaction.createEntry().getExtension(StockEntryInfo.getPropertySet(), true);
						commissionEntry.setAccount(account.getCommissionAccount());
						commissionEntry.setAmount(commission);
						commissionEntry.setSecurity(stock);
					}

					long fees = transactionElement.getAmount("FEES", 0);
					if (fees != 0) {
						StockEntry feesEntry = transaction.createEntry().getExtension(StockEntryInfo.getPropertySet(), true);
						feesEntry.setAccount(account.getTax1Account());
						feesEntry.setAmount(fees);
						feesEntry.setSecurity(stock);
					}

					if (units == null) {
						units = "1";   // TODO
					}
					Long quantity = stock.parse(units);

					StockEntry saleEntry = transaction.createEntry().getExtension(StockEntryInfo.getPropertySet(), true);
					saleEntry.setAccount(account);

					if (transactionElement.getTagName().startsWith("BUY")) {
						saleEntry.setAmount(quantity);
					} else {
						// For sales, units are negative in the OFX file, so it's the same
						saleEntry.setAmount(quantity);
					}

					saleEntry.setCommodity(stock);

					if (transactionElement.getTagName().equals("BUYMF")) {
						// Mutual fund purchase
					} else if (transactionElement.getTagName().equals("BUYSTOCK")) {
						// Stock purchase
					} else if (transactionElement.getTagName().equals("BUYOTHER")) {
						// Exchange traded fund purchase?
					} else if (transactionElement.getTagName().equals("SELLMF")) {
						// Mutual fund sale
					} else if (transactionElement.getTagName().equals("SELLSTOCK")) {
						// Stock sale
					} else if (transactionElement.getTagName().equals("SELLOTHER")) {
						// Exchange traded fund sale?
					} else {
						System.out.println("unknown element: " + transactionElement.getTagName());
						String elementXml = transactionElement.toXMLString(0);
						System.out.println(elementXml);
						throw new RuntimeException("unknown element: " + transactionElement.getTagName());
					}
				} else if (transactionElement.getTagName().equals("INCOME")
						|| transactionElement.getTagName().equals("REINVEST")) {
					
					String reinvestMemo = "";
					
					String incomeType = transactionElement.getString("INCOMETYPE");
					if ("DIV".equals(incomeType)) {
						StockEntry dividendEntry = transaction.createEntry().getExtension(StockEntryInfo.getPropertySet(), true);
						dividendEntry.setAccount(account.getDividendAccount());
						dividendEntry.setAmount(-total);
						dividendEntry.setMemo("dividend");
						dividendEntry.setSecurity(stock);
						reinvestMemo = " dividend";
					} else if ("CGLONG".equals(incomeType)) {
						StockEntry dividendEntry = transaction.createEntry().getExtension(StockEntryInfo.getPropertySet(), true);
						dividendEntry.setAccount(account.getDividendAccount());
						dividendEntry.setAmount(-total);
						dividendEntry.setMemo("capitial gains distribution - long term");
						dividendEntry.setSecurity(stock);
						reinvestMemo = " capital gains";
					} else {
						/*
						 * It might be "INTEREST", "MISC" or perhaps some other
						 * value. We defer to the user entered patterns to
						 * categorize these.
						 */
						Entry otherEntry = transaction.createEntry();
						otherEntry.setAmount(-total);
						
						String textToMatch = MessageFormat.format(
								"INCOMETYPE={0}\nMEMO={1}",
								incomeType,
								memo);
						String defaultDescription = MessageFormat.format(
								"{0}: {1}",
								incomeType.toLowerCase(),
								toTitleCase(memo));
						matcher.matchAndFill(textToMatch, firstEntry, otherEntry, toTitleCase(memo), defaultDescription);
					}
					
					/*
					 * If this is 'REINVEST' then we create a separate purchase transaction.
					 */
					if (transactionElement.getTagName().equals("REINVEST")) {
						Transaction reinvestTransaction = session.createTransaction();
						Entry firstReinvestEntry = reinvestTransaction.createEntry();
						firstReinvestEntry.setAccount(account);
						OfxEntryInfo.getFitidAccessor().setValue(firstReinvestEntry, fitid);

						reinvestTransaction.setDate(tradeDate);
						firstReinvestEntry.setValuta(settleDate);

						firstReinvestEntry.setAmount(total);

						firstReinvestEntry.setMemo("re-invest" + reinvestMemo);

						String units = transactionElement.getString("UNITS");

						/*
						 * Wells Fargo specifies a unit price of zero for
						 * re-invested gains. However we don't look at the unit
						 * price because we just store the currency cost and the
						 * number of share bought anyway.
						 */

						Long quantity = stock.parse(units);

						StockEntry buyEntry = reinvestTransaction.createEntry().getExtension(StockEntryInfo.getPropertySet(), true);
						buyEntry.setAccount(account);
						buyEntry.setAmount(quantity);
						buyEntry.setCommodity(stock);
					}
				} else if (transactionElement.getTagName().equals("TRANSFER")) {
					String units = transactionElement.getString("UNITS");
					Long quantity = stock.parse(units);

					/*
					 * Wells Fargo create TRANSFER entries with a quantity of zero.
					 * It is not known why these are created so we ignore any transfers
					 * with a zero quantity.
					 */
					if (quantity != 0) {
						/*
						 * JMoney does not allow stuff to just appear or
						 * disappear. Every credit must have a corresponding
						 * debit. Ideally we should probably have a special
						 * account that is used when we don't know where
						 * shares came from or where they went. However for
						 * the time being we move the shares back into the
						 * same account, which is not correct but it makes
						 * it easy for the user to manually edit the entry.
						 */							
						StockEntry firstStockEntry = firstEntry.getExtension(StockEntryInfo.getPropertySet(), true);
						
						firstStockEntry.setAmount(quantity);
						firstStockEntry.setCommodity(stock);

						StockEntry otherStockEntry = transaction.createEntry().getExtension(StockEntryInfo.getPropertySet(), true);
						otherStockEntry.setAccount(account);
						otherStockEntry.setAmount(-quantity);
						otherStockEntry.setCommodity(stock);
					}
				} else if (transactionElement.getTagName().equals("CLOSUREOPT")) {
					/*
					 * Options have expired worthless.  This is a rare case where we
					 * really do want assets to disappear into oblivion without a corresponding
					 * account to receive the assets.  We have two choices.  We either create a pseudo-account
					 * to receive the options or we relax our rule and create a transaction with a single entry.
					 * The problem with the first approach is that we have to understand that shares transferred
					 * to this special account are consider to be worthless.  We don't want reports to show that
					 * we still own them.  We could solve that one by the idea of ring-fenced accounts when producing
					 * reports, or just consider the account to be an expense account.
					 */
					
					String units = transactionElement.getString("UNITS");

					/*
					 * Wells Fargo specifies a unit price of zero for
					 * re-invested gains. However we don't look at the unit
					 * price because we just store the currency cost and the
					 * number of shares bought anyway.
					 */

					Long quantity = stock.parse(units);

					StockEntry firstStockEntry = firstEntry.getExtension(StockEntryInfo.getPropertySet(), true);
					
					firstStockEntry.setAmount(quantity);
					firstStockEntry.setCommodity(stock);

					StockEntry otherStockEntry = transaction.createEntry().getExtension(StockEntryInfo.getPropertySet(), true);
					otherStockEntry.setAccount(worthlessStockAccount);
					otherStockEntry.setAmount(-quantity);
					otherStockEntry.setCommodity(stock);
				} else if (transactionElement.getTagName().equals("JRNLSEC")) {
					/*
					 * We move the shares back into the same account, which is not correct
					 * but it makes it easy for the user to manually edit the entry.
					 */
					// TODO think of something better
					
					String units = transactionElement.getString("UNITS");

					/*
					 * Wells Fargo specifies a unit price of zero for
					 * re-invested gains. However we don't look at the unit
					 * price because we just store the currency cost and the
					 * number of share bought anyway.
					 */

					Long quantity = stock.parse(units);

					StockEntry firstStockEntry = firstEntry.getExtension(StockEntryInfo.getPropertySet(), true);
					
					firstStockEntry.setAmount(quantity);
					firstStockEntry.setCommodity(stock);

					StockEntry otherStockEntry = transaction.createEntry().getExtension(StockEntryInfo.getPropertySet(), true);
					otherStockEntry.setAccount(account);
					otherStockEntry.setAmount(-quantity);
					otherStockEntry.setCommodity(stock);
				} else {
					System.out.println("unknown element: " + transactionElement.getTagName());
					String elementXml = transactionElement.toXMLString(0);
					System.out.println(elementXml);
					throw new RuntimeException("unknown element: " + transactionElement.getTagName());
				}
			}
		}
	}

	private String toTitleCase(String text) {
		if (text == null) {
			return null;
		}
		
		String lowerCaseText = text.toLowerCase();
		char[] charArray = lowerCaseText.toCharArray();

		Pattern pattern = Pattern.compile("\\b([a-z])");
		Matcher matcher = pattern.matcher(lowerCaseText);

		while(matcher.find()) {
			int index = matcher.end(1) - 1;
			charArray[index] = Character.toUpperCase(charArray[index]);
		}

		return new String(charArray);
	}

	private Stock findStock(Session session, SimpleElement secIdElement) {
		String uniqueId = secIdElement.getString("UNIQUEID");
		String uniqueIdType = secIdElement.getString("UNIQUEIDTYPE");
		
		ScalarPropertyAccessor<String,? super Stock> securityIdField = null;
		if ("CUSIP".equals(uniqueIdType)) {
			securityIdField = SecurityInfo.getCusipAccessor();
		} else {
			// We don't recognize the id type, so use the symbol field
			// and hope it does not conflict with another use of the
			// symbol field.
			securityIdField = SecurityInfo.getSymbolAccessor();
		}

		if (uniqueId.length() == 0) {
			throw new RuntimeException("can this ever happen?");
		}

		Stock stock = null;
		for (Commodity commodity : session.getCommodityCollection()) {
			if (commodity instanceof Stock) {
				Stock eachStock = (Stock)commodity;
				if (uniqueId.equals(securityIdField.getValue(eachStock))) {
					stock = eachStock;
					break;
				}
			}
		}

		if (stock == null) {
			// Create it.
			stock = session.createCommodity(StockInfo.getPropertySet());
			if (securityIdField != null) {
				securityIdField.setValue(stock, uniqueId);
			}
			
			/*
			 * The name and ticker should be set later when the SECLIST element
			 * is processed.  However just in case that does not happen, we set
			 * a name because we mustn't create securities with blank names.
			 */
			stock.setName(uniqueIdType + ": " + uniqueId);
		}

		return stock;
	}
}
