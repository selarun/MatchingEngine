package com.diginex.matchingEngine.orderbook;

import com.diginex.matchingEngine.util.PriceUtils;

/**
 * Trade is unique by orderId
 * 
 * @author user
 *
 */
public class Trade {

	private final StringBuilder builder = new StringBuilder();

	private int orderId;
	private long price;
	private int quantity;	
	private long eventTime;	
	private int tradeId;

	

	public void init(int orderId, int qty,long price,int tradeId)
	 {
		this.orderId = orderId;
		this.quantity=qty;
		this.price = price;
		this.eventTime = System.currentTimeMillis();
		this.tradeId=tradeId;
	 }



	public long getPrice() {
		return price;
	}


	public int getQuantity() {
		return quantity;
	}


	public long getEventTime() {
		return eventTime;
	}	
	

	public String toString() {
		builder.setLength(0);
		builder.append("trade").append(" ");
		builder.append(orderId).append(",");
		builder.append(quantity).append("@").append(PriceUtils.convertPriceToDouble(price));
		return builder.toString();
	}



	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((builder == null) ? 0 : builder.hashCode());
		result = prime * result + (int) (eventTime ^ (eventTime >>> 32));
		result = prime * result + orderId;
		result = prime * result + (int) (price ^ (price >>> 32));
		result = prime * result + quantity;
		result = prime * result + tradeId;
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
		Trade other = (Trade) obj;
		if (builder == null) {
			if (other.builder != null)
				return false;
		} else if (!builder.equals(other.builder))
			return false;
		if (eventTime != other.eventTime)
			return false;
		if (orderId != other.orderId)
			return false;
		if (price != other.price)
			return false;
		if (quantity != other.quantity)
			return false;
		if (tradeId != other.tradeId)
			return false;
		return true;
	}



	public void clear() {
		// TODO Auto-generated method stub
		
	}
	
}