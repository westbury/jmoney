package net.sf.jmoney.stocks.gains;

/**
 * This class handles the situation where there are multiple sales of a stock on
 * the same day and/or multiple purchases of a stock on the same day and where
 * these sales or purchases are split across different accounts.
 * <P>
 * This is a very unlikely situation. It is almost certainly the least likely of
 * all cases covering in the capital gains calculations to actually occur in
 * practice. It is here because completeness demands it.
 * <P>
 * Consider the following scenario:
 * <P>
 * On 1st January, 100 shares of Acme were purchased in brokerage account A at a
 * price of $1.00 each and on the same day 50 shares of Acme were purchased in
 * brokerage account B at a price of $1.02.
 * <P>
 * On 1st February, 40 shares of Acme were transferred from account A to account
 * B.
 * <P>
 * On 1st March, 30 shares were sold in account A and on the same day 60 shares
 * were sold in account B.
 * <P>
 * What is the basis for each of the two lots of sales?
 * <P>
 * We have a couple of rules to consider. One is that we should get exactly the
 * same matching regardless of which batch we process first. The second rule is
 * that all purchases that could potentially match a sale are considered
 * equally. There is no rule that gives a preference to shares that were bought
 * in the same account. So in the example above, instead of tranferring 50
 * shares from account A to account B, we could instead have transferred 50
 * shares from account A to account C and then the next day transfer all the
 * shares in account B to account A. That would give us exactly the same
 * possible matching, except that the shares that were left in account A before
 * are now left in account C, and the shares that were left in account B before
 * are now left in account A. We want to get the same answer. The reason for
 * this rule is that it does not simplify anything to give a preference to
 * shares that have not been transferred, in fact it complicates things.
 * <P>
 *
 * The simplest algorithm that solves this is as follows:
 * <OL>
 * <LI>For every sale, go back through the graph and, for each place where there
 * is more than one prior node, split the amount in proportion to the amounts
 * passed in each connection.</LI>
 * <LI>When this is done, it is possible that one node will have more sales
 * matched to it than the amount of the purchase. In this situation we take the
 * excess and re-assign it back to the nodes from which it came. This is done is
 * proportion to the amount that originally came from each node.</LI>
 * <LI>When this is done, those nodes will now have amounts that still need to
 * be matched to prior nodes. We repeat the process but in proportion to the
 * available amount from each previous node.</LI>
 * <LI>Repeat back and forth and we will always eventually have a solution
 * (proof to be provided).</LI>
 * <OL>
 */
public class SpecialCase {
	// TODO
}
