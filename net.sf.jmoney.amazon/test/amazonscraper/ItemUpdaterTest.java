package amazonscraper;

import org.json.JSONObject;

public class ItemUpdaterTest implements IItemUpdater {

	private long netCost;
	private String amazonDescription;
	private String asinOrIsbn;
	private String accountDescription = "default category";
	private String imageCode;
	private String description;
	private int quantity = 1;
	private boolean isMovie;
	private String orderNumber;

	public ItemUpdaterTest(long netCost) {
		this.netCost = netCost;
	}

	@Override
	public long getNetCost() {
		return netCost;
	}

	@Override
	public void setNetCost(long netCost) {
		this.netCost = netCost;
		
	}

	@Override
	public String getAmazonDescription() {
		return amazonDescription;
	}

	@Override
	public String getAsinOrIsbn() {
		return asinOrIsbn;
	}

	@Override
	public void setIntoReturnedItemAccount() {
		accountDescription = "returned item category";
	}

	@Override
	public void setIntoGiftcardAccount() {
		accountDescription = "giftcard category";
	}

	@Override
	public String getImageCode() {
		return imageCode;
	}

	@Override
	public void setAsinOrIsbn(String asinOrIsbn) {
		this.asinOrIsbn = asinOrIsbn;
	}

	@Override
	public void setDescription(String description) {
		this.description = description;

	}

	@Override
	public void setOrderNumber(String orderNumber) {
		this.orderNumber = orderNumber;
	}

	@Override
	public void setQuantity(int quantity) {
		this.quantity = quantity;
	}

	@Override
	public void setMovie(boolean isMovie) {
		this.isMovie = isMovie;
	}

	public JSONObject toJson() {
		JSONObject result = new JSONObject()
				.put("itemAmount", netCost)
				.put("amazonDescription", amazonDescription)
				.put("orderNumber", orderNumber)
				.put("asinOrIsbn", asinOrIsbn)
				.put("category", accountDescription)
				.put("imageCode", imageCode)
				.put("description", description);
		if (quantity != 1) {
			result.put("quantity", quantity);
		}
		if (isMovie) {
			result.put("isMovie", true);
		}
		return result;
	}
}
