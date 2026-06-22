package com.shinhan.eclipse.service.securities;

import java.math.BigDecimal;

public record MarketIndex(
        String name,
        BigDecimal value,
        BigDecimal changeAmount,
        BigDecimal changeRate,
        boolean isUp,
        boolean isMarketOpen
) {}
