package com.diginex.matchingEngine.orderbook;

/**
 * Order is unique by orderId
 * 
 * @author user
 *
 */
public class Order {

	private int orderId;
	private Side orderSide;
	private long price;
	private int quantity;
	private long productId;
	private int filledQuantity = 0;
	private long eventTime;	 
	

	public void init(int orderId, Side orderSide, long price, int qty, long productId, long eventTime)
	 {
		this.orderId = orderId;
		this.orderSide = orderSide;
		this.price = price;
		this.quantity = qty;
		this.eventTime = eventTime;
		this.productId = productId;
	}

	public void init(int orderId, Side orderSide, long price, int qty, long productId) {
		init(orderId, orderSide, price, qty,productId, System.currentTimeMillis());
	}
	
	public void init(int orderId, Side orderSide, int qty, long productId) {				
		init(orderId, orderSide, Long.MIN_VALUE, qty,productId, System.currentTimeMillis());
	}

	public int  getOrderId() {
		return orderId;
	}

	public Side getOrderSide() {
		return orderSide;
	}

	public long getPrice() {
		return price;
	}

	public int getQuantity() {
		return this.quantity;
	}

	public int getRemainingQty() {
		return this.quantity - this.filledQuantity;
	}

	public long getProductId() {
		return this.productId;
	}

	public int getFilledQty() {
		return this.filledQuantity;
	}

	public void updateQty(int volume) {
		this.quantity = volume;
	}

	public void updatePrice(long price) {
		this.price = price;
	}
	

	public void executedQty(int filledQty) {
		this.filledQuantity += filledQty;
	}

	public long getEventTime() {
		return this.eventTime;
	}

	@Override
	public String toString() {
		return "Order [orderId=" + orderId + ", orderSide=" + orderSide + ", price=" + price + ", quantity=" + quantity
				+ ", filledQuantity=" + filledQuantity + ", eventTime=" + eventTime +  "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (eventTime ^ (eventTime >>> 32));
		result = prime * result + filledQuantity;
		result = prime * result + orderId;
		result = prime * result + ((orderSide == null) ? 0 : orderSide.hashCode());
		result = prime * result + (int) (price ^ (price >>> 32));
		result = prime * result + quantity;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Order other = (Order) obj;
		if (eventTime != other.eventTime)
			return false;
		if (filledQuantity != other.filledQuantity)
			return false;		
		if (orderId != other.orderId)
			return false;
		if (orderSide != other.orderSide)
			return false;
		if (price != other.price)
			return false;
		if (quantity != other.quantity)
			return false;
		return true;
	}

	public void clear() {
		this.orderId = -1;
		this.orderSide = Side.INVALID;
		this.price = -1l;
		this.quantity = -1;
		this.eventTime = -1l;
		this.productId = -1;
	}
	
	
}