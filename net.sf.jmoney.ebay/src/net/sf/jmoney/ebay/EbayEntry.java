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

package net.sf.jmoney.ebay;

import java.io.IOException;
import java.util.Date;

import net.sf.jmoney.fields.IBlob;
import net.sf.jmoney.fields.IPersistentBlob;
import net.sf.jmoney.model2.EntryExtension;
import net.sf.jmoney.model2.ExtendableObject;

/**
 * Property set implementation class for the properties added
 * to each Entry object by this plug-in.
 * 
 * @author Nigel Westbury
 */
public class EbayEntry extends EntryExtension {

	/**
	 * the Ebay order number.  More than one entry in the order history import may have the same order
	 * number.  If so, they are charged together as a single charge.
	 */
	private String orderNumber = null;
	
	private String itemNumber = null;
	
	private Date deliveryDate = null;

	private String ebayDescription = null;

	private String soldBy = null;

	private String imageCode = null;

	/**
	 * picture of the item in JPEG format
	 */
	private IBlob picture = null;

	/**
	 * JSON string containing map of item attributes (size, color etc)
	 */
	private String detail;
	
	/**
	 * A default constructor is mandatory for all extension objects.
	 * The default constructor sets the extension properties to
	 * appropriate default values.
	 * 
	 * @param extendedObject 
	 */
	public EbayEntry(ExtendableObject extendedObject) {
		super(extendedObject);
	}
	
	
	/**
	 * A Full constructor is mandatory for all extension objects.
	 * This constructor is called by the datastore to construct
	 * the extension objects when loading data.
	 */
	public EbayEntry(ExtendableObject extendedObject, String orderId, String itemNumber, Date deliveryDate, String ebayDescription, String soldBy, String imageCode, IBlob picture, String detail) {
		super(extendedObject);
		this.orderNumber = orderId;
		this.itemNumber = itemNumber;
		this.deliveryDate = deliveryDate;
		this.ebayDescription = ebayDescription;
		this.soldBy = soldBy;
		this.imageCode = imageCode;
		this.picture = picture;
		this.detail = detail;
	}
	
	public String getOrderNumber() {
		return orderNumber;
	}
	
	public void setOrderNumber(String orderNumber) {
		String oldOrderNumber = this.orderNumber;
		this.orderNumber = orderNumber;

		// Notify the change manager.
		processPropertyChange(EbayEntryInfo.getOrderNumberAccessor(), oldOrderNumber, orderNumber);
	}
	
	public String getItemNumber() {
		return itemNumber;
	}
	
	public void setItemNumber(String itemNumber) {
		String oldItemNumber = this.itemNumber;
		this.itemNumber = itemNumber;

		// Notify the change manager.
		processPropertyChange(EbayEntryInfo.getItemNumberAccessor(), oldItemNumber, itemNumber);
	}
	
	public Date getDeliveryDate() {
		return deliveryDate;
	}
	
	public void setDeliveryDate(Date deliveryDate) {
		Date oldDeliveryDate = this.deliveryDate;
		this.deliveryDate = deliveryDate;

		// Notify the change manager.
		processPropertyChange(EbayEntryInfo.getDeliveryDateAccessor(), oldDeliveryDate, deliveryDate);
	}

	public String getEbayDescription() {
		return ebayDescription;
	}
	
	public void setEbayDescription(String ebayDescription) {
		String oldEbayDescription = this.ebayDescription;
		this.ebayDescription = ebayDescription;

		// Notify the change manager.
		processPropertyChange(EbayEntryInfo.getEbayDescriptionAccessor(), oldEbayDescription, ebayDescription);
	}

	public String getSoldBy() {
		return soldBy;
	}
	
	public void setSoldBy(String soldBy) {
		String oldSoldBy = this.soldBy;
		this.soldBy = soldBy;

		// Notify the change manager.
		processPropertyChange(EbayEntryInfo.getSoldByAccessor(), oldSoldBy, soldBy);
	}

	public String getImageCode() {
		return imageCode;
	}
	
	public void setImageCode(String imageCode) {
		String oldImageCode = this.imageCode;
		this.imageCode = imageCode;

		// Notify the change manager.
		processPropertyChange(EbayEntryInfo.getImageCodeAccessor(), oldImageCode, imageCode);
	}

	public IBlob getPicture() {
		return picture;
	}
	
	/**
	 * Note that the object being set here is not IBlob but IBlobWriter. IBlobWriter extends
	 * IBlob but adds no methods. It is a marker interface that indicates the blob has been reliably
	 * cached and that the IBlob methods will work indefinitely, even if the original blob source
	 * has been deleted.
	 * 
	 * @param picture
	 */
	public void setPicture(IBlob picture) {
		// We must cache now, because the blob we are given may have a limited lifetime.
		IPersistentBlob persistentBlob;
		try {
			persistentBlob = picture == null ? null : picture.createPersistentBlob();
		} catch (IOException e) {
			// May not be the best way of handling this error, but it's a whole
			// lot better than having this error when the data is committed to the datastore.
			// (Which is what would happen if we didn't convert to a persisent blob here.)
			throw new RuntimeException(e);
		}
		
		IBlob oldPicture = this.picture;
		this.picture = persistentBlob;

		// Notify the change manager.
		processPropertyChange(EbayEntryInfo.getPictureAccessor(), oldPicture, persistentBlob);
	}


	public String getDetail() {
		return detail;
	}
	
	public void setDetail(String detail) {
		String oldDetail = this.detail;
		this.detail = detail;

		// Notify the change manager.
		processPropertyChange(EbayEntryInfo.getDetailAccessor(), oldDetail, detail);
	}

}
