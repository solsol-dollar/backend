package com.shinhan.eclipse.service.securities;

import java.math.BigDecimal;

public record HoldingItem(
        Long       holdingId,
        Long       productId,
        String     ticker,
        String     productName,
        String     productType,
        String     exchangeName,
        Integer    totalQuantity,
        BigDecimal averagePrice,
        String     currency,
        BigDecimal currentPrice,
        BigDecimal evaluatedAmount,
        BigDecimal profitLoss,
        BigDecimal profitLossRate
) {}
