package com.shinhan.eclipse.service.card;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record CardTransactionSummary(
        int totalCount,
        BigDecimal totalAmount,
        String currency,
        FxSavings fxSavings,
        List<CategorySpend> byCategory,
        TransactionItem topSpend,
        List<RecurringPayment> recurringPayments,
        List<TransactionItem> transactions
) {
    public record FxSavings(
            BigDecimal savingsKrw,
            BigDecimal savingsUsd
    ) {}
    public record CategorySpend(
            String category,
            BigDecimal amount,
            int count
    ) {}

    public record TransactionItem(
            Long id,
            String merchantName,
            String category,
            BigDecimal amount,
            String currency,
            LocalDateTime transactedAt
    ) {}

    public record RecurringPayment(
            String merchantName,
            String category,
            BigDecimal averageAmount,
            int dayOfMonth
    ) {}
}
