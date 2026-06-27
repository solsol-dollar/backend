package com.shinhan.eclipse.ledger.returnplan.dto;

import java.math.BigDecimal;
import java.util.List;

public record ImmediateAllocationRes(
        BigDecimal totalAmount,
        List<AllocationView> allocations
) {
    public record AllocationView(
            String destinationType,
            int ratio,
            BigDecimal amount
    ) {}
}
