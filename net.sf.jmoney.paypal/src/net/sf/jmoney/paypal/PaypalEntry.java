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

package net.sf.jmoney.paypal;

import java.net.URL;

import net.sf.jmoney.model2.EntryExtension;
import net.sf.jmoney.model2.ExtendableObject;

/**
 * Property set implementation class for the properties added
 * to each Entry object by this plug-in.
 * 
 * @author Nigel Westbury
 */
public class PaypalEntry extends EntryExtension {

	/**
	 * e-mail address of the merchant
	 */
	private String merchantEmail = null;
	
	/**
	 * The URL of the item that was purchased or sold
	 */
	private URL itemUrl = null;

	/**
	 * A default constructor is mandatory for all extension objects.
	 * The default constructor sets the extension properties to
	 * appropriate default values.
	 * 
	 * @param extendedObject 
	 */
	public PaypalEntry(ExtendableObject extendedObject) {
		super(extendedObject);
	}
	
	
	/**
	 * A Full constructor is mandatory for all extension objects.
	 * This constructor is called by the datastore to construct
	 * the extension objects when loading data.
	 */
	public PaypalEntry(ExtendableObject extendedObject, String merchantEmail, URL itemUrl) {
		super(extendedObject);
		this.merchantEmail = merchantEmail;
		this.itemUrl = itemUrl;
	}
	
	public String getMerchantEmail() {
		return merchantEmail;
	}
	
	public void setMerchantEmail(String merchantEmail) {
		String oldMerchantEmail = this.merchantEmail;
		this.merchantEmail = merchantEmail;

		// Notify the change manager.
		processPropertyChange(PaypalEntryInfo.getMerchantEmailAccessor(), oldMerchantEmail, merchantEmail);
	}
	
	public URL getItemUrl() {
		return itemUrl;
	}
	
	public void setItemUrl(URL itemUrl) {
		URL oldItemUrl = this.itemUrl;
		this.itemUrl = itemUrl;

		// Notify the change manager.
		processPropertyChange(PaypalEntryInfo.getItemUrlAccessor(), oldItemUrl, itemUrl);
	}
}
