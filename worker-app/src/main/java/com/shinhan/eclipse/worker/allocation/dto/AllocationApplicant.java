package com.shinhan.eclipse.worker.allocation.dto;

/** 배정 엔진 입력값 — 청약자 1명의 신청 정보. */
public record AllocationApplicant(Long customerId, int requestedQuantity) {}
