package com.shinhan.eclipse.ledger.returnplan.internal;

import com.shinhan.eclipse.domain.returnplan.ReturnPlanAllocation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

interface ReturnPlanAllocationRepository extends JpaRepository<ReturnPlanAllocation, Long> {
    List<ReturnPlanAllocation> findByReturnPlanId(Long returnPlanId);

    Optional<ReturnPlanAllocation> findByReturnPlanIdAndDestinationType(Long returnPlanId, String destinationType);
}
