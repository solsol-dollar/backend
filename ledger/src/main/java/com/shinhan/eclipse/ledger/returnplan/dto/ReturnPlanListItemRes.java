package com.shinhan.eclipse.ledger.returnplan.dto;

import com.shinhan.eclipse.domain.ipo.Ipo;
import com.shinhan.eclipse.domain.returnplan.ReturnPlan;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
public class ReturnPlanListItemRes {
    private final Long returnPlanId;
    private final Long subscriptionId;
    private final BigDecimal totalRefundAmount;
    private final String planStatus;
    /** 화면 표시용 보강 필드 (명세 외 추가). */
    private final String sourceTicker;
    private final String sourceCompanyName;
    private final LocalDate refundDate;
    /** 공휴일·주말을 반영한 실제 입금 예정일. null이면 refundDate로 fallback. */
    private final LocalDate depositDate;

    public static ReturnPlanListItemRes from(ReturnPlan plan, Ipo sourceIpo) {
        return ReturnPlanListItemRes.builder()
                .returnPlanId(plan.getId())
                .subscriptionId(plan.getSubscriptionId())
                .totalRefundAmount(plan.getTotalRefundAmount())
                .planStatus(plan.getPlanStatus())
                .sourceTicker(sourceIpo.getTicker())
                .sourceCompanyName(sourceIpo.getCompanyName())
                .refundDate(sourceIpo.getRefundDate())
                .depositDate(sourceIpo.getDepositDate())
                .build();
    }
}
