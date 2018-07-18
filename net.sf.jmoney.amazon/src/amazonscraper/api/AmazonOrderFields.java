package amazonscraper.api;

import java.util.List;

public interface AmazonOrderFields {

	String getOrderDate();

	String getOrderNumber();

	String getTotal();

	List<AmazonShipmentFields> getShipments();

}
