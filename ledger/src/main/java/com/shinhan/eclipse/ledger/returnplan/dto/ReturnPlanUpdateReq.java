package com.shinhan.eclipse.ledger.returnplan.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ReturnPlanUpdateReq(
        @NotEmpty @Valid List<AllocationItem> allocations
) {}
