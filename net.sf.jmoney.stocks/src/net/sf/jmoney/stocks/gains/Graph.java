package net.sf.jmoney.stocks.gains;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;

import net.sf.jmoney.stocks.StocksPlugin;
import net.sf.jmoney.stocks.model.Stock;
import net.sf.jmoney.stocks.model.StockAccount;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;

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
		TreeMap<Date, StockActivity> stockEntries = CapitalGainsCalculator.getStockActivity(account, stock, result);
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
				Status status = new Status(IStatus.ERROR, StocksPlugin.PLUGIN_ID,
						MessageFormat.format(
								"Two or more transactions involving {0} took place on {1} and these transactions took place in different but connected accounts.  This scenario is not currently supported.",
								stock.getName(),
								CapitalGainsCalculator.userDateFormat.format(eachKey)
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
						return node1.getKey().compareTo(node2.getKey());
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
						Status status = new Status(IStatus.ERROR, StocksPlugin.PLUGIN_ID,
								MessageFormat.format(
										"An internal error has occured.  A manual determination must be made for the cost basis for the sale of {0} which took place on {1}.",
										stock.getName(),
										CapitalGainsCalculator.userDateFormat.format(eachKey)
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
						/ priorActivity.getQuantity());

						// Reduce both the sale and the purchase amounts
						amountLeftToMatch -= quantityMatchedToThisPurchase;
						priorActivity.matchAmount(quantityMatchedToThisPurchase, thisCostBasis);

						/*
						 * If there is no later activity then this activity is the one of interest.
						 */
						if (allActivityNodes.higherKey(eachKey) == null) {
							// Add to the list of matches for this sale
							bases.add(new CostBasis(quantityMatchedToThisPurchase, priorActivity.getKey().date, thisCostBasis));
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