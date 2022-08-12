package net.sf.jmoney.importer.model;

import java.text.DateFormat;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.PlatformUI;

import net.sf.jmoney.importer.matcher.EntryData;
import net.sf.jmoney.importer.matcher.PatternMatch;
import net.sf.jmoney.importer.matcher.TransactionParamMetadata;
import net.sf.jmoney.importer.matcher.TransactionParamMetadataAccount;
import net.sf.jmoney.importer.matcher.TransactionParamMetadataString;
import net.sf.jmoney.model2.Account;
import net.sf.jmoney.model2.Commodity;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.Transaction;

public class TransactionTypeBasic extends TransactionType<EntryData> {

	private TransactionParamMetadataString descriptionParam = new TransactionParamMetadataString("description", "Description");
	private TransactionParamMetadataString memoParam = new TransactionParamMetadataString("memo", "Memo");
	private TransactionParamMetadataAccount accountParam = new TransactionParamMetadataAccount("account", "Category", Account.class);
	private TransactionParamMetadataString checkParam = new TransactionParamMetadataString("check", "Check");
	private TransactionParamMetadataString transDateParam = new TransactionParamMetadataString("transDate", "Transaction Date");
	private TransactionParamMetadataString valueDateParam = new TransactionParamMetadataString("valueDate", "Value Date");

	private Pattern transDatePattern = Pattern.compile("(\\d\\d?)/(\\d\\d?)");
	
	public TransactionTypeBasic() {
		super("basic", "Basic");
	}

	@Override
	public List<TransactionParamMetadata> getParameters() {
		return Arrays.asList(new TransactionParamMetadata[] {
				descriptionParam,
				memoParam,
				accountParam,
				checkParam,
				transDateParam,
				valueDateParam
		});
	}

	@Override
	public void createTransaction(Transaction transaction, Entry entry1, EntryData entryData, PatternMatch match) {

		Entry entry2 = transaction.createEntry();
		entry2.setAmount(-entryData.amount);

		/*
		 * transaction date param, if set, must be set to mm/dd format. The year will be calculated
		 * and must not be in the param value even if it is available from the input text.
		 */
		String transDate = transDateParam.obtainValue(match);
		if (transDate != null) {
			Matcher matcher = transDatePattern.matcher(transDate);
			if (matcher.matches()) {
				int month = Integer.parseInt(matcher.group(1));
				int day = Integer.parseInt(matcher.group(2));
				
				Date date = transaction.getDate();
				
				// Date object is always based on local time, which is ok because the
				// Date as milliseconds is never persisted, and the timezone won't change
				// within a session (will it?)
				Calendar cal = Calendar.getInstance();
				cal.setTime(date);
				int originalMonth = cal.get(Calendar.MONTH);
				int year = cal.get(Calendar.YEAR);
				if (month == 12 && originalMonth == Calendar.JANUARY) {
					year--;
				} else if (month == 1 && originalMonth == Calendar.DECEMBER) {
					year++;
				}
				cal.set(year,  month - 1, day);

				// Set previous transaction date as value date only if no value date is already set
				if (entry1.getValuta() != null) {
					entry1.setValuta(transaction.getDate());
				}
				transaction.setDate(cal.getTime());
			}
		}
		
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
			 * The basic entry type has a check field but this is set only if the import format has a field specifically
			 * for a check number.  When BasicEntryType
			 * is used, check numbers can also be extracted from the memo using patterns set
			 * up by the user.
			 */
			String checkNumber = checkParam.obtainValue(match);
			if (entry1 != null && !checkNumber.isEmpty()) {
				/*
				 * If there is a check number field in the import format then the
				 * check number will already have been set.
				 */
				if (entry1.getCheck() != null && !entry1.getCheck().equals(checkNumber)) {
					throw new RuntimeException("mismatched check numbers");
				}
				entry1.setCheck(checkNumber);
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
	public Entry findMatch(EntryData entryData, Account account, int numberOfDays, Set<Entry> ourEntries) {
		// By default we delegate to the implementation in the EntryData.  This method can be overridden
		// if matching is to be done based on the transaction type as determined from the input data.
		return entryData.findMatch(account, numberOfDays, ourEntries);
	}
}
