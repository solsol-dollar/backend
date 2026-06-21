package com.shinhan.eclipse.ledger.allocation.dto;

import com.shinhan.eclipse.domain.ipo.Ipo;
import com.shinhan.eclipse.domain.subscription.IpoSubscription;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
public class AllocationResultDetailRes {
    private final Long subscriptionResultId;
    private final BigDecimal subscriptionAmount;
    private final BigDecimal allocatedAmount;
    private final BigDecimal refundAmount;
    private final BigDecimal allocationRate;
    private final Integer allocatedShares;
    private final BigDecimal currentPrice;
    private final BigDecimal pnlUsd;
    private final LocalDate listingDate;
    private final Boolean hasReturnPlan;

    public static AllocationResultDetailRes of(IpoSubscription subscription, Ipo ipo, BigDecimal currentPrice, boolean hasReturnPlan) {
        BigDecimal pnlUsd = null;
        if (currentPrice != null && subscription.getAllocatedShares() != null && subscription.getAllocatedAmount() != null) {
            pnlUsd = currentPrice.multiply(BigDecimal.valueOf(subscription.getAllocatedShares()))
                    .subtract(subscription.getAllocatedAmount());
        }
        return AllocationResultDetailRes.builder()
                .subscriptionResultId(subscription.getId())
                .subscriptionAmount(subscription.getSubscriptionAmount())
                .allocatedAmount(subscription.getAllocatedAmount())
                .refundAmount(subscription.getRefundAmount())
                .allocationRate(subscription.getAllocationRate())
                .allocatedShares(subscription.getAllocatedShares())
                .currentPrice(currentPrice)
                .pnlUsd(pnlUsd)
                .listingDate(ipo.getListingDate())
                .hasReturnPlan(hasReturnPlan)
                .build();
    }
}
