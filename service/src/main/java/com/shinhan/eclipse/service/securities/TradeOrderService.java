package com.shinhan.eclipse.service.securities;

import java.util.List;

public interface TradeOrderService {
    TradeOrderResponse placeOrder(Long userId, TradeOrderRequest request);
    List<OrderHistoryItem> getOrders(Long userId);
    SellProfitsSummary getProfits(Long userId);
}
