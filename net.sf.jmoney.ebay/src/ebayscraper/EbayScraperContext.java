package ebayscraper;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.FileLocator;

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

	interface ITxrReader {
		DocumentMatcher createMatcherFromResource() throws TxrErrorInDocumentException;
	}

	// Lazily created
	private DocumentMatcher ordersMatcher = null;
	private ITxrReader ordersTxrSource = createTxrReader("ebay-orders.txr");
	
	// Lazily created
	private DocumentMatcher detailsMatcher = null;
	private ITxrReader detailsTxrSource = createTxrReader("ebay-details.txr");

	public Set<EbayOrder> orders = new HashSet<>();

	private EbayOrderAnalyzer analyzer;

	public EbayScraperContext(IContextUpdater contextUpdater) {
		this.contextUpdater = contextUpdater;
		
		analyzer = new EbayOrderAnalyzer(orders, contextUpdater);
	}

	private ITxrReader createTxrReader(String resourceName) {
		ClassLoader classLoader = getClass().getClassLoader();
		URL resourceUrl = classLoader.getResource(resourceName);

		try {
			URL txrFileUrl = FileLocator.resolve(resourceUrl);
			assert ("file".equals(txrFileUrl.getProtocol()));
			File binFile = new File(txrFileUrl.toURI());
			String binPath = binFile.getAbsolutePath();

			if (binPath.contains("/bin/")) {
				// Assume it's the bin path in an Eclipse source project.

				String srcPath = binPath.replace("/bin/", "/src/");
				File srcFile = new File(srcPath);

				return new ITxrReader() {
					@Override
					public DocumentMatcher createMatcherFromResource() throws TxrErrorInDocumentException {

						try (
							InputStream txrInputStream = new FileInputStream(srcFile)
						) {
							return new DocumentMatcher(txrInputStream, "UTF-8");
						} catch (UnsupportedEncodingException e) {
							throw new RuntimeException(e);
						} catch (FileNotFoundException e) {
							throw new RuntimeException(e);
						} catch (IOException e) {
							throw new RuntimeException(e);
						}
					}
				};

			} else {
				DocumentMatcher matcher = createMatcherFromResource(resourceName);
				return new ITxrReader() {
					@Override
					public DocumentMatcher createMatcherFromResource() {
						return matcher;
					}
				};
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}

	public void updateOrdersMatcher(String [] txrLines) {
		String txrData = String.join("\n", txrLines);
        try (InputStream txrInputStream = new ByteArrayInputStream(txrData.getBytes())) {
			ordersMatcher = new DocumentMatcher(txrInputStream, "UTF-8");
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (TxrErrorInDocumentException e) {
			throw new RuntimeException(e);
		}
	}

	public void updateDetailsMatcher(String [] txrLines) {
		String txrData = String.join("\n", txrLines);
        try (InputStream txrInputStream = new ByteArrayInputStream(txrData.getBytes())) {
        	detailsMatcher = new DocumentMatcher(txrInputStream, "UTF-8");
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (TxrErrorInDocumentException e) {
			throw new RuntimeException(e);
		}
	}
	
	private MatchResults extractOrderBindings(String inputText) throws TxrErrorInDocumentException, TxrMismatchException {
		ordersMatcher = ordersTxrSource.createMatcherFromResource();

		MatchResults bindings = ordersMatcher.process(inputText);
		if (bindings == null || bindings.getCollections(0).isEmpty()) {
			ClassLoader classLoader = getClass().getClassLoader();
			URL resource = classLoader.getResource("ebay-orders.txr");
			throw new TxrMismatchException(resource, inputText, "EBay orders page");
		}

		return bindings;
	}

	private MatchResults extractDetailsBindings(String inputText) throws TxrErrorInDocumentException, TxrMismatchException {
		detailsMatcher = detailsTxrSource.createMatcherFromResource();


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

	public void importOrders(String inputText) throws TxrMismatchException, TxrErrorInDocumentException {
		MatchResults bindings = extractOrderBindings(inputText);

		for (MatchResults orderBindings : bindings.getCollections(0)) {
			EbayOrderListOrderFields orderFields = new EbayOrderFieldExtractor(orderBindings);
			Date orderDate = analyzer.processEbayOrderList(orderFields);
		}
	}

	public void importDetails(String inputText) throws UnsupportedImportDataException, TxrMismatchException, TxrErrorInDocumentException {
		MatchResults orderBindings = extractDetailsBindings(inputText);

		EbayDetailPaymentFields orderFields = new EbayPaymentDetailFieldExtractor(orderBindings);
		
		analyzer.processEbayOrderDetails(orderFields);
	}

}
