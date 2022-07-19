package net.sf.jmoney.ebay.copytext;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.Display;

import ebayscraper.IItemUpdater;
import net.sf.jmoney.ebay.AccountFinder;
import net.sf.jmoney.ebay.EbayEntry;
import net.sf.jmoney.fields.IBlob;

public class ItemUpdater implements IItemUpdater {

	private AccountFinder accountFinder;
	
	private EbayEntry entry;
	
	/** Lazily created from image in entry */
	ImageData imageData = null;

	/** first part of description, without quantity appended */
	private String description;
	
	private int quantity;

	public ItemUpdater(EbayEntry entry, AccountFinder accountFinder) {
		this.entry = entry;
		this.accountFinder = accountFinder;
		
		if (entry.getMemo() == null) {
			description = "";
			quantity = 1;
		} else {
			description = entry.getMemo();
			quantity = 1;
		}
	}

	@Override
	public void setOrderNumber(String orderNumber) {
		entry.setOrderNumber(orderNumber);
	}

	@Override
	public void setItemNumber(String itemNumber) {
		entry.setItemNumber(itemNumber);
	}

	public EbayEntry getEntry() {
		return entry;
	}

	public void setImage(IBlob blob) {
		imageData = null;
		entry.setPicture(blob);
	}

	public Image getImage(Display display) {
		if (imageData == null) {
			IBlob pictureBlob = entry.getPicture();
			if (pictureBlob != null) {
				InputStream inputStream;
				try {
					inputStream = pictureBlob.createStream();
				} catch (IOException e1) {
					throw new RuntimeException(e1);
				}
				Image image = new Image(display, inputStream);
				
				imageData = image.getImageData();
				
				return image;
			} else {
				return null;
			}
		} else {
			return new Image(display, imageData);
		}
	}

	@Override
	public long getGrossCost() {
		return entry.getAmount();
	}

	@Override
	public void setGrossCost(long itemPrice) {
		entry.setAmount(itemPrice);
	}

	@Override
	public String getItemNumber() {
		return entry.getItemNumber();
	}

	@Override
	public String getEbayDescription() {
		return entry.getEbayDescription();
	}

	@Override
	public String getSoldBy() {
		return entry.getSoldBy();
	}

	@Override
	public String getImageCode() {
		return entry.getImageCode();
	}

	@Override
	public void setSoldBy(String seller) {
		entry.setSoldBy(seller);
	}

	@Override
	public void setDescription(String description) {
		entry.setEbayDescription(description);
		
		if (this.description.trim().isEmpty()) {
			this.description = description;
			buildDescription();
		}
	}

	@Override
	public void setQuantity(int quantity) {
		this.quantity = quantity;
		buildDescription();
	}

	private void buildDescription() {
		if (quantity == 1) {
			entry.setMemo(description);
		} else {
			entry.setMemo(description + " x" + quantity);
		}
	}

	@Override
	public void setPaidDate(Date paidDate) {
		// TODO Set the date on the charge entry?
		
	}

	@Override
	public void setDeliveryDate(Date deliveryDate) {
		entry.setDeliveryDate(deliveryDate);
	}

	@Override
	public void setImageCode(String imageCode) {
		entry.setImageCode(imageCode);
	}

}
