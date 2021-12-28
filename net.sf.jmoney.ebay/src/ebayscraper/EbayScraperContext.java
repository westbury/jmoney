package ebayscraper;

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

import analyzer.EbayOrder;
import analyzer.EbayOrderAnalyzer;
import analyzer.UnsupportedImportDataException;
import ebayscraper.api.EbayDetailOrderFields;
import ebayscraper.api.EbayOrderListOrderFields;
import ebayscraper.api.EbayDetailPaymentFields;
import net.sf.jmoney.importer.wizards.ImportException;
import txr.matchers.DocumentMatcher;
import txr.matchers.MatchResults;
import txr.parser.TxrErrorInDocumentException;

public class EbayScraperContext {

	IContextUpdater contextUpdater;

	// Lazily created
	private DocumentMatcher ordersMatcher = null;

	// Lazily created
	private DocumentMatcher detailsMatcher = null;

	public Set<EbayOrder> orders = new HashSet<>();

	private EbayOrderAnalyzer analyzer;

	public EbayScraperContext(IContextUpdater contextUpdater) {
		this.contextUpdater = contextUpdater;
		
		analyzer = new EbayOrderAnalyzer(orders, contextUpdater);
	}

	private MatchResults extractOrderBindings(String inputText) {
		if (ordersMatcher == null) {
			ordersMatcher = createMatcherFromResource("ebay-orders.txr");
		}

		MatchResults bindings = ordersMatcher.process(inputText);

		if (bindings == null || bindings.getCollections(0).isEmpty()) {
			throw new RuntimeException("Data does not appear to be copied from the orders page.");
		}

		return bindings;
	}

	private MatchResults extractDetailsBindings(String plainText) {
		if (detailsMatcher == null) {
			detailsMatcher = createMatcherFromResource("ebay-details.txr");
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
		} catch (TxrErrorInDocumentException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	public void importOrders(String inputText) {
		MatchResults bindings = extractOrderBindings(inputText);

		for (MatchResults orderBindings : bindings.getCollections(0)) {
			EbayOrderListOrderFields orderFields = new EbayOrderFieldExtractor(orderBindings);
			Date orderDate = analyzer.processEbayOrderList(orderFields);
		}
	}

	public void importDetails(String inputText) throws UnsupportedImportDataException {
		MatchResults orderBindings = extractDetailsBindings(inputText);

		EbayDetailPaymentFields orderFields = new EbayPaymentDetailFieldExtractor(orderBindings);
		
		analyzer.processEbayOrderDetails(orderFields);
	}

}
