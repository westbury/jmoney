package amazonscraper;

public class AmazonOrderItem {

	AmazonShipment shipment;
	
	private IItemUpdater updater;
	
	private String soldBy;

	private String author;

	private String returnDeadline;

	int quantity = 1;

	private long netCost;

	private String amazonDescription;
	
	public AmazonOrderItem(AmazonShipment shipment, IItemUpdater updater) {
		this.shipment = shipment;
		this.updater = updater;
		
		this.netCost = updater.getNetCost();
		this.amazonDescription = updater.getAmazonDescription();
//???		this.quantity = updater.getQuantity();
	}

	/**
	 * If the quantity is more than one then this is the
	 * line item price, ie the unit price times
	 * the quantity.
	 */
	public void setNetCost(long netCost) {
		this.netCost = netCost;
		updater.setNetCost(netCost);
	}

	/**
	 * If the quantity is more than one then this is the
	 * line item price, ie the unit price times
	 * the quantity.
	 * 
	 * @return
	 */
	public long getNetCost() {
		return netCost;
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

	public void setQuantity(int quantity) {
		this.quantity = quantity;
		updater.setQuantity(quantity);
	}

	public int getQuantity() {
		return quantity;
	}


	public IItemUpdater getUnderlyingItem() {
		return updater;
	}

	public AmazonShipment getShipment() {
		return shipment;
	}

	public void setMovie(boolean isMovie) {
		updater.setMovie(isMovie);
	}

	public String getAmazonDescription() {
		return amazonDescription;
	}

	public void setAmazonDescription(String amazonDescription) {
		this.amazonDescription = amazonDescription;
		updater.setDescription(amazonDescription);
	}
}
