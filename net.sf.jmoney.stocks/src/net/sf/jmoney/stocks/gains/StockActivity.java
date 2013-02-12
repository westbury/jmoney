package net.sf.jmoney.stocks.gains;

import java.text.MessageFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.jmoney.stocks.StocksPlugin;
import net.sf.jmoney.stocks.model.Security;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

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
			Status status = new Status(IStatus.WARNING, StocksPlugin.PLUGIN_ID, 
					MessageFormat.format(
							"Bad data on {0}.",
							CapitalGainsCalculator.userDateFormat.format(date)
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