/*
 * 
 *  JMoney - A Personal Finance Manager
 *  Copyright (c) 2010 Nigel Westbury <westbury@users.sourceforge.net>
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

package net.sf.jmoney.taxes.us.wizards;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import net.sf.jmoney.model2.Commodity;
import net.sf.jmoney.model2.DatastoreManager;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.TransactionInfo;
import net.sf.jmoney.stocks.model.Security;
import net.sf.jmoney.stocks.model.Stock;
import net.sf.jmoney.stocks.model.StockAccount;
import net.sf.jmoney.taxes.us.Activator;
import net.sf.jmoney.taxes.us.resources.Messages;
import net.sf.jmoney.views.feedback.Feedback;
import net.sf.jmoney.views.feedback.FeedbackView;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;

/**
 * A wizard to export data to a TXF file.
 * <P>
 * Currently only stock purchase and sales are exported, giving the tax software the information
 * it needs to complete the stock capital gains on Schedule D.
 */
public class TxfExportWizard extends Wizard implements IExportWizard {

	public class ActivityMap extends TreeMap<ActivityKey, Collection<ActivityNode>> {
		private static final long serialVersionUID = 1L;

		void add(ActivityNode node) {
			Collection<ActivityNode> nodesWithGivenKey = get(node.key);
			if (nodesWithGivenKey == null) {
				nodesWithGivenKey = new ArrayList<ActivityNode>();
				put(node.key, nodesWithGivenKey);
			}
			nodesWithGivenKey.add(node);
		}
	}

	/**
	 * Date format to be used when writing dates to the TXF file.
	 */
	private DateFormat txfFileDateFormat = new SimpleDateFormat("M/d/yy");

	/**
	 * Date format to be used when showing dates to the user in messages.
	 */
	private DateFormat userDateFormat = new SimpleDateFormat("MMM/dd/yy");

	//	private NumberFormat quantityFormat = new DecimalFormat("###,###,###");

	private IWorkbenchWindow window;

	private IStructuredSelection selection;

	private TxfExportWizardPage mainPage;

	public TxfExportWizard() {
		IDialogSettings workbenchSettings = Activator.getDefault().getDialogSettings();
		IDialogSettings section = workbenchSettings.getSection("TxfExportWizard");//$NON-NLS-1$
		if (section == null) {
			section = workbenchSettings.addNewSection("TxfExportWizard");//$NON-NLS-1$
		}
		setDialogSettings(section);
	}

	/**
	 * We will cache window object in order to be able to provide parent shell
	 * for the message dialog.
	 */
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		this.window = workbench.getActiveWorkbenchWindow();
		this.selection = selection;

		// Original JMoney disabled the export menu items when no
		// session was open.  I don't know how to do that in Eclipse,
		// so we display a message instead.
		DatastoreManager sessionManager = (DatastoreManager)window.getActivePage().getInput();
		if (sessionManager == null) {
			MessageDialog.openError(
					window.getShell(), 
					"Disabled Action Selected", 
			"You cannot export data unless you have a session open.  You must first open a session.");
			return;
		}

		/*
		 * This wizard exports all the selected accounts.  We check that the selected
		 * accounts are all stock accounts, and that at least one was selected.
		 */
		if (selection.isEmpty()) {
			MessageDialog.openError(
					window.getShell(), 
					"Unable to Export Capital Gains Data", 
					"You must select one or more stock accounts before running this wizard."
			);
			return;
		}
		for (Object selectedObject : selection.toList()) {
			if (!(selectedObject instanceof StockAccount)) {
				MessageDialog.openError(
						window.getShell(), 
						"Unable to Export Capital Gains Data", 
						"You have selected something that is not a stock account.  Only stock accounts can be selected when exporting capital gains data."
				);
				return;
			}
		}

		mainPage = new TxfExportWizardPage(window);
		addPage(mainPage);
	}

	@Override
	public boolean performFinish() {
		String fileName = mainPage.getFileName();
		final File txfFile = new File(fileName);

		if (dontOverwrite(txfFile))
			return false;

		int calendarYear = mainPage.getYear();
		Calendar calendar = Calendar.getInstance();
		calendar.clear();
		calendar.set(calendarYear, Calendar.JANUARY, 1);
		final Date startDate = calendar.getTime();
		calendar.set(calendarYear, Calendar.DECEMBER, 31);
		final Date endDate = calendar.getTime();

		MultiStatus result = exportFile(txfFile, startDate, endDate);

		if (!result.isOK()) {

			Feedback feedback = new Feedback(result.getMessage(), result) {
				@Override
				protected boolean canExecuteAgain() {
					return true;
				}

				@Override
				protected IStatus executeAgain() {
					return exportFile(txfFile, startDate, endDate);
				}
			};

			try {
				FeedbackView view = (FeedbackView)window.getActivePage().showView(FeedbackView.ID, null, IWorkbenchPage.VIEW_ACTIVATE);
				view.showResults(feedback);
			} catch (PartInitException e) {
				throw new RuntimeException(e);
			}
		}

		return true;
	}

	private MultiStatus exportFile(File txfFile, Date startDate, Date endDate) {
		MultiStatus result = new MultiStatus(Activator.PLUGIN_ID, IStatus.OK, "Export TXF File: " + txfFile.getPath(), null);

		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(txfFile));

			// write header
			writeln(writer, "V041");
			writeln(writer, "AJMoney");
			writeln(writer, "D " + txfFileDateFormat.format(new Date()));
			writeln(writer, "^");

			for (Object selectedObject : selection.toList()) {
				if (selectedObject instanceof StockAccount) {
					StockAccount account = (StockAccount)selectedObject;
					IStatus status = exportCapitalGains(account, startDate, endDate, writer);
					result.add(status);
				}
			}

			writer.close();
		} catch (IOException e) {
			MessageDialog.openError(window.getShell(), "Export Failed", e.getLocalizedMessage());
		}
		return result;
	}


	private boolean dontOverwrite(File file) {
		if (file.exists()) {
			String title = Messages.Dialog_FileExists;
			String question = NLS.bind(Messages.Dialog_OverwriteExistingFile, file.getPath());

			boolean answer = MessageDialog.openQuestion(
					window.getShell(),
					title,
					question);
			return !answer;
		} else {
			return false;
		}
	}

	private IStatus exportCapitalGains(StockAccount account, Date startDate, Date endDate, BufferedWriter writer) throws IOException {
		MultiStatus result = new MultiStatus(Activator.PLUGIN_ID, IStatus.INFO, "Export Account: " + account.getName(), null);

		/*
		 * Get all entries that involve a change in the number of stock.  This
		 * does not include dividends.  There could be potential issues when
		 * a company returns capital - don't know how that affects US capital gains
		 * taxes.
		 * 
		 * For each sale in the given period, we need to go back to find the cost basis.
		 * US Federal tax code uses FIFO (first in, first out), so that is what we do.  Instead of FIFO, it
		 * is possible to assign lots but that is not currently supported.
		 * 
		 * Stock are matched within each brokerage account.  So if, for example, stock in
		 * Acme Company was purchased separately in two different brokerage accounts, then
		 * the cost basis will always be done based on the cost when purchased in the same account.
		 * However stock may be transferred from one brokerage account to another.  In that case
		 * we look to the previous brokerage account to determine the cost basis.
		 * 
		 * We build up objects where each object contains all the information we need on a security or a group
		 * of connected securities.  Securities will be connected if there is a transaction that involves both
		 * securities.  This can happen when there is a take-over, merger, spin-off or some other restructuring
		 * involving more than one security.
		 * 
		 *   
		 * The process involves looking backwards and forwards.
		 * Consider the following actions in the following order:
		 * 
		 * 200 shares of Acme purchased in account A.
		 * 100 shares transferred to account B.
		 * 100 purchased in account A
		 * 200 sold in account A
		 * 100 sold in account B
		 * 
		 * Had these all been in the same account, the sale of 200 would be matched to the initial purchase (FIFO).
		 * However that would leave the sale of the 100 shares in account B being matched to the 100 shares in account A,
		 * even though the shares in account A were purchased after the shares appeared in account B.  This is not likely
		 * to be the expected behavior.
		 * 
		 * So, once the shares were transferred to account B, these became a separate identifiable lot.  When processing the
		 * sale in account A, we go back and see the transfer out to account B.  We want to match the transferred shares to
		 * a prior acquisition in account A and take those out of the equation.  It is not clear to which acquisition those
		 * are matched, though in this case there is only one possible acquisition.  
		 * 
		 * Now consider a slight variation.  Suppose the initial purchase of 200 shares was done in two lots of 100 shares,
		 * each lot at a separate price.
		 * 
		 * 100 shares of Acme purchased in account A in $10.
		 * 100 shares of Acme purchased in account A in $11.
		 * 100 shares transferred to account B.
		 * 100 sold in account A
		 * 100 sold in account B
		 * 
		 * In this case the sale in account A happened first so that should be matched to the purchase at $10.  Although they have become separate lots,
		 * what happens in one account affects another account.  Had the sale in account A not happened then the sale of stock in account B
		 * would have had a basis of $10 instead of $11.
		 * 
		 * So what happens is the 200 shares is considered a blob of shares.  This carries with it the order of purchases and the quantity
		 * and price for each purchase.  This blob ???????
		 * 
		 * Now consider what happens if another transfer is made:
		 * 
		 * Algorithm to find the basis for a given sale:
		 * 
		 * Step 1.  Go back in time and look for the earliest sale to which the sale can be matched.  This involves going back to find
		 * the earliest sale, but always matching if needed to prevent an amount being carried back that is less that the balance of
		 * stock in the account.
		 * 
		 * In most cases, step 1 suffices.  However we may come across a transfer into or out of the account.
		 * 
		 * If we find a transfer into the account:
		 * 
		 * We have two amounts.  The amount of all sales that are prior to the target sale but that occurred after the transfer into the account.
		 * This amount is matched first.  We also have the amount of the target sale that has not already been matched.  The is matched second.
		 * 
		 * 
		 * We now have two branches to go back through.  We create a processor to go back through each.  Each processor matches up sales in that branch
		 * to earlier purchases, so those are taken out of the equation by the processor.  The processor then returns the earliest date and amount
		 * to which a sale can be matched.
		 * 
		 * We 'merge' the two processors together by taking the earliest date from whichever processor and returning that.  One caveat:  The amount
		 * from one branch will be limited to the transferred amount.  It may be that not all the stock in that account was transferred.  In this
		 * scenario, the matching will depend on when the residue holdings in that account were sold, so see below for how this is handled.
		 * 
		 * The consumer of the
		 * merged branch then does the matching, matching up first the prior sales, then the target sale to get the target basis.
		 * 
		 * We may also come across a transfer out of the account.  If we see a transfer into the account then we also see a transfer out of an account.
		 * The reason is that we need to process the source account, and if the transfer was only a partial transfer of the complete holdings then
		 * we need to look at the sales of the residual holdings.
		 * 
		 * To handle this, we must go forwards through the other branch, all the way forward to the date of the target sale.  This is a competing matcher.
		 * It provides competing sales that use up purchases.   to see how   , matching that 
		 * 
		 * This works well until we get circular connections.  This will happen if securities are transferred to one account and then transferred back.
		 * Two processors could then be processing the same transactions, and we would need some code to recognize this.
		 * 
		 * 
		 * Therefore, to cope with circular connections, we do something simpler.
		 * 
		 * We create a graph of all purchases and sales.  Each points to one or more prior nodes.  Sales can be matched up to anything in a prior node.
		 * When we build this graph, we also put all nodes into a single list sorted by date.  We then process all sales, starting with the earliest,
		 * and match them up until we get to the target sale.
		 * 
		 * The only problem: In a transfer we can't match more than was transferred.  The transfer amount must be stored in the graph.  It must be
		 * decreased as amounts are matched across the transfer.
		 */

		Collection<Entry> entries = account.getSortedEntries(TransactionInfo
				.getDateAccessor(), false);

		for (Entry entry : entries) {
			if (!entry.getTransaction().getDate().before(startDate)
					&& !entry.getTransaction().getDate().after(endDate)) {
				if (entry.getCommodityInternal() instanceof Stock
						&& entry.getAmount() < 0) {
					// Have a disposal of stock

					Stock stock = (Stock)entry.getCommodity();
					long saleQuantity = -entry.getAmount();
					Date sellDate = entry.getTransaction().getDate();

					MultiStatus disposalResult = new MultiStatus(Activator.PLUGIN_ID, IStatus.INFO,
							MessageFormat.format(
									"Sale of {2} of {0} took place on {1}.",
									stock.getName(),
									userDateFormat.format(entry.getTransaction().getDate()),
									formatQuantity(saleQuantity)
							),
							null);

					try {

						/*
						 * We build a set of all currencies, securities, and other commodities
						 * that were acquired or lost if this transaction.  If there are multiple
						 * entries for the same commodity, the amounts are added together.
						 * We end up with a map of commodities to the amounts.  This map excludes
						 * the initial entry representing the disposal of stock.
						 */
						Map<Commodity,Long> transactionMap = new HashMap<Commodity,Long>();
						for (Entry eachEntry : entry.getTransaction().getEntryCollection()) {
							if (eachEntry != entry) {

								/*
								 * We don't currently support stock amounts going to another account.
								 * This could indicate a transfer, or it could be a stock split.
								 */
								if (eachEntry.getCommodityInternal() instanceof Security && eachEntry.getAccount() != account) {
									Status status = new Status(IStatus.ERROR, Activator.PLUGIN_ID, 
											MessageFormat.format(
													"The disposal involves {0} being transferred to/from another account.  Manually determination of the cost basis is required.",
													eachEntry.getCommodityInternal().getName()
											),
											null);
									disposalResult.add(status);
								}

								if (transactionMap.containsKey(entry.getCommodityInternal())) {
									long amount = transactionMap.get(eachEntry.getCommodityInternal());
									amount += eachEntry.getAmount();
									transactionMap.put(eachEntry.getCommodityInternal(), amount);
								} else {
									transactionMap.put(eachEntry.getCommodityInternal(), eachEntry.getAmount());
								}
							}
						}

						if (disposalResult.matches(IStatus.ERROR)) {
							result.add(disposalResult);
							continue;
						}

						Commodity[] commodityArray = transactionMap.keySet().toArray(new Commodity[0]);

						if (transactionMap.size() > 1) {
							Status status = new Status(IStatus.ERROR, Activator.PLUGIN_ID, 
									MessageFormat.format(
											"The security was exchanged for more than a single currency or a single replacement security.  {0} and {1} were involved.  Manually determination of the cost basis is required.",
											commodityArray[0].getName(),
											commodityArray[1].getName()
									),
									null);
							throw new UnsupportedDataException(status);
						}

						Commodity otherCommodity = commodityArray[0];

						/*
						 * If we acquired another stock in exchange for this stock then
						 * this is not a disposal for capital gains tax purposes.
						 */
						if (otherCommodity instanceof Security) {
							Status status = new Status(IStatus.INFO, Activator.PLUGIN_ID,
									MessageFormat.format(
											"{0} {1} were acquired in exchange for this stock.  This is not a disposal for capital gains tax purposes and nothing has been output to the TXF file.",
											formatQuantity(transactionMap.get(otherCommodity)),
											otherCommodity.getName()
									),
									null);
							throw new UnsupportedDataException(status);
						} else if (otherCommodity != account.getCurrency()) {
							Status status = new Status(IStatus.ERROR, Activator.PLUGIN_ID, 
									MessageFormat.format(
											"The stock was exchanged for {0}.  Disposals for anything other than the cash (in the currency of the account) or another security is too complicated.  Manual determination of the cost basis is required.",
											otherCommodity.getName()
									),
									null);
							throw new UnsupportedDataException(status);
						}

						long saleProceeds = transactionMap.get(otherCommodity);

						if (saleProceeds <= 0) {
							Status status = new Status(IStatus.ERROR, Activator.PLUGIN_ID, 
									"No cash was received for the securities.  Manually determination of the cost basis is required.",
									null);
							throw new UnsupportedDataException(status);
						}

						/*
						 * Build the graph.
						 */

						Graph graph = new Graph(stock, account, entry.getTransaction().getDate(), disposalResult);

						List<CostBasis> bases = graph.matchAndFetchTargetBasis(disposalResult);

						for (CostBasis basis : bases) {

							// Calculate the portion of the sale that matches this purchase.
							double thisProceeds = (double)basis.quantity
							* (double)saleProceeds
							/ (double)saleQuantity;

							/*
							 * Write out an entry to the TXF file.  Note that if the sale is matched
							 * to more than one purchase then multiple entries will be written.  We
							 * could combine the entries into just two entries, one for those that are
							 * matched to purchases that would make this a short term sale, and those that
							 * match to purchases that would make this a long term sale.  However, let's just
							 * leave the tax software to deal with that. 
							 */
							writeEntry(writer, stock, basis.quantity, basis.date, (long)basis.basis, sellDate, (long)thisProceeds);
						}

					} catch (UnsupportedDataException e) {
						disposalResult.add(e.getStatus());
					}

					result.add(disposalResult);
				}
			}
		}

		return result;
	}

	/**
	 * Returns the activity in the given account that affects the given stock.
	 * 
	 * Multi-stock transactions are not yet supported and will generate an error result.
	 *
	 * Activity from the start of time to the given date is returned.
	 * 
	 * Status messages may be added to the given multi-status.  This method will return a result
	 * even if an error status is set, leaving it up to the caller to check and handle appropriately. 
	 * @throws UnsupportedDataException 
	 */

	private TreeMap<Date, StockActivity> getStockActivity(StockAccount account, Stock stock, MultiStatus result) throws UnsupportedDataException {
		TreeMap<Date, StockActivity> stockEntries = new TreeMap<Date, StockActivity>();

		/*
		 * Total stock in account.  We initially accumulate all stock amounts here so we
		 * get the balance of this stock at the time the sale was made.  We then adjust
		 * it by reversing out the amounts as we go back through the history.
		 */
		long totalStock = 0;

		for (Entry entry2 : account.getEntries()) {
			if (entry2.getCommodityInternal() == stock) {
				// Have an acquisition or disposal of this stock

				long currencyAmount2 = 0;
				for (Entry eachEntry : entry2.getTransaction().getEntryCollection()) {
					if (eachEntry != entry2) {
						if (eachEntry.getCommodityInternal() != account.getCurrency()) {
							//											throw new ...
						}
						currencyAmount2 += eachEntry.getAmount();
					}
				}

				/*
				 * Put this into our tree. We also accumulate
				 * purchases and sales that were performed on the
				 * same day. We have no way of knowing the order of
				 * transactions on a day, so we add them together.
				 * This has the effect of using the average price.
				 * 
				 * If there are purchases and sales on the same day
				 * then we we don't know what order the sales were performed
				 * but with FIFO the order does not matter.  All sales would be
				 * matched to any earlier purchases and then to the average purchase on
				 * that same day.
				 */
				Date date = entry2.getTransaction().getDate();
				StockActivity activity = stockEntries.get(date);
				if (activity == null) {
					activity = new StockActivity(date);
					stockEntries.put(date, activity);
				}

				if ((activity.securityPurchaseQuantity != 0 && entry2.getAmount() <= 0)
						|| (activity.securitySaleQuantity != 0 && entry2.getAmount() >= 0)) {
					Status status = new Status(IStatus.WARNING, Activator.PLUGIN_ID, 
							MessageFormat.format(
									"Both purchases and sales of {0} took place on {1}.  This is supported but may indicate bad data.",
									stock.getName(),
									userDateFormat.format(entry2.getTransaction().getDate())
							),
							null);
					result.add(status);
				}

				activity.addPurchaseOrSale(entry2.getAmount(), currencyAmount2);

				totalStock += entry2.getAmount();
			}
		}

		return stockEntries;
	}

	/**
	 * The format is this:
	 * 
	 * <code>
V041
AMy Software
D 10/27/2004
^
TD
N323
C1
L1
P300 Aspreva Pharmaceuticals
D5/11/06
$9,883.99
D1/3/07
$6,271.81
^
TD
N321
C1
L1
P700 BTU Intl
D5/9/06
$13,506.99
D1/3/07
$6,884.79
^
	 <code>
	 * @throws IOException 
	 */
	private void writeEntry(BufferedWriter writer, Stock stock, long quantity, Date purchaseDate, long costBasis, Date sellDate, long saleProceeds) throws IOException {
		NumberFormat currencyFormat = NumberFormat.getInstance(Locale.US);
		currencyFormat.setMinimumFractionDigits(2);
		currencyFormat.setMaximumFractionDigits(2);

		writeln(writer, "TD");

		Calendar c = Calendar.getInstance();
		c.setTime(purchaseDate);
		c.add(Calendar.YEAR, 1);
		if (c.getTime().before(sellDate)) {
			// Long term capital gains
			writeln(writer, "N323");
		} else {
			// Short term capital gains
			writeln(writer, "N321");
		}

		writeln(writer, "C1"); // Copy 1, but no idea why this is in this file at all
		writeln(writer, "L1"); // Line 1, but no idea why this is in this file at all
		writeln(writer, "P" + formatQuantity(quantity) + " " + stock.getName());
		writeln(writer, "D" + txfFileDateFormat.format(purchaseDate));
		writeln(writer, "$" + formatCurrency(costBasis));
		writeln(writer, "D" + txfFileDateFormat.format(sellDate));
		writeln(writer, "$" + formatCurrency(saleProceeds));
		writeln(writer, "^");
	}

	private String formatQuantity(long quantity) {
		if (quantity % 1000 == 0) {
			return Long.toString(quantity/1000);
		} else {
			return new BigDecimal(quantity).divide(new BigDecimal(1000)).toPlainString();
		}
	}

	private String formatCurrency(long amount) {
		String x = Long.toString(amount);

		// Ensure at least 3 digits
		while (x.length() < 3) {
			x = "0" + x;
		}

		// Format the whole amount
		NumberFormat quantityFormat = new DecimalFormat("###,###,###");
		String result = quantityFormat.format(amount / 100);

		return result + "." + x.substring(x.length() - 2);
	}


	private void writeln(BufferedWriter writer, String line) throws IOException {
		writer.write(line);
		writer.newLine();
	}

	/**
	 * Activity for the target stock.
	 * <P>
	 * This activity could be:
	 * <UL>
	 * <LI>sale or purchase</LI>
	 * <LI>a swap of this stock for another stock or stocks (e.g. merger or
	 * spin-off)</LI>
	 * <LI>a transfer to or from another account</LI>
	 * <LI>a stock split</LI>
	 * </UL>
	 * All the activity for a given stock on a given day are included in a
	 * single instance of this object.
	 * <P>
	 * If a stock was both bought and sold on the same day then we must keep
	 * separate the sales from the purchases. We do accumulate all the sales
	 * together and accumulate all the purchases together, however.
	 */
	public class StockActivity implements Comparable<StockActivity> {

		Date date;

		long securityPurchaseQuantity = 0;

		long purchaseCost = 0;

		long securitySaleQuantity = 0;

		long saleCost = 0;

		/**
		 * All securities, other than this one, that were acquired or disposed of
		 * on this day. 
		 */
		Map<Security,Long> exchangedSecurities = new HashMap<Security,Long>();

		public List<TransferActivity> transfers;

		public StockActivity(Date date) {
			this.date = date;
		}

		/**
		 * Adds a purchase or sale to today's activity.
		 * 
		 * @param securityQuantity
		 *            positive for purchase, negative for sale, cannot be zero
		 * @param currencyAmount
		 *            negative for purchase, positive for sale, cannot be zero
		 * @throws UnsupportedDataException 
		 */
		public void addPurchaseOrSale(long securityQuantity, long currencyAmount) throws UnsupportedDataException {
			if (securityQuantity > 0 && currencyAmount < 0) {
				this.securityPurchaseQuantity += securityQuantity;
				this.purchaseCost += -currencyAmount;
			} else if (securityQuantity < 0 && currencyAmount > 0) {
				this.securitySaleQuantity += -securityQuantity;
				this.saleCost += currencyAmount;
			} else {
				Status status = new Status(IStatus.WARNING, Activator.PLUGIN_ID, 
						MessageFormat.format(
								"Bad data on {0}.",
								userDateFormat.format(date)
						),
						null);
				throw new UnsupportedDataException(status); 
			}
		}

		@Override
		public int compareTo(StockActivity otherActivity) {
			return date.compareTo(otherActivity.date);
		}
	}

	/**
	 * Graph for a single stock, from start of time to the given date.
	 */
	public class Graph {

		Stock stock;
		Date date;

		/**
		 * Set of all activity nodes in the graph, sorted by date first and then by a pre-defined
		 * order of activity:
		 * <OL>
		 * <LI>purchases</LI>
		 * <LI>sales</LI>
		 * </OL>
		 */
		ActivityMap allActivityNodes = new ActivityMap();

		public Graph(Stock stock, StockAccount account, Date date, MultiStatus result) throws UnsupportedDataException {
			this.stock = stock;
			this.date = date;

			/*
			 * When building this map, we convert StockActivity objects to ActivityNode objects.  Whereas a StockActivity
			 * object contains all the activity for a security on a single day, an ActivityNode object contains just a single
			 * type of activity, e.g. if there are purchases and sales on the same day then two objects will be created, a purchase
			 * object and a sale object.  This simplifies the processing because we can define the assumed order of transactions that
			 * took place on the same day and then process one thing at a time. 
			 */
			TreeMap<Date, StockActivity> stockEntries = getStockActivity(account, stock, result);
			long newStockBalance = 0;

			ActivityNode previousActivity = null;
			for (Date eachDate : stockEntries.keySet()) {
				StockActivity activityThisDay = stockEntries.get(eachDate);

				newStockBalance += activityThisDay.securityPurchaseQuantity - activityThisDay.securitySaleQuantity;

				// Security purchase
				if (activityThisDay.securityPurchaseQuantity != 0) {
					ActivityNode node = new PurchaseActivityNode(eachDate, newStockBalance, activityThisDay.securityPurchaseQuantity, activityThisDay.purchaseCost);
					allActivityNodes.add(node);

					if (previousActivity != null) {
						previousActivity.addNextNode(node);
						node.addPreviousNode(previousActivity);
					}

					previousActivity = node;
				}

				// Security sale
				if (activityThisDay.securitySaleQuantity != 0) {
					ActivityNode node = new SaleActivityNode(eachDate, newStockBalance, activityThisDay.securitySaleQuantity, activityThisDay.saleCost);
					allActivityNodes.add(node);
					if (previousActivity != null) {
						previousActivity.addNextNode(node);
						node.addPreviousNode(previousActivity);
					}

					previousActivity = node;
				}

				//				for (TransferActivity transfer : activityThisDay.transfers) {
				//					// TODO build nodes for this account if not already built.
				//
				//					// TODO add previous and next nodes
				//
				//					// TODO add a zero (dummy node) if the next activity is a transfer
				//					// that requires such a node
				//				}
			}

		}

		public List<CostBasis> matchAndFetchTargetBasis(MultiStatus result) {

			List<CostBasis> bases = new ArrayList<CostBasis>();

			for (ActivityKey eachKey : allActivityNodes.keySet()) {
				Collection<ActivityNode> activities = allActivityNodes.get(eachKey);

				if (activities.size() > 1) {
					Status status = new Status(IStatus.ERROR, Activator.PLUGIN_ID, 
							MessageFormat.format(
									"Two or more transactions involving {0} took place on {1} and these transactions took place in different but connected accounts.  This scenario is not currently supported.",
									stock.getName(),
									userDateFormat.format(eachKey)
							),
							null);
					result.add(status);
					return new ArrayList<CostBasis>();
				}
				ActivityNode activity = activities.iterator().next();

				long balancePriorToThisDate = activity.newBalance;
				if (activity instanceof PurchaseActivityNode) {
					balancePriorToThisDate -= ((PurchaseActivityNode)activity).quantity;
				} else if (activity instanceof SaleActivityNode) {
					balancePriorToThisDate += ((SaleActivityNode)activity).quantity;
				}

				/*
				 * See if we can match purchases made on this date to sales made on this date.
				 * To do this, we not only need both purchases and sales on this date but because
				 * matches are made FIFO, there must be both an insufficient long position prior
				 * to this date to cover the sales and an insufficient short position to cover the
				 * purchases.
				 */
				//				if (activity.saleQuantity > 0 && activity.purchaseQuantity > 0) {
				//					// Find sales today that cannot be matched to a prior long position
				//					long salesNotPreviouslyCovered;
				//					if (balancePriorToThisDate > 0) {
				//						salesNotPreviouslyCovered = Math.max(0, activity.saleQuantity - balancePriorToThisDate);
				//					} else {
				//						salesNotPreviouslyCovered = activity.saleQuantity;
				//					}
				//
				//					// Find purchases today that cannot be matched to a prior short position
				//					long purchasesNotPreviouslyCovered;
				//					if (balancePriorToThisDate < 0) {
				//						purchasesNotPreviouslyCovered = Math.max(0, activity.purchaseQuantity + balancePriorToThisDate);
				//					} else {
				//						purchasesNotPreviouslyCovered = activity.purchaseQuantity;
				//					}
				//					
				//					long intraDayMatchingQuantity = Math.min(purchasesNotPreviouslyCovered, salesNotPreviouslyCovered);
				//					if (intraDayMatchingQuantity > 0) {
				//						activity.saleQuantity -= intraDayMatchingQuantity;
				//						activity.purchaseQuantity -= intraDayMatchingQuantity;
				//
				////						match(activity, activity, intraDayMatchingQuantity);
				//
				//						// Calculate the portion of the cost that matches this purchase.
				//						double thisCostBasis = (double)quantityMatchedToThisPurchase
				//						* (double)priorActivity.purchaseCost
				//						/ (double)(-priorActivity.purchaseQuantity);
				//
				//						/*
				//						 * If there is no later activity then this activity is the one of interest.
				//						 */
				//						if (allActivityNodes.higherKey(eachKey) == null) {
				//							// Add to the list of matches for this sale
				//							bases.add(new CostBasis(quantityMatchedToThisPurchase, priorActivity.date, (long)thisCostBasis));
				//						}
				//					}
				//				}

				/*
				 * if there is an insufficient long position to cover a sale
				 * then that sale is not a taxable event. Likewise if there is
				 * an insufficient short position to cover a purchase then that
				 * purchase is not a taxable event.
				 * 
				 * There can be at most a taxable sale (if a prior long
				 * position) or a taxable purchase (if a prior short position).
				 */

				int multiplier = 0;
				if (balancePriorToThisDate > 0 && activity instanceof SaleActivityNode) {
					multiplier = 1;
				} else if (balancePriorToThisDate < 0 && activity instanceof PurchaseActivityNode) {
					multiplier = -1;
				}

				if (multiplier != 0) {
					/*
					 * We have a taxable event (sale when previously long or purchase when previously
					 * short).
					 */

					/*
					 * The security was previously long.  Match sales made this
					 * date, but only up to the previously long position.
					 * 
					 * or
					 * 
					 * The security was previously short.  Match purchases made this
					 * date, but only up to the previously short position.
					 * 
					 */

					/**
					 * Always positive, regardless of whether we are matching a sale to a previous long purchase
					 * or a purchase to a previous short sale.
					 */
					long quantityLeftToMatch = Math.min(activity.getQuantity(), balancePriorToThisDate * multiplier);

					/**
					 * Negative if this is a sale that we are matching to previous purchases,
					 * positive if this is a purchase that we are matching to previous (short)
					 * sales.
					 * 
					 * NO - ALWAYS POSITIVE
					 */
					long amountLeftToMatch = quantityLeftToMatch;

					/*
					 * Go back finding all possible purchases against which this can
					 * be matched. Match against the earliest.
					 */

					TreeSet<ActivityNode> priorNodes = new TreeSet<ActivityNode>(new Comparator<ActivityNode>() {
						@Override
						public int compare(ActivityNode node1, ActivityNode node2) {
							return node1.key.compareTo(node2.key);
						}
					});

					do {
						/*
						 * Add all the immediate prior nodes to the list of prior nodes.
						 */
						for (ActivityNode priorNode : activity.previousNodes) {
							boolean wasAdded = priorNodes.add(priorNode);
							if (!wasAdded) {
								// This can't happen because we have already errored out above when there
								// are multiple activities of the same type on the same date but that cannot
								// be accumulated together because they are in different accounts.
								throw new RuntimeException("internal error");
							}
						}

						if (priorNodes.isEmpty()) {
							/*
							 * This should not happen.  The balance of each security starts at
							 * zero and then changes according to the acquisitions and disposals
							 * of that security.  When we get back to the beginning, the balance
							 * will always be zero.  And when the balance reaches zero, all securities
							 * will be matched.
							 */
							Status status = new Status(IStatus.ERROR, Activator.PLUGIN_ID, 
									MessageFormat.format(
											"An internal error has occured.  A manual determination must be made for the cost basis for the sale of {0} which took place on {1}.",
											stock.getName(),
											userDateFormat.format(eachKey)
									),
									null);
							result.add(status);
							return new ArrayList<CostBasis>();
						}

						ActivityNode priorActivity = priorNodes.last();
						priorNodes.remove(priorActivity);

						/*
						 * Processing now depends on whether we are matching a sale
						 * to a previously long position or a purchase to a
						 * previously short position.
						 * 
						 * Prior entries will already have been matched if possible.
						 * So if we are matching a sale and we find a previous sale,
						 * we may as well stop right there. The previous sale had no
						 * prior purchase to which it could match (and was thus a
						 * short- sale), so we are not going to find a match for the
						 * current sale either. The same applies if we are matching
						 * a purchase and we find a previous un-matched purchase.
						 * However it is possible that there are previous matches
						 * down other branches, so we remove that one branch and
						 * continue down any other branches.
						 */

						if (multiplier == 1 && priorActivity instanceof SaleActivityNode) {
							/*
							 * We have a previous sale that was not fully matched, so stop this
							 * branch right now.
							 */
							// This should not be able to happen because we don't go down a branch
							// if a short position is being passed from that branch.
							throw new RuntimeException("internal error");
						}
						if (multiplier == -1 && priorActivity instanceof PurchaseActivityNode) {
							/*
							 * We have a previous purchase that was not fully matched, so stop this
							 * branch right now.
							 */
							// This should not be able to happen because we don't go down a branch
							// if a long position is being passed from that branch.
							throw new RuntimeException("internal error");
						}

						/**
						 * Purchase quantity (or sale quantity if a prior short sale), but always
						 * positive regardless of which.
						 */
						long purchaseQuantity = priorActivity.getQuantity() * multiplier;
						assert(purchaseQuantity > 0);

						long balancePriorToThisPurchase = priorActivity.newBalance*multiplier - purchaseQuantity;
						
						/*
						 * If the prior balance was short (long) then something
						 * is wrong because some or all of this purchase (sale)
						 * should have been matched.
						 */
						assert(balancePriorToThisPurchase >= 0);

						if (balancePriorToThisPurchase < amountLeftToMatch) {
							// Part of this activity is the cost basis.
							long quantityMatchedToThisPurchase = amountLeftToMatch - balancePriorToThisPurchase;

							// Calculate the portion of the cost that matches this purchase.
							long thisCostBasis = (long)
							((double)quantityMatchedToThisPurchase
							* (double)priorActivity.getCost()
							/ (double)priorActivity.getQuantity());

							// Reduce both the sale and the purchase amounts
							amountLeftToMatch -= quantityMatchedToThisPurchase;
							priorActivity.matchAmount(quantityMatchedToThisPurchase, thisCostBasis);

							/*
							 * If there is no later activity then this activity is the one of interest.
							 */
							if (allActivityNodes.higherKey(eachKey) == null) {
								// Add to the list of matches for this sale
								bases.add(new CostBasis(quantityMatchedToThisPurchase, priorActivity.key.date, thisCostBasis));
							}

							/*
							 * If the full sale has been matched to purchases, we are done matching
							 * this sale.
							 */
							if (amountLeftToMatch == 0) {
								break;
							}
						}
						
						activity = priorActivity;
					} while (true);
				}
			}
			return bases;
		}
	}
	public class CostBasis {

		long quantity;
		Date date;
		long basis;

		public CostBasis(long quantity, Date date, long basis) {
			this.quantity = quantity;
			this.date = date;
			this.basis = basis;
		}
	}

	public class TransferActivity {

	}

	/**
	 * An immutable key for the activity node.
	 * <P>
	 * Technically it does not matter whether purchases on the same date are
	 * presumed to have occurred before or after the sales. If the security was
	 * owned before this date then the sales will be matched to the earliest
	 * possible purchases, and if there is not sufficient security owned before
	 * this date to cover the sales then they will be matched to the purchases
	 * made on this date even if the purchases were later in the day (this being
	 * a short-sale).
	 */
	public class ActivityKey implements Comparable<ActivityKey> {
		final Date date;
		final int orderSequence;

		ActivityKey(Date date, int orderSequence) {
			this.date = date;
			this.orderSequence = orderSequence;
		}

		@Override
		public int compareTo(ActivityKey otherNode) {
			int dateOrder = date.compareTo(otherNode.date);
			if (dateOrder != 0) {
				return dateOrder;
			}

			int typeOrder = orderSequence - otherNode.orderSequence;
			return typeOrder;
		}
	}

	public abstract class ActivityNode {

		ActivityKey key;

		/** 
		 * quantity of security purchased or sold, never zero or negative
		 */
		long quantity;

		/** 
		 * cost of or proceeds from security purchased or sold, never zero or negative
		 */
		long cost;

		/**
		 * The stock balance in the account AFTER this activity
		 */
		long newBalance;

		/**
		 * When going through the matching process, this is the amount
		 * of the stock quantity of a purchase that has been matched to
		 * a sale.
		 * <P>
		 * This field is not applicable if the node is not a purchase (or a short sale). 
		 */
		public long securityAmountMatched = 0;

		Set<ActivityNode> previousNodes = new HashSet<ActivityNode>();
		Set<ActivityNode> nextNodes = new HashSet<ActivityNode>();

		/**
		 * Create a node that represents a purchase (if quantity is positive and amount is negative)
		 * or a sale (if quantity is negative and amount is positive) of the security.
		 * 
		 * @param purchaseQuantity
		 * @param newStockBalance 
		 * @param saleProceeds 
		 */
		public ActivityNode(Date date, long newStockBalance) {
			this.key = new ActivityKey(date, this.getOrderSequence());
			this.newBalance = newStockBalance;
		}

		/**
		 * 
		 * @param quantityMatched the quantity which has been matched, always positive regardless
		 * 			of whether this is a purchase and a sale is being matched to it or this
		 * 			is a short sale and a purchase is being matched to it
		 * @param amountMatched the basis amount which has been matched, always positive regardless
		 * 			of whether this is a purchase and a sale is being matched to it or this
		 * 			is a short sale and a purchase is being matched to it
		 */
		public void matchAmount(long quantityMatched, long amountMatched) {
			quantity -= quantityMatched;
			cost -= amountMatched;
			assert (quantity >= 0);
			assert(cost >= 0);
		}

		public long getCost() {
			return cost;
		}

		public long getQuantity() {
			return quantity;
		}

		public void addPreviousNode(ActivityNode previousNode) {
			previousNodes.add(previousNode);
		}

		public void addNextNode(ActivityNode nextNode) {
			nextNodes.add(nextNode);
		}

		protected abstract int getOrderSequence();
	}

	class PurchaseActivityNode extends ActivityNode {

		public PurchaseActivityNode(Date date,long newStockBalance, long purchaseQuantity, long purchaseCost) {
			super(date, newStockBalance);

			this.quantity = purchaseQuantity;
			this.cost = purchaseCost;
		}

		@Override
		protected int getOrderSequence() {
			return 1;
		}
	}

	class SaleActivityNode extends ActivityNode {

		public SaleActivityNode(Date date, long newStockBalance, long saleQuantity,	long saleProceeds) {
			super(date, newStockBalance);

			this.quantity = saleQuantity;
			this.cost = saleProceeds;
		}

		@Override
		protected int getOrderSequence() {
			return 2;
		}
	}

	public class UnsupportedDataException extends Exception {
		private static final long serialVersionUID = 1L;

		private IStatus status;

		public UnsupportedDataException(IStatus status) {
			this.status = status;
		}

		public IStatus getStatus() {
			return status;
		}
	}

	/**
	 * This class handles the situation where there are multiple sales of
	 * a stock on the same day and/or multiple purchases of a stock on the
	 * same day and where these sales or purchases are split across different
	 * accounts.
	 * <P>
	 * This is a very unlikely situation.  It is almost certainly the least likely
	 * of all cases covering in the capital gains calculations to actually occur
	 * in practice.  It is here because completeness demands it.
	 * <P>
	 * Consider the following scenario:
	 * 
	 * On 1st January, 100 shares of Acme were purchased in brokerage account A at
	 * a price of $1.00 each and on the same day 50 shares of Acme were purchased in
	 * brokerage account B at a price of $1.02.
	 * 
	 * On 1st February, 40 shares of Acme were transferred from account A to account B.
	 * 
	 * On 1st March, 30 shares were sold in account A and on the same day 60 shares were sold
	 * in account B. 
	 * 
	 * What is the basis for each of the two lots of sales?
	 * 
	 * We have a couple of rules to consider.  One is that we should get exactly the
	 * same matching regardless of which batch we process first.  The second rule is that all
	 * purchases that could potentially match a sale are considered equally.  There is no
	 * rule that gives a preference to shares that were bought in the same account.  So in the
	 * example above, instead of tranferring 50 shares from account A to account B, we could instead
	 * have transferred 50 shares from account A to account C and then the next day transfer all the shares in account B
	 * to account A.  That would give us exactly the same possible matching, except that the shares that
	 * were left in account A before are now left in account C, and the shares that were left in account B
	 * before are now left in account A.  We want to get the same answer.  The reason for this rule
	 * is that it does not simplify anything to give a preference to shares that have not been transferred,
	 * in fact it complicates things.
	 * 
	 * 
	 * The simplest algorithm that solves this is as follows:
	 * 
	 * 1. For every sale, go back through the graph and, for each place where there
	 * is more than one prior node, split the amount in proportion to the amounts passed
	 * in each connection.
	 * 
	 * 2. When this is done, it is possible that one node will have more sales matched to
	 * it than the amount of the purchase.
	 */


	/*
	 * Consider this scenario.  Buy 100 shares in account A at $1 each.  A year later short 100 shares in account B at $2 each.
	 * Now because these accounts are not connected (there has been no transfer between them), the sale is not considered a
	 * realization of gains.  While not being sure if this is the proper treatment, it is how they are treated by this algorithm.
	 * 
	 * If the shares in account A are sold or shares are bought in account B then a realization would have been made at that time.
	 * However, suppose 100 shares are transferred from account A to account B and the positions closed out.  Clearly there must
	 * be a realization at that point.  We don't know the value on the day of the transfer and nor is it relevant.  A gain of $200
	 * is realized when that transfer is done.
	 * 
	 * We really don't want to look at accounts that are not connected.  Suppose one account is a person's name
	 * and the other account is in a company name (and the company is not an S type corporation or anything like that).
	 * Then we really shouldn't be matching them up.  Only if a transfer of stock is made between the accounts should
	 * we be matching them up.
	 * 
	 * So this leaves us with the situation where a transfer can realize a gain or loss.  The date of the gain or loss should
	 * really be the last of the date of the purchase and the date of the sale.  This means it is retroactive and would require
	 * a filing of an amended return.
	 * 
	 * What a mess.
	 * 
	 * So we really have to consider all accounts selected by the user for the capital gains report
	 * to be connected in the sense that a long position in one can match a short position in another,
	 * even if no transfers have been made that connect the two accounts.
	 */
}