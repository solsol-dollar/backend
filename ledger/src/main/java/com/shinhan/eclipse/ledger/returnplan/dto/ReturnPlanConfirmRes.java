package com.shinhan.eclipse.ledger.returnplan.dto;

import com.shinhan.eclipse.domain.returnplan.ReturnPlan;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ReturnPlanConfirmRes {
    private final Long returnPlanId;
    private final LocalDateTime confirmedAt;
    private final String planStatus;

    public static ReturnPlanConfirmRes from(ReturnPlan plan) {
        return ReturnPlanConfirmRes.builder()
                .returnPlanId(plan.getId())
                .confirmedAt(plan.getConfirmedAt())
                .planStatus(plan.getPlanStatus())
                .build();
    }
}
