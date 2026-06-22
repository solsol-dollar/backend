package com.shinhan.eclipse.ledger.returnplan.dto;

import jakarta.validation.constraints.NotNull;

public record ReturnPlanCreateReq(
        @NotNull Long subscriptionId
) {}
