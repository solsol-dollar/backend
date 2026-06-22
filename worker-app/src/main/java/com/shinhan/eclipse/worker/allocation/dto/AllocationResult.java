package com.shinhan.eclipse.worker.allocation.dto;

import java.math.BigDecimal;

/** 배정 엔진 출력값 — 청약자 1명의 최종 배정 결과. */
public record AllocationResult(
        Long customerId,
        int requestedQuantity,
        int equalAllocated,
        int proportionalAllocated,
        int additionalAllocated,
        int finalAllocated,
        BigDecimal allocationRate
) {}
