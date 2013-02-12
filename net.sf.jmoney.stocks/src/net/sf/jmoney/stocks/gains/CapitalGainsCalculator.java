package net.sf.jmoney.stocks.gains;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import net.sf.jmoney.model2.Commodity;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.TransactionInfo;
import net.sf.jmoney.stocks.StocksPlugin;
import net.sf.jmoney.stocks.model.Security;
import net.sf.jmoney.stocks.model.Stock;
import net.sf.jmoney.stocks.model.StockAccount;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;

public class CapitalGainsCalculator {

	/**
	 * Date format to be used when showing dates to the user in messages.
	 */
	static DateFormat userDateFormat = new SimpleDateFormat("MMM/dd/yy");

	public static IStatus exportCapitalGains(StockAccount account, Date startDate, Date endDate, Collection<StockPurchaseAndSale> matchedPurchaseAndSales) throws IOException {
		MultiStatus result = new MultiStatus(StocksPlugin.PLUGIN_ID, IStatus.INFO, "Export Account: " + account.getName(), null);

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

					MultiStatus disposalResult = new MultiStatus(StocksPlugin.PLUGIN_ID, IStatus.INFO,
							MessageFormat.format(
									"Sale of {2} of {0} took place on {1}.",
									stock.getName(),
									userDateFormat.format(entry.getTransaction().getDate()),
									formatQuantity(saleQuantity)
							),
							null);

					System.out.println(MessageFormat.format(
							"Sale of {2} of {0} took place on {1}.",
							stock.getName(),
							userDateFormat.format(entry.getTransaction().getDate()),
							formatQuantity(saleQuantity)
					));

					if (stock.getName().startsWith("Laird")) {
						System.out.println("Laird found");
					}

					try {

						/*
						 * We build a set of all currencies, securities, and other commodities
						 * that were acquired or lost in this transaction.  If there are multiple
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
									Status status = new Status(IStatus.ERROR, StocksPlugin.PLUGIN_ID,
											MessageFormat.format(
													"The disposal involves {0} being transferred to/from another account.  Manually determination of the cost basis is required.",
													eachEntry.getCommodityInternal().getName()
											),
											null);
									disposalResult.add(status);
								}

								if (transactionMap.containsKey(eachEntry.getCommodityInternal())) {
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
							Status status = new Status(IStatus.ERROR, StocksPlugin.PLUGIN_ID,
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
							Status status = new Status(IStatus.INFO, StocksPlugin.PLUGIN_ID,
									MessageFormat.format(
											"{0} {1} were acquired in exchange for this stock.  This is not a disposal for capital gains tax purposes and nothing has been output to the TXF file.",
											formatQuantity(transactionMap.get(otherCommodity)),
											otherCommodity.getName()
									),
									null);
							throw new UnsupportedDataException(status);
						} else if (otherCommodity != account.getCurrency()) {
							Status status = new Status(IStatus.ERROR, StocksPlugin.PLUGIN_ID,
									MessageFormat.format(
											"The stock was exchanged for {0}.  Disposals for anything other than the cash (in the currency of the account) or another security is too complicated.  Manual determination of the cost basis is required.",
											otherCommodity.getName()
									),
									null);
							throw new UnsupportedDataException(status);
						}

						long saleProceeds = transactionMap.get(otherCommodity);

						if (saleProceeds <= 0) {
							Status status = new Status(IStatus.ERROR, StocksPlugin.PLUGIN_ID,
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
							/ saleQuantity;

							/*
							 * Write out an entry to the TXF file.  Note that if the sale is matched
							 * to more than one purchase then multiple entries will be written.  We
							 * could combine the entries into just two entries, one for those that are
							 * matched to purchases that would make this a short term sale, and those that
							 * match to purchases that would make this a long term sale.  However, let's just
							 * leave the tax software to deal with that.
							 */
							matchedPurchaseAndSales.add(new StockPurchaseAndSale(stock, basis.quantity, basis.date, basis.basis, sellDate, (long)thisProceeds));

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
	 *
	 * @param date the date of the taxable event, so no transactions after this date are
	 * 				considered
	 * @throws UnsupportedDataException
	 */

	static TreeMap<Date, StockActivity> getStockActivity(StockAccount account, Stock stock, Date dateOfTaxableEvent, MultiStatus result) throws UnsupportedDataException {
		TreeMap<Date, StockActivity> stockEntries = new TreeMap<Date, StockActivity>();

		/*
		 * Total stock in account.  We initially accumulate all stock amounts here so we
		 * get the balance of this stock at the time the sale was made.  We then adjust
		 * it by reversing out the amounts as we go back through the history.
		 */
		long totalStock = 0;

		for (Entry entry2 : account.getEntries()) {
			if (entry2.getCommodityInternal() == stock && !entry2.getTransaction().getDate().after(dateOfTaxableEvent)) {
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
					Status status = new Status(IStatus.WARNING, StocksPlugin.PLUGIN_ID,
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

	static private String formatQuantity(long quantity) {
		if (quantity % 1000 == 0) {
			return Long.toString(quantity/1000);
		} else {
			return new BigDecimal(quantity).divide(new BigDecimal(1000)).toPlainString();
		}
	}

}
