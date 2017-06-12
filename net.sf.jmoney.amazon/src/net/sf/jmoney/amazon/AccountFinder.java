package net.sf.jmoney.amazon;

import java.util.Iterator;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;

import net.sf.jmoney.importer.wizards.ImportException;
import net.sf.jmoney.model2.BankAccount;
import net.sf.jmoney.model2.CapitalAccount;
import net.sf.jmoney.model2.Currency;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.IncomeExpenseAccount;
import net.sf.jmoney.model2.Session;
import net.sf.jmoney.model2.Transaction;

public class AccountFinder {

	private Session session;

	private Currency currency;

	public AccountFinder(Session session, String currencyCode) {
		this.session = session;
		this.currency = session.getCurrencyForCode(currencyCode);
	}

	public AccountFinder(Session session, Currency currency) {
		this.session = session;
		this.currency = currency;
	}

	public IncomeExpenseAccount findUnmatchedAccount()
			throws ImportException {
		/*
		 * Look for a category account that has a name that starts with "Amazon unmatched"
		 * and a currency that matches the currency of the charge account.
		 */
		IncomeExpenseAccount unmatchedAccount = null;
		for (Iterator<IncomeExpenseAccount> iter = session.getIncomeExpenseAccountIterator(); iter.hasNext(); ) {
			IncomeExpenseAccount eachAccount = iter.next();
			if (eachAccount.getName().startsWith("Amazon unmatched")
					&& eachAccount.getCurrency() == currency) {
				unmatchedAccount = eachAccount;
				break;
			}
		}
		if (unmatchedAccount == null) {
			throw new ImportException("No account exists with a name that begins 'Amazon unmatched' and a currency of " + currency.getName() + ".");
		}
		return unmatchedAccount;
	}

	public IncomeExpenseAccount findPostageAndPackagingAccount()
			throws ImportException {
		/*
		 * Look for a category account that has a name that starts with "Postage and Packaging"
		 * and a currency that matches the currency of the charge account.
		 */
		IncomeExpenseAccount postageAndPackagingAccount = null;
		for (Iterator<IncomeExpenseAccount> iter = session.getIncomeExpenseAccountIterator(); iter.hasNext(); ) {
			IncomeExpenseAccount eachAccount = iter.next();
			if (eachAccount.getName().startsWith("Postage and Packaging")
					&& eachAccount.getCurrency() == currency) {
				postageAndPackagingAccount = eachAccount;
				break;
			}
		}
		if (postageAndPackagingAccount == null) {
			throw new ImportException("No account exists with a name that begins 'Postage and Packaging' and a currency of " + currency.getName() + ".");
		}
		return postageAndPackagingAccount;
	}

	public BankAccount findGiftcardAccount() throws ImportException {
		BankAccount giftCardAccount = null;
		for (Iterator<CapitalAccount> iter = session.getCapitalAccountIterator(); iter.hasNext(); ) {
			CapitalAccount eachAccount = iter.next();
			if (eachAccount.getName().startsWith("Amazon gift")
					&& eachAccount.getCommodity(null) == currency) {
				giftCardAccount = (BankAccount)eachAccount;
				break;
			}
		}
		if (giftCardAccount == null) {
			throw new ImportException("No account exists with a name that begins 'Amazon gift' and has a currency of " + currency.getName() + ".");
		}
		
		return giftCardAccount;
	}

	public IncomeExpenseAccount findMiscellaneousAccount() throws ImportException {
		IncomeExpenseAccount account = null;
		for (Iterator<IncomeExpenseAccount> iter = session.getIncomeExpenseAccountIterator(); iter.hasNext(); ) {
			IncomeExpenseAccount eachAccount = iter.next();
			if (eachAccount.getName().startsWith("Misc U")
					&& eachAccount.getCommodity(null) == currency) {
				account = (IncomeExpenseAccount)eachAccount;
				break;
			}
		}
		if (account == null) {
			throw new ImportException("No account exists with a name that begins 'Misc U' and has a currency of " + currency.getName() + ".");
		}
		
		return account;
	}

	public static BankAccount findChargeAccount(Shell shell, Session session, String lastFourDigits)
			throws ImportException {
		BankAccount chargedAccount = null;
		for (Iterator<CapitalAccount> iter = session.getCapitalAccountIterator(); iter.hasNext(); ) {
			CapitalAccount eachAccount = iter.next();
			if (eachAccount instanceof BankAccount) {
				BankAccount eachBankAccount = (BankAccount)eachAccount;
				String accountNumber = eachBankAccount.getAccountNumber();
				if (accountNumber != null && accountNumber.endsWith(lastFourDigits)) {
					chargedAccount = eachBankAccount;
					break;
				}
			}
		}
		if (chargedAccount == null) {
			boolean result = MessageDialog.openQuestion(shell, "Account not Found",
					"No account exists with an account number ending with " + lastFourDigits + "."
							+ "  Do you want to skip this one and continue importing the rest?  Press 'No' to cancel the entire import.");
			if (!result) {
				throw new ImportException("Import cancelled due to missing account.");
			}
		}
		return chargedAccount;
	}

	/*
	 * Look for an income and expense account that can be used by default for the purchases.
	 * The currency of this account must match the currency of the charge account.
	 */
	public IncomeExpenseAccount findDefaultPurchaseAccount() throws ImportException {
		IncomeExpenseAccount defaultPurchaseAccount = null;
		for (Iterator<IncomeExpenseAccount> iter = session.getIncomeExpenseAccountIterator(); iter.hasNext(); ) {
			IncomeExpenseAccount eachAccount = iter.next();
			if (eachAccount.getName().startsWith("Amazon purchase")
					&& eachAccount.getCurrency() == currency) {
				defaultPurchaseAccount = eachAccount;
				break;
			}
		}
		if (defaultPurchaseAccount == null) {
			throw new ImportException("No account exists with a name that begins 'Amazon purchase' and a currency of " + currency.getName() + ".");
		}
		return defaultPurchaseAccount;
	}

	/**
	 * Look for an income and expense account that can be used for returned
	 * items (both the original sale and the return).
	 * <P>
	 * When items are returned, we keep both the original transaction with the
	 * sale and create a second transaction for the return. In both cases the
	 * income/expense category for the item is set to a special 'returns'
	 * account (a special 'returns' account allows us to keep details of the
	 * item which we need to maintain the integrity of further imports, but also
	 * keep the reports of other income/expense accounts clean).
	 */
	public IncomeExpenseAccount findReturnedItemsAccount() throws ImportException {
		IncomeExpenseAccount returnedItemsAccount = null;
		for (Iterator<IncomeExpenseAccount> iter = session.getIncomeExpenseAccountIterator(); iter.hasNext(); ) {
			IncomeExpenseAccount eachAccount = iter.next();
			if (eachAccount.getName().startsWith("Returned items")
					&& eachAccount.getCurrency() == currency) {
				returnedItemsAccount = eachAccount;
				break;
			}
		}
		if (returnedItemsAccount == null) {
			throw new ImportException("No account exists with a name that begins 'Returned items' and a currency of " + currency.getName() + ".");
		}
		return returnedItemsAccount;
	}

	static public void assertValid(Transaction trans) {
		long total = 0;
		System.out.println("" );

		for (Entry entry : trans.getEntryCollection()) {
			System.out.println("" + entry.getAmount());
			total += entry.getAmount();
		}
		if (total != 0) {
			throw new RuntimeException("unbalanced");
		}
	}

}
