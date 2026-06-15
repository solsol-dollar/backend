package com.shinhan.eclipse.ledger.trade;

import com.shinhan.eclipse.domain.trade.TradeOrder;

import java.math.BigDecimal;
import java.util.List;

public interface TradeOrderFacade {
    TradeOrder placeOrder(Long userId, Long productId, Long accountId,
                          String orderSide, Integer quantity, BigDecimal requestedPrice);
    List<TradeOrder> getOrders(Long userId);
}
