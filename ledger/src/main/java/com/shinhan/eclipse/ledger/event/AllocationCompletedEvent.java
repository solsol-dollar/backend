package com.shinhan.eclipse.ledger.event;

import java.math.BigDecimal;

public record AllocationCompletedEvent(
        Long subscriptionId,
        Long userId,
        BigDecimal refundAmount
) {}
