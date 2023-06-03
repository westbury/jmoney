package net.sf.jmoney.portfolioanalysis;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import jxl.Sheet;
import jxl.Workbook;
import jxl.read.biff.BiffException;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import jxl.write.WriteException;
import jxl.write.biff.RowsExceededException;
import net.sf.jmoney.model2.DatastoreManager;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.IncomeExpenseAccount;
import net.sf.jmoney.model2.Session;
import net.sf.jmoney.model2.Session.NoAccountFoundException;
import net.sf.jmoney.model2.Session.SeveralAccountsFoundException;
import net.sf.jmoney.pricehistory.Price;
import net.sf.jmoney.pricehistory.external.Prices;
import net.sf.jmoney.stocks.model.Security;
import net.sf.jmoney.stocks.model.StockAccount;
import net.sf.jmoney.stocks.model.StockEntry;
import net.sf.jmoney.stocks.model.StockEntryInfo;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;

public class PortfolioExportWizard extends Wizard implements IExportWizard {

	public static final String ID = "net.sf.jmoney.excel.accountexportwizard";

	private IWorkbenchWindow window;

	private AccountSelectionWizardPage accountSelectionPage;

	//	private FileSelectionWizardPage templateFilePage;

	private FileSelectionWizardPage outputFilePage;

	private Date startDate;
	private Date endDate;

	private IRepositoryService repositoryService;

	private Map<String, String> targetPrices = new HashMap<String, String>();

	public PortfolioExportWizard() {
		repositoryService = Activator.getDefault().getRepositoryService();

		IDialogSettings workbenchSettings = Activator.getDefault().getDialogSettings();
		IDialogSettings section = workbenchSettings.getSection("ExcelExportWizard");
		if (section == null) {
			section = workbenchSettings.addNewSection("ExcelExportWizard");
		}
		setDialogSettings(section);

		/*
		 * Get the ending year of the tax return. Always go to the prior year
		 * (i.e. tax return if never done before the end of the tax year and is
		 * never done more than a year after the end of the tax year).
		 */
		Calendar calendar = Calendar.getInstance();
		endDate = calendar.getTime();

		calendar = Calendar.getInstance();
		calendar.set(Calendar.YEAR, 2006);
		calendar.set(Calendar.MONTH, Calendar.DECEMBER);
		calendar.set(Calendar.DAY_OF_MONTH, 26);
		startDate = calendar.getTime();
	}

	public void init(IWorkbench workbench, IStructuredSelection selection) {
		this.window = workbench.getActiveWorkbenchWindow();
		DatastoreManager sessionManager = (DatastoreManager)window.getActivePage().getInput();
		Session session = sessionManager.getSession();

		//		accountSelectionPage = new AccountSelectionWizardPage(window);
		//		addPage(accountSelectionPage);

		String longTemplateDescription = 
			"Before exporting the data, you must have a template file.  This template file should have a sheet called 'property_template'. " +
			"This sheet will be replaced by muliple sheets, one for each property. " +
			"The template sheet should have a row in which the column A contains the text '<entry>'.  This row will be replaced by multiple rows, one row for each item of income or expense for the property.";

		//		templateFilePage = new FileSelectionWizardPage(window, "templatePage", "Select the template Excel file", longTemplateDescription);
		//		addPage(templateFilePage);

		outputFilePage = new FileSelectionWizardPage(window, "outputPage", "Choose the output Excel file", "You must select the file to write the output.");
		addPage(outputFilePage);
	}

	@Override
	public boolean performFinish() {

		//		Collection<IncomeExpenseAccount> propertyAccounts = accountSelectionPage.getSelectedAccounts();

		//		String templateFileName = templateFilePage.getFileName();
		//		File templateFile = new File(templateFileName);

		String outputFileName = outputFilePage.getFileName();
		File outputFile = new File(outputFileName);

		try {
			outputFile(outputFile, null /*propertyAccounts */);
		} catch (RowsExceededException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (WriteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (BiffException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return true;
	}

	public void outputFile(File outputFile, Collection<IncomeExpenseAccount> propertyAccounts) throws IOException, RowsExceededException, WriteException, BiffException {

		//		Workbook templateWorkbook = Workbook.getWorkbook(templateFile);

		//		WritableWorkbook outputWorkbook = Workbook.createWorkbook(outputFile, templateWorkbook);
		WritableWorkbook outputWorkbook = Workbook.createWorkbook(outputFile);

		/* Find the template sheet.  Note that we loop around looking for it rather than
		 * simply getting it by name because that is the only way we can get the index
		 * of the sheet.
		 */
		int templateSheetIndex = -1;
		for (int i = 0; i < outputWorkbook.getNumberOfSheets(); i++) {
			if (outputWorkbook.getSheet(i).getName().equals("Portfolio")) {
				templateSheetIndex = i;
			}
		}

		WritableSheet templateSheet = outputWorkbook.getSheet("Portfolio");

		DatastoreManager sessionManager = (DatastoreManager)window.getActivePage().getInput();
		Session session = sessionManager.getSession();
		StockAccount account;
		try {
			account = (StockAccount)session.getAccountByShortName("Wells Fargo brokerage");
		} catch (SeveralAccountsFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new RuntimeException(e);
		} catch (NoAccountFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new RuntimeException(e);
		}

		readPrudentSpeculatorTargets();
		
		/*
		 * We want to find all securities that were owned at any point during the given period.
		 * There might not be any activity on the security during the period but we still want
		 * to include it.  If the security was sold in full before the start of the period or no security
		 * was purchased until after the period, or it was sold in full before the period and re-purchased
		 * after the period then we are not interested in the security.
		 * 
		 * We keep a list of all money invested in the security and all proceeds including dividends and any other payments or
		 * income.
		 * 
		 * We also group securities together if they are in some way connected.  For example in a corporate take-over,
		 * the new security and the old security are connected.  We produce a single block of transactions for both securities.
		 * By doing this, we don't care about the corporate take-over, instead treating it all as one investment decision
		 * and looking at the final performance of that investment decision. 
		 */

		class SecurityWrapper {
			/**
			 * Maps a security to a running total of the number of this security held in the account.
			 * <P>
			 * In most cases there will be just the one security in this collection.
			 * However where there is stuff like corporate mergers, spin-offs and other
			 * such stuff, all the transactions involving any of the related securities are
			 * kept in one instance of this object.
			 */
			private Map<Security, Long> securities = new HashMap<Security, Long>();
			
			public Collection<SecuritiesTransaction> transactions = new TreeSet<SecuritiesTransaction>(new Comparator<SecuritiesTransaction>() {
				@Override
				public int compare(SecuritiesTransaction trans1, SecuritiesTransaction trans2) {
					return trans1.date.compareTo(trans2.date);
				}
			});

			public SecurityWrapper(Security security) {
				this.securities.put(security, Long.valueOf(0));
			}

			class SecuritiesTransaction {
				Date date;
				String description;
				long quantity;
				double amount;
				double price;

				public SecuritiesTransaction(Date date, String description, long quantity, double amount, double price) {
					this.date = date;
					this.description = description;
					this.quantity = quantity;
					this.amount = amount;
					this.price = price;
				}

			}

			public boolean containsSecurity(Security security) {
				return securities.containsKey(security);
			}

			/**
			 * Merges the information from the given other security into the
			 * information for this security.
			 * <P>
			 * This method is called when it is discovered that two securities are
			 * related through a merger, spin-off or other such event.
			 * <P>
			 * In most cases there will only be activity in one security, the other being
			 * a new security in the brokerage account with no prior activity.  However that
			 * is not necessarily the case.  For example, if a merger took place and both securities
			 * in both companies were previously owned in the account then there would be prior activity
			 * for both security.  The transactions would be merged.
			 * 
			 * @param otherSecurityWrapper
			 */
			public void mergeFrom(SecurityWrapper otherSecurityWrapper) {
				securities.putAll(otherSecurityWrapper.securities);
				transactions.addAll(otherSecurityWrapper.transactions);
				
			}

			public void addToSecurityBalance(Security security, long amount) {
				long previousBalance = securities.get(security);
				securities.put(security, previousBalance + amount);
			}

			public long getSecurityBalance(Security security) {
				return securities.get(security);
			}
		}


		Map<Security, SecurityWrapper> securityInfoMap = new HashMap<Security, SecurityWrapper>();

		/*
		 * Find the security balances at the start of the period.  The period starts at the beginning
		 * of the start date, so we don't include transactions on the start date.
		 */
		for (Entry entry : account.getEntries()) {
			if (entry.getTransaction().getDate().before(startDate)) {
				StockEntry entry2 = entry.getExtension(StockEntryInfo.getPropertySet(), false);
				if (entry2 != null && entry2.getCommodity() instanceof Security) {
					Security security = (Security)entry2.getCommodity();
					SecurityWrapper securityWrapper = securityInfoMap.get(security);
					if (securityWrapper == null) {
						securityWrapper = new SecurityWrapper(security);
						securityInfoMap.put(security, securityWrapper);
					}

					securityWrapper.addToSecurityBalance(security, entry.getAmount());
				}
			}
		}

		Map<Security, Prices> priceSourceMap = new HashMap<Security, Prices>();
		
		/*
		 * Add an entry that represents the market value at the start of the period
		 * (unless the balance is zero which means all the security was sold before the
		 * start of the period).  
		 */
		for (Security security : securityInfoMap.keySet()) {
			SecurityWrapper securityWrapper = securityInfoMap.get(security);
			long securityBalance = securityWrapper.getSecurityBalance(security);
			if (securityBalance != 0) {
				Prices priceSource = priceSourceMap.get(security);
				if (priceSource == null) {
					priceSource = new Prices(security);
					priceSourceMap.put(security, priceSource);
				}
				Price priceData = priceSource.getPrice(startDate);
				if (priceData == null) {
					securityWrapper.transactions.add(securityWrapper.new SecuritiesTransaction(startDate, "initial value " + formatQuantity(securityBalance) + security.getSymbol(), securityBalance, 0, 0));
				} else {
					securityWrapper.transactions.add(securityWrapper.new SecuritiesTransaction(priceData.getDate(), "initial value " + formatQuantity(securityBalance) + security.getSymbol(), securityBalance, securityBalance * priceData.getPrice(), priceData.getPrice()));
				}
			}
		}

		Set<Entry> entriesToIgnore = new HashSet<Entry>();
		
		/*
		 * Go through all transactions between the start date and the end date looking for transactions
		 * that show either money being put into the investment in the security or proceeds of any kind from
		 * the investment in the security.  An entry is created for each one.
		 * 
		 * We also keep track of the total quantity of the security owned.  This is necessary so we can get
		 * the market value of any security that is owned at the end of the period. 
		 */
		for (Entry entry : account.getEntries()) {
			if (!entry.getTransaction().getDate().before(startDate)
					&& !entry.getTransaction().getDate().after(endDate)) {
				
				if (entriesToIgnore.contains(entry)) {
					entriesToIgnore.remove(entry);
					continue;
				}
				
				StockEntry entry2 = entry.getExtension(StockEntryInfo.getPropertySet(), false);
				
				if (entry.getCommodity() instanceof Security) {
					Security security = (Security)entry.getCommodity();
					SecurityWrapper securityWrapper = securityInfoMap.get(security);
					if (securityWrapper == null) {
						securityWrapper = new SecurityWrapper(security);
						securityInfoMap.put(security, securityWrapper);
					}

					securityWrapper.addToSecurityBalance(security, entry.getAmount());

					// Add up all other entries
					long currencyAmount = 0;
					StringBuffer description = new StringBuffer();
					for (Entry otherEntry : entry.getTransaction().getEntryCollection()) {
						if (!otherEntry.equals(entry)) {
							if (otherEntry.getCommodityInternal() == null) {
								// Bad currency.  What do we do??????
								System.out.println("No currency in connection with " + entry.getCommodity().getName());
							} else {
								if (!otherEntry.getCommodityInternal().equals(account.getCurrency())) {
									if (otherEntry.getCommodityInternal() instanceof Security) {
										Security otherSecurity = (Security)otherEntry.getCommodityInternal();
										
										/* We have a transaction that involves two different securities.  This might happen if there is a corporate
										 * take-over or a spin-off, for example.  When this happens,
										 * we treat the two securities as a single investment.  We merge the two SecurityWrapper objects into one, and map
										 * both securities to the same security wrapper in our map.  
										 */
										
										/*
										 * If the other stock entry is in another account then we don't support this.
										 * This could be a problem if you have a stock broker that moves every few years
										 * from one brokerage to another.  If you simply re-name the brokerage account and change
										 * the account import options etc. then this won't be a problem.  If you create a new brokerage
										 * account and transfer the security then this will be a problem because you want your investment analysis
										 * to look at the overall investment in the security, seeing through the transfer.  However we are outputting just
										 * the investment in this brokerage account so we treat such a transfer the same as if the security was bought
										 * (if a transfer in) or sold (if a transfer out).  Normally for a buy or a sell we use the actual price of the
										 * transaction.  However in this case we need to use the market price.
										 */
										
										if (otherEntry.getAccount() != account) {
											/* This may be a stock split account.  In that case it is not really coming in or out
											of the account.  Ideally stock split accounts should be income and expense accounts,
											and we would then be able to identify this situation.  We can ignore this entry and
											still get a correct investment report.
											*/
											System.out.println("stock split on " + otherSecurity.getName());
											continue;
										}
										
										if (!securityWrapper.containsSecurity(otherSecurity)) {
											SecurityWrapper otherSecurityWrapper = securityInfoMap.get(otherSecurity);
											if (otherSecurityWrapper == null) {
												otherSecurityWrapper = new SecurityWrapper(otherSecurity);
											}
											
											// Merge all the information about the other security into this security wrapper.
											securityWrapper.mergeFrom(otherSecurityWrapper);
											
											// Now point the other security to this same security wrapper
											securityInfoMap.put(otherSecurity, securityWrapper);
										}
										
										securityWrapper.addToSecurityBalance(otherSecurity, otherEntry.getAmount());

										if (otherEntry.getAmount() > 0) {
											description.append("receive " + formatQuantity(otherEntry.getAmount()) + " " + otherSecurity.getSymbol());
										} else if (otherEntry.getAmount() < 0) {
											description.append("deliver " + formatQuantity(-otherEntry.getAmount()) + " " + otherSecurity.getSymbol());
										}
										description.append(", ");
										
										/* We are processing all the entries in this transaction here.  We don't want to process
										 * it again when the entries iterator finds the other entries.  We therefore add these
										 * to the set of entries to be ignored when the outer entries loop gets to them.  
										 */
										entriesToIgnore.add(otherEntry);
										
									} else {
										// Bad currency.  What do we do??????
										System.out.println("Bad Currency - " + otherEntry.getCommodity().getName() + " in connection with " + entry.getCommodity().getName());
									}
								} else {
									currencyAmount += otherEntry.getAmount();
								}
							}
						}
					}

					if (entry.getAmount() > 0) {
						if (description.length() == 0) {
							description.append("buy ");
						} else {
							description.append("receive ");
						}
						description.append(formatQuantity(entry.getAmount()) + " " + security.getSymbol());
					} else if (entry.getAmount() < 0) {
						if (description.length() == 0) {
							description.append("sell ");
						} else {
							description.append("deliver ");
						}
						description.append(formatQuantity(-entry.getAmount()) + " " + security.getSymbol());
					}
					
					securityWrapper.transactions.add(securityWrapper.new SecuritiesTransaction(entry.getTransaction().getDate(), description.toString(), entry.getAmount(), currencyAmount, 0));
				} else if (entry2 != null && entry2.getSecurity() != null) {

					Security security = entry2.getSecurity();
					SecurityWrapper securityWrapper = securityInfoMap.get(security);
					if (securityWrapper == null) {
						securityWrapper = new SecurityWrapper(security);
						securityInfoMap.put(security, securityWrapper);
					}

					// TODO a dividend entry???
				}
			}
		}

		/*
		 * For any security that is still owned at the end of the period,
		 * output an entry with the final value.
		 */
		for (Security security : securityInfoMap.keySet()) {
			SecurityWrapper securityWrapper = securityInfoMap.get(security);
			long securityBalance = securityWrapper.getSecurityBalance(security);
			if (securityBalance != 0) {
				IOHLC ohlc = getMarketPrice(security, endDate, false);
				if (ohlc == null) {
					securityWrapper.transactions.add(securityWrapper.new SecuritiesTransaction(endDate, "final value " + formatQuantity(securityBalance) + security.getSymbol(), securityBalance, 0, 0));
				} else {
					double price = ohlc.getClose();
					securityWrapper.transactions.add(securityWrapper.new SecuritiesTransaction(ohlc.getDate(), "final value " + formatQuantity(securityBalance) + security.getSymbol(), securityBalance, -securityBalance * price, price));
				}
			}
		}

		Set<String> turnaroundStocks = new HashSet<String>();
		Set<String> xFunds = new HashSet<String>();

		turnaroundStocks.addAll(Arrays.asList(new String[] {
				"ADCT",
				"EK",
				"IPG",
				"JDSU",
				"NWL",
				"NR",
				"PMTC",
				"POR",
				"PRST",
				"PRM",
				"RSTO",
				"RAD",
				"SIX",
				"THC",
				"ZHNE"
		}));

		xFunds.addAll(Arrays.asList(new String[] {
				"CVY",
				"EZU",
				"FEZ",
				"NFO"
		}));

		if (templateSheetIndex == -1) {
			WritableSheet prudentSheet = outputWorkbook.createSheet("Prudent Speculator", 0);
			WritableSheet turnaroundSheet = outputWorkbook.createSheet("Turnaround", 1);
			WritableSheet xfundSheet = outputWorkbook.createSheet("X-Fund", 2);

			int prudentRow = 1;
			int turnaroundRow = 1;
			int xfundRow = 1;

			/*
			 * We want to process each SecurityWrapper just once.  However the same one will be in
			 * the map multiple times if it covers multiple related security.  We therefore copy the
			 * SecurityWrapper objects into a set to remove duplicates.
			 * 
			 * We also want to output the securities sorted by the symbol, so we make the set an
			 * ordered tree-set.
			 */
			Set<SecurityWrapper> securityWrappers = new TreeSet<SecurityWrapper>(new Comparator<SecurityWrapper>() {
				@Override
				public int compare(SecurityWrapper security1, SecurityWrapper security2) {
					/*
					 * This is complicated because there may be multiple securities.  For sorting purposes, we use the first
					 * security (in alphabetical order) that we still own at the end of the period.   If we don't own any
					 * of the stock by the end, we use the first alphabetically of any that we have owned.
					 */
					
					return getSortSymbol(security1).compareTo(getSortSymbol(security1));
				}

				private String getSortSymbol(SecurityWrapper security) {
					String first = null;
					for (Security sec : security.securities.keySet()) {
						if (security.securities.get(sec).longValue() != 0) {
							if (first == null || first.compareTo(sec.getSymbol()) > 0) {
								first = sec.getSymbol();
							}
						}
					}
					
					/*
					 * If we found none, try again but include securities with zero balances
					 */
					if (first == null) {
						for (Security sec : security.securities.keySet()) {
							if (first == null || first.compareTo(sec.getSymbol()) > 0) {
								first = sec.getSymbol();
							}
						}
					}

					return first;
				}
			});
			securityWrappers.addAll(securityInfoMap.values());
			
			for (SecurityWrapper securityWrapper : securityWrappers) {

				// This is a bit of a kludge.  We just get the first security and use that to output
				// certain information.
				Security security = securityWrapper.securities.keySet().iterator().next();
				
				WritableSheet outputSheet;
				int row;
				if (turnaroundStocks.contains(security.getSymbol())) {
					outputSheet = turnaroundSheet;
					row = turnaroundRow;
					//				} else if (xFunds.contains(security.getSymbol())) {
				} else if (security.getSymbol() != null && security.getSymbol().length() == 5 && !security.getSymbol().equals("NEWCQ")) {
					outputSheet = xfundSheet;
					row = xfundRow;
				} else {
					outputSheet = prudentSheet;
					row = prudentRow;
				}

				jxl.write.Label nameCell = new jxl.write.Label(0, row, security.getName());
				outputSheet.addCell(nameCell);

				jxl.write.Label symbolCell = new jxl.write.Label(1, row, security.getSymbol());
				outputSheet.addCell(symbolCell);

				jxl.write.Label cusipCell = new jxl.write.Label(2, row, security.getCusip());
				outputSheet.addCell(cusipCell);

				for (SecurityWrapper.SecuritiesTransaction securityTransaction : securityWrapper.transactions) {
					jxl.write.Label typeCell = new jxl.write.Label(3, row, securityTransaction.description);
					outputSheet.addCell(typeCell);

					if (securityTransaction.price != 0) {
						jxl.write.Number startPriceCell = new jxl.write.Number(4, row, securityTransaction.price);
						outputSheet.addCell(startPriceCell);
					}

					jxl.write.DateTime dateCell = new jxl.write.DateTime(5, row, securityTransaction.date, jxl.write.DateTime.GMT);
					outputSheet.addCell(dateCell);

					jxl.write.Number currencyAmountCell = new jxl.write.Number(6, row, securityTransaction.amount/100);
					outputSheet.addCell(currencyAmountCell);

					row++;
				}

				if (outputSheet == prudentSheet) {
					if (security.getSymbol() != null) {
						String targetPrice = targetPrices.get(security.getSymbol());
						jxl.write.Label targetPriceCell = new jxl.write.Label(7, row, targetPrice);
						outputSheet.addCell(targetPriceCell);
					}
				}
				
				row++;

				if (turnaroundStocks.contains(security.getSymbol())) {
					turnaroundRow = row;
				} else if (security.getSymbol() != null 
						&& (xFunds.contains(security.getSymbol()) 
								|| security.getSymbol().length() == 5)) {
					xfundRow = row;
				} else {
					prudentRow = row;
				}
			}



		} else {
			//			int propertySheetIndex = templateSheetIndex + 1;
			//			for (IncomeExpenseAccount property : propertyAccounts) {
			//				WritableSheet outputSheet = outputWorkbook.createSheet(property.getName(), propertySheetIndex++);
			//				copySheet(templateSheet, outputSheet, property);
			//			}
			//			outputWorkbook.removeSheet(templateSheetIndex);
		}

		// All sheets and cells added. Now write out the workbook
		outputWorkbook.write();
		outputWorkbook.close();
	}

	private void readPrudentSpeculatorTargets() {
		File outputFile = new File("e:\\accounts-to-import\\gp021910.xls");
		
		try {
		Workbook outputWorkbook = Workbook.getWorkbook(outputFile);

		/* Find the template sheet.  Note that we loop around looking for it rather than
		 * simply getting it by name because that is the only way we can get the index
		 * of the sheet.
		 */
		int templateSheetIndex = -1;
		for (int i = 0; i < outputWorkbook.getNumberOfSheets(); i++) {
			if (outputWorkbook.getSheet(i).getName().equals("Sheet1")) {
				templateSheetIndex = i;
			}
		}
		
		Sheet sheet = outputWorkbook.getSheet(templateSheetIndex);
		
		for (int row = 5; ; row++) {
		String symbol = sheet.getCell(2, row).getContents();
		String targetPrice = sheet.getCell(8, row).getContents();
		
		if (symbol.length() == 0) {
			break;
		}
		
		targetPrices.put(symbol, targetPrice);
		}
		
		outputWorkbook.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (BiffException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private String formatQuantity(long quantity) {
		if (quantity % 1000 == 0) {
			return Long.toString(quantity/1000);
		} else {
			return new BigDecimal(quantity).divide(new BigDecimal(1000)).toPlainString();
		}
	}

	private IOHLC getMarketPrice(Security security, Date date, boolean notEarlierThan) {
	}

	//	private void copySheet(WritableSheet templateSheet, WritableSheet outputSheet, IncomeExpenseAccount property) throws RowsExceededException, WriteException {
	//		int outputRow = 0;
	//
	////		WritableFont arial12font = new WritableFont(WritableFont.ARIAL, 12,
	////				WritableFont.BOLD, true);
	////		WritableCellFormat arial12format = new WritableCellFormat(arial12font);
	//
	//		int numrows = templateSheet.getRows();
	//		int numcols = templateSheet.getColumns();
	//		for (int sourceRow = 0 ; sourceRow < numrows ; sourceRow++)
	//		{
	//
	//			WritableCell column1Cell = templateSheet.getWritableCell(0, sourceRow);
	//			String value = column1Cell.getContents();
	//			if (value.equals("<entry>")) {
	//
	//				for (IncomeExpenseAccount childAccount : property.getSubAccountCollection()) {
	//					String taxCategory = childAccount.getPropertyValue(BritishTaxCategoryInfo.getTaxCategoryAccessor());
	//
	//					int column = -1;
	//					if ("property.rent".equalsIgnoreCase(taxCategory)) {
	//						column = 1;
	//					} else if ("property.repairs".equalsIgnoreCase(taxCategory)) {
	//						column = 3;
	//					} else if ("property.capitalExpenses".equalsIgnoreCase(taxCategory)) {
	//						column = 4;
	//					} else if ("property.professionalFees".equalsIgnoreCase(taxCategory)) {
	//						column = 5;
	//					} else if ("property.insurance".equalsIgnoreCase(taxCategory)) {
	//						column = 6;
	//					} else if ("property.other".equalsIgnoreCase(taxCategory)) {
	//						column = 7;
	//					}
	//
	//					if (column != -1) {
	//						// Output entry in this column
	//
	//						for (Entry entry : childAccount.getEntries()) {
	//							Date date = entry.getValuta();
	//							if (date == null) {
	//								date = entry.getTransaction().getDate(); 
	//							}
	//							if (date.after(startDate) && date.before(endDate)) {
	//
	//								jxl.write.DateTime dateCell = new jxl.write.DateTime(0, outputRow, date);
	//								copyFormat(templateSheet, sourceRow, 0, dateCell);
	//								outputSheet.addCell(dateCell);
	//
	//								double amount = ((double)entry.getAmount()) / 100;
	//								if (column == 1) {
	//									amount = -amount;
	//								}
	//								
	//								jxl.write.Number numberCell = new jxl.write.Number(column, outputRow, amount);
	//								copyFormat(templateSheet, sourceRow, column, numberCell);
	//								outputSheet.addCell(numberCell);
	//
	//								copyCellAsIs(templateSheet, outputSheet, sourceRow,	outputRow, 2);
	//
	//								String catagoryName = entry.getAccount().getName();
	//								if (catagoryName.startsWith(property.getName() + " - ")) {
	//									catagoryName = catagoryName.substring(property.getName().length() + 3);
	//								}
	//								Label catagoryLabel = new Label(8, outputRow, catagoryName);
	//								copyFormat(templateSheet, sourceRow, 8, catagoryLabel);
	//								outputSheet.addCell(catagoryLabel);
	//
	//								Label memoLabel = new Label(9, outputRow, entry.getMemo());
	//								copyFormat(templateSheet, sourceRow, 9, memoLabel);
	//								outputSheet.addCell(memoLabel);;
	//
	//								outputRow++;
	//							}
	//						}
	//					}
	//				}		
	//			} else {
	//				// Copy row as is
	//				for (int column = 0 ; column < numcols ; column++) {
	//					copyCellAsIs(templateSheet, outputSheet, sourceRow,	outputRow, column);
	//				}
	//				outputRow++;
	//			}
	//		}
	//	}
	//
	//	private void copyCellAsIs(WritableSheet templateSheet,
	//			WritableSheet outputSheet, int sourceRow, int outputRow, int column)
	//			throws WriteException, RowsExceededException {
	//		WritableCell sourceCell = templateSheet.getWritableCell(column, sourceRow);
	//		WritableCell outputCell = sourceCell.copyTo(column, outputRow);
	//		jxl.format.CellFormat sourceFormat = sourceCell.getCellFormat();
	//		if (sourceFormat != null) {
	//			WritableCellFormat outputFormat = new WritableCellFormat(sourceFormat);
	//			outputCell.setCellFormat(outputFormat);
	//		}
	//		outputSheet.addCell(outputCell);
	//	}
	//
	//	private void copyFormat(WritableSheet templateSheet, int sourceRow,
	//			int column, WritableCell outputCell) {
	//		WritableCell sourceCell = templateSheet.getWritableCell(column, sourceRow);
	//		jxl.format.CellFormat sourceFormat = sourceCell.getCellFormat();
	//		if (sourceFormat != null) {
	//			WritableCellFormat outputFormat = new WritableCellFormat(sourceFormat);
	//			outputCell.setCellFormat(outputFormat);
	//		}
	//	}

	public class PropertyElementContainer {
		private String name;
		private Map<String, PropertyElement> childs = new HashMap<String, PropertyElement>();

		public PropertyElementContainer(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public PropertyElement[] getChilds() {
			Collection<PropertyElement> c = childs.values();
			return c.toArray(new PropertyElement[c.size()]);
		}

		public PropertyElement createChild(String id, String name) {
			PropertyElement element = childs.get(id);
			if (element == null) {
				element = new PropertyElement(id, name, this);
				childs.put(id, element);
			}
			return element;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return name;
		}
	}

	public class PropertyElement {
		private String id;
		private String name;
		private String value;
		private PropertyElementContainer parent;

		public PropertyElement(String id, String name, PropertyElementContainer parent) {
			this.id = id;
			this.name = name;
			this.parent = parent;
		}

		public String getValue() {
			return value;
		}

		public void setValue(String value) {
			this.value = value;
		}

		public String getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public PropertyElementContainer getParent() {
			return parent;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return name;
		}
	}

	//	protected PropertyElementContainer[] buildInput() {
	//		Map<String, PropertyElementContainer> containers = new HashMap<String, PropertyElementContainer>();
	//
	//		IExtensionPoint extensionPoint = Platform.getExtensionRegistry().getExtensionPoint("org.eclipsetrader.core.connectors");
	//		if (extensionPoint != null) {
	//			IConfigurationElement[] configElements = extensionPoint.getConfigurationElements();
	//			for (int j = 0; j < configElements.length; j++) {
	//				String name = configElements[j].getDeclaringExtension().getLabel();
	//				if (name == null || name.equals(""))
	//					name = configElements[j].getDeclaringExtension().getContributor().getName();
	//
	//				if ("property".equals(configElements[j].getName())) {
	//					PropertyElementContainer container = containers.get(name);
	//					if (container == null) {
	//						container = new PropertyElementContainer(name);
	//						containers.put(name, container);
	//					}
	//					container.createChild(configElements[j].getAttribute("id"), configElements[j].getAttribute("name"));
	//				}
	//			}
	//		}
	//
	//		return containers.values().toArray(new PropertyElementContainer[containers.values().size()]);
	//	}

}
