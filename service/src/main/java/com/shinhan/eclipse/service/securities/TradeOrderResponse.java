package com.shinhan.eclipse.service.securities;

import com.shinhan.eclipse.domain.trade.TradeOrder;

import java.math.BigDecimal;

public record TradeOrderResponse(
        Long       orderId,
        String     orderStatus,
        String     ticker,
        String     orderSide,
        Integer    quantity,
        BigDecimal executedPrice,
        BigDecimal executedAmount,
        String     currency
) {
    public static TradeOrderResponse of(TradeOrder order, String ticker) {
        return new TradeOrderResponse(
                order.getId(),
                order.getOrderStatus(),
                ticker,
                order.getOrderSide(),
                order.getQuantity(),
                order.getExecutedPrice(),
                order.getExecutedAmount(),
                order.getCurrency()
        );
    }
}
