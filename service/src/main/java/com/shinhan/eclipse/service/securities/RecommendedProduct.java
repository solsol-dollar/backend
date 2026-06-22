package com.shinhan.eclipse.service.securities;

import java.math.BigDecimal;

public record RecommendedProduct(
        String     ticker,
        String     productName,
        String     sector,
        String     exchangeName,
        BigDecimal currentPrice,
        String     reason
) {}
