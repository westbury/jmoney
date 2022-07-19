package ebayscraper;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import analyzer.EbayOrder;
import analyzer.EbayOrderAnalyzer;
import analyzer.UnsupportedImportDataException;
import ebayscraper.api.EbayDetailPaymentFields;
import ebayscraper.api.EbayOrderListOrderFields;
import net.sf.jmoney.importer.wizards.TxrMismatchException;
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

	private MatchResults extractOrderBindings(String inputText) throws TxrMismatchException {
		if (ordersMatcher == null) {
			ordersMatcher = createMatcherFromResource("ebay-orders.txr");
		}

		MatchResults bindings = ordersMatcher.process(inputText);
		if (bindings == null || bindings.getCollections(0).isEmpty()) {
			ClassLoader classLoader = getClass().getClassLoader();
			URL resource = classLoader.getResource("ebay-orders.txr");
			throw new TxrMismatchException(resource, inputText, "EBay orders page");
		}

		return bindings;
	}

	private MatchResults extractDetailsBindings(String inputText) throws TxrMismatchException {
		if (detailsMatcher == null) {
			detailsMatcher = createMatcherFromResource("ebay-details.txr");
		}

		MatchResults orderBindings = detailsMatcher.process(inputText);

		if (orderBindings == null) {
			ClassLoader classLoader = getClass().getClassLoader();
			URL resource = classLoader.getResource("ebay-details.txr");
			throw new TxrMismatchException(resource, inputText, "EBay details page");
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

	public void importOrders(String inputText) throws TxrMismatchException {
		MatchResults bindings = extractOrderBindings(inputText);

		for (MatchResults orderBindings : bindings.getCollections(0)) {
			EbayOrderListOrderFields orderFields = new EbayOrderFieldExtractor(orderBindings);
			Date orderDate = analyzer.processEbayOrderList(orderFields);
		}
	}

	public void importDetails(String inputText) throws UnsupportedImportDataException, TxrMismatchException {
		MatchResults orderBindings = extractDetailsBindings(inputText);

		EbayDetailPaymentFields orderFields = new EbayPaymentDetailFieldExtractor(orderBindings);
		
		analyzer.processEbayOrderDetails(orderFields);
	}

}
