		/**
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

package net.sf.jmoney.stocks.gains;