package com.diginex.matchingEngine.orderbook;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.diginex.matchingEngine.util.PoolManager;

public class TradeCollector {

	private AtomicInteger tradeId = new AtomicInteger(0);

	private final PoolManager poolManager = PoolManager.getInstance();

	private final List<Trade> trades = new ArrayList<>(PoolManager.DEFAULT_TRADE_CAPACITY);

	public void init() {
		trades.clear();
	}

	public String addTrade(int orderId, int qty, long price) {
		Trade trade = poolManager.takeTrade();
		trade.init(orderId, qty, price, nextTradeId());
		trades.add(trade);
		return trade.toString();
	}

	public int nextTradeId() {
		return tradeId.incrementAndGet();
	}

	public void printTrades() {
		trades.forEach(System.out::println);
	}

	public int getTradeSize() {
		return trades.size();
	}

	public String getTradeDetails(int index) {
		if (index < trades.size()) {
			return trades.get(index).toString();
		}
		return "";
	}
}