package ebayscraper.api;

import java.util.List;

public interface EbayOrderFields {

	String getOrderDate();

	String getOrderNumber();

	String getTotal();

	String getDescription();

	String getSeller();

	List<EbayItemFields> getItems();

}
