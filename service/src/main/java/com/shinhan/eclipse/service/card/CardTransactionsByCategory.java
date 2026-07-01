package com.shinhan.eclipse.service.card;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record CardTransactionsByCategory(
        List<CategoryGroup> categories
) {
    public record CategoryGroup(
            String category,
            BigDecimal totalAmount,
            int count,
            List<TransactionItem> transactions
    ) {}

    public record TransactionItem(
            Long id,
            String merchantName,
            BigDecimal amount,
            String currency,
            LocalDateTime transactedAt
    ) {}
}
