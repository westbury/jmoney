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

package net.sf.jmoney.amazon;

import java.util.Date;

import net.sf.jmoney.fields.IBlob;
import net.sf.jmoney.model2.EntryExtension;
import net.sf.jmoney.model2.ExtendableObject;

/**
 * Property set implementation class for the properties added
 * to each Entry object by this plug-in.
 * 
 * @author Nigel Westbury
 */
public class AmazonEntry extends EntryExtension {

	/**
	 * the Amazon order id, being unique for each order but the same for all items
	 * ordered at the same time
	 */
	private String orderId = null;
	
	/**
	 * the carrier and tracking number, being used when importing items and
	 * orders to match the items to the orders
	 */
	private String trackingNumber = null;
	
	/**
	 * the shipment date which is not necessarily the same as the order date,
	 * this field being also used to match items and orders because sometimes
	 * the tracking number is blank
	 */
	private Date shipmentDate = null;

	private String asinOrIsbn = null;

	private String imageCode = null;

	/**
	 * picture of the item in JPEG format
	 */
	private IBlob picture = null;
	
	/**
	 * A default constructor is mandatory for all extension objects.
	 * The default constructor sets the extension properties to
	 * appropriate default values.
	 * 
	 * @param extendedObject 
	 */
	public AmazonEntry(ExtendableObject extendedObject) {
		super(extendedObject);
	}
	
	
	/**
	 * A Full constructor is mandatory for all extension objects.
	 * This constructor is called by the datastore to construct
	 * the extension objects when loading data.
	 */
	public AmazonEntry(ExtendableObject extendedObject, String orderId, String trackingNumber, Date shipmentDate, String asinOrIsbn, String imageCode, IBlob picture) {
		super(extendedObject);
		this.orderId = orderId;
		this.trackingNumber = trackingNumber;
		this.shipmentDate = shipmentDate;
		this.asinOrIsbn = asinOrIsbn;
		this.imageCode = imageCode;
		this.picture = picture;
	}
	
	public String getOrderId() {
		return orderId;
	}
	
	public void setOrderId(String orderId) {
		String oldOrderId = this.orderId;
		this.orderId = orderId;

		// Notify the change manager.
		processPropertyChange(AmazonEntryInfo.getOrderIdAccessor(), oldOrderId, orderId);
	}
	
	public String getTrackingNumber() {
		return trackingNumber;
	}
	
	public void setTrackingNumber(String trackingNumber) {
		String oldTrackingNumber = this.trackingNumber;
		this.trackingNumber = trackingNumber;

		// Notify the change manager.
		processPropertyChange(AmazonEntryInfo.getTrackingNumberAccessor(), oldTrackingNumber, trackingNumber);
	}
	
	public Date getShipmentDate() {
		return shipmentDate;
	}
	
	public void setShipmentDate(Date shipmentDate) {
		Date oldShipmentDate = this.shipmentDate;
		this.shipmentDate = shipmentDate;

		// Notify the change manager.
		processPropertyChange(AmazonEntryInfo.getShipmentDateAccessor(), oldShipmentDate, shipmentDate);
	}

	public String getAsinOrIsbn() {
		return asinOrIsbn;
	}
	
	public void setAsinOrIsbn(String asinOrIsbn) {
		String oldAsinOrIsbn = this.asinOrIsbn;
		this.asinOrIsbn = asinOrIsbn;

		// Notify the change manager.
		processPropertyChange(AmazonEntryInfo.getAsinOrIsbnAccessor(), oldAsinOrIsbn, asinOrIsbn);
	}

	public String getImageCode() {
		return imageCode;
	}
	
	public void setImageCode(String imageCode) {
		String oldImageCode = this.imageCode;
		this.imageCode = imageCode;

		// Notify the change manager.
		processPropertyChange(AmazonEntryInfo.getImageCodeAccessor(), oldImageCode, imageCode);
	}

	public IBlob getPicture() {
		return picture;
	}
	
	public void setPicture(IBlob picture) {
		IBlob oldPicture = this.picture;
		this.picture = picture;

		// Notify the change manager.
		processPropertyChange(AmazonEntryInfo.getPictureAccessor(), oldPicture, picture);
	}
}
