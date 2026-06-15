package com.shinhan.eclipse.ledger.returnplan.internal;

import com.shinhan.eclipse.domain.returnplan.ReturnPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface ReturnPlanRepository extends JpaRepository<ReturnPlan, Long> {
    List<ReturnPlan> findByUserId(Long userId);
}
