package com.shinhan.eclipse.service.securities;

public interface TradeOrderService {
    TradeOrderResponse placeOrder(Long userId, TradeOrderRequest request);
}
