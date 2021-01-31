package com.diginex.matchingEngine.util;

import org.apache.log4j.Logger;

import com.diginex.matchingEngine.orderbook.Order;
import com.diginex.matchingEngine.orderbook.Trade;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.stream.IntStream;

public class PoolManager {

	private static Logger log = Logger.getLogger(PoolManager.class);
	public static int DEFAULT_CAPACITY = 10000;
	public static int DEFAULT_TRADE_CAPACITY = 10000;

	private final Queue<Order> orderPools;
	private final Queue<Trade> tradePools;


	private static final PoolManager SINGLE_INSTANCE_ORDER_POOL_INSTANCE = new PoolManager();

	public static PoolManager getInstance() {
		return SINGLE_INSTANCE_ORDER_POOL_INSTANCE;
	}

	public PoolManager() {
		System.out.print("Created");
		orderPools = new ArrayDeque<>(DEFAULT_CAPACITY);
		tradePools = new ArrayDeque<>(DEFAULT_TRADE_CAPACITY);
		IntStream.range(0, DEFAULT_CAPACITY).forEach((i) -> {
			orderPools.add(allocateNewOrder());
		});
		log.info("Initialised the order pool with Capacity =  " + orderPools.size());
		
		IntStream.range(0, DEFAULT_TRADE_CAPACITY).forEach((i) -> {
			tradePools.add(allocateTrade());
		});
		
		log.info("Initialised the trades pool with Capacity =  " + orderPools.size());

	}

	private Order allocateNewOrder() {
		Order newOrder = new Order();		
		return newOrder;
	}

	public Order takeOrder() {
		if (orderPools.size() == 0) {
		    log.error("Creating New Order as pool is out of capacity");
			return allocateNewOrder();
		}
		return orderPools.poll();
	}
	
	private Trade allocateTrade() {
		Trade trade = new Trade();		
		return trade;
	}

	public Trade takeTrade() {
		if (tradePools.size() == 0) {
		    log.error("Creating New Trade as pool is out of capacity");
			return allocateTrade();
		}
		return tradePools.poll();
	}

	public void release(Order order) {
		if (order != null) {
			order.clear();
			orderPools.add(order);
		}
	}
	
	public void release(Trade trade) {
		if (trade != null) {
			trade.clear();
			tradePools.add(trade);
		}
	}

}