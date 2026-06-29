package com.shinhan.eclipse.service.exchange.market;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MarketRateData(
        BigDecimal price,
        BigDecimal tts,
        BigDecimal ttb,
        BigDecimal spread,
        BigDecimal change,
        BigDecimal changeRate,
        String sign,
        BigDecimal high,
        BigDecimal low,
        BigDecimal open,
        String source,
        LocalDateTime updatedAt
) {}
