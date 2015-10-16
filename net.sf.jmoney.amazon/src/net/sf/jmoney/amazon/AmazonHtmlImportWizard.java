package net.sf.jmoney.amazon;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import net.sf.jmoney.importer.Activator;
import net.sf.jmoney.importer.MatchingEntryFinder;
import net.sf.jmoney.importer.wizards.ImportException;
import net.sf.jmoney.model2.CapitalAccount;
import net.sf.jmoney.model2.Currency;
import net.sf.jmoney.model2.CurrencyAccount;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.IDataManagerForAccounts;
import net.sf.jmoney.model2.IncomeExpenseAccount;
import net.sf.jmoney.model2.Session;
import net.sf.jmoney.model2.Transaction;
import net.sf.jmoney.model2.TransactionManagerForAccounts;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * Items are grouped based on order id, tracking number, and shipment date.  A single order may be split into multiple
 * shipments.  Items are charged as they are shipped, and it is assumed that all items shipped on the
 * same day are charged as a single charge.
 * <P>
 * Having grouped the items, we still may not know the amount charged.  That is because there may be
 * shipping and other costs.  Those other costs are not available in the item import.  Therefore we do
 * not try to match items to charges on a credit card.  It is the import of the orders that makes this
 * connection.  If items have been imported but the orders have not then the items are added up and charged
 * to a special 'pending Amazon charges' account.  When the orders are imported, these entries are matched
 * to the credit card or bank account entries as appropriate.
 * <P>
 * The idea is that the orders and items can both be imported separately.  Data is put into the datastore.
 * If only one is imported then the data from that one import is in the datastore, but if both are imported
 * then everything is matched.  It should not matter what order the Amazon items, the Amazon orders, and the
 * charge or bank account are imported.
 *
 * @author westbury.nigel2
 *
 */
public class AmazonHtmlImportWizard extends Wizard implements IImportWizard {

	protected IWorkbenchWindow window;

	protected AmazonHtmlImportWizardPage mainPage;

	private Session session;

	private IncomeExpenseAccount unknownAmazonPurchaseAccount;

	private CurrencyAccount chargeAccount;

	private CurrencyAccount giftCertificateAccount;

	private Currency thisCurrency;

	private Pattern deliveryEstimatePattern;

	private Pattern deliveredOnPattern;

	/**
	 * Used on order list page
	 */
	private Pattern dispatchDatePattern1;

	/**
	 * Used in order detail page
	 */
	private Pattern dispatchDatePattern;

	private Pattern trackingInfoPattern;

	private Pattern trackingInfoPattern2;

	/**
	 * This form of the constructor is used when being called from
	 * the Eclipse 'import' menu.
	 */
	public AmazonHtmlImportWizard() {
		IDialogSettings workbenchSettings = Activator.getDefault().getDialogSettings();
		IDialogSettings section = workbenchSettings.getSection("AmazonHtmlImportWizard");//$NON-NLS-1$
		if (section == null) {
			section = workbenchSettings.addNewSection("AmazonHtmlImportWizard");//$NON-NLS-1$
		}
		setDialogSettings(section);
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		this.window = workbench.getActiveWorkbenchWindow();

		mainPage = new AmazonHtmlImportWizardPage(window);

		addPage(mainPage);
		
		/*
		 * Almost always the months are given as three letters but once I these:
		 * 
		 * "Delivery estimate Monday 24 Jun 2013 - Friday 5 July 2013"
		 * "Dispatched on Tuesday 30 April 2013"
		 */

		try {
			deliveryEstimatePattern = Pattern.compile("Delivery estimate \\w{3,6}day (\\d{1,2} \\w{3,5} \\d{4,4})( - \\w{3,6}day (\\d{1,2} \\w{3,5} \\d{4,4}))?");
			deliveredOnPattern = Pattern.compile("Delivered On: \\w{3,6}day (\\d{1,2} \\w{3,5} \\w{4,4})");
			dispatchDatePattern1 = Pattern.compile("Dispatched on \\w{3,6}day (\\d{1,2} \\w{3,5} \\d{4,4}).*");
			dispatchDatePattern = Pattern.compile("Delivery #\\d: Dispatched on (\\d{1,2} \\w{3,5} \\d{4,4}).*");
			trackingInfoPattern = Pattern.compile(".*1 package via (.*) with tracking number (.*)");
			trackingInfoPattern2 = Pattern.compile(".*1 package via (.*)");
	} catch (PatternSyntaxException e) {
			throw new RuntimeException("pattern failed");
		}
	}

	@Override
	public boolean performFinish() {
		IDataManagerForAccounts datastoreManager = (IDataManagerForAccounts)window.getActivePage().getInput();
		if (datastoreManager == null) {
			MessageDialog.openError(window.getShell(), "Unavailable", "You must open an accounting session before you can create an account.");
			return false;
		}

		/*
		 * Create a transaction to be used to import the entries.  This allows the entries to
		 * be more efficiently written to the back-end datastore and it also groups
		 * the entire import as a single change for undo/redo purposes.
		 */
		TransactionManagerForAccounts transactionManager = new TransactionManagerForAccounts(datastoreManager);
		session = transactionManager.getSession();

		/*
		 * This code is written for Amazon UK and has been
		 * tested for Amazon UK only.  If you want to modify
		 * this code to get data from Amazon in other counties
		 * then you will need to change this line.
		 */
		thisCurrency = session.getCurrencyForCode("GBP");

		try {
			/*
			 * Find the income and expense account to which entries are placed by default.
			 */
			unknownAmazonPurchaseAccount = null;
			for (Iterator<IncomeExpenseAccount> iter = session.getIncomeExpenseAccountIterator(); iter.hasNext(); ) {
				IncomeExpenseAccount eachAccount = iter.next();
				if (eachAccount.getName().startsWith("Amazon purchase")
						&& eachAccount.getCurrency() == thisCurrency) {
					unknownAmazonPurchaseAccount = eachAccount;
					break;
				}
			}
			if (unknownAmazonPurchaseAccount == null) {
				throw new ImportException("No account exists with a name that begins 'Amazon purchase' and a currency of " + thisCurrency.getName() + ".");
			}

			/*
			 * Find the account which is charged for the purchases.
			 */
			chargeAccount = null;
			for (Iterator<CapitalAccount> iter = session.getCapitalAccountIterator(); iter.hasNext(); ) {
				CapitalAccount eachAccount = iter.next();
//				if (eachAccount.getName().startsWith("Nationwide Flex")
				if (eachAccount.getName().endsWith("1816")
//				if (eachAccount.getName().endsWith("9076")
						&& eachAccount instanceof CurrencyAccount
						&& ((CurrencyAccount)eachAccount).getCurrency() == thisCurrency) {
//				if (eachAccount.getName().endsWith("1775")
//						&& eachAccount instanceof CurrencyAccount) {
					chargeAccount = (CurrencyAccount)eachAccount;
					break;
				}
			}
			if (chargeAccount == null) {
				throw new ImportException("No account exists with a name that begins 'Nationwide Flex' and a currency of " + thisCurrency.getName() + ".");
			}

			/*
			 * Find the account which is used for Amazon gift certificate payments.
			 */
			giftCertificateAccount = null;
			for (Iterator<CapitalAccount> iter = session.getCapitalAccountIterator(); iter.hasNext(); ) {
				CapitalAccount eachAccount = iter.next();
				if (eachAccount.getName().startsWith("Amazon gift certificate")
						&& eachAccount instanceof CurrencyAccount) {
					giftCertificateAccount = (CurrencyAccount)eachAccount;
					break;
				}
			}
			if (giftCertificateAccount == null) {
				throw new ImportException("No account exists with a name that begins 'Amazon gift certificate' and a currency of " + thisCurrency.getName() + ".");
			}

			String fileName = mainPage.getFileName();
			if (fileName != null) {
				File htmlFile = new File(fileName);


//URL myURL;
//try {
//	myURL = new URL("https://www.amazon.co.uk/ap/signin?_encoding=UTF8&openid.assoc_handle=gbflex&openid.claimed_id=http%3A%2F%2Fspecs.openid.net%2Fauth%2F2.0%2Fidentifier_select&openid.identity=http%3A%2F%2Fspecs.openid.net%2Fauth%2F2.0%2Fidentifier_select&openid.mode=checkid_setup&openid.ns=http%3A%2F%2Fspecs.openid.net%2Fauth%2F2.0&openid.ns.pape=http%3A%2F%2Fspecs.openid.net%2Fextensions%2Fpape%2F1.0&openid.pape.max_auth_age=0&openid.return_to=https%3A%2F%2Fwww.amazon.co.uk%2Fgp%2Fyourstore%2Fhome%3Fie%3DUTF8%26ref_%3Dgno_signin");
//	HttpURLConnection conn = (HttpURLConnection) myURL.openConnection();
//
//	int n=1; // n=0 has no key, and the HTTP return status in the value field
//	boolean done = false;
//	while (!done){
//	    String headerKey = conn.getHeaderFieldKey(n);
//	    String headerVal = conn.getHeaderField(n);
//	    if (headerKey!=null || headerVal!=null) {
//	        System.out.println(headerKey+"="+headerVal);
//	    } else {
//		done = true;
//	    }
//	    n++;
//	}
//
//	String cookie = conn.getHeaderField("Set-Cookie");
//
//	if (cookie != null) {
//		int index = cookie.indexOf(";");
//		if(index >= 0) cookie = cookie.substring(0, index);
//	}
//
//	System.out.println(cookie);
////	URLConnection conn2 = anotherURL.openConnection();
////	conn.setRequestProperty("Cookie",cookie); // do this BEFORE getInputStream and BEFORE getHeader* !
//
//
//} catch (MalformedURLException e1) {
//	// TODO Auto-generated catch block
//	e1.printStackTrace();
//} catch (IOException e) {
//	// TODO Auto-generated catch block
//	e.printStackTrace();
//}



				try {
//					Document doc = Jsoup.connect("http://www.amazon.co.uk").get();
//					Document doc = Jsoup.connect("https://www.amazon.co.uk/gp/css/summary/edit.html/ref=oh_or_o05_?ie=UTF8&orderID=736-1287517-3217961").get();
					Document doc = Jsoup.parse(htmlFile, "UTF-8", "http://example.com/");

					for (Element element : doc.getElementsByClass("action-box")) {

						System.out.println("id=" + element.id() + ", class=" + element.className());
					}

					List<AmazonOrder> orders = new ArrayList<AmazonOrder>();

					for (Element roundedBox : doc.getElementsByClass("action-box")) {
						AmazonOrder order = new AmazonOrder();

						Elements orderLevelElements = roundedBox.getElementsByClass("order-level");
						assert (orderLevelElements.size() == 1);
						Element orderLevelElement = orderLevelElements.get(0);

						/*
						 * Process the order information found in the
						 * order-level element (order date, order id, and order
						 * total).
						 */
						for (Iterator<Element> iter = orderLevelElement.children().iterator(); iter.hasNext(); ) {
							Element child = iter.next();

							if (child.text().equals("Order placed")) {
								child = iter.next();
								order.orderDate = child.text();
							} else if (child.className().equals("order-links")) {
								Element firstOrderLink = child.child(0);
								String href = firstOrderLink.attr("href");
								int startIndex = href.indexOf("orderID=") + "orderID=".length();
								int endIndex = href.indexOf("&", startIndex);
								if (endIndex == -1) {
									order.id = href.substring(startIndex);
								} else {
									order.id = href.substring(startIndex, endIndex);
								}
							} else if (child.className().equals("order-details")) {
								String recipient = null;
								for (Element listItem : child.children()) {
									if ("gift-line".equals(listItem.className())) {
										/*
										 * This line indicates the item was a gift.
										 * We need to extract the "Recipient" line to find out
										 * for whom it was a gift.
										 */
										order.giftRecipient = recipient;
									} else {
										String listItemTitle = listItem.children().get(0).text();
										String listItemValue = listItem.children().get(1).text();
										if (listItemTitle.equals("Order #")) {
											order.id2 = listItemValue;
										} else if (listItemTitle.equals("Total")) {
											order.total = convertAmount(listItemValue);
										} else if (listItemTitle.equals("Recipient")) {
											recipient = listItemValue;
										}
									}
								}
							}
						}

						/*
						 * Inside 'ship-contain', we have 'status' and 'ship-listing'.  These come
						 * in these twos with no single element for each shipment.
						 */

						/*
						 * Process the shipment information found in the
						 * 'ship-contain' element.  There will be a 'ship-contain' element
						 * for each shipment that makes up the order.
						 * (delivery date).
						 */

						for (Element shipContainElement : roundedBox.getElementsByClass("ship-contain")) {
							/*
							 * Inside 'ship-contain', we have 'status' and 'ship-listing'.  These come
							 * in these twos with no single element for each shipment.
							 * However for older records (back to 2004) there is no status.
							 */

							System.out.println(shipContainElement);
							for (Iterator<Element> shipContainChildIter = shipContainElement.children().iterator(); shipContainChildIter.hasNext(); ) {
								Element shipContainChild = shipContainChildIter.next();
								System.out.println(shipContainChild.className());

								Element statusElement = null;
								Element shipListingElement;

								String classNames [] = shipContainChild.className().split("\\s");
								if (classNames[0].equals("status")) {
									statusElement = shipContainChild;

									shipContainChild = shipContainChildIter.next();
									String classNames2 [] = shipContainChild.className().split("\\s");
									if (!classNames2[0].equals("ship-listing")) {
										throw new RuntimeException("'primary-action' expected after h2");
									}
									shipListingElement = shipContainChild;
								} else if (classNames[0].equals("ship-listing")) {
									shipListingElement = shipContainChild;
								} else {
									continue;
								}

								/*
								 * There appears to be an empty ship-listing at the end.
								 * If it has no <li> elements then ignore it.
								 */
								if (shipListingElement.getElementsByTag("li").isEmpty()) {
									continue;
								}
								
								AmazonShipment shipment = new AmazonShipment();
								order.shipments.add(shipment);

								/*
								 * Inside 'status', we have 'deliv-text', h2, and 'primary-action'.
								 */
								if (statusElement != null) {
									System.out.println(statusElement);

									Elements delivTextElements = statusElement.getElementsByClass("deliv-text");
									assert (delivTextElements.size() == 1);
									Element deliveryTextElement = delivTextElements.get(0);

									System.out.println("deliv-text:  " + deliveryTextElement.toString());
									String deliveryText = deliveryTextElement.text();

									Matcher matcher;
									if ((matcher = deliveryEstimatePattern.matcher(deliveryText)).matches()) {
										String datePart = matcher.group(1);
										shipment.deliveryDate = datePart;
									} else if ((matcher = deliveredOnPattern.matcher(deliveryText)).matches()) {
										String datePart = matcher.group(1);
										shipment.deliveryDate = datePart;
									} else if ((matcher = dispatchDatePattern1.matcher(deliveryText)).matches()) {
										String datePart = matcher.group(1);
										shipment.dispatchDate = datePart;
									} else {
										throw new RuntimeException("Unexpected delivery date: " + deliveryText);
									}
								}

								/*
								 * Find the <ul> element with a class of "shipment".
								 * This will contain a <li> for each item in the shipment.
								 */
								Elements shipmentElements = shipListingElement.getElementsByClass("shipment");
								if (shipmentElements.size() != 1) {
									throw new RuntimeException("Expected single shipment element in status element");
								}
								assert (shipmentElements.size() == 1);
								Element shipmentElement = shipmentElements.get(0);
								assert (shipmentElement.tagName().equalsIgnoreCase("ul"));
								
								/*
								 * There will be a <li> for each item in this
								 * shipment. Be sure to get just the <li>
								 * elements that are immediate children because
								 * there are some deeper nested <li> elements
								 * that we don't want.
								 *
								 * Also note that there may be children to the
								 * shipmentElement that are not <li> elements
								 * but may be <div> elements. So we ignore
								 * anything that is not an <li> element.
								 */
								for (Element listItem : shipmentElement.children()) {
									if (listItem.tagName().equals("li")) {
										if ("more".equals(listItem.className())) {
											
											/*
											 * Now if there are more
											 * than 10 then they will not be listed and a 'more' element will
											 * have been found.  In that case set into the order so that later
											 * validation knows that not all items will have been created from the
											 * initial page.
											 */
											order.moreItems = true;
										} else {
											AmazonItem item = new AmazonItem();
											shipment.items.add(item);

											Element imgElement = listItem.getElementsByTag("img").get(0);
											item.img = imgElement.attr("src");

											Element descriptionElement = listItem.getElementsByClass("item-title").get(0);
											item.description = descriptionElement.text();

											/*
											 * Almost all items have a seller element but not all
											 */
											Elements sellerElements = listItem.getElementsByClass("seller");
											if (!sellerElements.isEmpty()) {
												String seller = sellerElements.get(0).text().trim();
												if (seller.startsWith("Sold by:")) {
													item.seller = seller.substring("Sold by:".length()).trim();
												} else if (seller.endsWith("( seller profile )")) {
													item.seller = seller.substring(1, seller.length() - "( seller profile )".length());
												} else {
													item.seller = seller;
												}
											}

											for (Element x : listItem.getElementsByTag("a")) {
												String href = x.attr("href");
												if (href != null) {
													int index = href.indexOf("&asins=");
													if (index != -1) {
														int beginIndex = index + "&asins=".length();
														int endIndex = href.indexOf("&", beginIndex);
														String asinAttr = href.substring(beginIndex, endIndex);
														if (item.asin == null) {
															item.asin = asinAttr;
														} else if (!item.asin.equals(asinAttr)) {
															System.out.println("Multiple asins! " + item.asin + " and " + asinAttr);
														}
													}
												}
											}
										}
									}
								}
							}
						}

						String price = null;

						for (Element priceElement : roundedBox.getElementsByClass("price")) {
							price = priceElement.text();
						}

						processOrderPage(order);

						orders.add(order);
					}

					writeTransactions(orders);

					transactionManager.commit("Import Amazon from HTML");

					if (mainPage.IsDeleteFile()) {
						boolean isDeleted = htmlFile.delete();
						if (!isDeleted) {
							MessageDialog.openWarning(window.getShell(), "HTML file not deleted",
									MessageFormat.format(
											"All entries in {0} have been imported and an attempt was made to delete the file.  However the file deletion failed.",
											htmlFile.getName()));
						}
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		} catch (ImportException e) {
			MessageDialog.openError(window.getShell(), "HTML import failed",
					MessageFormat.format(
							"{0} No data was imported.", e.getLocalizedMessage()));
			return false;
		}

		return true;
	}

	private void processOrderPage(AmazonOrder order) throws IOException, ImportException {
		String fileName = MessageFormat.format("C:\\Documents and Settings\\user1\\My Documents\\Downloads\\Amazon.co.uk - Order {0}.htm", order.id);
		File htmlFile = new File(fileName);
		if (htmlFile.exists()) {
			Document doc = Jsoup.parse(htmlFile, "UTF-8", "http://example.com/");

			
			int deliveryNumber = 0;
			for (Element deliveryAElement : doc.getElementsByTag("a")) {
				String deliveryName = "shipped-items-" + deliveryNumber;
				if (deliveryName.equals(deliveryAElement.attr("name"))) {
					processShipment(order, deliveryAElement);
					deliveryNumber++;
				}
			}
			if (deliveryNumber != order.shipments.size()) {
				throw new ImportException("Different number of shipments in order listing than in details page for order " + order.id);
			}
		} else {
			/*
			 * File does not exist.  We let it go if there is one shipment
			 * with one item in the order.  However when there are more than
			 * one shipment or item then we really need the detail because the
			 * order listing page only contains the total price of the order.
			 */
			if (order.shipments.size() != 1
					|| order.shipments.get(0).items.size() != 1) {
				throw new ImportException("You must save the details for order " + order.id);
			}

			/*
			 * If there is one shipment with one item, copy the order total
			 * to the item price.
			 */
			order.shipments.get(0).total = order.total;
			order.shipments.get(0).totalPaidInCash = order.total;
			order.shipments.get(0).items.get(0).price = order.total;
		}
	}

	private void processShipment(AmazonOrder order, Element deliveryAElement) throws ImportException {
		Elements childElements = deliveryAElement.children();

		Matcher matcher;

		Element tableBodyElement = childElements.get(1)  // Second table
				.children().get(0)  // Now on tbody
				.children().get(0)  // Now on tr
				.children().get(0)  // Now on td
				.children().get(0)  // Now on table
				.children().get(0);  // Now on tbody

		// Second tr, second td (first td has delivery address),
		// 5th child is a table with interesting figures,
		// tbody
		Element elementZ = tableBodyElement.children().get(1).children().get(1);
		System.out.println(elementZ.toString());

		// Pass any <input> elements.
		int childIndex = 0;
		while (elementZ.children().get(childIndex).tagName().equals("input")) {
			childIndex++;
		}

		Elements itemRows = elementZ.children().get(childIndex).children().get(0).children();

		int j = 0;
		// Second tr, second td (first td has delivery address),
		// 2nd child is a table with item prices,
		// tbody
		System.out.println("itemRows: " + itemRows.toString());

		Set<String> itemsInShipment = new HashSet<String>();
		for (Element xx : itemRows) {
			if (!xx.getElementsByTag("input").isEmpty()) {
				String hrefWithAsin = xx.children().get(1).getElementsByTag("a").attr("href");
				int beginIndex = hrefWithAsin.indexOf("/ASIN/");
				if (beginIndex != -1) {
					beginIndex += "/ASIN/".length();

					int endIndex = hrefWithAsin.indexOf("/", beginIndex);
					String asin = hrefWithAsin.substring(beginIndex, endIndex);
					itemsInShipment.add(asin);
				} else {
					itemsInShipment.add("?");  // Indicates asin unknown
				}
			}
		}

		// Find the shipment with this set of items
		AmazonShipment originalShipment = null;
		for (AmazonShipment eachShipment : order.shipments) {
			if (eachShipment.containsExactly(itemsInShipment)) {
				if (originalShipment != null) {
					throw new ImportException("duplicate shipment");
				}
				originalShipment = eachShipment;
				
				// Don't carry on to check for dups because these may happen
				// if one is created in the hack below.
//				break;
			}
		}
		if (originalShipment == null) {
			/*
			 * There is a bug in the Amazon code.  When two of the same item are
			 * ordered and then shipped separately, the order page shows them in the
			 * same shipment.  We 
			 */
			
			if (order.shipments.size() == 1) {
				AmazonShipment existingShipment = order.shipments.get(0);
				AmazonShipment dupShipment = new AmazonShipment();
				dupShipment.carrier = existingShipment.carrier;
				dupShipment.dispatchDate = existingShipment.dispatchDate;
				dupShipment.items.add(existingShipment.items.remove(0));
				order.shipments.add(dupShipment);
				
				/*
				 * Return the dup this time.  The next time the original should be found.
				 */
				originalShipment = dupShipment;
			} else {

			throw new ImportException("no shipment matches the set of items");
			}
		}
		
		/*
		 * Add the dispatch date.  This does not appear in the order listing
		 * so will be set only when the order details page is available.
		 */
		String dispatchDateText = childElements.get(0).text();
		if ((matcher = dispatchDatePattern.matcher(dispatchDateText)).matches()) {
			String datePart = matcher.group(1);
			originalShipment.dispatchDate = datePart;
		} else {
			// Not important, just ignore for now
//			throw new RuntimeException("Unexpected dispatch date: " + dispatchDateText);
		}

		/*
		 * Add the tracking information.  This does not appear in the order listing
		 * so will be set only when the order details page is available.
		 */
		Element elementRowWithTrackingInfo = tableBodyElement.children().get(0);  // Now on tr
		String trackingText = elementRowWithTrackingInfo.text();
		if ((matcher = trackingInfoPattern.matcher(trackingText)).matches()) {
			originalShipment.carrier = matcher.group(1);
			originalShipment.trackingNumber = matcher.group(2);
		} else if ((matcher = trackingInfoPattern2.matcher(trackingText)).matches()) {
				originalShipment.carrier = matcher.group(1);
				originalShipment.trackingNumber = null;
		} else {
			// Some orders don't even give the carrier, so ignore
			originalShipment.carrier = null;
			originalShipment.trackingNumber = null;
		}

		if (originalShipment.dispatchDate == null) {
			Pattern dispatchDatePattern2 = Pattern.compile("Dispatch estimate:.(\\d{1,2} \\w{3,3} \\d{4,4}).*");
			if ((matcher = dispatchDatePattern2.matcher(trackingText)).matches()) {
				String datePart = matcher.group(1);
				originalShipment.dispatchDate = datePart;
			}
		}
	
		long subTotal = 0;
		long postageAndPackaging = 0;
		long beforeVat = 0;
		long vat = 0;
		long total = 0;
		long totalPaidInCash = 0;
		long totalPaidInGiftCertificate = 0;

		for (Element xx : elementZ.children().get(childIndex+3).children().get(0).children()) {
			Elements tdElements = xx.children();
			String text1 = tdElements.get(0).text();
			String text2 = tdElements.get(1).text();
			if (text1.equals("Item(s) Subtotal:")) {
				subTotal = convertAmount(text2);
			} else if (text1.equals("Postage & Packing:")) {
				postageAndPackaging = convertAmount(text2);
			} else if (text1.equals("Total before VAT:")) {
				beforeVat = convertAmount(text2);
			} else if (text1.equals("VAT:")) {
				vat = convertAmount(text2);
			} else if (text1.equals("Total:")) {
				total = convertAmount(text2);
			} else if (text1.equals("Total for this Delivery:")) {
				totalPaidInCash = convertAmount(text2);
			} else if (text1.equals("Total paid by Gift Certificate:")) {
				if (text2.startsWith("-")) {
					text2 = text2.substring(1);
				}
				totalPaidInGiftCertificate = convertAmount(text2);
			}
		}

		/*
		 * If there are P&P or VAT amounts, we ignore those. Any imbalance in
		 * the transaction total will cause the difference to be distributed
		 * among the item prices later.
		 */
		
		if (total == 0) {
			throw new ImportException("total appears to be zero!");
		}

		originalShipment.total = total;
		originalShipment.totalPaidInCash = totalPaidInCash;
		originalShipment.totalPaidInGiftCertificate = totalPaidInGiftCertificate;

		Iterator<AmazonItem> iterItems = originalShipment.items.iterator();
		for (Element xx : itemRows) {

			/*
			 * Process it only if it contains an element with a tag of <input>.
			 * Otherwise it is a separator row or something.
			 */
			System.out.println("nrw" + j + ": " + xx.toString());

			if (!xx.getElementsByTag("input").isEmpty()) {
				processItem(iterItems.next(), xx);
			}

			j++;
		}
		if (iterItems.hasNext()) {
			throw new ImportException("more items in shipment in order listing than in details page!");
		}
		
		System.out.println("here");
	}

	private long convertAmount(String amountText) {
		String x = amountText.substring(1).replaceFirst("\\.", "");
		return Long.valueOf(x);
	}

	private void processItem(AmazonItem originalItem, Element xx) throws ImportException {
		/*
		 * Second child is td from which ASIN can be extracted
		 * Third child is td with price
		 */
		String hrefWithAsin = xx.children().get(1).getElementsByTag("a").attr("href");
		int beginIndex = hrefWithAsin.indexOf("/ASIN/");
		if (beginIndex != -1) {
			beginIndex += "/ASIN/".length();
			int endIndex = hrefWithAsin.indexOf("/", beginIndex);
			String asin = hrefWithAsin.substring(beginIndex, endIndex);
			System.out.println("ASIN: " + asin);
			if (!originalItem.asin.equals(asin)) {
				throw new ImportException("item lists do not match");
			}
		}

		String priceText = xx.children().get(2).text();
		System.out.println("price: " + priceText);

		
		
		/*
		 * Once in a while the price is missing.  We put in a price of one penny and
		 * this has to be manually corrected.
		 */
		if (priceText == null) {
			originalItem.price = 1;
		} else {
			originalItem.price = convertAmount(priceText);
		}
	}

	private void writeTransactions(List<AmazonOrder> orders) throws ImportException {
		for (AmazonOrder order: orders) {

			for (AmazonShipment shipment: order.shipments) {
				Transaction trans;

				AmazonEntry chargeEntry = null;
				if (shipment.totalPaidInCash != 0) {
					/*
					 * Auto-match the new entry in the charge account the same way that any other
					 * entry would be auto-matched.  This combines the entry if the entry already exists in the
					 * charge account (typically because transactions have been downloaded from the bank and imported).
					 *
					 * An entry in the charge account has already been matched to an
					 * Amazon order if it has an order id set.  This matcher will not return
					 * entries that have already been matched.
					 *
					 * Although we have already eliminated orders that have already been imported,
					 * this test ensures we don't mess up when more than one order can match to the
					 * same debit in the charge account.  This is not likely but two orders of the same
					 * amount and the same or very close dates may cause this.
					 *
					 * Note that we search ten days ahead for a matching entry in the charge account.
					 * Although Amazon usually charge on the day of shipment, we don't actually know the
					 * shipment date, we have just the order and the delivery date.  We go forward ten days
					 * from the order date though it may be better to search from the order date to the delivery
					 * date or a few days after the delivery date.
					 */
					MatchingEntryFinder matchFinder = new MatchingEntryFinder() {
						@Override
						protected boolean doNotConsiderEntryForMatch(Entry entry) {
							return AmazonEntryInfo.getOrderIdAccessor().getValue(entry) != null;
						}
					};
					Entry matchedEntryInChargeAccount = matchFinder.findMatch(chargeAccount, -shipment.totalPaidInCash, order.getOrderDate(), 10, null);

					/*
					 * Create an entry for the amount charged to the charge account.
					 * 
					 * Note that if a match is found but that entry has split entries then we don't
					 * merge the transactions.  We leave two transactions that must be manually merged.
					 * This is just because it is too hard otherwise to ensure we don't lose data. 
					 */
					if (matchedEntryInChargeAccount == null
							|| matchedEntryInChargeAccount.getTransaction().hasMoreThanTwoEntries()) {
						
						if (matchedEntryInChargeAccount != null && matchedEntryInChargeAccount.getTransaction().hasMoreThanTwoEntries()) {
							MessageDialog.openInformation(getShell(), "Problem Transaction", "For amount " + shipment.totalPaidInCash + ", transaction already split so you must manually merge.");
						}

						trans = session.createTransaction();
						trans.setDate(order.getOrderDate());

						chargeEntry = trans.createEntry().getExtension(AmazonEntryInfo.getPropertySet(), true);
						chargeEntry.setAccount(chargeAccount);
						chargeEntry.setAmount(-shipment.totalPaidInCash);
					} else {
						trans = matchedEntryInChargeAccount.getTransaction();
						chargeEntry = matchedEntryInChargeAccount.getExtension(AmazonEntryInfo.getPropertySet(), true);

						Entry otherMatchedEntry = matchedEntryInChargeAccount.getTransaction().getOther(matchedEntryInChargeAccount);
						// Any checks on the other entry before we delete it?
						matchedEntryInChargeAccount.getTransaction().deleteEntry(otherMatchedEntry);
					}
				} else {
					trans = session.createTransaction();
					trans.setDate(order.getOrderDate());
				}
				
				AmazonEntry giftCertificateEntry = null;
				if (shipment.totalPaidInGiftCertificate != 0) {
					System.out.println("Gift certs: " + shipment.totalPaidInGiftCertificate);

					giftCertificateEntry = trans.createEntry().getExtension(AmazonEntryInfo.getPropertySet(), true);
					giftCertificateEntry.setAccount(giftCertificateAccount);
					giftCertificateEntry.setAmount(-shipment.totalPaidInGiftCertificate);
				} else {
					if (chargeAccount == null) {
						throw new ImportException("There must be either charge account or gift certificate payment");
					}
				}
	
				Set<String> sellers = new HashSet<String>();

				List<Entry> itemEntries = new ArrayList<Entry>();
				
				for (AmazonItem item : shipment.items) {
					/*
					 * We don't know the price of the individual items because
					 * this requires following links.  So for time being, just
					 * distribute the shipment cost among the items anyhow.
					 */
					long itemAmount = item.getPrice();

					String description = item.description;
					if (order.giftRecipient != null) {
						description = "gift to " + order.giftRecipient + ", " + description;
					}
					
					AmazonEntry itemEntry = trans.createEntry().getExtension(AmazonEntryInfo.getPropertySet(), true);
					itemEntry.setAccount(unknownAmazonPurchaseAccount);
					itemEntry.setAmount(itemAmount);
					itemEntry.setMemo(description);
					itemEntry.setOrderId(order.id);
					itemEntry.setShipmentDate(shipment.getDispatchDate());
					itemEntry.setAsinOrIsbn(item.asin);
					if (shipment.carrier == null) {
						itemEntry.setTrackingNumber(shipment.trackingNumber);
					} else if (shipment.trackingNumber == null) {
						itemEntry.setTrackingNumber(shipment.carrier);
					} else {
						itemEntry.setTrackingNumber(MessageFormat.format(
								"{0}: {1}",
								shipment.carrier,
								shipment.trackingNumber));
					}

					/*
					 * See http://superuser.com/questions/123911/how-to-get-the-full-
					 * image-when-the-shopping-site-like-amazon-shows-you-only-a-pa
					 * and also http://aaugh.com/imageabuse.html
					 *
					 * SL400 means scale (maintaining the height to width ratio) so that
					 * the largest dimension is 400 pixels.
					 */
					String urlString = MessageFormat
							.format("http://z2-ec2.images-amazon.com/images/P/{0}.01._SL400_SCRMZZZZZZZ_V217104471.jpg",
									item.asin);
					try {
						URL picture = new URL(urlString);
						itemEntry.setPicture(new UrlBlob(picture));
					} catch (MalformedURLException e) {
						// Should not happen so convert to an unchecked exception
						throw new RuntimeException(e);
					}

					itemEntries.add(itemEntry.getBaseObject());
					
					sellers.add(item.seller);
				}

				StringBuffer sellerText = new StringBuffer();
				String separator = "";
				for (String seller : sellers) {
					sellerText.append(separator).append(seller);
					separator = ",";
				}

				if (chargeEntry != null) {
					chargeEntry.setMemo("Amazon - " + sellerText.toString());
					chargeEntry.setOrderId(order.id);
					chargeEntry.setShipmentDate(shipment.getDispatchDate());
				}
				if (giftCertificateEntry != null) {
					giftCertificateEntry.setMemo("Amazon - " + sellerText.toString());
					giftCertificateEntry.setOrderId(order.id);
					giftCertificateEntry.setShipmentDate(shipment.getDispatchDate());
				}
				
				/*
				 * Distribute the postage and packaging amongst the items
				 * in proportion to the price of each item.
				 */
				if (shipment.total != 0) {
					AmazonOrderImportWizard.distribute(trans, itemEntries);
				}

				AmazonItemImportWizard.assertValid(trans);
			}
		}
	}
}