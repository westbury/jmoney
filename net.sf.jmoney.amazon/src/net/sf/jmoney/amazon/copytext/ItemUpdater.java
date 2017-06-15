package net.sf.jmoney.amazon.copytext;

import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.Display;

import amazonscraper.IItemUpdater;
import net.sf.jmoney.amazon.AccountFinder;
import net.sf.jmoney.amazon.AmazonEntry;
import net.sf.jmoney.fields.IBlob;
import net.sf.jmoney.importer.wizards.ImportException;
import net.sf.jmoney.model2.IncomeExpenseAccount;

public class ItemUpdater implements IItemUpdater {

	private AccountFinder accountFinder;
	
	private AmazonEntry entry;
	
	/** Lazily created from image in entry */
	ImageData imageData = null;

	/** first part of description, without quantity appended */
	private String description;
	
	private int quantity;

	private boolean isMovie = false;
	
	protected static Pattern descWithQuantityPattern;
	static {
		descWithQuantityPattern = Pattern.compile("(.*) x(\\d+)");
	}

	protected static Pattern streamedMoviePattern;
	static {
		streamedMoviePattern = Pattern.compile("streamed movie - \"(.*)\"");
	}

	public ItemUpdater(AmazonEntry entry, AccountFinder accountFinder) {
		this.entry = entry;
		this.accountFinder = accountFinder;
		
		if (entry.getMemo() == null) {
			description = "";
			quantity = 1;
		} else {
			Matcher m = streamedMoviePattern.matcher(entry.getMemo());
			if (m.matches()) {
				description = m.group(1);
				quantity = 1;
				isMovie = true;
			} else {
			m = descWithQuantityPattern.matcher(entry.getMemo());
			if (m.matches()) {
				description = m.group(1);
				quantity = Integer.parseInt(m.group(2));
			} else {
				description = entry.getMemo();
				quantity = 1;
			}
			}
		}
	}

	@Override
	public void setOrderNumber(String orderNumber) {
		entry.setOrderId(orderNumber);
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

	@Override
	public long getAmount() {
		return entry.getAmount();
	}

	@Override
	public String getAmazonDescription() {
		return entry.getAmazonDescription();
	}

	@Override
	public String getAsinOrIsbn() {
		return entry.getAsinOrIsbn();
	}

	@Override
	public String getImageCode() {
		return entry.getImageCode();
	}

	@Override
	public void setIntoReturnedItemAccount() {
		try {
			IncomeExpenseAccount returnedItemAccount = accountFinder.findReturnedItemsAccount();
			entry.setAccount(returnedItemAccount);
		} catch (ImportException e) {
			// If there is no 'returned item' account then we should error.
			throw new RuntimeException(e);
		}
	}

	@Override
	public void setAsinOrIsbn(String asinOrIsbn) {
		entry.setAsinOrIsbn(asinOrIsbn);
	}

	@Override
	public void setDescription(String description) {
		entry.setAmazonDescription(description);
		
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

	@Override
	public void setMovie(boolean isMovie) {
		this.isMovie = isMovie;
		buildDescription();
	}

	private void buildDescription() {
		if (isMovie) {
			assert quantity == 1;
			entry.setMemo("streamed movie - \"" + description + "\"");
		} else if (quantity == 1) {
			entry.setMemo(description);
		} else {
			entry.setMemo(description + " x" + quantity);
		}
	}

}
