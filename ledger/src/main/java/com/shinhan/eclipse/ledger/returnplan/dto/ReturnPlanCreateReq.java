package com.shinhan.eclipse.ledger.returnplan.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ReturnPlanCreateReq(
        @NotNull @Positive Long subscriptionId
) {}
