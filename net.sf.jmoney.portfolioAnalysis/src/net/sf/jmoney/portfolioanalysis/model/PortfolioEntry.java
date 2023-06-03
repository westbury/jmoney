/*
 *
 *  JMoney - A Personal Finance Manager
 *  Copyright (c) 2004 Nigel Westbury <westbury@users.sourceforge.net>
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

package net.sf.jmoney.portfolioanalysis.model;

import net.sf.jmoney.model2.EntryExtension;
import net.sf.jmoney.model2.ExtendableObject;

/**
 * An extension object that extends Entry objects. This extension object
 * maintains information that allows portfolio performance to be tracked.
 * <P>
 * A portfolio is not necessarily the same as an account. A portfolio is a group
 * of investments for which you wish to monitor the combined performance. For
 * example, you may purchase stock based on recommendations given on Jim
 * Cramer's Mad Money show (if you are so inclined). You don't open a new
 * account for such purchases, you use existing accounts but you want to mark
 * individual purchases in that account. You may have more than one brokerage
 * account and you invest according to Cramer's recommendations in whichever
 * account happens to have spare cash.
 * <P>
 * When a purchase is made, the portfolio or reason for the purchase is
 * indicated. This plug-in will track that security to the portfolio, looking
 * through spin-offs, take-overs and any other form of corporate re-structuring.
 * <P>
 * It may be you want to take a security out of a portfolio. For example, Cramer
 * says sell the security but your car mechanic says buy it. You decide to hold
 * the stock but you want to move the stock from one portfolio to another. This
 * is done by a transaction in which, for example, 100 stock in Acme is
 * exchanged for 100 stock in Acme. Both entries can then be marked accordingly.
 * This will cause a transaction to show up when you are, say, balancing your
 * brokerage account or doing your taxes when in fact you don't want it to show
 * up. Such a transaction should be shown as a no-op transaction and hopefully
 * will not interfere.
 * 
 * @author Nigel Westbury
 */
public class PortfolioEntry extends EntryExtension {
	
	private String portfolioName = null;
	
	/**
	 * A default constructor is mandatory for all extension objects.
	 * The default constructor sets the extension properties to
	 * appropriate default values.
	 * 
	 * @param extendedObject 
	 */
	public PortfolioEntry(ExtendableObject extendedObject) {
		super(extendedObject);
	}
	
	/**
	 * A Full constructor is mandatory for all extension objects.
	 * This constructor is called by the datastore to construct
	 * the extension objects when loading data.
	 * 
	 */
	public PortfolioEntry(ExtendableObject extendedObject, String portfolioName) {
		super(extendedObject);
		this.portfolioName = portfolioName;
	}
	
	/**
	 * Returns the name of the portfolio into which this security is placed.
	 * This is normally set for acquisitions only but could theoretically be set
	 * for disposals so that if the security is held in multiple portfolios and
	 * a partial disposal occurs then the user can indicate the portfolio from
	 * which the disposal is to be made.
	 */
	public String getPortfolioName() {
		return portfolioName;
	}
	
	public void setPortfolioName(String portfolioName) {
		String oldPortfolioName = this.portfolioName;
		this.portfolioName = portfolioName;
		processPropertyChange(PortfolioEntryInfo.getPortfolioNameAccessor(), oldPortfolioName, portfolioName);
	}
}
