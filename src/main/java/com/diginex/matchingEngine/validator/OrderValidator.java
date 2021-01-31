package com.diginex.matchingEngine.validator;

import org.apache.log4j.Logger;

import com.diginex.matchingEngine.orderbook.Order;
import com.diginex.matchingEngine.orderbook.OrderReason;
import com.diginex.matchingEngine.orderbook.Side;
import com.diginex.matchingEngine.util.PriceUtils;
import com.diginex.matchingEngine.util.ProductUtil;

/**
 * 
 * @author user
 *
 */
public class OrderValidator {
	private static Logger log = Logger.getLogger(OrderValidator.class);

	public OrderReason validNew(Order order, Order exisitingOrderWithSameId) {
		if (exisitingOrderWithSameId != null) {
			log.error("Order already exists with same orderId:" + order);
			return OrderReason.ORDER_ALREADY_EXISTS;
		}

		if (order.getQuantity() <= 0) {
			log.error("Invalid Order orderQty:" + order);
			return OrderReason.INVALID_ORDER_QUANTITY;
		}

		if (!isValidSide(order.getOrderSide())) {
			log.error("Invalid Order Side:" + order);
			return OrderReason.INVALID_SIDE;
		}

		if (!ProductUtil.isValidLotSize(order.getProductId(), order.getQuantity())) {
			log.error("Invalid Order orderQty(Not in lot size):" + order);
			return OrderReason.INVALID_ORDER_QUANTITY;
		}

		if (!PriceUtils.isPriceInValidTickSize(order.getPrice())) {
			log.error("Invalid Order Price(Not in tick):" + order);
			return OrderReason.INVALID_PRICE;
		}

		return OrderReason.VALID;
	}

	public OrderReason validAmend(Order order, Order currentSnapShotOfOrder) {
		if (currentSnapShotOfOrder == null) {
			log.error("Unknown order to amend:" + order);
			return OrderReason.UNKNOWN_ORDER;
		}

		if (order.getQuantity() <= 0) {
			log.error("Invalid Order orderQty:" + order);
			return OrderReason.INVALID_ORDER_QUANTITY;
		}

		if (!isValidSide(order.getOrderSide())) {
			log.error("Invalid Order Side:" + order);
			return OrderReason.INVALID_SIDE;
		}

		if (currentSnapShotOfOrder.getRemainingQty() == 0) {
			log.error("Order state is already completed:" + currentSnapShotOfOrder);
			return OrderReason.TOO_LATE_TO_REPLACE;
		}

		if (order.getQuantity() < currentSnapShotOfOrder.getFilledQty()) {
			log.error("Order Qty less than executedQty:" + order);
			return OrderReason.INVALID_REPLACE_BELOW_FILLED_QTY;
		}

		if (!ProductUtil.isValidLotSize(order.getProductId(), order.getQuantity())) {
			log.error("Invalid Order orderQty(Not in lot size):" + order);
			return OrderReason.INVALID_ORDER_QUANTITY;
		}

		if (!PriceUtils.isPriceInValidTickSize(order.getPrice())) {
			log.error("Invalid Order Price(Not in tick):" + order);
			return OrderReason.INVALID_PRICE;
		}
		return OrderReason.VALID;
	}

	public OrderReason validCancel(int orderId, Order currentSnapShotOfOrder) {
		if (currentSnapShotOfOrder == null) {
			log.error("Unknown order id to cancel:" + orderId);
			return OrderReason.UNKNOWN_ORDER;
		}

		if (currentSnapShotOfOrder.getRemainingQty() == 0) {
			log.error("Order state is already completed:" + currentSnapShotOfOrder);
			return OrderReason.TOO_LATE_TO_CANCEL;
		}

		return OrderReason.VALID;
	}

	private boolean isValidSide(Side side) {
		if (Side.BUY == side || Side.SELL == side)
			return true;
		return false;

	}

}