package amazonscraper;

public class AmazonOrderItem {

	AmazonShipment shipment;
	
	private IItemUpdater updater;
	
	private String soldBy;

	private String author;

	private String returnDeadline;

	int quantity = 1;

	private long unitPrice;
	
	public AmazonOrderItem(AmazonShipment shipment, IItemUpdater updater) {
		this.shipment = shipment;
		this.updater = updater;
	}

	public void setUnitPrice(long unitPrice) {
		this.unitPrice = unitPrice;
	}

	public long getUnitPrice() {
		return unitPrice;
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
}
