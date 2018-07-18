package amazonscraper;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import amazonscraper.api.AmazonOrderDetailFields;
import amazonscraper.api.AmazonOrderFields;
import analyzer.AmazonOrder;
import analyzer.AmazonOrderAnalyzer;
import analyzer.UnsupportedImportDataException;
import net.sf.jmoney.importer.wizards.ImportException;
import txr.matchers.DocumentMatcher;
import txr.matchers.MatchResults;

public class AmazonScraperContext {

	IContextUpdater contextUpdater;

	// Lazily created
	private DocumentMatcher ordersMatcher = null;

	// Lazily created
	private DocumentMatcher detailsMatcher = null;

	public Set<AmazonOrder> orders = new HashSet<>();

	private AmazonOrderAnalyzer analyzer;

	public AmazonScraperContext(IContextUpdater contextUpdater) {
		this.contextUpdater = contextUpdater;
		
		analyzer = new AmazonOrderAnalyzer();
	}

	private MatchResults extractOrderBindings(String inputText) {
		if (ordersMatcher == null) {
			ordersMatcher = createMatcherFromResource("amazon-orders.txr");
		}

		MatchResults bindings = ordersMatcher.process(inputText);

		if (bindings == null || bindings.getCollections(0).isEmpty()) {
			throw new RuntimeException("Data does not appear to be copied from the orders page.");
		}

		return bindings;
	}

	private MatchResults extractDetailsBindings(String plainText) {
		if (detailsMatcher == null) {
			detailsMatcher = createMatcherFromResource("amazon-details.txr");
		}

		MatchResults orderBindings = detailsMatcher.process(plainText);

		if (orderBindings == null) {
			throw new RuntimeException("Data does not appear to be a details page.");
		}

		return orderBindings;
	}

	private DocumentMatcher createMatcherFromResource(String resourceName) {
		ClassLoader classLoader = getClass().getClassLoader();
		URL resource = classLoader.getResource(resourceName);
		try (InputStream txrInputStream = resource.openStream()) {
			return new DocumentMatcher(txrInputStream, "UTF-8");
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	public void importOrders(String inputText) {
		MatchResults bindings = extractOrderBindings(inputText);

		for (MatchResults orderBindings : bindings.getCollections(0)) {
			AmazonOrderFields orderFields = new AmazonOrderFieldExtractor(orderBindings);
			Date orderDate = analyzer.processAmazonOrder(orderFields);
		}
	}

	public void importDetails(String inputText) throws UnsupportedImportDataException {
		MatchResults orderBindings = extractDetailsBindings(inputText);

		AmazonOrderDetailFields orderFields = new AmazonOrderDetailFieldExtractor(orderBindings);
		
		analyzer.processAmazonOrderDetails(orderFields);
	}

	/**
	 * Looks to see if this order is already in this view.  If not,
	 * creates the AmazonOrder object for this order.
	 * <P>
	 * Note that when an AmazonOrder object is created, the order may or may
	 * not already exist in the accounting datastore.  If the order did not
	 * already exist in the datastore then a new transaction is created.
	 * 
	 * @param orderNumber
	 * @param orderDate
	 * @param session
	 * @return
	 * @throws ImportException 
	 */
	private AmazonOrder getAmazonOrderWrapper(String orderNumber, Date orderDate) {
		// Look to see if this order is already in the view.
		Optional<AmazonOrder> order = orders.stream().filter(x -> x.getOrderNumber().equals(orderNumber))
				.findFirst();

		if (order.isPresent()) {
			return order.get();
		}

		AmazonOrder newOrder = contextUpdater.createAmazonOrder(orderNumber, orderDate);
		orders.add(newOrder);
		return newOrder;
	}

}
