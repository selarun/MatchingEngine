package com.diginex.matchingEngine.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import com.diginex.matchingEngine.orderbook.Order;
import com.diginex.matchingEngine.orderbook.Side;

public class OrderInputReader {

	public static List<Order> generateOrders(String fileName) {
		List<Order> orders = new ArrayList<>();

		Path filePath;
		try {
			filePath = Paths.get(fileName);

			List<String> lines;

			lines = Files.readAllLines(filePath);
			Order order;
			for (String line : lines) {
				String orderDetails[] = line.split(",");
				if (orderDetails.length == 4) {
					order = OrderInputReader.generateOrder(orderDetails);
				} else {
					System.out.println("Invalid OrderData(Skipped):" + orderDetails);
					continue;
				}
				orders.add(order);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} 

		return orders;

	}

	public static Order generateOrder(String[] orderDetails) {
		int orderId = Integer.parseInt(orderDetails[0]);
		Side side = Side.valueOf(orderDetails[1].toUpperCase());
		long price = PriceUtils.convertPriceToLong(Double.parseDouble(orderDetails[2]));
		int qty = Integer.parseInt(orderDetails[3]);
		Order order = PoolManager.getInstance().takeOrder();
		order.init(orderId, side, price, qty, ProductUtil.TESTPRODUCTID);
		return order;

	}

}