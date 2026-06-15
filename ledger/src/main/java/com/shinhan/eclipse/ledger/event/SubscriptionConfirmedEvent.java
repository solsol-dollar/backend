package com.shinhan.eclipse.ledger.event;

import java.math.BigDecimal;

public record SubscriptionConfirmedEvent(
        Long subscriptionId,
        Long userId,
        Long ipoId,
        BigDecimal amountUsd
) {}
