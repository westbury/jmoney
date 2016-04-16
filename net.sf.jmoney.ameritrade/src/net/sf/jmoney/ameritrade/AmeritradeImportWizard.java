/*
 * 
 *  JMoney - A Personal Finance Manager
 *  Copyright (c) 2004,2009 Nigel Westbury <westbury@users.sourceforge.net>
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

package net.sf.jmoney.ameritrade;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import net.sf.jmoney.associations.AssociationMetadata;
import net.sf.jmoney.importer.model.ReconciliationEntryInfo;
import net.sf.jmoney.importer.wizards.CsvImportToAccountWizard;
import net.sf.jmoney.importer.wizards.CsvTransactionReader;
import net.sf.jmoney.importer.wizards.ImportException;
import net.sf.jmoney.importer.wizards.MultiRowTransaction;
import net.sf.jmoney.model2.Account;
import net.sf.jmoney.model2.Commodity;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.IDatastoreManager;
import net.sf.jmoney.model2.Session;
import net.sf.jmoney.model2.Session.NoAccountFoundException;
import net.sf.jmoney.model2.Session.SeveralAccountsFoundException;
import net.sf.jmoney.model2.Transaction;
import net.sf.jmoney.stocks.model.Bond;
import net.sf.jmoney.stocks.model.BondInfo;
import net.sf.jmoney.stocks.model.Security;
import net.sf.jmoney.stocks.model.Stock;
import net.sf.jmoney.stocks.model.StockAccount;
import net.sf.jmoney.stocks.model.StockEntry;
import net.sf.jmoney.stocks.model.StockEntryInfo;
import net.sf.jmoney.stocks.model.StockInfo;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWizard;

/**
 * A wizard to import data from a comma-separated file that has been downloaded
 * from Ameritrade.
 */
public class AmeritradeImportWizard extends CsvImportToAccountWizard implements IWorkbenchWizard {

	private ImportedDateColumn column_date                     = new ImportedDateColumn("DATE", new SimpleDateFormat("MM/dd/yyyy"));
	private ImportedTextColumn column_uniqueId                 = new ImportedTextColumn("TRANSACTION ID");
	private ImportedTextColumn column_description              = new ImportedTextColumn("DESCRIPTION");
	private ImportedTextColumn column_quantityString           = new ImportedTextColumn("QUANTITY");
	private ImportedTextColumn column_symbol                   = new ImportedTextColumn("SYMBOL");
	private ImportedTextColumn column_price                    = new ImportedTextColumn("PRICE");
	private ImportedAmountColumn column_commission             = new ImportedAmountColumn("COMMISSION");
	private ImportedAmountColumn column_amount                 = new ImportedAmountColumn("AMOUNT");
	private ImportedAmountColumn column_balance                = new ImportedAmountColumn("NET CASH BALANCE");
	private ImportedAmountColumn column_regFee                 = new ImportedAmountColumn("REG FEE");
	private ImportedAmountColumn column_shortTermRedemptionFee = new ImportedAmountColumn("SHORT-TERM RDM FEE");
	private ImportedAmountColumn column_fundRedemptionFee      = new ImportedAmountColumn("FUND REDEMPTION FEE");
	private ImportedAmountColumn column_deferredSalesCharge    = new ImportedAmountColumn("DEFERRED SALES CHARGE");

	Pattern patternAdr;
	Pattern patternForeignTax;
	Pattern patternBondInterest;
	Pattern patternBondInterestRate;
	Pattern patternBondMaturityDate;
	Pattern patternMandatoryReverseSplit;
	BondSalePatternMatcher patternBondSale;

//	static {
//		try {
//			patternAdr = Pattern.compile("ADR FEES \\([A-Z]*\\)");
//			patternForeignTax = Pattern.compile("FOREIGN TAX WITHHELD \\([A-Z]*\\)");
//		} catch (PatternSyntaxException e) {
//			throw new RuntimeException("pattern failed"); 
//		}
//	}

	private StockAccount account;
	
	private Account interestAccount;

	private Account expensesAccount;

	private Account foreignTaxAccount;

	// Don't put this here.  Methods should all be using sessionInTransaction
	private Session session;

	private static DateFormat maturityDateFormat = new SimpleDateFormat("MM/dd/yyyy"); 

	public AmeritradeImportWizard() {
		// TODO check these dialog settings are used by the base class
		// so the default filename location is separate for each import type.
		IDialogSettings workbenchSettings = Activator.getDefault().getDialogSettings();
		IDialogSettings section = workbenchSettings.getSection("AmeritradeImportWizard");//$NON-NLS-1$
		if (section == null) {
			section = workbenchSettings.addNewSection("AmeritradeImportWizard");//$NON-NLS-1$
		}
		setDialogSettings(section);
	}

	/**
	 * This form of this method is called when the wizard is initiated from the
	 * 'import' menu.  No account is available from the context so we search
	 * for a Paypal account.  If there is more than one then we fail (it would
	 * be better to add a page that prompts the user for the Paypal account to
	 * use, or perhaps try to identify the account from the imported file).
	 */
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
		Account account;

		IDatastoreManager sessionManager = (IDatastoreManager)window.getActivePage().getInput();
		
		session = sessionManager.getSession();
		
		if (selection.size() == 1
				&& selection.getFirstElement() instanceof StockAccount) {
			account = (StockAccount)selection.getFirstElement();
		} else {
			try {
				account = session.getAccountByShortName("Ameritrade");
			} catch (NoAccountFoundException e) {
				MessageDialog.openError(Display.getDefault().getActiveShell(), "Account not Set Up", "No account exists called 'Paypal'");
				throw new RuntimeException(e); 
			} catch (SeveralAccountsFoundException e) {
				MessageDialog.openError(Display.getDefault().getActiveShell(), "Multiple Accounts Set Up", "Multiple accounts exists called 'Paypal'");
				throw new RuntimeException(e); 
			}
		}

		if (!(account instanceof StockAccount)) {
			MessageDialog.openError(Display.getDefault().getActiveShell(), "Account not Set Up", "The account called 'Paypal' must be a Paypal account.");
			throw new RuntimeException("Selected Ameritrade account not a stock account"); 
		}
		
		init(window, account);
	}

	@Override
	protected void setAccount(Account accountInsideTransaction)	throws ImportException {
		if (!(accountInsideTransaction instanceof StockAccount)) {
			throw new ImportException("Bad configuration: This import can be used for stock accounts only.");
		}
		
		this.account = (StockAccount)accountInsideTransaction;
		this.session = accountInsideTransaction.getSession();

		interestAccount = getAssociatedAccount("net.sf.jmoney.ameritrade.interest");
		expensesAccount = getAssociatedAccount("net.sf.jmoney.ameritrade.expenses");
		foreignTaxAccount = getAssociatedAccount("net.sf.jmoney.ameritrade.foreigntaxes");
		
		try {
			patternAdr = Pattern.compile("ADR FEES \\([A-Z]*\\)");
			patternForeignTax = Pattern.compile("FOREIGN TAX WITHHELD \\([A-Z]*\\)");
			patternBondInterest = Pattern.compile("INTEREST INCOME - SECURITIES \\(([0-9,A-Z]*)\\) (.*)");
			patternBondInterestRate = Pattern.compile(" coupon\\: (\\d\\.\\d\\d)\\%");
			patternBondMaturityDate = Pattern.compile(" maturity: (\\d\\d/\\d\\d/20\\d\\d)");
			patternMandatoryReverseSplit = Pattern.compile("MANDATORY REVERSE SPLIT \\(([0-9,A-Z]*)\\)");
			patternBondSale = new BondSalePatternMatcher();
		} catch (PatternSyntaxException e) {
			throw new RuntimeException("pattern failed"); 
		}
	}
	
	@Override
	public void importLine(CsvTransactionReader reader) throws ImportException {
		Date date = column_date.getDate();
		String uniqueId = column_uniqueId.getText();
		String memo = column_description.getText();
		String quantityString = column_quantityString.getText();
		String security = column_symbol.getText();
		String price = column_price.getText();
		Long commission = column_commission.getAmount();
		Long total = column_amount.getAmount();
		long salesFee = column_regFee.getNonNullAmount();
		long shortTermRedemptionFee = column_shortTermRedemptionFee.getNonNullAmount();
		long fundRedemptionFee = column_fundRedemptionFee.getNonNullAmount();
		long deferredSalesCharge = column_deferredSalesCharge.getNonNullAmount();
		
		/*
		 * See if an entry already exists with this uniqueId.
		 */
		for (Entry entry : account.getEntries()) {
			if (uniqueId.equals(ReconciliationEntryInfo.getUniqueIdAccessor().getValue(entry))) {
				// This row has already been imported so ignore it.
				return;
			}
		}

		/*
		 * For some extraordinary reason, every entry has the net amount in the 'AMOUNT' column
		 * except for bond interest entries and the tax withholding entries on bond interest.
		 * Those have the new amount in the 'SHORT-TERM RDM FEE' column.  This is probably a bug
		 * in the Ameritrade export code, which seems to output a mess to say the least.  So if
		 * the 'AMOUNT' column is empty, use the 'SHORT-TERM RDM FEE' column. 
		 */
		if (total == null || total.longValue() == 0) {
			total = shortTermRedemptionFee;
		}
		
        long totalSalesFee = salesFee + deferredSalesCharge;
        long totalRedemptionFee = shortTermRedemptionFee + fundRedemptionFee;

			Matcher matcher;

			if (memo.startsWith("Sold ") || memo.startsWith("Bought ")) {

				Security stockOrBond;

				/*
				 * If there is no symbol then assume it is a bond purchase or sale.
				 * Ameritrade rather fall short when it comes to supplying information.
				 * They don't even give any information indicating what is being purchased!
				 * We therefore put in an entry to purchase something with a name of
				 * 'unknown bond'.  The transaction can later be manually edited
				 * to specify the correct bond. 
				 */
				if (security.isEmpty()) {
					if (memo.equals("Bought " + quantityString + "M @ " + price)) {
						// This is a bond purchase

						Bond unknownBond = null;
						for (Commodity commodity : session.getCommodityCollection()) {
							if (commodity instanceof Bond) {
								Bond eachBond = (Bond)commodity;
								if (eachBond.getName().equals("unknown bond")) {
									unknownBond = eachBond;
									break;
								}
							}
						}
						if (unknownBond == null) {
							// Create it.
							unknownBond = session.createCommodity(BondInfo.getPropertySet());
							unknownBond.setName("unknown bond");
						}

						stockOrBond = unknownBond;
					} else {
						// Don't know what this one is.
						throw new RuntimeException("unknown row");
					}
					
					quantityString += "000";  // Denominate in dollars so the price is relative to par
				} else {
					stockOrBond = getStockBySymbol(session, security);
				}

				long quantity = stockOrBond.parse(quantityString);

				Transaction trans = session.createTransaction();
				trans.setDate(date);

				StockEntry mainEntry = createStockEntry(trans);
				mainEntry.setAccount(account);
				mainEntry.setCommodity(account.getCurrency());
				mainEntry.setAmount(total);
				ReconciliationEntryInfo.getUniqueIdAccessor().setValue(mainEntry.getBaseObject(), uniqueId);
				
				StockEntry saleEntry = createStockEntry(trans);
				saleEntry.setAccount(account);

				if (memo.startsWith("Bought ")) {
					saleEntry.setAmount(quantity);
				} else {
					saleEntry.setAmount(-quantity);
				}

				saleEntry.setCommodity(stockOrBond);

				if (commission != null && commission.longValue() != 0) {
					StockEntry commissionEntry = trans.createEntry().getExtension(StockEntryInfo.getPropertySet(), true);
					commissionEntry.setAccount(account.getCommissionAccount());
					commissionEntry.setAmount(commission);
					commissionEntry.setSecurity(stockOrBond);
				}

				if (totalSalesFee != 0) {
					StockEntry entry = trans.createEntry().getExtension(StockEntryInfo.getPropertySet(), true);
					entry.setAccount(account.getTax1Account());
					entry.setAmount(totalSalesFee);
					entry.setSecurity(stockOrBond);
				}

				if (totalRedemptionFee != 0) {
					StockEntry entry = trans.createEntry().getExtension(StockEntryInfo.getPropertySet(), true);
					entry.setAccount(account.getTax2Account());
					entry.setAmount(totalRedemptionFee);
					entry.setSecurity(stockOrBond);
				}

			} else if (patternBondSale.matches(memo)) {
				if (!security.isEmpty()) {
					throw new ImportException("Isn't there usually a security for bond disposals?");
				}
				security = patternBondSale.getSymbol();
				Security stockOrBond = findSecurityByCusip(session, security);
				
				boolean isDisposal = true;
				
				Transaction trans = session.createTransaction();
				trans.setDate(date);

				StockEntry mainEntry = createStockEntry(trans);
				mainEntry.setAccount(account);
				mainEntry.setCommodity(account.getCurrency());
				mainEntry.setAmount(total);
				ReconciliationEntryInfo.getUniqueIdAccessor().setValue(mainEntry.getBaseObject(), uniqueId);
				
				StockEntry saleEntry = createStockEntry(trans);
				saleEntry.setAccount(account);

				long quantity = stockOrBond.parse(quantityString);
				
				if (!isDisposal) {
					saleEntry.setAmount(quantity);
				} else {
					saleEntry.setAmount(-quantity);
				}

				saleEntry.setCommodity(stockOrBond);

				if (commission != null && commission.longValue() != 0) {
					StockEntry commissionEntry = trans.createEntry().getExtension(StockEntryInfo.getPropertySet(), true);
					commissionEntry.setAccount(account.getCommissionAccount());
					commissionEntry.setAmount(commission);
					commissionEntry.setSecurity(stockOrBond);
				}

				if (totalSalesFee != 0) {
					StockEntry entry = trans.createEntry().getExtension(StockEntryInfo.getPropertySet(), true);
					entry.setAccount(account.getTax1Account());
					entry.setAmount(totalSalesFee);
					entry.setSecurity(stockOrBond);
				}

				if (totalRedemptionFee != 0) {
					StockEntry entry = trans.createEntry().getExtension(StockEntryInfo.getPropertySet(), true);
					entry.setAccount(account.getTax2Account());
					entry.setAmount(totalRedemptionFee);
					entry.setSecurity(stockOrBond);
				}

				
			} else if ((matcher = patternBondInterest.matcher(memo)).matches()) {
				Transaction trans = session.createTransaction();
				trans.setDate(date);

				Entry mainEntry = trans.createEntry();
				mainEntry.setAccount(account);
				mainEntry.setMemo("bond interest");
				mainEntry.setCommodity(account.getCurrency());
				mainEntry.setAmount(total);
				ReconciliationEntryInfo.getUniqueIdAccessor().setValue(mainEntry, uniqueId);

				StockEntry otherEntry = trans.createEntry().getExtension(StockEntryInfo.getPropertySet(), true);
				otherEntry.setAccount(account.getDividendAccount());
				otherEntry.setMemo("bond interest");
				otherEntry.setAmount(-total);

				/*
				 * The 'stock' won't be set.  That is because for bonds, no symbol is put in the SYMBOL column.
				 * Instead ' maturity: 01/15/2016' is put in that column.  ' coupon: 5.35%' is put in the QUANTITY
				 * column.  We extract the cusip from the memo and look for the bond.  The bond may not already exist,
				 * especially as the bond purchase records do not give enough information to identify the bond.
				 * We therefore extract the cusip from the memo, use that to look up the bond, and if we don't find
				 * the bond, we extract the coupon interest rate and maturity date and create the bond.
				 */
				String cusip = matcher.group(1);
				Security security2 = findSecurityByCusip(session, cusip);
				if (security2 != null && !(security2 instanceof Bond)) {
					throw new RuntimeException("Bond sale entry, but cusip matches a non-bond security");
				}
				Bond bond = (Bond)security2;
				
				for (Commodity commodity : session.getCommodityCollection()) {
					if (commodity instanceof Bond) {
						Bond eachBond = (Bond)commodity;
						if (security.equals(eachBond.getCusip())) {
							bond = eachBond;
							break;
						}
					}
				}
				if (bond == null) {
					Matcher interestRateMatcher = patternBondInterestRate.matcher(quantityString);
					Matcher maturityDateMatcher = patternBondMaturityDate.matcher(security);

					if (interestRateMatcher.matches() && maturityDateMatcher.matches()) {
						// All matches so create the bond

						String maturityDateString = maturityDateMatcher.group(1);
						String interestRateString = interestRateMatcher.group(1);

						Date maturityDate;
						try {
							maturityDate = maturityDateFormat.parse(maturityDateString);
						} catch (ParseException e) {
							throw new ImportException("bad maturity date");
						}

						// Create it.  The name is not available in the import file,
						// so for time being we use the cusip as the name.
						bond = session.createCommodity(BondInfo.getPropertySet());
						bond.setName(cusip);
						bond.setCusip(cusip);
						bond.setMaturityDate(maturityDate);
						bond.setInterestRate(new BigDecimal(interestRateString).multiply(new BigDecimal(100)).intValueExact());
					} else {
						throw new RuntimeException("invalid bond interest");
					}
				}

				otherEntry.setSecurity(bond); 

			} else if (memo.startsWith("QUALIFIED DIVIDEND ")
					|| memo.startsWith("ORDINARY DIVIDEND ")) {
				Stock stock = getStockBySymbol(session, security);

				Transaction trans = session.createTransaction();
				trans.setDate(date);

				Entry mainEntry = trans.createEntry();
				mainEntry.setAccount(account);
				if (memo.startsWith("QUALIFIED DIVIDEND ")) {
					mainEntry.setMemo("qualified dividend");
				} else {
					mainEntry.setMemo("ordinary dividend");
				}
				mainEntry.setCommodity(account.getCurrency());
				mainEntry.setAmount(total);
				ReconciliationEntryInfo.getUniqueIdAccessor().setValue(mainEntry, uniqueId);

				StockEntry otherEntry = trans.createEntry().getExtension(StockEntryInfo.getPropertySet(), true);
				otherEntry.setAccount(account.getDividendAccount());
				if (memo.startsWith("QUALIFIED DIVIDEND ")) {
					otherEntry.setMemo("qualified");
				} else {
					otherEntry.setMemo("ordinary");
				}
				otherEntry.setAmount(-total);
				otherEntry.setSecurity(stock); 

				// Now seems to add (<symbol>) after the following text, so we use 'startsWith'	
			} else if (memo.equals("W-8 WITHHOLDING") || memo.startsWith("BACKUP WITHHOLDING (W-9)")) {
				/*
				 * Ameritrade might output a row for 'MONEY MARKET INTEREST' with an amount of zero,
				 * and if there is withholding tax then there will be an entry for that too with
				 * an amount of zero.
				 * JMoney does not like entries with zero amounts and users don't want to see them
				 * either so don't include these.
				 */
				if (total != 0) {
					Transaction trans = session.createTransaction();
					trans.setDate(date);

					Entry mainEntry = trans.createEntry();
					mainEntry.setAccount(account);
					mainEntry.setMemo("withholding");
					mainEntry.setCommodity(account.getCurrency());
					mainEntry.setAmount(total);
					ReconciliationEntryInfo.getUniqueIdAccessor().setValue(mainEntry, uniqueId);

					Entry otherEntry = trans.createEntry();
					otherEntry.setAccount(account.getWithholdingTaxAccount());
					if (memo.equals("W-8 WITHHOLDING")) {
						otherEntry.setMemo("W-8");
					} else {
						otherEntry.setMemo("W-9");
					}
					otherEntry.setAmount(-total);
				}
				//		        } else if (memo.startsWith("MANDATORY - NAME CHANGE ")) {

				//		        } else if (memo.startsWith("STOCK SPLIT ")) {

			} else if (memo.equals("FREE BALANCE INTEREST ADJUSTMENT")) {
				if (interestAccount == null) {
					throw new ImportException("A 'FREE BALANCE INTEREST ADJUSTMENT' transaction occurs but no category is set for 'Interest'. "
							+ "Go to the 'Account Associations' in the properties for the Ameritrade account and set the category to be used for interest payments.");
				}

				Transaction trans = session.createTransaction();
				trans.setDate(date);

				Entry mainEntry = trans.createEntry();
				mainEntry.setAccount(account);
				mainEntry.setMemo("free balance interest");
				mainEntry.setCommodity(account.getCurrency());
				mainEntry.setAmount(total);
				ReconciliationEntryInfo.getUniqueIdAccessor().setValue(mainEntry, uniqueId);

				Entry otherEntry = trans.createEntry();
				otherEntry.setAccount(interestAccount);
				otherEntry.setAmount(-total);
				//		        } else if (memo.equals("CLIENT REQUESTED ELECTRONIC FUNDING DISBURSEMENT (FUNDS NOW)")) {

				//		        } else if (memo.equals("WIRE OUTGOING (ACD WIRE DISBURSEMENTS)")) {

			} else if (memo.equals("MARGIN INTEREST ADJUSTMENT")) {
				if (interestAccount == null) {
					throw new ImportException("A 'MARGIN INTEREST ADJUSTMENT' transaction occurs but no category is set for 'Interest'. "
							+ "Go to the 'Account Associations' in the properties for the Ameritrade account and set the category to be used for interest payments.");
				}

				Transaction trans = session.createTransaction();
				trans.setDate(date);

				Entry mainEntry = trans.createEntry();
				mainEntry.setAccount(account);
				mainEntry.setMemo("margin interest");
				mainEntry.setCommodity(account.getCurrency());
				mainEntry.setAmount(total);
				ReconciliationEntryInfo.getUniqueIdAccessor().setValue(mainEntry, uniqueId);

				Entry otherEntry = trans.createEntry();
				otherEntry.setAccount(interestAccount);
				otherEntry.setAmount(-total);
			} else if (memo.equals("MONEY MARKET INTEREST (MMDA10)")) {
				/*
				 * Ameritrade might output a row for 'MONEY MARKET INTEREST' with an amount of zero.
				 * JMoney does not like entries with zero amounts and users don't want to see them
				 * either so don't include these.
				 */
				if (total != 0) {
					if (interestAccount == null) {
						throw new ImportException("A 'MONEY MARKET INTEREST' transaction occurs but no category is set for 'Interest'. "
								+ "Go to the 'Account Associations' in the properties for the Ameritrade account and set the category to be used for interest payments.");
					}

					Transaction trans = session.createTransaction();
					trans.setDate(date);

					Entry mainEntry = trans.createEntry();
					mainEntry.setAccount(account);
					mainEntry.setMemo("money market interest");
					mainEntry.setCommodity(account.getCurrency());
					mainEntry.setAmount(total);
					ReconciliationEntryInfo.getUniqueIdAccessor().setValue(mainEntry, uniqueId);

					Entry otherEntry = trans.createEntry();
					otherEntry.setAccount(interestAccount);
					otherEntry.setAmount(-total);
				}
			} else if (memo.startsWith("FOREIGN TAX WITHHELD ")) {
				if (foreignTaxAccount == null) {
					throw new ImportException("A 'FOREIGN TAX WITHHELD' transaction occurs but no account is set for 'Foreign Tax'. "
							+ "Go to the 'Account Associations' in the properties for the Ameritrade account and set the account to be used for foreign tax amounts.");
				}

				Stock stock = getStockBySymbol(session, security);

				Transaction trans = session.createTransaction();
				trans.setDate(date);

				Entry mainEntry = trans.createEntry();
				mainEntry.setAccount(account);
				mainEntry.setMemo("Foreign tax withheld");
				mainEntry.setCommodity(account.getCurrency());
				mainEntry.setAmount(total);
				ReconciliationEntryInfo.getUniqueIdAccessor().setValue(mainEntry, uniqueId);

				StockEntry otherEntry = trans.createEntry().getExtension(StockEntryInfo.getPropertySet(), true);
				otherEntry.setAccount(expensesAccount);
				otherEntry.setMemo("Foreign tax withheld???");
				otherEntry.setAmount(-total);
				otherEntry.setSecurity(stock);
			} else if ((matcher = patternAdr.matcher(memo)).matches()) {
				if (expensesAccount == null) {
					throw new ImportException("An 'ADR Fee' transaction occurs but no account is set for expenses. "
							+ "Go to the 'Account Associations' in the properties for the Ameritrade account and set the account to be used for expenses.");
				}

				Stock stock = getStockBySymbol(session, security);

				Transaction trans = session.createTransaction();
				trans.setDate(date);

				Entry mainEntry = trans.createEntry();
				mainEntry.setAccount(account);
				mainEntry.setMemo("ADR fee");
				mainEntry.setCommodity(account.getCurrency());
				mainEntry.setAmount(total);
				ReconciliationEntryInfo.getUniqueIdAccessor().setValue(mainEntry, uniqueId);

				StockEntry otherEntry = createStockEntry(trans);
				otherEntry.setAccount(expensesAccount);
				otherEntry.setMemo("ADR fee");
				otherEntry.setAmount(-total);
				otherEntry.setSecurity(stock);
			} else if (memo.equals("INTRA-ACCOUNT TRANSFER")) {
				MultiRowTransaction thisMultiRowProcessor = new IntraAccountTransfer(date, total);
				currentMultiRowProcessors.add(thisMultiRowProcessor);
			} else if (memo.equals("MONEY MARKET PURCHASE (MMDA10)")) {
				if (!"MMDA10".equals(column_symbol.getText())) {
					throw new ImportException("Security must be 'MMDA10' for security part of Money Market transfers.");
				};
				MultiRowTransaction thisMultiRowProcessor = new MoneyMarketPurchase(date, true);
				currentMultiRowProcessors.add(thisMultiRowProcessor);
			} else if (memo.equals("MONEY MARKET PURCHASE")) {
				MultiRowTransaction thisMultiRowProcessor = new MoneyMarketPurchase(date, false);
				currentMultiRowProcessors.add(thisMultiRowProcessor);
			} else if (memo.equals("MONEY MARKET REDEMPTION (MMDA10)")) {
				if (!"MMDA10".equals(column_symbol.getText())) {
					throw new ImportException("Security must be 'MMDA10' for security part of Money Market transfers.");
				};
				MultiRowTransaction thisMultiRowProcessor = new MoneyMarketRedemption(date, true);
				currentMultiRowProcessors.add(thisMultiRowProcessor);
			} else if (memo.equals("MONEY MARKET REDEMPTION")) {
				MultiRowTransaction thisMultiRowProcessor = new MoneyMarketRedemption(date, false);
				currentMultiRowProcessors.add(thisMultiRowProcessor);
			} else if ((matcher = patternMandatoryReverseSplit.matcher(memo)).matches()) {
				String extractedSecurity = matcher.group(1);

				if (!currentMultiRowProcessors.isEmpty()) {
					throw new ImportException("something is wrong");
				}

				MultiRowTransaction thisMultiRowProcessor = new MandatorySplit(session, date, extractedSecurity, quantityString);
				currentMultiRowProcessors.add(thisMultiRowProcessor);
			} else if (memo.startsWith("MANDATORY - EXCHANGE ")) {
				Stock stock = getStockBySymbol(session, security);

				/*
				 * These usually come in pairs, with the first being the old stock
				 * and the second entry being the new stock.  There is no other way of
				 * knowing which is which, except perhaps by looking to see what we currently
				 * have in the account.
				 */
				Long quantity = stock.parse(quantityString);

				if (!currentMultiRowProcessors.isEmpty()) {
					throw new ImportException("something is wrong");
				}

				MultiRowTransaction thisMultiRowProcessor = new MandatoryExchange(date, quantity, stock);
				currentMultiRowProcessors.add(thisMultiRowProcessor);
			} else {
				throw new ImportException("Entry found with unknown memo: '" + memo + "'.");
			}
	}

	private Stock getStockBySymbol(Session session, String symbol) {
		// Find the security
		Stock stock = null;
		if (symbol.length() != 0) {
			Security security = findSecurityBySymbol(session, symbol);
			
			if (security != null && !(security instanceof Stock)) {
				// This symbol is not a stock symbol but appears to be something else
				// such as a bond.
				throw new RuntimeException("mismatched symbol");
			}
			stock = (Stock)security;

			if (stock == null) {
				// Create it.  The name is not available in the import file,
				// so for time being we use the symbol as the name.
				stock = session.createCommodity(StockInfo.getPropertySet());
				stock.setName(symbol);
				stock.setSymbol(symbol);
			}
		}		        
		return stock;
	}

	private Security findSecurityBySymbol(Session session, String symbol) {
		for (Commodity commodity : session.getCommodityCollection()) {
			if (commodity instanceof Security) {
				Security eachSecurity = (Security)commodity;
				if (symbol.equals(eachSecurity.getSymbol())) {
					return eachSecurity;
				}
			}
		}
		return null;
	}

	private Security findSecurityByCusip(Session session, String cusip) {
		for (Commodity commodity : session.getCommodityCollection()) {
			if (commodity instanceof Security) {
				Security eachSecurity = (Security)commodity;
				if (cusip.equals(eachSecurity.getCusip())) {
					return eachSecurity;
				}
			}
		}
		return null;
	}

	private Stock findStockBySymbol(Session session, String symbol) {
		for (Commodity commodity : session.getCommodityCollection()) {
			if (commodity instanceof Stock) {
				Stock eachStock = (Stock)commodity;
				if (symbol.equals(eachStock.getSymbol())) {
					return eachStock;
				}
			}
		}
		return null;
	}

	private Stock findStockByCusip(Session session, String cusip) {
		for (Commodity commodity : session.getCommodityCollection()) {
			if (commodity instanceof Stock) {
				Stock eachStock = (Stock)commodity;
				if (cusip.equals(eachStock.getCusip())) {
					return eachStock;
				}
			}
		}
		return null;
	}

    public StockEntry createStockEntry(Transaction trans) {
    	return trans.createEntry().getExtension(StockEntryInfo.getPropertySet(), true);
	}
    
	public class MandatoryExchange implements MultiRowTransaction {

		private Date date;
		private long originalQuantity;
		private Stock originalStock;
		private long newQuantity;
		private Stock newStock;
		private Date fractionalSharesDate;
		private long fractionalSharesAmount;

		private boolean done = false;
		private String originalTransactionId;
		private String newTransactionId;

		/**
		 * Initial constructor called when first "Mandatory Exchange" row found.
		 * 
		 * @param date
		 * @param quantity
		 * @param stock
		 */
		public MandatoryExchange(Date date, long quantity, Stock stock) {
			this.date = date;
			this.originalQuantity = quantity;
			this.originalStock = stock;
			this.originalTransactionId = column_uniqueId.getText();
		}

		/**
		 * Called when second "Mandatory Exchange" row found.
		 * 
		 * @param quantity
		 * @param stock
		 */
		private void setReplacementStock(long quantity, Stock stock, String transactionId) {
			this.newQuantity = quantity;
			this.newStock = stock;
			this.newTransactionId = transactionId;
		}

		/**
		 * Called when an amount indicating it is paid in lieu of fractional
		 * shares is found.  The transaction can complete if this row is not
		 * found.
		 * 
		 * @param date
		 * @param total
		 */
		private void setCashForFractionalShares(Date date, long total) {
			this.fractionalSharesDate = date;
			this.fractionalSharesAmount = total;
		}

		public boolean processCurrentRow(Session session) throws ImportException {
			String memo = column_description.getText();
			String quantityString = column_quantityString.getText();
			String security = column_symbol.getText();
			Long total = column_amount.getAmount();

			
			if (memo.startsWith("MANDATORY - EXCHANGE ")) {
				Stock stock = getStockBySymbol(session, security);

				/*
				 * These usually come in pairs, with the first being the old stock
				 * and the second entry being the new stock.  There is no other way of
				 * knowing which is which, except perhaps by looking to see what we currently
				 * have in the account.
				 */
				Long quantity = stock.parse(quantityString);

				if (!this.date.equals(column_date.getDate())) {
					throw new ImportException("dates don't match");
				}
				setReplacementStock(quantity, stock, column_uniqueId.getText());

				return true;
			} else if (memo.startsWith("CASH IN LIEU OF FRACTIONAL SHARES ")) {
				setCashForFractionalShares(date, total);
				done = true;

				return true;
			}

			return false;
		}

		public void createTransaction(Session session) {
			Transaction trans = session.createTransaction();
			trans.setDate(date);

			StockEntry originalSharesEntry = trans.createEntry().getExtension(StockEntryInfo.getPropertySet(), true);
			originalSharesEntry.setAccount(account);
			originalSharesEntry.setAmount(-originalQuantity);
			originalSharesEntry.setCommodity(originalStock);
			originalSharesEntry.setMemo("mandatory exchange");
			ReconciliationEntryInfo.getUniqueIdAccessor().setValue(originalSharesEntry.getBaseObject(), originalTransactionId);

			StockEntry newSharesEntry = trans.createEntry().getExtension(StockEntryInfo.getPropertySet(), true);
			newSharesEntry.setAccount(account);
			newSharesEntry.setAmount(newQuantity);
			newSharesEntry.setCommodity(newStock);
			newSharesEntry.setMemo("mandatory exchange");
			ReconciliationEntryInfo.getUniqueIdAccessor().setValue(newSharesEntry.getBaseObject(), newTransactionId);

			if (fractionalSharesAmount != 0) {
				StockEntry fractionalEntry = trans.createEntry().getExtension(StockEntryInfo.getPropertySet(), true);
				fractionalEntry.setAccount(account);
				fractionalEntry.setAmount(fractionalSharesAmount);
				fractionalEntry.setValuta(fractionalSharesDate);
				fractionalEntry.setCommodity(account.getCurrency());
				fractionalEntry.setSecurity(newStock);
				fractionalEntry.setMemo("cash in lieu of fractional shares");
			} else {
				// We must have a currency entry in the account in order to see an entry.
				StockEntry fractionalEntry = trans.createEntry().getExtension(StockEntryInfo.getPropertySet(), true);
				fractionalEntry.setAccount(account);
				fractionalEntry.setCommodity(account.getCurrency());
				fractionalEntry.setAmount(fractionalSharesAmount);
				fractionalEntry.setMemo("exchange of stock");
			}
//			ReconciliationEntryInfo.getUniqueIdAccessor().setValue(fractionalEntry.getBaseObject(), uniqueId);
		}

		public boolean isDone() {
			return done;
		}
	}

	public class IntraAccountTransfer implements MultiRowTransaction {

		private Date date;
		private long priorAmount;

		private boolean done = false;

		/**
		 * Initial constructor called when first "INTRA-ACCOUNT TRANSFER" row found.
		 * 
		 * @param date
		 * @param amount
		 */
		public IntraAccountTransfer(Date date, long amount) {
			this.date = date;
			this.priorAmount = amount;
		}

		public boolean processCurrentRow(Session session) throws ImportException {
			String memo = column_description.getText();
			Long total = column_amount.getAmount();

			if (!this.date.equals(column_date.getDate())) {
				/*
				 * We appear to have moved to the next date without finding the
				 * second part of the transfer.
				 */
				throw new ImportException("Second part of transfer not found with same date.");
			}
			
			if (memo.equals("INTRA-ACCOUNT TRANSFER")) {
				if (priorAmount + total != 0) {
					throw new ImportException("Entry found with unbalanced account transfer: '" + memo + "'.");
				}

				done = true;
				return true;
			}

			return false;
		}

		public void createTransaction(Session session) {
			// The two entries just cancel so we don't do anything.
		}

		public boolean isDone() {
			return done;
		}
	}

	public class MoneyMarketPurchase implements MultiRowTransaction {

		private Date date;

		private boolean done = false;

		private boolean isSecurityPartFirst;

		/**
		 * Initial constructor called when first "MONEY MARKET PURCHASE" row found.
		 * 
		 * @param date
		 * @param isSecurityPart true if the part with the money market security came first,
		 * 			false if this is the part without the security
		 */
		public MoneyMarketPurchase(Date date, boolean isSecurityPart) {
			this.date = date;
			this.isSecurityPartFirst = isSecurityPart;
		}

		public boolean processCurrentRow(Session session) throws ImportException {
			String memo = column_description.getText();
			Long total = column_amount.getAmount();

			if (!this.date.equals(column_date.getDate())) {
				/*
				 * We appear to have moved to the next date without finding the
				 * second part of the transfer.
				 */
				throw new ImportException("Second part of transfer not found with same date.");
			}
			
			if (memo.equals("MONEY MARKET PURCHASE (MMDA10)")) {
				if (isSecurityPartFirst) {
					throw new ImportException("Two security parts in Money Market Redemption?");
				}
				
				if (total != 0) {
					throw new ImportException("Entry found with unexpected non-zero amount.");
				}
				
				if (!"MMDA10".equals(column_symbol.getText())) {
					throw new ImportException("Security must be 'MMDA10' for Money Market transfers.");
				};

				done = true;
				return true;
			} else if (memo.equals("MONEY MARKET PURCHASE")) {
				if (!isSecurityPartFirst) {
					throw new ImportException("Two cash parts in Money Market Redemption?");
				}

				if (total == 0) {
					throw new ImportException("Entry found with unexpected zero amount.");
				}

				done = true;
				return true;
			}
			return false;
		}

		public void createTransaction(Session session) {
			// The two entries just cancel so we don't do anything.
		}

		public boolean isDone() {
			return done;
		}
	}

	public class MoneyMarketRedemption implements MultiRowTransaction {

		private Date date;

		private boolean done = false;

		private boolean isSecurityPartFirst;

		/**
		 * Initial constructor called when first "MONEY MARKET PURCHASE" row found.
		 * 
		 * @param date
		 * @param isSecurityPart true if the part with the money market security came first,
		 * 			false if this is the part without the security
		 */
		public MoneyMarketRedemption(Date date, boolean isSecurityPart) {
			this.date = date;
			this.isSecurityPartFirst = isSecurityPart;
		}

		public boolean processCurrentRow(Session session) throws ImportException {
			String memo = column_description.getText();
			Long total = column_amount.getAmount();

			if (!this.date.equals(column_date.getDate())) {
				/*
				 * We appear to have moved to the next date without finding the
				 * second part of the transfer.
				 */
				throw new ImportException("Second part of transfer not found with same date.");
			}
			
			if (memo.equals("MONEY MARKET REDEMPTION (MMDA10)")) {
				if (isSecurityPartFirst) {
					throw new ImportException("Two security parts in Money Market Redemption?");
				}
				
				if (total != 0) {
					throw new ImportException("Entry found with unexpected non-zero amount.");
				}
				
				if (!"MMDA10".equals(column_symbol.getText())) {
					throw new ImportException("Security must be 'MMDA10' for Money Market transfers.");
				};

				done = true;
				return true;
			} else if (memo.equals("MONEY MARKET REDEMPTION")) {
				if (!isSecurityPartFirst) {
					throw new ImportException("Two cash parts in Money Market Redemption?");
				}

				if (total == 0) {
					throw new ImportException("Entry found with unexpected zero amount.");
				}

				done = true;
				return true;
			}

			return false;
		}

		public void createTransaction(Session session) {
			// The two entries just cancel so we don't do anything.
		}

		public boolean isDone() {
			return done;
		}
	}

	public class MandatorySplit implements MultiRowTransaction {

		private Date date;
		private long originalQuantity;
		private Stock originalStock;
		private String originalTransactionId;

		private long newQuantity;
		private Stock newStock;
		private String newTransactionId;

		private Date fractionalSharesDate;
		private long fractionalSharesAmount;

		private Pattern patternMandatoryReverseSplit;

		private boolean done = false;

		/**
		 * Initial constructor called when first "Mandatory Reverse Split" row found.
		 * 
		 * @param date
		 * @param extractedSecurity
		 * @param quantityString
		 */
		public MandatorySplit(Session sessionInTransaction, Date date, String extractedSecurity, String quantityString) {
			this.date = date;

			originalStock = getStockBySymbolOrCusip(sessionInTransaction,	extractedSecurity);

			/*
			 * These usually come in pairs, with the first being the old stock
			 * and the second entry being the new stock.  There is no other way of
			 * knowing which is which, except perhaps by looking to see what we currently
			 * have in the account.
			 */
			originalQuantity = originalStock.parse(quantityString);
			
			originalTransactionId = column_uniqueId.getText();
			
			try {
				patternMandatoryReverseSplit = Pattern.compile("MANDATORY REVERSE SPLIT \\(([0-9,A-Z]*)\\)");
			} catch (PatternSyntaxException e) {
				throw new RuntimeException("pattern failed", e); 
			}
		}

		/**
		 * Called when second "Mandatory Exchange" row found.
		 * 
		 * @param quantity
		 * @param stock
		 * @param string 
		 */
		private void setReplacementStock(long quantity, Stock stock, String transactionId) {
			this.newQuantity = quantity;
			this.newStock = stock;
			this.newTransactionId = transactionId;
		}

		/**
		 * Called when an amount indicating it is paid in lieu of fractional
		 * shares is found.  The transaction can complete if this row is not
		 * found.
		 * 
		 * @param date
		 * @param total
		 */
		private void setCashForFractionalShares(Date date, long total) {
			this.fractionalSharesDate = date;
			this.fractionalSharesAmount = total;
		}

		public boolean processCurrentRow(Session sessionInTransaction) throws ImportException {
			String memo = column_description.getText();
			String quantityString = column_quantityString.getText();
			long total = column_amount.getNonNullAmount();
			
			Matcher matcher = patternMandatoryReverseSplit.matcher(memo);
			if (matcher.matches()) {
				String security = matcher.group(1);
				Stock stock = getStockBySymbolOrCusip(sessionInTransaction,	security);

				/*
				 * These usually come in pairs, with the first being the old stock
				 * and the second entry being the new stock.  There is no other way of
				 * knowing which is which, except perhaps by looking to see what we currently
				 * have in the account.
				 */
				Long quantity = stock.parse(quantityString);

				if (!this.date.equals(date)) {
					throw new RuntimeException("dates don't match");
				}
				setReplacementStock(quantity, stock, column_uniqueId.getText());

				return true;
			} else if (memo.startsWith("CASH IN LIEU OF FRACTIONAL SHARES ")) {
				setCashForFractionalShares(date, total);
				done = true;

				return true;
			}

			done = true;
			return false;
		}

		public void createTransaction(Session sessionInTransaction) {
			Transaction trans = sessionInTransaction.createTransaction();
			trans.setDate(date);

			StockEntry originalSharesEntry = trans.createEntry().getExtension(StockEntryInfo.getPropertySet(), true);
			originalSharesEntry.setAccount(account);
			originalSharesEntry.setAmount(-originalQuantity);
			originalSharesEntry.setCommodity(originalStock);
			originalSharesEntry.setMemo("mandatory reverse split");
			ReconciliationEntryInfo.getUniqueIdAccessor().setValue(originalSharesEntry.getBaseObject(), originalTransactionId);

			StockEntry newSharesEntry = trans.createEntry().getExtension(StockEntryInfo.getPropertySet(), true);
			newSharesEntry.setAccount(account);
			newSharesEntry.setAmount(newQuantity);
			newSharesEntry.setCommodity(newStock);
			newSharesEntry.setMemo("mandatory reverse split");
			ReconciliationEntryInfo.getUniqueIdAccessor().setValue(newSharesEntry.getBaseObject(), newTransactionId);

			StockEntry fractionalEntry = trans.createEntry().getExtension(StockEntryInfo.getPropertySet(), true);
			fractionalEntry.setAccount(account);
			if (fractionalSharesAmount != 0) {
				fractionalEntry.setAmount(fractionalSharesAmount);
				fractionalEntry.setValuta(fractionalSharesDate);
				fractionalEntry.setCommodity(account.getCurrency());
				fractionalEntry.setSecurity(newStock);
				fractionalEntry.setMemo("cash in lieu of fractional shares");
			} else {
				// We must have a currency entry in the account in order to see an entry.
				fractionalEntry.setAmount(fractionalSharesAmount);
				fractionalEntry.setCommodity(account.getCurrency());
				fractionalEntry.setMemo("exchange of stock");
			}
//			ReconciliationEntryInfo.getUniqueIdAccessor().setValue(fractionalEntry.getBaseObject(), uniqueId);
		}

		private Stock getStockBySymbolOrCusip(Session sessionInTransaction,	String security) {
			Stock stock = findStockBySymbol(sessionInTransaction, security);
			if (stock == null) {
				stock = findStockByCusip(sessionInTransaction, security);
				if (stock == null) {
					// Create it.  The name is not available in the import file,
					// so for time being we use the symbol/cusip as the name.
					stock = sessionInTransaction.createCommodity(StockInfo.getPropertySet());
					stock.setName(security);

					/*
					 * Sometimes Ameritrade uses the ticker symbol and sometimes Ameritrade uses
					 * the cusip.  We assume it is a ticker symbol if the length is 5 or less.
					 */
					if (security.length() <= 5) {
						stock.setSymbol(security);
					} else {
						stock.setCusip(security);
					}
				}
			}
			return stock;
		}

		public boolean isDone() {
			return done;
		}
	}

	@Override
	protected ImportedColumn[] getExpectedColumns() {
		return new ImportedColumn [] {
				column_date,
				column_uniqueId,
				column_description,
				column_quantityString,
				column_symbol,
				column_price,
				column_commission,
				column_amount,
				column_balance,
				column_regFee,
				column_shortTermRedemptionFee,
				column_fundRedemptionFee,
				column_deferredSalesCharge
		};
	}

	@Override
	protected String getSourceLabel() {
		return "Ameritrade";
	}

	@Override
	public AssociationMetadata[] getAssociationMetadata() {
		return new AssociationMetadata[] {
				new AssociationMetadata("net.sf.jmoney.ameritrade.interest", "Interest Account"),
				new AssociationMetadata("net.sf.jmoney.ameritrade.expenses", "Expenses Account"),
				new AssociationMetadata("net.sf.jmoney.ameritrade.foreigntaxes", "Foreign Tax Account"),
		};
	}

	@Override
	public String getDescription() {
		return "The selected CSV file will be imported.  As you have not selected an account into which the import is to be made, " +
				"an investment account called 'Ameritrade' must exist and the data will be imported into that account. " +
				"The file must have been downloaded from Ameritrade for this import to work.  To download from Ameritrade, go to Statements, History. " +
				"If entries have already been imported, this import will not create duplicates.";
	}
}
