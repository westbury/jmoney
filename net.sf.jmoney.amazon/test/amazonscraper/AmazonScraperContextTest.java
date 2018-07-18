package amazonscraper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.json.JSONObject;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;

public class AmazonScraperContextTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	/**
	 * The basic test for the 'orders' page.
	 * 
	 * This test is a purchase of a single item, no postage and packaging costs or
	 * any other costs.  The item unit price is the same as the order total.
	 * 
	 * @throws UnsupportedImportDataException
	 */
	@Test
	public void basicSingleItemOrderTest() throws UnsupportedImportDataException {
		JsonItem expectedItem = new JsonItem(2553)
				.setCategory("default category");
		JsonShipment expectedShipment = new JsonShipment(-2553, expectedItem)
				.setPostageAndPackagingAmount(0);
		JsonOrder expectedOrder = new JsonOrder(expectedShipment);
		JSONObject expected = new JsonOrders(expectedOrder).object;
		
		testOrderImport(expected, "amazon-orders-basic.txt");
	}

	/**
	 * The basic test for the 'orders' page but with a quantity of more than one of
	 * the item.
	 * 
	 * This test is a purchase of a quantity of a single item, no postage and packaging costs or
	 * any other costs.  The item unit price times the quantity is the same as the order total.
	 * 
	 * @throws UnsupportedImportDataException
	 */
	@Test
	public void itemWithQuantityOrderTest() throws UnsupportedImportDataException {
		JsonItem expectedItem = new JsonItem(1988)
				.setQuantity(4);
		JsonShipment expectedShipment = new JsonShipment(-1988, expectedItem)
				.setPostageAndPackagingAmount(0);
		JsonOrder expectedOrder = new JsonOrder(expectedShipment);
		JSONObject expected = new JsonOrders(expectedOrder).object;
		
		testOrderImport(expected, "amazon-orders-with-quantity.txt");
	}

	/**
	 * This test has both a promotional discount and multiple shipments.
	 * 
	 * @throws UnsupportedImportDataException
	 */
	@Test
	public void promotionAndMultipleShipmentDetailTest() throws UnsupportedImportDataException {
		JsonItem expectedItem1 = new JsonItem(648);
		JsonShipment expectedShipment1 = new JsonShipment(-648, expectedItem1)
				.setPromotionAmount(0);

		JsonItem expectedItem2 = new JsonItem(559);
		JsonShipment expectedShipment2 = new JsonShipment(-559, expectedItem2)
				.setPromotionAmount(0);
		
		JsonItem expectedItem3 = new JsonItem(1198)
				.setQuantity(2);
		JsonItem expectedItem4 = new JsonItem(344);
		JsonShipment expectedShipment3 = new JsonShipment(-1442, expectedItem3, expectedItem4)
				.setPromotionAmount(100);
		
		JsonOrder expectedOrder = new JsonOrder(expectedShipment1, expectedShipment2, expectedShipment3);
		JSONObject expected = new JsonOrders(expectedOrder).object;
		
		testDetailImport(expected, "amazon-details-promotion-and-many-shipments.txt");
	}

	@Test
	public void overseasReturnedDetailsTest() throws UnsupportedImportDataException {
		JsonItem expectedItemSale = new JsonItem(8941)
				.setCategory("returned item category");
		JsonItem expectedItemReturn = new JsonItem(-8941)
				.setCategory("returned item category");
		JsonShipment expectedShipment1 = new JsonShipment(-11258, expectedItemSale)
				.setImportFeesDepositAmount(1876)
				.setPostageAndPackagingAmount(441);
		JsonShipment expectedShipment2 = new JsonShipment(10823, expectedItemReturn)
				.setImportFeesDepositAmount(-1882)
				.setPostageAndPackagingAmount(0);
		JsonOrder expectedOrder = new JsonOrder(expectedShipment1, expectedShipment2);
		JSONObject expected = new JsonOrders(expectedOrder).object;

		testDetailImport(expected, "amazon-details-overseas-return.txt");
	}

	@Test
	public void overseasReturnedOrdersAndDetailsTest() throws UnsupportedImportDataException {
		JsonItem expectedItemSale = new JsonItem(8941)
				.setCategory("returned item category");
		JsonItem expectedItemReturn = new JsonItem(-8941)
				.setCategory("returned item category");
		JsonShipment expectedShipment1 = new JsonShipment(-11258, expectedItemSale)
				.setImportFeesDepositAmount(1876)
				.setPostageAndPackagingAmount(441);
		JsonShipment expectedShipment2 = new JsonShipment(10823, expectedItemReturn)
				.setImportFeesDepositAmount(-1882)
				.setPostageAndPackagingAmount(0);
		JsonOrder expectedOrder = new JsonOrder(expectedShipment1, expectedShipment2);

		JsonItem expectedItemAnother = new JsonItem(2553)
				.setCategory("default category");
		JsonShipment expectedShipmentAnother = new JsonShipment(-2553, expectedItemAnother)
				.setPostageAndPackagingAmount(0);
		JsonOrder expectedOrderAnother = new JsonOrder(expectedShipmentAnother);
		
		JSONObject expected = new JsonOrders(expectedOrder, expectedOrderAnother).object;
		
		testOrderAndDetailImport(expected, "amazon-orders-overseas-return.txt", "amazon-details-overseas-return.txt");
	}

	@Test
	public void quantityVatAndGiftcardOrdersAndDetailsTest() throws UnsupportedImportDataException {
		JsonItem expectedItemGrey = new JsonItem(1396)
				.setDescription("L'Oreal Paris Superliner Perfect Slim Eyeliner Grey 05 6m")
				.setQuantity(2);
		JsonItem expectedItemGreen = new JsonItem(699)
				.setDescription("L'Oréal Paris Super Liner Perfect Slim, Green");
		JsonShipment expectedShipmentGrey = new JsonShipment(-1326, expectedItemGrey)
				.setGiftcardAmount(70);
		JsonShipment expectedShipmentGreen = new JsonShipment(0, expectedItemGreen)
				.setGiftcardAmount(699);
		JsonOrder expectedOrderGrey = new JsonOrder(expectedShipmentGrey);
		JsonOrder expectedOrderGreen = new JsonOrder(expectedShipmentGreen);

		JSONObject expected = new JsonOrders(expectedOrderGrey, expectedOrderGreen).object;
		
		ContextUpdaterTest contextUpdater = new ContextUpdaterTest();
		AmazonScraperContext scraperContext = new AmazonScraperContext(contextUpdater);
		
		String ordersText = getTextFromResource("orders-with-quantity-vat-giftcard.txt");
		scraperContext.importOrders(ordersText);
		
		String detailsText1 = getTextFromResource("details-with-quantity-vat-giftcard-grey.txt");
		scraperContext.importDetails(detailsText1);
		
		String detailsText2 = getTextFromResource("details-with-quantity-vat-giftcard-green.txt");
		scraperContext.importDetails(detailsText2);
		
		System.out.println(contextUpdater.toJson().toString(4));
		
		JSONAssert.assertEquals(expected, contextUpdater.toJson(), false);
	}

	/**
	 * 'Orders' page with the sale of an item and the exchange of that item.
	 * 
	 * @throws UnsupportedImportDataException
	 */
	@Test
	public void saleAndExchangeOrderTest() throws UnsupportedImportDataException {
		JsonItem expectedOriginalItem = new JsonItem(13995)
				.setOrderNumber("204-0999598-1294729")
				.setDescription("NYDJ Women's Alina Jeggings Jeans, Blue (Enzyme Wash), 14 UK Long (40 EU Long)")
				.setCategory("giftcard category");
//		JsonItem expectedReturnOfItem = new JsonItem(-13995)
//				.setOrderNumber("204-8149274-1332306")
//				.setDescription("NYDJ Women's Alina Jeggings Jeans, Blue (Enzyme Wash), 14 UK Long (40 EU Long)")
//				.setCategory("returned item category")
//				.setQuantity(-1);
		JsonItem expectedReplacementItem = new JsonItem(13995)
				.setOrderNumber("204-8149274-1332306")
				.setDescription("NYDJ Women's Alina Jeggings Jeans, Blue (Enzyme Wash), 16 UK Long (42 EU Long)")
				.setCategory("default category");
		JsonShipment expectedShipment1 = new JsonShipment(-13995, expectedOriginalItem)
				.setPostageAndPackagingAmount(0);
		JsonShipment expectedShipment2 = new JsonShipment(-13995, /*expectedReturnOfItem,*/ expectedReplacementItem)
				.setPostageAndPackagingAmount(0)
				.setGiftcardAmount(0);
		JsonOrder expectedOrder1 = new JsonOrder(expectedShipment1);
		JsonOrder expectedOrder2 = new JsonOrder(expectedShipment2);
		JSONObject expected = new JsonOrders(expectedOrder1, expectedOrder2).object;
		
		ContextUpdaterTest contextUpdater = new ContextUpdaterTest();
		AmazonScraperContext scraperContext = new AmazonScraperContext(contextUpdater);

		// First check import of just orders, which results in an unbalanced transaction.
		String ordersText = getTextFromResource("orders-with-exchanged-item.txt");
		scraperContext.importOrders(ordersText);

		System.out.println(contextUpdater.toJson().toString(4));

		JSONAssert.assertEquals(expected, contextUpdater.toJson(), false);

		// Now add the details.
		expectedShipment2.setChargeAmount(0).setGiftcardAmount(13995);
		
		String detailsText = getTextFromResource("details-with-exchanged-item.txt");
		scraperContext.importDetails(detailsText);

		System.out.println(contextUpdater.toJson().toString(4));

		JSONAssert.assertEquals(expected, contextUpdater.toJson(), false);
	}

	/**
	 * An unusually complex movie, imported from the 'orders' page.
	 * 
	 * @throws UnsupportedImportDataException
	 */
	@Test
	public void complexMovieOrderTest() throws UnsupportedImportDataException {
		JsonItem expectedItem = new JsonItem(748)
				.isMovie(true)
				.setDescription("Mad Men - Season 4");
		JsonShipment expectedShipment = new JsonShipment(-748, expectedItem)
				.setPostageAndPackagingAmount(0);
		JsonOrder expectedOrder = new JsonOrder(expectedShipment);
		JSONObject expected = new JsonOrders(expectedOrder).object;
		
		testOrderImport(expected, "amazon-orders-with-complex-movie.txt");
	}

	/**
	 * Helper method for tests where a single 'orders' page is being
	 * imported.
	 * 
	 * @param expected
	 * @param ordersPageResourceName
	 */
	private void testOrderImport(JSONObject expected, String ordersPageResourceName) {
		ContextUpdaterTest contextUpdater = new ContextUpdaterTest();
		AmazonScraperContext scraperContext = new AmazonScraperContext(contextUpdater);

		String ordersText = getTextFromResource(ordersPageResourceName);
		scraperContext.importOrders(ordersText);
		
		System.out.println(contextUpdater.toJson().toString(4));

		JSONAssert.assertEquals(expected, contextUpdater.toJson(), false);
	}

	/**
	 * Helper method for tests where a single 'orders' page is being
	 * imported followed by a single 'details' page.
	 * 
	 * @param expected
	 * @param ordersPageResourceName
	 * @param detailsPageResourceName
	 * @throws UnsupportedImportDataException 
	 */
	private void testDetailImport(JSONObject expected, String detailsPageResourceName) throws UnsupportedImportDataException {
		ContextUpdaterTest contextUpdater = new ContextUpdaterTest();
		AmazonScraperContext scraperContext = new AmazonScraperContext(contextUpdater);

		String detailsText = getTextFromResource(detailsPageResourceName);
		scraperContext.importDetails(detailsText);
		
		System.out.println(contextUpdater.toJson().toString(4));

		JSONAssert.assertEquals(expected, contextUpdater.toJson(), false);
	}

	/**
	 * Helper method for tests where a single 'orders' page is being
	 * imported followed by a single 'details' page.
	 * 
	 * @param expected
	 * @param ordersPageResourceName
	 * @param detailsPageResourceName
	 * @throws UnsupportedImportDataException 
	 */
	private void testOrderAndDetailImport(JSONObject expected, String ordersPageResourceName, String detailsPageResourceName) throws UnsupportedImportDataException {
		ContextUpdaterTest contextUpdater = new ContextUpdaterTest();
		AmazonScraperContext scraperContext = new AmazonScraperContext(contextUpdater);

		String ordersText = getTextFromResource(ordersPageResourceName);
		scraperContext.importOrders(ordersText);
		
		String detailsText = getTextFromResource(detailsPageResourceName);
		scraperContext.importDetails(detailsText);
		
		System.out.println(contextUpdater.toJson().toString(4));

		JSONAssert.assertEquals(expected, contextUpdater.toJson(), false);
	}

	private String getTextFromResource(String resourceName) {
		ClassLoader classLoader = getClass().getClassLoader();
		try (
				InputStream inputStream = classLoader.getResourceAsStream(resourceName);
			    BufferedReader in = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
		) {
		    String inputLine;
		 
		    StringBuffer outputBuffer = new StringBuffer();
		    while ((inputLine = in.readLine()) != null) {
		        outputBuffer.append(inputLine).append('\n');
		    }
		    
		    return outputBuffer.toString();
		} catch (IOException e) {
		    throw new RuntimeException(e);
		}
	}

}
