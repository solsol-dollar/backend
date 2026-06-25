package com.shinhan.eclipse.ledger.returnplan.internal;

import com.shinhan.eclipse.domain.returnplan.ReturnPlanAllocation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

interface ReturnPlanAllocationRepository extends JpaRepository<ReturnPlanAllocation, Long> {
    List<ReturnPlanAllocation> findByReturnPlanIdOrderByIdAsc(Long returnPlanId);

    Optional<ReturnPlanAllocation> findByReturnPlanIdAndDestinationType(Long returnPlanId, String destinationType);

    /** 대시보드 요약 등에서 여러 플랜의 분배 내역을 한 번에 가져와 N+1을 피하기 위한 배치 조회. */
    List<ReturnPlanAllocation> findByReturnPlanIdIn(List<Long> returnPlanIds);
}
