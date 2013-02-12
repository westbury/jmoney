package net.sf.jmoney.stocks.gains;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public abstract class ActivityNode {

	private ActivityKey key;

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

	public ActivityKey getKey() {
		return key;
	}
}