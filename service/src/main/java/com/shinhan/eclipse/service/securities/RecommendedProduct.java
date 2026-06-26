package com.shinhan.eclipse.service.securities;

import java.math.BigDecimal;

public record RecommendedProduct(
        Long       id,
        String     ticker,
        String     productName,
        String     productType,
        String     sector,
        String     exchangeName,
        BigDecimal currentPrice,
        BigDecimal changeRate,
        String     sign,
        String     reason
) {}
