package com.shinhan.eclipse.service.securities;

import java.math.BigDecimal;
import java.util.Map;

public record ProductStats(
        String ticker,
        BigDecimal week52High,
        BigDecimal week52Low,
        Map<String, BigDecimal> returns  // "1M", "3M", "6M", "1Y" -> 수익률(%)
) {}
