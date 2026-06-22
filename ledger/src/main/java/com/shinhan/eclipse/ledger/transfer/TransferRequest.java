package com.shinhan.eclipse.ledger.transfer;

import java.math.BigDecimal;

public record TransferRequest(
        Long       fromAccountId,
        Long       toAccountId,
        BigDecimal amount
) {}
