package com.shinhan.eclipse.ledger.transfer;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransferResult(
        Long          transactionId,
        Long          fromAccountId,
        String        fromAccountType,
        Long          toAccountId,
        String        toAccountType,
        BigDecimal    amount,
        String        currency,
        String        status,
        LocalDateTime completedAt
) {}