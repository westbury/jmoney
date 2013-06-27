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

	private Currency thisCurrency;

	private Pattern deliveryEstimatePattern;

	private Pattern deliveredOnPattern;

	private Pattern dispatchDatePattern;

	private Pattern trackingInfoPattern;

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

		try {
			deliveryEstimatePattern = Pattern.compile("Delivery estimate \\w{3,6}day (\\d{1,2} \\w{3,3} \\d{4,4})( - \\w{3,6}day (\\d{1,2} \\w{3,3} \\d{4,4}))?");
			deliveredOnPattern = Pattern.compile("Delivered On: \\w{3,6}day (\\d{1,2} \\w{3,3} \\w{4,4})");
			dispatchDatePattern = Pattern.compile("\\s*Delivery \\#\\d: Dispatched on (\\d{1,2} \\w{3,3} \\w{4,4})\\s*");
			trackingInfoPattern = Pattern.compile(".*1 package via (.*) with tracking number (.*)");
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
				if (eachAccount.getName().startsWith("Nationwide Flex")
						&& eachAccount instanceof CurrencyAccount
						&& ((CurrencyAccount)eachAccount).getCurrency() == thisCurrency) {
					chargeAccount = (CurrencyAccount)eachAccount;
					break;
				}
			}
			if (chargeAccount == null) {
				throw new ImportException("No account exists with a name that begins 'Nationwide Flex' and a currency of " + thisCurrency.getName() + ".");
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

									System.out.println(doc.toString());

									int x2 = doc.toString().indexOf("Meg");
									String y = doc.toString().substring(x2 - 100, x2 + 100);

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
								for (Element listItem : child.children()) {
									String listItemTitle = listItem.children().get(0).text();
									String listItemValue = listItem.children().get(1).text();
									if (listItemTitle.equals("Order #")) {
										order.id2 = listItemValue;
									} else if (listItemTitle.equals("Total")) {
										order.total = listItemValue;
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
							 */

							System.out.println(shipContainElement);
							for (Iterator<Element> shipContainChildIter = shipContainElement.children().iterator(); shipContainChildIter.hasNext(); ) {
								Element shipContainChild = shipContainChildIter.next();
								System.out.println(shipContainChild.className());

								String classNames [] = shipContainChild.className().split("\\s");
								if (classNames[0].equals("status")) {
									Element statusElement = shipContainChild;

									shipContainChild = shipContainChildIter.next();
									String classNames2 [] = shipContainChild.className().split("\\s");
									if (!classNames2[0].equals("ship-listing")) {
										throw new RuntimeException("'primary-action' expected after h2");
									}
									Element shipListingElement = shipContainChild;

									/*
									 * Inside 'status', we have 'deliv-text', h2, and 'primary-action'.
									 */
									System.out.println(statusElement);

									Elements delivTextElements = statusElement.getElementsByClass("deliv-text");
									assert (delivTextElements.size() == 1);
									Element deliveryTextElement = delivTextElements.get(0);


									AmazonShipment shipment = new AmazonShipment();
									order.shipments.add(shipment);

									System.out.println("deliv-text:  " + deliveryTextElement.toString());
									String deliveryText = deliveryTextElement.text();

									Matcher matcher;
									if ((matcher = deliveryEstimatePattern.matcher(deliveryText)).matches()) {
										String datePart = matcher.group(1);
										shipment.deliveryDate = datePart;
									} else if ((matcher = deliveredOnPattern.matcher(deliveryText)).matches()) {
										String datePart = matcher.group(1);
										shipment.deliveryDate = datePart;
									} else {
										throw new RuntimeException("Unexpected delivery date: " + deliveryText);
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
											AmazonItem item = new AmazonItem();
											shipment.items.add(item);

											Element imgElement = listItem.getElementsByTag("img").get(0);
											item.img = imgElement.attr("src");

											Element descriptionElement = listItem.getElementsByClass("item-title").get(0);
											item.description = descriptionElement.text();

											Element sellerElement = listItem.getElementsByClass("seller").get(0);
											String seller = sellerElement.text().trim();
											if (seller.startsWith("Sold by:")) {
												item.seller = seller.substring("Sold by:".length()).trim();
											} else {
												item.seller = seller;
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
		// TEMP
		order.id = "026-6515743-5374741";

		String fileName = MessageFormat.format("C:\\Documents and Settings\\user1\\My Documents\\Downloads\\Amazon.co.uk - Order {0}.htm", order.id);
		File htmlFile = new File(fileName);
		if (htmlFile.exists()) {
			Document doc = Jsoup.parse(htmlFile, "UTF-8", "http://example.com/");

			int deliveryNumber = 0;
			Iterator<AmazonShipment> iterShipments = order.shipments.iterator();
			for (Element deliveryAElement : doc.getElementsByTag("a")) {
				String deliveryName = "shipped-items-" + deliveryNumber;
				if (deliveryName.equals(deliveryAElement.attr("name"))) {
					processShipment(order, iterShipments.next(), deliveryAElement);
					deliveryNumber++;
				}
			}
			if (iterShipments.hasNext()) {
				throw new ImportException("More shipments in order listing than in details page for order " + order.id);
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
			order.shipments.get(0).items.get(0).price = order.total;
		}
	}

	private void processShipment(AmazonOrder order, AmazonShipment originalShipment, Element deliveryAElement) throws ImportException {
		Elements childElements = deliveryAElement.children();

		Matcher matcher;

		/*
		 * Add the dispatch date.  This does not appear in the order listing
		 * so will be set only when the order details page is available.
		 */
		dispatchDatePattern = Pattern.compile("Delivery #\\d: Dispatched on (\\d{1,2} \\w{3,3} \\d{4,4}).*");

		String dispatchDateText = childElements.get(0).text();
		if ((matcher = dispatchDatePattern.matcher(dispatchDateText)).matches()) {
			String datePart = matcher.group(1);
			originalShipment.dispatchDate = datePart;
		} else {
			throw new RuntimeException("Unexpected dispatch date: " + dispatchDateText);
		}

		Element tableBodyElement = childElements.get(1)  // Second table
				.children().get(0)  // Now on tbody
				.children().get(0)  // Now on tr
				.children().get(0)  // Now on td
				.children().get(0)  // Now on table
				.children().get(0);  // Now on tbody

		/*
		 * Add the tracking information.  This does not appear in the order listing
		 * so will be set only when the order details page is available.
		 */
		Element elementRowWithTrackingInfo = tableBodyElement.children().get(0);  // Now on tr
		String trackingText = elementRowWithTrackingInfo.text();
		if ((matcher = trackingInfoPattern.matcher(trackingText)).matches()) {
			originalShipment.carrier = matcher.group(1);
			originalShipment.trackingNumber = matcher.group(2);
		} else {
			throw new RuntimeException("Unexpected dispatch date: " + dispatchDateText);
		}

		long subTotal = 0;
		long postageAndPackaging = 0;
		long beforeVat = 0;
		long vat = 0;
		long total = 0;

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
			}
		}

		if (postageAndPackaging != 0) {
			throw new ImportException("non-zero postage and packing not yet supported");
		}
		if (vat != 0) {
			throw new ImportException("non-zero VAT not yet supported");
		}

		originalShipment.total = total;

		int j = 0;
		// Second tr, second td (first td has delivery address),
		// 2nd child is a table with item prices,
		// tbody
		System.out.println("XXX: " + elementZ.children().get(childIndex).children().get(0).toString());

		Iterator<AmazonItem> iterItems = originalShipment.items.iterator();

		Elements itemRows = elementZ.children().get(childIndex).children().get(0).children();
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
			throw new ImportException("More items in order listing page than in details page! " + order.id);
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
		int beginIndex = hrefWithAsin.indexOf("/ASIN/") + "/ASIN/".length();
		int endIndex = hrefWithAsin.indexOf("/", beginIndex);
		String asin = hrefWithAsin.substring(beginIndex, endIndex);

		String priceText = xx.children().get(2).text();
		System.out.println("ASIN: " + asin);
		System.out.println("price: " + priceText);

		if (!originalItem.asin.equals(asin)) {
			throw new ImportException("item lists do not match");
		}

		originalItem.price = priceText;
	}

	private void writeTransactions(List<AmazonOrder> orders) throws ImportException {
		for (AmazonOrder order: orders) {

			for (AmazonShipment shipment: order.shipments) {

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
					protected boolean alreadyMatched(Entry entry) {
						return AmazonEntryInfo.getOrderIdAccessor().getValue(entry) != null;
					}
				};
				Entry matchedEntryInChargeAccount = matchFinder.findMatch(chargeAccount, -shipment.getTotal(), order.getOrderDate(), 10, null);

				/*
				 * Create an entry for the amount charged to the charge account.
				 */
				Transaction trans;
				AmazonEntry chargeEntry;
				if (matchedEntryInChargeAccount == null) {
					trans = session.createTransaction();
					trans.setDate(order.getOrderDate());

					chargeEntry = trans.createEntry().getExtension(AmazonEntryInfo.getPropertySet(), true);
					chargeEntry.setAccount(chargeAccount);
					chargeEntry.setAmount(-shipment.getTotal());
				} else {
					trans = matchedEntryInChargeAccount.getTransaction();
					chargeEntry = matchedEntryInChargeAccount.getExtension(AmazonEntryInfo.getPropertySet(), true);

					if (matchedEntryInChargeAccount.getTransaction().hasMoreThanTwoEntries()) {
						throw new ImportException("matched entry in charge account has more than one other entry");
					}

					Entry otherMatchedEntry = matchedEntryInChargeAccount.getTransaction().getOther(matchedEntryInChargeAccount);
					// Any checks on the other entry before we delete it?
					matchedEntryInChargeAccount.getTransaction().deleteEntry(otherMatchedEntry);
				}

				Set<String> sellers = new HashSet<String>();

				for (AmazonItem item : shipment.items) {
					/*
					 * We don't know the price of the individual items because
					 * this requires following links.  So for time being, just
					 * distribute the shipment cost among the items anyhow.
					 */
					long itemAmount = item.getPrice();

					AmazonEntry itemEntry = trans.createEntry().getExtension(AmazonEntryInfo.getPropertySet(), true);
					itemEntry.setAccount(unknownAmazonPurchaseAccount);
					itemEntry.setAmount(itemAmount);
					itemEntry.setMemo(item.description);
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

					sellers.add(item.seller);
				}

				StringBuffer sellerText = new StringBuffer();
				String separator = "";
				for (String seller : sellers) {
					sellerText.append(separator).append(seller);
					separator = ",";
				}

				chargeEntry.setMemo("Amazon - " + sellerText.toString());
				chargeEntry.setOrderId(order.id);
				chargeEntry.setShipmentDate(shipment.getDeliveryDate());
			}
		}
	}
}