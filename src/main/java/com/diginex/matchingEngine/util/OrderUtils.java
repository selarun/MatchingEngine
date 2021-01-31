package com.diginex.matchingEngine.util;

import com.diginex.matchingEngine.orderbook.Order;
import com.diginex.matchingEngine.orderbook.Side;

public class OrderUtils {

	public static boolean isBuy(Side side) {
		return Side.BUY == side;
	}
	
	public static boolean isSell(Side side) {
		return Side.SELL == side;
	}
	
	public static boolean isAmendDownOnly(Order currentSnapShotOrder,Order amendOrder) {
		if(currentSnapShotOrder.getPrice()!=amendOrder.getPrice()) 
			return false;		
		return currentSnapShotOrder.getQuantity() > amendOrder.getQuantity();
	
	}
	
}