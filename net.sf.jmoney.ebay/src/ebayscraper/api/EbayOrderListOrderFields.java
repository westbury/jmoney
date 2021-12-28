package ebayscraper.api;

import java.util.List;

public interface EbayOrderListOrderFields {

	String getOrderDate();

	String getOrderNumber();

	String getTotal();

	String getSeller();

	List<EbayOrderListItemFields> getItems();

}
