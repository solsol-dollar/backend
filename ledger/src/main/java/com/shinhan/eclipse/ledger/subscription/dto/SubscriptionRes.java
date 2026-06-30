package com.shinhan.eclipse.ledger.subscription.dto;

import com.shinhan.eclipse.domain.ipo.Ipo;
import com.shinhan.eclipse.domain.subscription.IpoSubscription;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class SubscriptionRes {
    private static final BigDecimal AGENCY_DEPOSIT_RATE = new BigDecimal("1.01");

    private final Long subscriptionId;
    private final Long subscriptionResultId;
    private final String resultStatus;
    private final Long ipoId;
    private final Integer requestedShares;
    private final BigDecimal subscriptionAmount;
    /** 청약대행증거금 = 청약신청금액 × 1.01 (명세 외 추가). */
    private final BigDecimal subscriptionAgencyDeposit;
    private final String currency;
    private final String subscriptionStatus;
    private final LocalDateTime subscribedAt;
    private final LocalDateTime confirmedAt;
    /** 화면 표시용 IPO 정보 (명세 외 추가). */
    private final String ticker;
    private final String companyName;
    private final BigDecimal offerPriceMin;
    private final BigDecimal offerPriceMax;
    private final BigDecimal confirmedOfferPrice;
    private final LocalDate listingDate;
    private final String logoUrl;

    public static SubscriptionRes from(IpoSubscription subscription, Ipo ipo) {
        return SubscriptionRes.builder()
                .subscriptionId(subscription.getId())
                .subscriptionResultId("COMPLETED".equals(subscription.getResultStatus()) || "DEPOSITED".equals(subscription.getResultStatus()) ? subscription.getId() : null)
                .resultStatus(subscription.getResultStatus())
                .ipoId(subscription.getIpoId())
                .requestedShares(subscription.getRequestedShares())
                .subscriptionAmount(subscription.getSubscriptionAmount())
                .subscriptionAgencyDeposit(subscription.getSubscriptionAmount()
                        .multiply(AGENCY_DEPOSIT_RATE).setScale(2, RoundingMode.HALF_UP))
                .currency(subscription.getCurrency())
                .subscriptionStatus(subscription.getSubscriptionStatus())
                .subscribedAt(subscription.getSubscribedAt())
                .confirmedAt(subscription.getConfirmedAt())
                .ticker(ipo.getTicker())
                .companyName(ipo.getCompanyName())
                .offerPriceMin(ipo.getOfferPriceMin())
                .offerPriceMax(ipo.getOfferPriceMax())
                .confirmedOfferPrice(ipo.getConfirmedOfferPrice())
                .listingDate(ipo.getListingDate())
                .logoUrl(ipo.getLogoUrl())
                .build();
    }
}
