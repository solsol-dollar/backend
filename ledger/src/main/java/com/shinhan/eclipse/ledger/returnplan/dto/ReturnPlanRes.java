package com.shinhan.eclipse.ledger.returnplan.dto;

import com.shinhan.eclipse.domain.ipo.Ipo;
import com.shinhan.eclipse.domain.returnplan.ReturnPlan;
import com.shinhan.eclipse.domain.returnplan.ReturnPlanAllocation;
import com.shinhan.eclipse.domain.subscription.IpoSubscription;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class ReturnPlanRes {
    private final Long returnPlanId;
    private final Long subscriptionId;
    private final BigDecimal totalRefundAmount;
    /** 이 환불금의 출처가 된 IPO (화면 표시용 보강 필드, 명세 외 추가). */
    private final String sourceTicker;
    private final String sourceCompanyName;
    private final LocalDate refundDate;
    /** 출처 청약/배정 정보 (상세 화면 상단 청약금/배정률/배정금 표시용, 명세 외 추가). */
    private final BigDecimal subscriptionAmount;
    private final BigDecimal allocationRate;
    private final BigDecimal allocatedAmount;
    private final NextIpoInfo nextIpoInfo;
    private final BigDecimal savingsRate;
    private final List<AllocationView> allocations;

    public static ReturnPlanRes of(ReturnPlan plan, List<ReturnPlanAllocation> allocations, Ipo nextIpo,
                                    Ipo sourceIpo, IpoSubscription sourceSubscription) {
        return ReturnPlanRes.builder()
                .returnPlanId(plan.getId())
                .subscriptionId(plan.getSubscriptionId())
                .totalRefundAmount(plan.getTotalRefundAmount())
                .sourceTicker(sourceIpo.getTicker())
                .sourceCompanyName(sourceIpo.getCompanyName())
                .refundDate(sourceIpo.getRefundDate())
                .subscriptionAmount(sourceSubscription.getSubscriptionAmount())
                .allocationRate(sourceSubscription.getAllocationRate())
                .allocatedAmount(sourceSubscription.getAllocatedAmount())
                .nextIpoInfo(nextIpo == null ? null : NextIpoInfo.from(nextIpo))
                .savingsRate(plan.getSavingsInterestRate())
                .allocations(allocations.stream().map(AllocationView::from).toList())
                .build();
    }

    @Getter
    @Builder
    public static class NextIpoInfo {
        private final Long ipoId;
        private final String ticker;
        private final LocalDate subscriptionStartDate;

        public static NextIpoInfo from(Ipo ipo) {
            return NextIpoInfo.builder()
                    .ipoId(ipo.getId())
                    .ticker(ipo.getTicker())
                    .subscriptionStartDate(ipo.getSubscriptionStartDate())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class AllocationView {
        private final String destinationType;
        private final Integer ratio;
        private final BigDecimal amount;

        public static AllocationView from(ReturnPlanAllocation allocation) {
            return AllocationView.builder()
                    .destinationType(allocation.getDestinationType())
                    .ratio(allocation.getAllocationRatio().intValue())
                    .amount(allocation.getAllocationAmount())
                    .build();
        }
    }
}
