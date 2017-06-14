package amazonscraper;

import java.util.Date;

public interface IContextUpdater {

	AmazonOrder createAmazonOrderUpdater(String orderNumber, Date orderDate);

}
