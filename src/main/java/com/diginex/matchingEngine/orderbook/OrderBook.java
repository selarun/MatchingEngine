package com.diginex.matchingEngine.orderbook;

import java.text.NumberFormat;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.function.BiPredicate;

import org.apache.log4j.Logger;

import com.diginex.matchingEngine.store.OrderStore;
import com.diginex.matchingEngine.util.OrderUtils;
import com.diginex.matchingEngine.util.PoolManager;
import com.diginex.matchingEngine.util.PriceUtils;
import com.diginex.matchingEngine.util.ProductUtil;
import com.diginex.matchingEngine.validator.OrderValidator;

public class OrderBook {

	private static Logger log = Logger.getLogger(OrderBook.class);

	private final TradeCollector tradeCollector;

	private final OrderValidator orderValidator;

	private final OrderStore orderStore;

	private boolean isMatchingOn = true;

	public OrderBook(TradeCollector tradeCollector) {
		this.tradeCollector = tradeCollector;
		this.orderStore = new OrderStore();
		this.orderValidator = new OrderValidator();
		initOrderBook();
	}

	private static final Comparator<Order> BUY_COMPARTOR = new Comparator<Order>() {
		@Override
		public int compare(Order o1, Order o2) {
			if (o1.getPrice() == o2.getPrice()) {
				return Long.compare(o2.getEventTime(), o1.getEventTime());
			}

			if (o1.getPrice() == Long.MIN_VALUE) {
				return -1;
			}

			if (o2.getPrice() == Long.MIN_VALUE) {
				return 1;
			}

			return Long.compare(o2.getPrice(), o1.getPrice());
		}
	};

	private static final Comparator<Order> SELL_COMPARTOR = new Comparator<Order>() {
		public int compare(Order o1, Order o2) {
			if (o1.getPrice() == o2.getPrice()) {
				return Long.compare(o2.getEventTime(), o1.getEventTime());
			}

			if (o1.getPrice() == Long.MIN_VALUE) {
				return -1;
			}

			if (o2.getPrice() == Long.MIN_VALUE) {
				return 1;
			}

			return Long.compare(o1.getPrice(), o2.getPrice());
		}
	};
	
	
	private static final BiPredicate<Order, Order> BUY_PREDICATE = (x, y) -> PriceUtils.isMarketOrder(x) ||PriceUtils.isMarketOrder(y) ||
			x.getPrice() <= y.getPrice();

	private static final BiPredicate<Order, Order> SELL_PREDICATE = (x, y) -> PriceUtils.isMarketOrder(x) ||PriceUtils.isMarketOrder(y) || x.getPrice() >= y.getPrice();

	private Queue<Order> offers = new PriorityQueue<>(PoolManager.DEFAULT_CAPACITY, SELL_COMPARTOR);

	private Queue<Order> bids = new PriorityQueue<>(PoolManager.DEFAULT_CAPACITY,BUY_COMPARTOR);

	private void initOrderBook() {
		offers.clear();
		bids.clear();
	}

	public OrderReason processNewOrder(Order order) {
		Order existingOrder = orderStore.getOrderFromStore(order.getOrderId());
		OrderReason orderReason = orderValidator.validNew(order, existingOrder);
		if (orderReason != OrderReason.VALID) {
			return orderReason;
		}
		orderStore.addOrUpdateOrder(order);
		addOrderToMatcher(order);
		return orderReason;

	}

	public OrderReason processCancelOrder(int orderId) {

		Order currentSnapShotOrder = orderStore.getOrderFromStore(orderId);
		OrderReason orderReason = orderValidator.validCancel(orderId, currentSnapShotOrder);
		if (orderReason != OrderReason.VALID) {
			return orderReason;
		}

		Order removeNextOrderFromQueueToTriggerMatching;
		if (OrderUtils.isBuy(currentSnapShotOrder.getOrderSide())) {
			bids.remove(currentSnapShotOrder);
			removeNextOrderFromQueueToTriggerMatching = bids.poll();
		} else {
			offers.remove(currentSnapShotOrder);
			removeNextOrderFromQueueToTriggerMatching = offers.poll();
		}
		// The reason for running matching using topofbook is manily possible to give a
		// chance to
		// below order to try incase if the matching predicate additional criteria to
		// price
		if(removeNextOrderFromQueueToTriggerMatching!=null) {
		  addOrderToMatcher(removeNextOrderFromQueueToTriggerMatching);
		}
		
		return orderReason;

	}

	public OrderReason processAmendOrder(Order amendOrder) {
		Order currentSnapShotOrder = orderStore.getOrderFromStore(amendOrder.getOrderId());
		OrderReason orderReason = orderValidator.validAmend(amendOrder, currentSnapShotOrder);
		if (orderReason != OrderReason.VALID) {
			return orderReason;
		}

		if (currentSnapShotOrder.getFilledQty() == amendOrder.getQuantity()) {
			// Amend to filled qty,so remove from the queue;
			orderStore.addOrUpdateOrder(amendOrder);
			removeOrderFromQueue(currentSnapShotOrder);
			return orderReason;

		}
		if (OrderUtils.isAmendDownOnly(currentSnapShotOrder, amendOrder)) {
			currentSnapShotOrder.updateQty(amendOrder.getQuantity()); // Same order is stored in the queue , priority is
																		// not lost
			return orderReason;
		} else {
			removeOrderFromQueue(currentSnapShotOrder);
		}
		orderStore.addOrUpdateOrder(amendOrder);
		addOrderToMatcher(amendOrder);
		return orderReason;

	}

	private void removeOrderFromQueue(Order order) {
		if (OrderUtils.isBuy(order.getOrderSide())) {
			bids.remove(order);
		} else {
			offers.remove(order);
		}
	}

	private void addOrderToMatcher(Order order) {
		Queue<Order> queueToMatchAgainst;
		Queue<Order> queueToAddTo;
		BiPredicate<Order, Order> priceComparator;
		if (order.getOrderSide() == Side.BUY) {
			priceComparator = BUY_PREDICATE;
			queueToAddTo = bids;
			queueToMatchAgainst = offers;
		} else {
			priceComparator = SELL_PREDICATE;
			queueToAddTo = offers;
			queueToMatchAgainst = bids;
		}
		match(queueToMatchAgainst, queueToAddTo, priceComparator, order);
	}

	public void disableMatching() {
		this.isMatchingOn = false;
		log.error("Matching is disabled");
	}

	public void enableMatching() {
		this.isMatchingOn = true;
		log.error("Matching is enabled");
		Order topBidOrder = getNextTopOrder(Side.BUY);
		match(offers, bids, BUY_PREDICATE, topBidOrder);
	}

	private void match(Queue<Order> queueToMatchAgainst, Queue<Order> queueToAddTo,
			BiPredicate<Order, Order> priceComparator, Order order) {

		if (queueToMatchAgainst.isEmpty() || !isMatchingOn) {
			addOrderToQueue(queueToAddTo, order);
		} else {

			do {
				int matchedQty = 0;
				Order topOfOBOrder = queueToMatchAgainst.peek();
				if (topOfOBOrder == null)
					break;
				if (priceComparator.test(topOfOBOrder, order)) {
					matchedQty = Math.min(topOfOBOrder.getRemainingQty(), order.getRemainingQty());
					generateTrade(order, topOfOBOrder, matchedQty);
					order.executedQty(matchedQty);
					topOfOBOrder.executedQty(matchedQty);
					if (topOfOBOrder.getRemainingQty() == 0) {
						queueToMatchAgainst.poll();

					}
				} else {
					break;
				}
				if (order.getRemainingQty() == 0) {
					order = getNextTopOrder(order.getOrderSide()); // In case we call matcher without any input
																	// order/market data event.Possibly case could be if
																	// matcher was suspend/Resume later
				}
			} while (order != null && order.getRemainingQty() > 0);
			if (order != null && order.getRemainingQty() > 0) {
				addOrderToQueue(queueToAddTo, order);
			}
		}
	}

	private Order getNextTopOrder(Side side) {
		if (OrderUtils.isBuy(side) && !bids.isEmpty()) {
			return bids.poll();
		}
		if (OrderUtils.isSell(side) && !offers.isEmpty()) {
			return offers.poll();
		}
		return null;
	}

	private void addOrderToQueue(Queue<Order> addQueue, Order order) {
		addQueue.add(order);
	}

	private void generateTrade(Order orderMatched, Order matchingOrder, int qty) {
		long tradePrice =  orderMatched.getPrice();
		if(PriceUtils.isMarketOrder(orderMatched)) {
			if(PriceUtils.isMarketOrder(matchingOrder)) {
				tradePrice = ProductUtil.getClosePx(matchingOrder.getProductId());
			}
			else {
				tradePrice = matchingOrder.getPrice();
			}
		}
		generateTradeDetails(orderMatched, qty, tradePrice);
		generateTradeDetails(matchingOrder, qty, tradePrice);
	}

	private void generateTradeDetails(Order matchedOrder, int qty, long price) {				
		log.debug(tradeCollector.addTrade(matchedOrder.getOrderId(),qty,price));
	}

	public void printOpenOrderBook() {
		int size = Math.max(bids.size(), offers.size());
		int bidSize = bids.size();
		int offerSize = offers.size();

		NumberFormat nf = NumberFormat.getInstance();
		Order order = null;
		for (int i = 0; i < size; i++) {
			String bidString = String.format("%11s %6s", "", "");
			String offerString = "";
			if (i < bidSize) {
				order = bids.poll();
				bidString = String.format("%11s %6.2f", nf.format(order.getQuantity()),
						PriceUtils.convertPriceToDouble(order.getPrice()));
			}
			if (i < offerSize) {
				order = offers.poll();
				offerString = String.format(" %6.2f %11s", PriceUtils.convertPriceToDouble(order.getPrice()),
						nf.format(order.getRemainingQty()));
			}
			log.debug(bidString + " |" + offerString);
			System.out.println(bidString + " |" + offerString);
		}
	}

	/**
	 * Added for testing purpose
	 */
	public void clearOrderBook() {
		bids.clear();
		offers.clear();
		// orderStore.clearOrderStore();
	}

}