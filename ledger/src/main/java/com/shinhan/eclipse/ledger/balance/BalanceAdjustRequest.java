package com.shinhan.eclipse.ledger.balance;

import java.math.BigDecimal;

public record BalanceAdjustRequest(
    Long userId,
    Long accountId,
    BigDecimal amount,
    String type,   // "DEDUCT" | "ADD"
    String reason
) {}
