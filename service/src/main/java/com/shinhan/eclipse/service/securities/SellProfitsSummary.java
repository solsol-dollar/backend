package com.shinhan.eclipse.service.securities;

import java.math.BigDecimal;
import java.util.List;

public record SellProfitsSummary(
        BigDecimal totalProfitKrw,
        boolean isProfit,
        List<SellProfitItem> items
) {}
