package com.diginex.matchingEngine.util;

import com.diginex.matchingEngine.orderbook.Order;

public class PriceUtils {

	public static final double DEFAULT_DENO = 1e6;
	public static final long DEFAULT_TICK_SIZE = (long) 0.1e6;

	public static long convertPriceToLong(double price) {
		return (long) (price * DEFAULT_DENO);
	}

	public static double convertPriceToDouble(long price) {
		return price / DEFAULT_DENO;
	}

	public static boolean isPriceInValidTickSize(long price) {
		return price==Long.MIN_VALUE ||  price % DEFAULT_TICK_SIZE == 0;
	}
	
	public static boolean isMarketOrder(Order o) {
		return o.getPrice()==Long.MIN_VALUE ;
	}

}