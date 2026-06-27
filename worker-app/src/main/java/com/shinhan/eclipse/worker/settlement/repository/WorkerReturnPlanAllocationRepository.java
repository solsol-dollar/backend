package com.shinhan.eclipse.worker.settlement.repository;

import com.shinhan.eclipse.domain.returnplan.ReturnPlanAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WorkerReturnPlanAllocationRepository extends JpaRepository<ReturnPlanAllocation, Long> {

    Optional<ReturnPlanAllocation> findByReturnPlanIdAndDestinationType(Long returnPlanId, String destinationType);
}
