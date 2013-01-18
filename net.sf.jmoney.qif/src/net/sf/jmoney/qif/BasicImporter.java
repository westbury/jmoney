package net.sf.jmoney.qif;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.sf.jmoney.isolation.ReferenceViolationException;
import net.sf.jmoney.model2.Account;
import net.sf.jmoney.model2.BankAccountInfo;
import net.sf.jmoney.model2.CapitalAccount;
import net.sf.jmoney.model2.Currency;
import net.sf.jmoney.model2.CurrencyAccount;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.IncomeExpenseAccount;
import net.sf.jmoney.model2.IncomeExpenseAccountInfo;
import net.sf.jmoney.model2.Session;
import net.sf.jmoney.model2.Transaction;
import net.sf.jmoney.qif.parser.QifAccount;
import net.sf.jmoney.qif.parser.QifCategoryLine;
import net.sf.jmoney.qif.parser.QifDate;
import net.sf.jmoney.qif.parser.QifFile;
import net.sf.jmoney.qif.parser.QifSplitTransaction;
import net.sf.jmoney.qif.parser.QifTransaction;

public class BasicImporter implements IQifImporter {

	/**
	 * A local copy of all bank accounts in the current session, stored by name.
	 * Before use accountMap must be initialized by calling the buildAccountMap
	 * method
	 */
	private Map<String, CapitalAccount> accountMap = new HashMap<String, CapitalAccount>();

	/**
	 * A local copy of all categories in the current session, stored by name.
	 * Before use this must be initialized by calling the buildCategoryMap
	 * method
	 */
	private Map<String, IncomeExpenseAccount> categoryMap = new HashMap<String, IncomeExpenseAccount>();

	/**
	 * Creates a temporary map of all the accounts in the given session using
	 * the account's name as the key.
	 */
	private void buildAccountMap(Session session) {
		for (Account account: session.getAccountCollection()) {
			if (account instanceof CapitalAccount) {
				CapitalAccount capitalAccount = (CapitalAccount)account;
				accountMap.put(account.getName(), capitalAccount);
			}
		}
	}

	/**
	 * Creates a temporary map of all the categories in the given session using
	 * the categories' names as keys.
	 */
	private void buildCategoryMap(Session session) {
		for (Account account: session.getAccountCollection()) {
			if (account instanceof IncomeExpenseAccount) {
				IncomeExpenseAccount category = (IncomeExpenseAccount) account;
				categoryMap.put(category.getName(), category);
			}
		}
	}

	public String importData(QifFile qifFile, Session session, Account selectedAccount) {

		buildAccountMap(session);
		buildCategoryMap(session);

		/*
		 * Import transactions that have no account information.
		 */
		if (!qifFile.transactions.isEmpty()) {
			if (selectedAccount == null) {
				throw new RuntimeException("No account selected and transactions are listed in the QIF file with no account information.");
			}
			
			if (!(selectedAccount instanceof CurrencyAccount)) {
				// TODO: process error properly
				if (QIFPlugin.DEBUG) System.out.println("account is not a currency account");
				throw new RuntimeException("selected account is not a currency account");
			}

			CurrencyAccount currencyAccount = (CurrencyAccount)selectedAccount;

			importAccount(session, currencyAccount, qifFile.transactions, false);
		}
		
		/*
		 * Import transactions that do have account information.
		 */
		for (QifAccount qifAccount : qifFile.accountList) {

			if (qifAccount.getTransactions().size() == 0) {
				/*
				 * This account has no normal (non-investment) transactions. It
				 * may be an account definition that does not include
				 * transactions, or it may be an investment account. Either way,
				 * we don't process it because here we are only importing normal
				 * transactions.
				 */
				continue;
			}
			CapitalAccount account = getAccount(qifAccount.getName(), session);
			if (!(account instanceof CurrencyAccount)) {
				// TODO: process error properly
				if (QIFPlugin.DEBUG) System.out.println("account is not a currency account");
				throw new RuntimeException("account is not a currency account");
			}

			CurrencyAccount currencyAccount = (CurrencyAccount)account;
			currencyAccount.setStartBalance(qifAccount.startBalance);

			importAccount(session, currencyAccount, qifAccount.getTransactions(), true);
		}

		return "some transactions";
	}

	/**
	 * Imports an account from a QIF-file.
	 * <P>
	 * As soon as a split category, memo, or amount is found when one has
	 * already been specified for the split, a new split is created. If split
	 * lines are specified then any category specified in the 'L' line is
	 * ignored.  The first split is put into the entry that had been initially
	 * created for the category.
	 * <P>
	 * Split entries that involve transfers are complicated.  One or more of the
	 * splits in a split entry may be a transfer.  When a transfer is in a split,
	 * the QIF export of the other account in the transfer will not show the split.
	 * It will show only a simple transfer.  (At least, that is how MS-Money exports
	 * QIF data).  When we see a transfer, and we find a match indicating that the
	 * transfer is already is the datastore, we must see whether either end of the
	 * transfer is a split transaction and we must be sure to keep the split entries.
	 * Normally, when we find a transfer is already in the datastore, we simply leave
	 * it there and delete our transaction.  However, if our transaction is a split
	 * entry then we instead keep our transaction and delete the other transaction.
	 * In the latter case, it is possible that additional data has been set in the
	 * other transaction and we must copy that data across to our transaction.
	 * <P>
	 * If the transaction in this account is split, and there are one or more transfers
	 * in the split entries, and if the other account in a transfer has already been imported,
	 * then an entry will have been entered into this account for the transfer amount of that
	 * split.  If there are multiple transfers in the split then multiple entries will exist
	 * in this account.  All of those entries and their transactions must be deleted.
	 * 
	 * @param useQuickenCategories true if this is an import of entries with account information
	 * 			in which case a category is expected in the QIF file and will be used, false if
	 * 			this is an import of entries downloaded from a bank with no account information
	 * 			in which case no category is expected and auto-matching will be used
	 */
	private void importAccount(Session session, CurrencyAccount account,
			List<QifTransaction> transactions, boolean useQuickenCategories) {
		
		// TODO: This should come from the account????
		Currency currency = session.getDefaultCurrency();

		for (QifTransaction qifTransaction : transactions) {
			// Create a new transaction
			Transaction transaction = session.createTransaction();

			// Add the first entry for this transaction and set the account
			QIFEntry firstEntry = transaction.createEntry().getExtension(QIFEntryInfo.getPropertySet(), true);
			firstEntry.setAccount(account);

			transaction.setDate(convertDate(qifTransaction.getDate()));
			long amount = adjustAmount(qifTransaction.getAmount(), currency);
			firstEntry.setAmount(amount);
			firstEntry.setReconcilingState(qifTransaction.getStatus());
			firstEntry.setCheck(qifTransaction.getCheckNumber());
			firstEntry.setMemo(qifTransaction.getPayee());

			// Process the category
			if (qifTransaction.getSplits().size() == 0) {
				// Add the second entry for this transaction
				Entry secondEntry = transaction.createEntry();

				secondEntry.setAmount(-amount);

				if (useQuickenCategories) {
					QifCategoryLine categoryLine = qifTransaction.getCategory();
					if (categoryLine == null) {
						throw new RuntimeException("When transactions are listed in the QIF file with account information, there must be category information.");
					}
					Account category = findCategory(session, categoryLine);
					secondEntry.setAccount(category);

					if (category instanceof CapitalAccount) {
						// isTransfer = true;
					} else {
						IncomeExpenseAccount incomeExpenseCategory = (IncomeExpenseAccount)category;
						if (incomeExpenseCategory.isMultiCurrency()) {
							secondEntry.setIncomeExpenseCurrency(currency);
						} else {
							/*
							 * Quicken categories are (I think) always
							 * multi-currency. This means that under the quicken
							 * model, all expenses are in the same currency as the
							 * account from which the expense came. For example, I
							 * am visiting a customer in Europe and I incur a
							 * business expense in Euro, but I charge to my US
							 * dollar billed credit card. Under the JMoney model,
							 * the expense category for the client can be set to
							 * 'Euro only' and the actual cost in Euro may be
							 * entered, resulting in an expense report for the European
							 * client that has all amounts in Euro exactly matching
							 * the receipts. The Quicken model, however, is
							 * problematic. The expense shows up in US dollars. The
							 * report may translate at some exchange rate, but the
							 * amounts on the expense report will then not match the
							 * receipts.
							 * 
							 * This gives us a problem in this import. If the
							 * currency of the bank account does not match the
							 * currency of the category then we do not have
							 * sufficient information. Quicken only gives us the
							 * amount in the currency of the bank account.
							 */
							if (!incomeExpenseCategory.getCurrency().equals(currency)) {
								// TODO: resolve this.  For time being, the amount is set even though
								// the currency is different, thus assuming an exchange rate of
								// one to one.
							}
						}
					}
				} else {
					if (qifTransaction.getCategory() != null) {
						throw new RuntimeException("When transactions are listed in the QIF file with no account information (downloaded from bank), there must not be any category information.");
					}
					// TODO: Auto-import here
					IncomeExpenseAccount category = getCategory("Unknown Category", session);
				}
			}	


			firstEntry.setMemo(qifTransaction.getMemo());

			String address = null;
			for (String line : qifTransaction.getAddressLines()) {
				if (address == null) {
					address = line;
				} else {
					address = address + '\n' + line; 
				}
			}
			firstEntry.setAddress(address);


			// Split transactions.
			for (QifSplitTransaction qifSplit : qifTransaction.getSplits()) {					
				Entry splitEntry = transaction.createEntry();
				splitEntry.setAccount(findCategory(session, qifSplit.getCategory()));
				splitEntry.setMemo(qifSplit.getMemo());
				splitEntry.setAmount(-adjustAmount(qifSplit.getAmount(), currency));
			}

			// If we have a transfer then we need to search through the other
			// account to see if a matching entry exists and then keep only one
			// (if one is a split transaction, we should keep that one, otherwise
			// it does not matter which we keep so keep the old one).

			for (Iterator iter = transaction.getEntryCollection().iterator(); iter.hasNext(); ) {
				Entry entry = (Entry)iter.next();
				if (!entry.equals(firstEntry)) {

					// Force a category in each account.
					// This is required by the JMoney data model.
					if (entry.getAccount() == null) {
						entry.setAccount(getCategory("Unknown Category", session));
					}

					if (entry.getAccount() instanceof IncomeExpenseAccount) {
						// If this entry is for a multi-currency account,
						// set the currency to be the same as the currency for this
						// bank account.
						if (((IncomeExpenseAccount)entry.getAccount()).isMultiCurrency()) {
							entry.setIncomeExpenseCurrency(currency);
						}
					}

					if (entry.getAccount() instanceof CapitalAccount) {
						Entry oldEntry = findMatch(account, transaction.getDate(), -entry.getAmount(), transaction);
						if (oldEntry != null) {
							if (transaction.hasMoreThanTwoEntries()) {
								// Our transaction is split.  The other should
								// not be, so delete the other transaction,
								// leaving only our transaction.
								Transaction  oldTransaction = oldEntry.getTransaction(); 
								if (oldTransaction.hasMoreThanTwoEntries()) {
									// We have problems.  Both are split.
									// For time being, leave both, but we should
									// alert the user or something.  Actually, this
									// should not happen (at least MS-Money does not seem
									// to allow this to happen), so perhaps it does not
									// really matter what we do.
								} else {
									// Copy some of the properties across from the old
									// before we delete it.

									Entry oldOtherEntry = oldTransaction.getOther(oldEntry);									
									entry.setCheck(oldOtherEntry.getCheck());
									entry.setValuta(oldOtherEntry.getValuta());

									try {
										session.deleteTransaction(oldTransaction);
									} catch (ReferenceViolationException e) {
										/*
										 * Neither transactions nor entries or any other object type
										 * contained in a transaction can have references to them. Therefore
										 * this exception should not happen. It is possible that third-party
										 * plug-ins might extend the model in a way that could cause this
										 * exception, in which case we probably will need to think about how
										 * we can be more user-friendly.
										 */
										throw new RuntimeException("This is an unlikely error and should not happen unless plug-ins are doing something complicated.", e);
									}
								}
							} else {
								// Delete the transaction that we have created,
								// leaving only the existing transaction.
								try {
									session.deleteTransaction(transaction);
								} catch (ReferenceViolationException e) {
									/*
									 * Neither transactions nor entries or any other object type
									 * contained in a transaction can have references to them. Therefore
									 * this exception should not happen. It is possible that third-party
									 * plug-ins might extend the model in a way that could cause this
									 * exception, in which case we probably will need to think about how
									 * we can be more user-friendly.
									 */
									throw new RuntimeException("This is an unlikely error and should not happen unless plug-ins are doing something complicated.", e);
								}

								// We must stop processing because this transaction
								// is now dead.
								break;
							}
						}
					}
				}
			}
		}
	}

	private Date convertDate(QifDate date) {
		Calendar calendar = Calendar.getInstance();
		calendar.clear();
		calendar.set(Calendar.YEAR, date.getYear());
		calendar.set(Calendar.MONTH, date.getMonth()-1);
		calendar.set(Calendar.DAY_OF_MONTH, date.getDay());
		return calendar.getTime();
	}

	private long adjustAmount(BigDecimal amount, Currency currency) {
		// TODO: revisit this method.
		return amount.movePointRight(currency.getDecimals()).longValue();
	}

	/**
	 * Find an entry in this account that has the given date and amount.
	 * 
	 * @param date
	 * @param amount
	 * @param ourTransaction
	 *            When we look for a match, ignore this transaction. This is the
	 *            transaction we have added so of course it will match. We are
	 *            looking for another transaction that matches.
	 */
	private Entry findMatch(CapitalAccount capAccount, Date date, long amount, Transaction ourTransaction) {
		Collection otherEntries = capAccount.getEntries();
		for (Iterator iter = otherEntries.iterator(); iter.hasNext();) {
			Entry otherEntry = (Entry) iter.next();
			Transaction otherTransaction = otherEntry.getTransaction();
			if (!otherTransaction.equals(ourTransaction)) {
				// Transaction dates must match
				// Entry amounts must match
				if (otherTransaction.getDate().equals(date)
						&& otherEntry.getAmount() == amount) {
					return otherEntry;
				}
			}
		}
		return null;
	}

	/**
	 * Returns the account in "session" associated with the category specified
	 * by "line".
	 */
	private Account findCategory(Session session, QifCategoryLine qifCategoryLine) {
		switch (qifCategoryLine.getType()) {
		case CategoryType:
			return getCategory(qifCategoryLine.getName(), session);
		case SubCategoryType:
			IncomeExpenseAccount category = getCategory(qifCategoryLine.getName(), session);
			return getSubcategory(qifCategoryLine.getSubCategoryName(), category);
		case TransferType:
			return getAccount(qifCategoryLine.getName(), session);
		}
		throw new RuntimeException("bad case");
	}

	/**
	 * Returns the account with the specified name. If there is no account in
	 * the session with that name then a new account is created
	 * 
	 * @param name
	 *            the name of account to get
	 * @param session
	 *            the session to check for the account
	 */
	private CapitalAccount getAccount(String name, Session session) {
		// Test to see if we have an account with the same name in our map
		CapitalAccount account = accountMap.get(name);
		// If not then create a new account, set the name and add it to the map
		if (account == null) {
			account = session.createAccount(BankAccountInfo.getPropertySet());
			account.setName(name);
			accountMap.put(name, account);
		}
		return account;
	}

	/**
	 * Returns the category with the specified name. If it doesn't exist a new
	 * category will be created.
	 */
	private IncomeExpenseAccount getCategory(String name, Session session) {
		IncomeExpenseAccount category;
		category = categoryMap.get(name);
		if (category == null) {
			category = session.createAccount(IncomeExpenseAccountInfo.getPropertySet());
			category.setName(name);
			categoryMap.put(name, category);
		}
		return category;
	}

	/**
	 * Returns the sub-category with the specified name. If it doesn't exist a
	 * new sub-category will be created. We don't use a map for sub categories
	 * instead we just iterate through them trying to find a match.
	 */
	private IncomeExpenseAccount getSubcategory(
			String name,
			IncomeExpenseAccount category) {

		for (IncomeExpenseAccount subcategory : category.getSubAccountCollection()) {
			if (subcategory.getName().equals(name))
				return subcategory;
		}

		IncomeExpenseAccount subcategory = category.createSubAccount();
		subcategory.setName(name);
		return subcategory;
	}

}
