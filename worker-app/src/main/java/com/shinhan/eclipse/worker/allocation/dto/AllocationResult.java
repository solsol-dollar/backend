package com.shinhan.eclipse.worker.allocation.dto;

import java.math.BigDecimal;

/** 배정 엔진 출력값 — 청약자 1명의 최종 배정 결과 (신청자별 독립 랜덤 배정). */
public record AllocationResult(
        Long customerId,
        int requestedQuantity,
        int finalAllocated,
        BigDecimal allocationRate
) {}
