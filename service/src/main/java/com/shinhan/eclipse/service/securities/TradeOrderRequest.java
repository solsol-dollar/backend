package com.shinhan.eclipse.service.securities;

import java.math.BigDecimal;

public record TradeOrderRequest(
        Long       productId,
        Long       accountId,
        String     orderSide,
        Integer    quantity,
        BigDecimal requestedPrice
) {}
