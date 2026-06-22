package com.shinhan.eclipse.service.securities;

import java.math.BigDecimal;
import java.util.List;

public record HoldingsSummary(
        BigDecimal totalCurrentValueUsd,
        BigDecimal totalCostUsd,
        BigDecimal dayChangeUsd,
        BigDecimal cashUsd,
        BigDecimal cashKrw,
        List<HoldingItem> holdings
) {}
