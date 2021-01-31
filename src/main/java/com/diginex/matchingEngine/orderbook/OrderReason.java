package com.diginex.matchingEngine.orderbook;

public enum OrderReason {
    VALID,
    ORDER_ALREADY_EXISTS,
    INVALID_ORDER_QUANTITY,
    UNKNOWN_ORDER,
    INVALID_REPLACE_BELOW_FILLED_QTY,
    TOO_LATE_TO_REPLACE,
    TOO_LATE_TO_CANCEL,
    INVALID_PRICE,
    INVALID_SIDE
}