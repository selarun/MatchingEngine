package com.diginex.matchingEngine.orderbook.engine;


import java.util.Scanner;

import com.diginex.matchingEngine.orderbook.Order;
import com.diginex.matchingEngine.orderbook.OrderBook;
import com.diginex.matchingEngine.orderbook.TradeCollector;
import com.diginex.matchingEngine.util.OrderInputReader;


public class Matcher {
	public static void main(String[] args) {
		TradeCollector tradeCollector = new TradeCollector();
		OrderBook orderBook = new OrderBook(tradeCollector);

		Scanner scan = new Scanner(System.in);
		String orderDetailsLine = null;

		try {
			while (scan.hasNextLine()) {
				orderDetailsLine = scan.nextLine().toLowerCase();
				if(orderDetailsLine.equalsIgnoreCase("Exit")) {
					break; //Idea to break with Exit as input or Ctrl K or EOF
				}
				String orderDetails[] = orderDetailsLine.split(",");
				Order newOrder = null;			 
				if (orderDetails.length == 4) {
					newOrder = OrderInputReader.generateOrder(orderDetails);
				} else {
					System.out.println("Invalid OrderData(Skipped):" + orderDetailsLine);
					continue;
				}

				orderBook.processNewOrder(newOrder);
				tradeCollector.printTrades();
			}
			System.out.println();
			orderBook.printOpenOrderBook();
		} finally {
			scan.close();
		}

	}

}
