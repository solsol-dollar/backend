package com.shinhan.eclipse.worker.allocation.dto;

import java.math.BigDecimal;

public record AllocationResult(
        Long customerId,
        int requestedQuantity,
        int finalAllocated,
        BigDecimal allocationRate
) {}
