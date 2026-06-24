package com.shinhan.eclipse.ledger.subscription.dto;

import com.shinhan.eclipse.domain.account.FinancialAccount;
import com.shinhan.eclipse.domain.subscription.IpoSubscription;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * 청약 취소 응답 (명세 외 추가). REQUESTED 상태에서만 취소 가능하고 그 시점엔 아직 계좌 차감이
 * 없으므로, refundAmount는 실제 환불이 아니라 취소되는 청약신청금액을 보여주는 표시값이다.
 */
@Getter
@Builder
public class SubscriptionCancelRes {
    private final Long subscriptionId;
    private final BigDecimal refundAmount;
    private final String currency;
    private final Long refundAccountId;
    private final String refundAccountNumberMasked;
    private final String refundInstitutionName;

    public static SubscriptionCancelRes of(IpoSubscription subscription, FinancialAccount refundAccount) {
        return SubscriptionCancelRes.builder()
                .subscriptionId(subscription.getId())
                .refundAmount(subscription.getSubscriptionAmount())
                .currency(subscription.getCurrency())
                .refundAccountId(refundAccount.getId())
                .refundAccountNumberMasked(refundAccount.getAccountNumberMasked())
                .refundInstitutionName(refundAccount.getInstitutionName())
                .build();
    }
}
