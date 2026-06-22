package com.shinhan.eclipse.service.inflow.internal;

import com.shinhan.eclipse.domain.inflow.IdleDollarTrigger;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface IdleDollarTriggerRepository extends JpaRepository<IdleDollarTrigger, Long> {
    List<IdleDollarTrigger> findByUserIdOrderByDetectedAtDesc(Long userId);
    List<IdleDollarTrigger> findByAccountIdAndTriggerStatus(Long accountId, String triggerStatus);
}
