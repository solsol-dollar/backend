package com.shinhan.eclipse.ledger.exchange;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ExchangeResult(
        Long          transactionId,
        BigDecimal    exchangeRate,
        BigDecimal    sourceAmount,
        String        fromCurrency,
        BigDecimal    targetAmount,
        String        toCurrency,
        String        status,
        LocalDateTime completedAt
) {}