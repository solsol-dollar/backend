package com.shinhan.eclipse.service.securities;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderHistoryItem(
        Long orderId,
        LocalDateTime orderedAt,
        String ticker,
        String productName,
        String orderSide,
        String orderStatus,
        BigDecimal executedPrice,
        Integer quantity
) {}
