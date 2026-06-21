package com.shinhan.eclipse.ledger.subscription.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class SubscriptionListRes {
    private final List<SubscriptionRes> subscriptions;
}
