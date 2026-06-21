package com.shinhan.eclipse.ledger.subscription.dto;

import com.shinhan.eclipse.domain.subscription.IpoSubscription;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record SubscriptionReq(
        @NotNull Long ipoId,
        @NotNull Long securitiesAccountId,
        @NotNull @Positive Integer shares,
        @NotNull @Positive BigDecimal offerPrice
) {
    public IpoSubscription toEntity(Long userId) {
        return IpoSubscription.request(userId, ipoId, securitiesAccountId, shares, offerPrice);
    }
}
