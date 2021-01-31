package com.diginex.matchingEngine.store;

import org.apache.log4j.Logger;

import com.diginex.matchingEngine.orderbook.Order;
import com.diginex.matchingEngine.util.PoolManager;

/**
 * Order Store which stores the input orders and also market snapshot data key
 * by orderId 
 * 
 * @author user
 *
 */
public class OrderStore {
	private static Logger log = Logger.getLogger(OrderStore.class);

	private final Order [] orderStore = new Order[PoolManager.DEFAULT_CAPACITY];

	/**
	 * Return order from the store for the given orderId
	 * @param orderId
	 * @return
	 */
	public Order getOrderFromStore(int orderId) {
		if(orderId > orderStore.length) {
			log.error("Unknown OrderId to the store:" + orderId );
			return null;
		}
		return orderStore[orderId -1];
	}

	public void addOrUpdateOrder(Order order) {
		if (order == null)
			return;
		
		if(order.getOrderId() >  orderStore.length) {
			log.error("Invalid OrderId not added to the store:" + order );
			return;
		}
		
		log.debug("Updated Order to the store:" + order);
		orderStore[order.getOrderId()-1] = order;
	}
	
	
	

}