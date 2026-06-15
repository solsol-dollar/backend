package com.shinhan.eclipse.ledger.event;

public record RemittanceFailedEvent(
        Long subscriptionId,
        Long userId,
        String reason
) {}
