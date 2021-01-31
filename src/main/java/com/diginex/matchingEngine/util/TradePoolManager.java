package com.diginex.matchingEngine.util;

import org.apache.log4j.Logger;

import com.diginex.matchingEngine.orderbook.Order;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.stream.IntStream;

public class TradePoolManager {

	private static Logger log = Logger.getLogger(TradePoolManager.class);
	public static int DEFAULT_CAPACITY = 10000;
	private final Queue<Order> orderPools;

	private static final TradePoolManager SINGLE_INSTANCE_ORDER_POOL_INSTANCE = new TradePoolManager();

	public static TradePoolManager getInstance() {
		return SINGLE_INSTANCE_ORDER_POOL_INSTANCE;
	}

	public TradePoolManager() {
		System.out.print("Created");
		orderPools = new ArrayDeque<>(DEFAULT_CAPACITY);
		IntStream.range(0, DEFAULT_CAPACITY).forEach((i) -> {
			orderPools.add(allocateNew(true));
		});
		log.info("Initialised the order pool with Capacity =  " + orderPools.size());
	}

	private Order allocateNew(boolean startUp) {
		Order newOrder = new Order();
		if (!startUp)
			log.error("Creating New Order as pool is out of capacity");
		return newOrder;
	}

	public Order take() {
		if (orderPools.size() == 0) {
			return allocateNew(false);
		}
		return orderPools.poll();
	}

	public void release(Order order) {
		if (order != null) {
			order.clear();
			orderPools.add(order);
		}
	}

}