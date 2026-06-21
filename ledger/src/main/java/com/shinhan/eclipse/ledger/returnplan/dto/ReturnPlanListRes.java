package com.shinhan.eclipse.ledger.returnplan.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ReturnPlanListRes {
    private final List<ReturnPlanListItemRes> returnPlans;
}
