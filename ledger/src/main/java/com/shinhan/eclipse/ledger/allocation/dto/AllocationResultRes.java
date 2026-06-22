package com.shinhan.eclipse.ledger.allocation.dto;

import com.shinhan.eclipse.domain.ipo.Ipo;
import com.shinhan.eclipse.domain.subscription.IpoSubscription;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class AllocationResultRes {
    private final Long subscriptionResultId;
    private final Long subscriptionId;
    private final Long ipoId;
    private final String ticker;
    private final String companyName;
    private final BigDecimal subscriptionAmount;
    private final BigDecimal allocatedAmount;
    private final BigDecimal refundAmount;
    private final BigDecimal allocationRate;
    private final Integer allocatedShares;
    private final BigDecimal currentPrice;
    private final BigDecimal pnlUsd;
    private final String allocationStatus;
    private final LocalDateTime allocatedAt;

    public static AllocationResultRes of(IpoSubscription subscription, Ipo ipo, BigDecimal currentPrice) {
        BigDecimal pnlUsd = computePnl(subscription, currentPrice);
        return AllocationResultRes.builder()
                .subscriptionResultId(subscription.getId())
                .subscriptionId(subscription.getId())
                .ipoId(subscription.getIpoId())
                .ticker(ipo.getTicker())
                .companyName(ipo.getCompanyName())
                .subscriptionAmount(subscription.getSubscriptionAmount())
                .allocatedAmount(subscription.getAllocatedAmount())
                .refundAmount(subscription.getRefundAmount())
                .allocationRate(subscription.getAllocationRate())
                .allocatedShares(subscription.getAllocatedShares())
                .currentPrice(currentPrice)
                .pnlUsd(pnlUsd)
                .allocationStatus(subscription.getResultStatus() == null ? "PENDING" : subscription.getResultStatus())
                .allocatedAt(subscription.getConfirmedAt())
                .build();
    }

    private static BigDecimal computePnl(IpoSubscription subscription, BigDecimal currentPrice) {
        if (currentPrice == null || subscription.getAllocatedShares() == null || subscription.getAllocatedAmount() == null) {
            return null;
        }
        BigDecimal currentValue = currentPrice.multiply(BigDecimal.valueOf(subscription.getAllocatedShares()));
        return currentValue.subtract(subscription.getAllocatedAmount());
    }
}
