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

package net.sf.jmoney.stocks.model;

import java.util.Date;

import net.sf.jmoney.isolation.IObjectKey;
import net.sf.jmoney.model2.EntryExtension;
import net.sf.jmoney.model2.ExtendableObject;

/**
 * Property set implementation class for the properties added
 * to each Entry object by this plug-in.
 * 
 * @author Nigel Westbury
 */
public class StockEntry extends EntryExtension {

	/**
	 * The security (Stock or Bond) involved in this entry.
	 * <P>
	 * This property is used for entries such as dividend payments, where no
	 * change the amount of the security is involved but we do want to associate
	 * the cash amount with a security.
	 */
	protected IObjectKey securityKey = null;
	
	/**
	 * The date on which the deal was made. On most stock exchanges this is
	 * different from the settlement date on which the money and stock is paid
	 * or received.
	 * <P>
	 * This property is applicable only if this entry represents a stock
	 * credited to the account as a result of a purchase or stock debited from
	 * the account as a result of a sale.
	 */
	private Date bargainDate = null;

	/**
	 * A default constructor is mandatory for all extension objects.
	 * The default constructor sets the extension properties to
	 * appropriate default values.
	 * 
	 * @param extendedObject 
	 */
	public StockEntry(ExtendableObject extendedObject) {
		super(extendedObject);
	}
	
	
	/**
	 * A Full constructor is mandatory for all extension objects.
	 * This constructor is called by the datastore to construct
	 * the extension objects when loading data.
	 */
	public StockEntry(ExtendableObject extendedObject, IObjectKey securityKey, Date bargainDate) {
		super(extendedObject);
		this.securityKey = securityKey;
		this.bargainDate = bargainDate;
	}
	
	/**
	 * Gets the security involved in this entry.
	 * <P>
	 * This property is used for entries such as dividend payments, where no
	 * change the amount of the security is involved but we do want to associate
	 * the cash amount with a security.
	 * 
	 * @return An object of type Stock or Bond, or null if this entry does not
	 * 		represent a dividend or other entry that is associated with a security
	 * 		but does not involve a gain or loss in the amount of that security in
	 * 		the account
	 */
	public Security getSecurity() {
		return securityKey == null ? null : (Security)securityKey.getObject();
	}
	
	/**
	 * Sets the security involved in this entry.
	 * <P>
	 * This property is used for entries such as dividend payments, where no
	 * change the amount of the security is involved but we do want to associate
	 * the cash amount with a security.
	 * 
	 * @param security An object of type Stock or Bond.
	 */
	public void setSecurity(Security security) {
		Security oldSecurity = getSecurity();
		this.securityKey = (security == null) ? null : security.getObjectKey();

		// Notify the change manager.
		processPropertyChange(StockEntryInfo.getSecurityAccessor(), oldSecurity, security);
	}

	/**
	 * @return The date on which the deal was made.
	 */
	public Date getBargainDate() {
		return bargainDate;
	}
	
	/**
	 * @param bargainDate The date on which the deal was made.
	 */
	public void setBargainDate(Date bargainDate) {
		Date oldBargainDate = this.bargainDate;
		this.bargainDate = bargainDate;
		processPropertyChange(StockEntryInfo.getBargainDateAccessor(), oldBargainDate, bargainDate);
	}
}
