package com.shinhan.eclipse.service.securities;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SellProfitItem(
        Long orderId,
        LocalDateTime date,
        String productType,
        String ticker,
        String productName,
        BigDecimal totalSaleAmountUsd,
        BigDecimal profitRate,
        boolean isProfit
) {}
