package com.shinhan.eclipse.service.securities;

import java.math.BigDecimal;

public record RankingItem(
        Long id,
        String ticker,
        String productName,
        BigDecimal close,
        BigDecimal changeRate,
        String sign
) {}
