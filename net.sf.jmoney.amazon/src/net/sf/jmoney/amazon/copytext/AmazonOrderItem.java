package net.sf.jmoney.amazon.copytext;

import java.io.IOException;
import java.io.InputStream;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.Display;

import net.sf.jmoney.amazon.AmazonEntry;
import net.sf.jmoney.fields.IBlob;

public class AmazonOrderItem {

	private AmazonEntry entry;
	
	/** Lazily created from image in entry */
	ImageData imageData = null;

	private String soldBy;

	private String author;

	private String returnDeadline;
	
	public AmazonOrderItem(AmazonEntry entry) {
		this.entry = entry;
	}

	public String getDescription() {
		return entry.getMemo();
	}

	public AmazonEntry getEntry() {
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

	public void setSoldBy(String soldBy) {
		this.soldBy = soldBy;
	}

	public String getSoldBy() {
		return soldBy;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	public String getAuthor() {
		return author;
	}

	public void setReturnDeadline(String returnDeadline) {
		this.returnDeadline = returnDeadline;
	}

	public String getReturnDeadline() {
		return returnDeadline;
	}

}
