package com.shinhan.eclipse.ledger.subscription.dto;

import com.shinhan.eclipse.domain.subscription.IpoSubscription;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class SubscriptionRes {
    private final Long subscriptionId;
    private final Long ipoId;
    private final Integer requestedShares;
    private final BigDecimal subscriptionAmount;
    private final String currency;
    private final String subscriptionStatus;
    private final LocalDateTime subscribedAt;
    private final LocalDateTime confirmedAt;

    public static SubscriptionRes from(IpoSubscription subscription) {
        return SubscriptionRes.builder()
                .subscriptionId(subscription.getId())
                .ipoId(subscription.getIpoId())
                .requestedShares(subscription.getRequestedShares())
                .subscriptionAmount(subscription.getSubscriptionAmount())
                .currency(subscription.getCurrency())
                .subscriptionStatus(subscription.getSubscriptionStatus())
                .subscribedAt(subscription.getSubscribedAt())
                .confirmedAt(subscription.getConfirmedAt())
                .build();
    }
}
