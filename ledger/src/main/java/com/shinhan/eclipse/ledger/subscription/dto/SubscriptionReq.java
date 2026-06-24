package com.shinhan.eclipse.ledger.subscription.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/** shares 대신 화면에서 입력받는 USD 금액(subscriptionAmount)을 받는다 — 주수 환산은 서버(SubscriptionFacadeImpl)에서 한다. */
public record SubscriptionReq(
        @NotNull Long ipoId,
        @NotNull Long securitiesAccountId,
        @NotNull @Positive BigDecimal subscriptionAmount,
        @NotNull @Positive BigDecimal offerPrice
) {}
