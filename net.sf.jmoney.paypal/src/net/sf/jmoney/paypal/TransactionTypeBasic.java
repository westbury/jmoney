package net.sf.jmoney.paypal;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.PlatformUI;

import net.sf.jmoney.importer.matcher.PatternMatch;
import net.sf.jmoney.importer.matcher.TransactionParamMetadata;
import net.sf.jmoney.importer.matcher.TransactionParamMetadataAccount;
import net.sf.jmoney.importer.matcher.TransactionParamMetadataString;
import net.sf.jmoney.importer.model.TransactionType;
import net.sf.jmoney.model2.Account;
import net.sf.jmoney.model2.Commodity;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.Transaction;

public class TransactionTypeBasic extends TransactionType<PaypalEntryData> {

	private TransactionParamMetadataString descriptionParam = new TransactionParamMetadataString("description", "Description");
	private TransactionParamMetadataString memoParam = new TransactionParamMetadataString("memo", "Memo");
	private TransactionParamMetadataAccount accountParam = new TransactionParamMetadataAccount("account", "Category", Account.class);
	private TransactionParamMetadataString transDateParam = new TransactionParamMetadataString("transDate", "Transaction Date");
	private TransactionParamMetadataString valueDateParam = new TransactionParamMetadataString("valueDate", "Value Date");

	public TransactionTypeBasic() {
		super("basic", "Basic");
	}

	@Override
	public List<TransactionParamMetadata> getParameters() {
		return Arrays.asList(new TransactionParamMetadata[] {
				descriptionParam,
				memoParam,
				accountParam,
				transDateParam,
				valueDateParam
		});
	}

	@Override
	public void createTransaction(Transaction transaction, Entry entry1, PaypalEntryData entryData, PatternMatch match) {

		Entry entry2 = transaction.createEntry();
		entry2.setAmount(-entryData.amount);



		// TODO sort out currency of account.
		/*
		 * Before setting the account, check that if a default
		 * account was previously set then the currency is the
		 * same.  The amount will end up being just plain wrong
		 * if we change the currency.

   				if (entry2.getAccount() != null) {
   					Commodity currencyBefore = entry2.getCommodity();
   	           		entry2.setAccount(pattern.getAccount());
   					Commodity currencyAfter = entry2.getCommodity();
   					if (currencyBefore != currencyAfter) {
   						MessageDialog.openError(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), "Problem Transaction", "Currency is being changed by pattern match.");
   						throw new RuntimeException("currency change on pattern match");
		 */


		String memo = memoParam.obtainValue(match);
		if (entry1 != null && memo != null) {
			entry1.setMemo(memo);
		}

		String description = descriptionParam.obtainValue(match);
		if (entry2 != null && description != null) {
			entry2.setMemo(description);
		}

		/*
		 * Before setting the account, check that if a default
		 * account was previously set then the currency is the
		 * same.  The amount will end up being just plain wrong
		 * if we change the currency.
		 */
		if (entry2.getAccount() != null) {
			Commodity currencyBefore = entry2.getCommodity();
			entry2.setAccount(accountParam.obtainAccount(match.pattern));
			Commodity currencyAfter = entry2.getCommodity();
			if (currencyBefore != currencyAfter) {
				MessageDialog.openError(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), "Problem Transaction", "Currency is being changed by pattern match.");
				throw new RuntimeException("currency change on pattern match");
			}
		} else {
			entry2.setAccount(accountParam.obtainAccount(match.pattern));
		}
	}

	@Override
	public Entry findMatch(PaypalEntryData entryData, Account account, int numberOfDays, Set<Entry> ourEntries) {
		// By default we delegate to the implementation in the EntryData.  This method can be overridden
		// if matching is to be done based on the transaction type as determined from the input data.
		return entryData.findMatch(account, numberOfDays, ourEntries);
	}
}
