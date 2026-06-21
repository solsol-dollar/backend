package com.shinhan.eclipse.common.exchange;

import java.math.BigDecimal;
import java.time.Instant;

public record ExchangeRateInfo(
        String     currencyCode,
        String     currencyName,
        BigDecimal baseRate,
        BigDecimal buyingRate,
        BigDecimal sellingRate,
        Instant    fetchedAt
) {}
