package com.diginex.matchingEngine.util;

public class ProductUtil {

	public static long TESTPRODUCTID = 10001111;
	public static long DEFAULT_LOT_SIZE = 10;

	public static boolean isValidLotSize(long productId, int orderQty) {
		return orderQty % DEFAULT_LOT_SIZE == 0;
	}

	/**
	 * For now it returns constant
	 *  Can be sourced from Md data 
	 *  Product close px if it is first trade 
	 * @param productId
	 * @return
	 */
	public static long getClosePx(long productId) {
		return 1000;
	}
}