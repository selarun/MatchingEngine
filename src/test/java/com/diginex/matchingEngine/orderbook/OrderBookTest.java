package com.diginex.matchingEngine.orderbook;


import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.diginex.matchingEngine.store.OrderStore;
import com.diginex.matchingEngine.util.GcLogUtil;
import com.diginex.matchingEngine.util.OrderInputReader;
import com.diginex.matchingEngine.util.PoolManager;
import com.diginex.matchingEngine.util.PriceUtils;
import com.diginex.matchingEngine.util.ProductUtil;




public class OrderBookTest implements UtilityTest {

	private static Logger log = Logger.getLogger(OrderBookTest.class);
	final TradeCollector collector = new TradeCollector();
	final OrderBook orderBook = new OrderBook(collector);
	Path tempPath;

	long testProductId = ProductUtil.TESTPRODUCTID;
	long testDefaultTick = PriceUtils.DEFAULT_TICK_SIZE;
	long testDefaultLotSize = ProductUtil.DEFAULT_LOT_SIZE;

	@BeforeClass
	public static void setUpTesting() {
		log.info("Starting Test run");
	}
	
	@AfterClass
	public static void tearDownTesting() {
		log.info("End of Test run");
	}
	@BeforeEach
	public void setUp(TestInfo testInfo) throws IOException {	
		log.info("Running Test:"+testInfo.getTestMethod().get());
		tempPath = Files.createTempDirectory("output");
		BasicConfigurator.configure();
		collector.init();
		GcLogUtil.startLoggingGc();
		
	}

	@AfterEach
	public void tearDown(TestInfo testInfo) {
		orderBook.clearOrderBook();	
		
		log.info("End of Test:"+testInfo.getTestMethod().get());
	}

	private Order createOrder(int orderId, Side side, double price, int qty) {
		long px = PriceUtils.convertPriceToLong(price);
		Order order = PoolManager.getInstance().takeOrder();
		order.init(orderId, side, px, qty, testProductId);
		return order;
	}

	private Order createOrder(int orderId, Side side, int qty) {
		Order order = PoolManager.getInstance().takeOrder();
		order.init(orderId, side,  qty, testProductId);
		return order;
	}

	@Test
	public void testNoMatchCase() {
		Order buyPassive = createOrder(1000, Side.BUY, 100, 100);
		Order sellPassive = createOrder(10001, Side.SELL, 101, 100);
		assertThat(orderBook.processNewOrder(buyPassive), equalTo(OrderReason.VALID));
		assertThat(orderBook.processNewOrder(sellPassive), equalTo(OrderReason.VALID));
		assertThat("No match expected", collector.getTradeSize(), equalTo(0));
	}

	@Test
	public void testInvalidOrderQty() {
		Order buyPassive = createOrder(1000, Side.BUY, 100, 0);
		Order sellPassive = createOrder(10001, Side.SELL, 101, -100);
		assertThat(orderBook.processNewOrder(buyPassive), equalTo(OrderReason.INVALID_ORDER_QUANTITY));
		assertThat(orderBook.processNewOrder(sellPassive), equalTo(OrderReason.INVALID_ORDER_QUANTITY));
	}

	@Test
	public void testInvalidOrderSide() {
		Order buyPassive = createOrder(1000, Side.INVALID, 100, 100);
		Order sellPassive = createOrder(10001, Side.INVALID, 101, 100);
		assertThat(orderBook.processNewOrder(buyPassive), equalTo(OrderReason.INVALID_SIDE));
		assertThat(orderBook.processNewOrder(sellPassive), equalTo(OrderReason.INVALID_SIDE));
	}

	@Test
	public void testInvalidOrderQtyNotInLotSizeOnNew() {
		Order buyOrder = createOrder(1000, Side.BUY, 100, 15);
		Order sellOrder = createOrder(1000, Side.SELL, 100, 15);
		assertThat(orderBook.processNewOrder(buyOrder), equalTo(OrderReason.INVALID_ORDER_QUANTITY));
		assertThat(orderBook.processNewOrder(sellOrder), equalTo(OrderReason.INVALID_ORDER_QUANTITY));

	}

	@Test
	public void testInvalidPriceNotInTickOnNew() {
		Order buyOrder = createOrder(1000, Side.BUY, 100.05, 100);
		Order sellOrder = createOrder(1000, Side.SELL, 100.05, 10);
		assertThat(orderBook.processNewOrder(buyOrder), equalTo(OrderReason.INVALID_PRICE));
		assertThat(orderBook.processNewOrder(sellOrder), equalTo(OrderReason.INVALID_PRICE));

	}

	@Test
	public void testDuplicateOrder() {
		Order buyPassive = createOrder(1000, Side.BUY, 100, 100);
		Order buyPassive1 = createOrder(1000, Side.BUY, 101, 100);
		assertThat(orderBook.processNewOrder(buyPassive), equalTo(OrderReason.VALID));
		assertThat(orderBook.processNewOrder(buyPassive1), equalTo(OrderReason.ORDER_ALREADY_EXISTS));
	}

	@Test
	public void testAggressivePartialMatch() {
		Order buy = createOrder(1000, Side.BUY, 100, 200);
		Order sellAgg = createOrder(1001, Side.SELL, 99, 200);
		assertThat(orderBook.processNewOrder(buy), equalTo(OrderReason.VALID));
		assertThat(orderBook.processNewOrder(sellAgg), equalTo(OrderReason.VALID));
		assertThat("match expected", collector.getTradeSize(), equalTo(2));
	}
	
	
	@Test
	public void testMatchMarketOrders() {
		Order buyMkt = createOrder(1000, Side.BUY,  200);
		Order sellMkt = createOrder(1001, Side.SELL,100);
		Order sellMkt1 = createOrder(1002, Side.SELL,100);
		assertThat(orderBook.processNewOrder(buyMkt), equalTo(OrderReason.VALID));
		assertThat(orderBook.processNewOrder(sellMkt), equalTo(OrderReason.VALID));
		assertThat(orderBook.processNewOrder(sellMkt1), equalTo(OrderReason.VALID));
		assertThat("match expected", collector.getTradeSize(), equalTo(4));
	}

	@ParameterizedTest
	@CsvSource(value = { "1:src/test/resources/inputOrders_1.txt:src/test/resources/expectedActualOutput_1.txt" ,
			"2:src/test/resources/inputOrders_2.txt:src/test/resources/expectedActualOutput_2.txt" }, delimiter = ':')
	public void testInputFiles(int index, String inputFile, String expectedActualOutput)
			throws IOException {
		String actualOutput = "actualOutput.txt" + index;
		Path filePath = tempPath.resolve(actualOutput);
		Path expectedActualOutputPath = Paths.get(expectedActualOutput);
		
		File actualFile = filePath.toFile();
		if (actualFile.exists()) {
			actualFile.delete();
			actualFile.createNewFile();
		}
		PrintStream ps = new PrintStream(actualFile);
		System.setOut(ps);
		List<Order> list = OrderInputReader.generateOrders(inputFile);
		list.forEach(orderBook::processNewOrder);
		collector.printTrades();
		orderBook.printOpenOrderBook();
		assertTrue("Expected output dont match.Check the output file",
				compareTwoFiles(expectedActualOutputPath, filePath));

	}



	@Test
	public void testOrderAmendedPriceOnlyToAggTriggerTrades() {		
	
		int orderId = 1000;
		int buyOrderIdMatched = orderId;
		int sellOrderIdMatched = ++orderId;
		Order buyOrder = createOrder(buyOrderIdMatched, Side.BUY, 70, 100);
		Order sellOrder = createOrder(sellOrderIdMatched, Side.SELL, 102, 100);		
		assertThat(orderBook.processNewOrder(buyOrder), equalTo(OrderReason.VALID));
		assertThat(orderBook.processNewOrder(sellOrder), equalTo(OrderReason.VALID));
		assertThat("No match expected", collector.getTradeSize(), equalTo(0));
		buyOrder = createOrder(++orderId, Side.BUY, 100, 100);
		assertThat(orderBook.processNewOrder(buyOrder), equalTo(OrderReason.VALID));
		assertThat("No match expected ", collector.getTradeSize(), equalTo(0));
		Order buyAmendOrder = createOrder(buyOrderIdMatched, Side.BUY, 103, 100);		
		assertThat(orderBook.processAmendOrder(buyAmendOrder), equalTo(OrderReason.VALID));
		assertThat("Match expected after amend price to aggressive for Buy Order", collector.getTradeSize(),
				equalTo(2));
		assertThat("Trade doesnt match", collector.getTradeDetails(0), equalTo("trade " +buyOrderIdMatched + ",100@103.0"));
		assertThat("Trade doesnt match", collector.getTradeDetails(1), equalTo("trade " +sellOrderIdMatched + ",100@103.0"));

	}

	@Test
	public void testOrderAmendedQtyDownOnly() {	

		int orderId = 1000;
		int buyOrderIdMatched = orderId;
		int sellOrderIdMatched = ++orderId;
		Order buyOrder = createOrder(buyOrderIdMatched, Side.BUY, 70, 100);
		Order sellOrder = createOrder(sellOrderIdMatched, Side.SELL, 102, 100);		
		assertThat(orderBook.processNewOrder(buyOrder), equalTo(OrderReason.VALID));
		assertThat(orderBook.processNewOrder(sellOrder), equalTo(OrderReason.VALID));
		assertThat("No match expected", collector.getTradeSize(), equalTo(0));
		Order sellAmendOrder = createOrder(sellOrderIdMatched, Side.SELL, 102, 50);		
		assertThat(orderBook.processAmendOrder(sellAmendOrder), equalTo(OrderReason.VALID));
		assertThat("No match expected ", collector.getTradeSize(), equalTo(0));
		
		buyOrderIdMatched = ++orderId;
		buyOrder = createOrder(buyOrderIdMatched, Side.BUY, 103, 100);
		assertThat(orderBook.processNewOrder(buyOrder), equalTo(OrderReason.VALID));		
		assertThat("Match expected as sell amend qty down dont lose priority", collector.getTradeSize(),
				equalTo(2));
		assertThat("Trade doesnt match", collector.getTradeDetails(0), equalTo("trade " +buyOrderIdMatched + ",50@103.0"));
		assertThat("Trade doesnt match", collector.getTradeDetails(1), equalTo("trade " +sellOrderIdMatched + ",50@103.0"));

	}
//
	@Test
	public void testOrderAmendedQtyUpOnly() {
		int orderId = 1000;
		int buyOrderIdMatched = orderId;
		int sellOrderIdMatched = ++orderId;
		Order buyOrder = createOrder(buyOrderIdMatched, Side.BUY, 70, 100);
		Order sellOrder = createOrder(sellOrderIdMatched, Side.SELL, 102, 100);		
		assertThat(orderBook.processNewOrder(buyOrder), equalTo(OrderReason.VALID));
		assertThat(orderBook.processNewOrder(sellOrder), equalTo(OrderReason.VALID));
		assertThat("No match expected", collector.getTradeSize(), equalTo(0));
		Order sellOrder1 = createOrder(++orderId, Side.SELL, 102, 100);	
		assertThat(orderBook.processNewOrder(sellOrder1), equalTo(OrderReason.VALID));
		assertThat("No match expected", collector.getTradeSize(), equalTo(0));
		Order sellAmendOrder = createOrder(sellOrderIdMatched, Side.SELL, 102, 150);//Lose queue priority	
		assertThat(orderBook.processAmendOrder(sellAmendOrder), equalTo(OrderReason.VALID));
		assertThat("No match expected ", collector.getTradeSize(), equalTo(0));
		
		buyOrderIdMatched = ++orderId;
		buyOrder = createOrder(buyOrderIdMatched, Side.BUY, 103, 50);
		assertThat(orderBook.processNewOrder(buyOrder), equalTo(OrderReason.VALID));		
		assertThat("Match expected", collector.getTradeSize(),
				equalTo(2));
		assertThat("Trade doesnt match", collector.getTradeDetails(0), equalTo("trade " +buyOrderIdMatched + ",50@103.0"));
		assertThat("Trade doesnt match", collector.getTradeDetails(1), equalTo("trade " +sellOrder1.getOrderId() + ",50@103.0"));

	}

	@Test
	public void testOrderAmendedPricePassive() {
		int orderId = 1000;
		int buyOrderIdMatched = orderId;
		int sellOrderIdMatched = ++orderId;
		Order buyOrder = createOrder(buyOrderIdMatched, Side.BUY, 70, 100);
		Order sellOrder = createOrder(sellOrderIdMatched, Side.SELL, 102, 100);		
		assertThat(orderBook.processNewOrder(buyOrder), equalTo(OrderReason.VALID));
		assertThat(orderBook.processNewOrder(sellOrder), equalTo(OrderReason.VALID));
		assertThat("No match expected", collector.getTradeSize(), equalTo(0));
		Order sellOrder1 = createOrder(++orderId, Side.SELL, 103, 100);	
		assertThat(orderBook.processNewOrder(sellOrder1), equalTo(OrderReason.VALID));
		assertThat("No match expected", collector.getTradeSize(), equalTo(0));
		Order sellAmendOrder = createOrder(sellOrderIdMatched, Side.SELL, 103, 150);//Lose queue priority	
		assertThat(orderBook.processAmendOrder(sellAmendOrder), equalTo(OrderReason.VALID));
		assertThat("No match expected ", collector.getTradeSize(), equalTo(0));
		
		buyOrderIdMatched = ++orderId;
		buyOrder = createOrder(buyOrderIdMatched, Side.BUY, 103, 50);
		assertThat(orderBook.processNewOrder(buyOrder), equalTo(OrderReason.VALID));		
		assertThat("Match expected", collector.getTradeSize(),
				equalTo(2));
		assertThat("Trade doesnt match", collector.getTradeDetails(0), equalTo("trade " +buyOrderIdMatched + ",50@103.0"));
		assertThat("Trade doesnt match", collector.getTradeDetails(1), equalTo("trade " +sellOrder1.getOrderId() + ",50@103.0"));

	}
	
	@Test
	public void testOrderAmendedPriceAggresive() {
		int orderId = 1000;
		int buyOrderIdMatched = orderId;
		int sellOrderIdMatched = ++orderId;
		Order buyOrder = createOrder(buyOrderIdMatched, Side.BUY, 70, 100);
		Order sellOrder = createOrder(sellOrderIdMatched, Side.SELL, 102, 100);		
		assertThat(orderBook.processNewOrder(buyOrder), equalTo(OrderReason.VALID));
		assertThat(orderBook.processNewOrder(sellOrder), equalTo(OrderReason.VALID));
		assertThat("No match expected", collector.getTradeSize(), equalTo(0));
		Order sellOrder1 = createOrder(++orderId, Side.SELL, 101, 100);	
		assertThat(orderBook.processNewOrder(sellOrder1), equalTo(OrderReason.VALID));
		assertThat("No match expected", collector.getTradeSize(), equalTo(0));
		Order sellAmendOrder = createOrder(sellOrderIdMatched, Side.SELL, 101, 150);//Lose queue priority	
		assertThat(orderBook.processAmendOrder(sellAmendOrder), equalTo(OrderReason.VALID));
		assertThat("No match expected ", collector.getTradeSize(), equalTo(0));
		
		buyOrderIdMatched = ++orderId;
		buyOrder = createOrder(buyOrderIdMatched, Side.BUY, 101, 50);
		assertThat(orderBook.processNewOrder(buyOrder), equalTo(OrderReason.VALID));		
		assertThat("Match expected", collector.getTradeSize(),
				equalTo(2));
		assertThat("Trade doesnt match", collector.getTradeDetails(0), equalTo("trade " +buyOrderIdMatched + ",50@101.0"));
		assertThat("Trade doesnt match", collector.getTradeDetails(1), equalTo("trade " +sellOrder1.getOrderId() + ",50@101.0"));

	}

	@Test
	public void testOrderAmendedToMarket() {
		int orderId = 1000;
		int buyOrderIdMatched = orderId;
		int sellOrderIdMatched = ++orderId;
		Order buyOrder = createOrder(buyOrderIdMatched, Side.BUY, 70, 100);
		Order sellOrder = createOrder(sellOrderIdMatched, Side.SELL, 102, 100);		
		assertThat(orderBook.processNewOrder(buyOrder), equalTo(OrderReason.VALID));
		assertThat(orderBook.processNewOrder(sellOrder), equalTo(OrderReason.VALID));
		assertThat("No match expected", collector.getTradeSize(), equalTo(0));
		Order sellOrder1 = createOrder(++orderId, Side.SELL, 101, 100);	
		assertThat(orderBook.processNewOrder(sellOrder1), equalTo(OrderReason.VALID));
		assertThat("No match expected", collector.getTradeSize(), equalTo(0));
		Order sellAmendOrder = createOrder(sellOrderIdMatched, Side.SELL,150);
		assertThat(orderBook.processAmendOrder(sellAmendOrder), equalTo(OrderReason.VALID));
		
		assertThat("Match expected", collector.getTradeSize(),
				equalTo(2));
		assertThat("Trade doesnt match", collector.getTradeDetails(0), equalTo("trade " +sellAmendOrder.getOrderId() + ",100@70.0"));
		assertThat("Trade doesnt match", collector.getTradeDetails(1), equalTo("trade " +buyOrderIdMatched + ",100@70.0"));

	}


	@Test
	public void testInvalidOrderQtyOnAmend() {
		Order buyOrder = createOrder(1000, Side.BUY, 100, 100);
		Order sellOrder = createOrder(1001, Side.SELL, 100, 100);
		assertThat(orderBook.processNewOrder(buyOrder), equalTo(OrderReason.VALID));
		assertThat(orderBook.processNewOrder(sellOrder), equalTo(OrderReason.VALID));
		buyOrder = createOrder(1000, Side.BUY, 100, -100);
		sellOrder = createOrder(1001, Side.SELL, 100, -100);
		assertThat(orderBook.processAmendOrder(buyOrder), equalTo(OrderReason.INVALID_ORDER_QUANTITY));
	}
	
	@Test
	public void testInvalidOrderSideOnAmend() {
		Order buyOrder = createOrder(1000, Side.BUY, 100, 100);
		Order sellOrder = createOrder(1001, Side.SELL, 100, 100);
		assertThat(orderBook.processNewOrder(buyOrder), equalTo(OrderReason.VALID));
		assertThat(orderBook.processNewOrder(sellOrder), equalTo(OrderReason.VALID));
		buyOrder = createOrder(1000, Side.INVALID, 100, 100);
		sellOrder = createOrder(1001, Side.INVALID, 100, 100);
		assertThat(orderBook.processAmendOrder(buyOrder), equalTo(OrderReason.INVALID_SIDE));
	}

	@Test
	public void testInvalidOrderQtyNotInLotSizeOnAmend() {
		Order buyOrder = createOrder(1000, Side.BUY, 100, 100);
		assertThat(orderBook.processNewOrder(buyOrder), equalTo(OrderReason.VALID));
		buyOrder = createOrder(1000, Side.BUY, 100, 15);
		assertThat(orderBook.processAmendOrder(buyOrder), equalTo(OrderReason.INVALID_ORDER_QUANTITY));
	}

	@Test
	public void testInvalidPriceNotInTickOnAmend() {
		Order buyOrder = createOrder(1000, Side.BUY, 100, 100);
		assertThat(orderBook.processNewOrder(buyOrder), equalTo(OrderReason.VALID));
		buyOrder = createOrder(1000, Side.BUY, 100.05, 100);
		assertThat(orderBook.processAmendOrder(buyOrder), equalTo(OrderReason.INVALID_PRICE));
	}

	@Test
	public void testOrderAmendedOnFilledOrder() {
		Order buyOrder = createOrder(1000, Side.BUY, 100, 100);
		Order sellOrder = createOrder(1001, Side.SELL, 100, 100);
		assertThat(orderBook.processNewOrder(buyOrder), equalTo(OrderReason.VALID));
		assertThat(orderBook.processNewOrder(sellOrder), equalTo(OrderReason.VALID));
		assertThat("Match expected", collector.getTradeSize(), equalTo(2));
		Order sellAmendOrder = createOrder(1001, Side.SELL, 99, 90);
		assertThat(orderBook.processAmendOrder(sellAmendOrder), equalTo(OrderReason.TOO_LATE_TO_REPLACE));
	}

	@Test
	public void testOrderAmendedBelowFilledOrder() {		
		Order buyOrder = createOrder(1000, Side.BUY, 100, 80);
		Order sellOrder = createOrder(1001, Side.SELL, 100, 100);
		assertThat(orderBook.processNewOrder(buyOrder), equalTo(OrderReason.VALID));
		assertThat(orderBook.processNewOrder(sellOrder), equalTo(OrderReason.VALID));
		assertThat("Match expected", collector.getTradeSize(), equalTo(2));
		Order sellAmendOrder = createOrder(1001, Side.SELL, 99, 70);
		assertThat(orderBook.processAmendOrder(sellAmendOrder), equalTo(OrderReason.INVALID_REPLACE_BELOW_FILLED_QTY));
	}

	@Test
	public void testOrderAmendedToFilledOrder() {
		
		Order buyOrder = createOrder(1000, Side.BUY, 100, 80);
		Order sellOrder = createOrder(1001, Side.SELL, 100, 100);
		assertThat(orderBook.processNewOrder(buyOrder), equalTo(OrderReason.VALID));
		assertThat(orderBook.processNewOrder(sellOrder), equalTo(OrderReason.VALID));
		assertThat("Match expected", collector.getTradeSize(), equalTo(2));
		collector.init();
		Order sellAmendOrder = createOrder(1001, Side.SELL, 100, 80);
		assertThat(orderBook.processAmendOrder(sellAmendOrder), equalTo(OrderReason.VALID));
		assertThat("No match expected", collector.getTradeSize(), equalTo(0));
	}

	@Test
	public void testOrderCancelledOnOpenOrder() {
		Order buyOrder = createOrder(1000, Side.BUY, 100, 80);
		Order sellOrder = createOrder(1001, Side.SELL, 100, 100);
		assertThat(orderBook.processNewOrder(buyOrder), equalTo(OrderReason.VALID));
		assertThat(orderBook.processNewOrder(sellOrder), equalTo(OrderReason.VALID));
		assertThat("Match expected", collector.getTradeSize(), equalTo(2));
		assertThat(orderBook.processCancelOrder(sellOrder.getOrderId()), equalTo(OrderReason.VALID));
	}
	
	@Test
	public void testOrderCancelledOnFilledOrder() {
		Order buyOrder = createOrder(1000, Side.BUY, 100, 100);
		Order sellOrder = createOrder(1001, Side.SELL, 100, 80);
		assertThat(orderBook.processNewOrder(buyOrder), equalTo(OrderReason.VALID));
		assertThat(orderBook.processNewOrder(sellOrder), equalTo(OrderReason.VALID));
		assertThat("Match expected", collector.getTradeSize(), equalTo(2));
		assertThat(orderBook.processCancelOrder(sellOrder.getOrderId()), equalTo(OrderReason.TOO_LATE_TO_CANCEL));
	}

	@Test
	public void testLoadTestingWithPassiveOrders() {
	
		int numOfOrders = 1000;		
		Level currentLevelBefore  = changeLogLevelForLoadTesting(Level.ERROR);
		for (int i = 0; i < numOfOrders; i++) {			
			Order buyOrder = createOrder(numOfOrders + i, Side.BUY, 80, 100);
			Order sellOrder = createOrder(2 * numOfOrders + i, Side.SELL, 95, 100);
			assertThat(orderBook.processNewOrder(buyOrder), equalTo(OrderReason.VALID));
			assertThat(orderBook.processNewOrder(sellOrder), equalTo(OrderReason.VALID));
			assertThat("No Major GC is expected",GcLogUtil.getMajorGc(),equalTo(0));
			assertThat("No Minor GC is expected",GcLogUtil.getMinorGc(),equalTo(0));			
			changeLogLevelForLoadTesting(currentLevelBefore);
		}	

	}
	
	
	@Test
	public void testLoadTestingWithAggresiveeOrders() {
	
		int numOfOrders = 1000;		
		Level currentLevelBefore  = changeLogLevelForLoadTesting(Level.ERROR);
		for (int i = 0; i < numOfOrders; i++) {			
			Order buyOrder = createOrder(numOfOrders + i, Side.BUY, 80, 100);
			Order sellOrder = createOrder(2 * numOfOrders + i, Side.SELL, 75, 50);
			assertThat(orderBook.processNewOrder(buyOrder), equalTo(OrderReason.VALID));
			assertThat(orderBook.processNewOrder(sellOrder), equalTo(OrderReason.VALID));
			assertThat("No Major GC is expected",GcLogUtil.getMajorGc(),equalTo(0));
			assertThat("No Minor GC is expected",GcLogUtil.getMinorGc(),equalTo(0));			
			changeLogLevelForLoadTesting(currentLevelBefore);
		}	

	}
//	
//	
	@Test
	public void testMatchingEnableAndDisable() {		
		orderBook.disableMatching();
		
		Order buyOrder1 = createOrder(1000, Side.BUY, 100, 80);
		Order sellOrder1 = createOrder(1001, Side.SELL, 100, 100);
		Order buyOrder2 = createOrder(1002, Side.BUY, 99, 120);
		Order sellOrder2 = createOrder(1003, Side.SELL, 100, 100);
		assertThat(orderBook.processNewOrder(buyOrder1), equalTo(OrderReason.VALID));
		assertThat(orderBook.processNewOrder(buyOrder2), equalTo(OrderReason.VALID));	
		assertThat(orderBook.processNewOrder(sellOrder1), equalTo(OrderReason.VALID));
		assertThat(orderBook.processNewOrder(sellOrder2), equalTo(OrderReason.VALID));

		assertThat("No Match expected even for aggresive matching order as matching is disabled", collector.getTradeSize(), equalTo(0));
		assertThat(orderBook.processCancelOrder(buyOrder2.getOrderId()), equalTo(OrderReason.VALID));
		buyOrder1 = createOrder(buyOrder1.getOrderId(), Side.BUY, 100, 200); //Amend the order
		assertThat(orderBook.processAmendOrder(buyOrder1), equalTo(OrderReason.VALID));
		orderBook.enableMatching();
		assertThat("Match expected after enabling matching", collector.getTradeSize(), equalTo(4));
		assertThat("Trade doesnt match", collector.getTradeDetails(0), equalTo("trade 1000,100@100.0"));
		assertThat("Trade doesnt match", collector.getTradeDetails(1), equalTo("trade 1001,100@100.0"));
		assertThat("Trade doesnt match", collector.getTradeDetails(2), equalTo("trade 1000,100@100.0"));
		assertThat("Trade doesnt match", collector.getTradeDetails(3), equalTo("trade 1003,100@100.0"));



	}
	
	private Level changeLogLevelForLoadTesting(Level level) {
		Level currentLevel = Logger.getLogger(OrderStore.class).getLevel();
		Logger.getLogger(OrderStore.class).setLevel(level);
		return currentLevel;
	}

}