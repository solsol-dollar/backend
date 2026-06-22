package com.shinhan.eclipse.service.inflow.internal;

import com.shinhan.eclipse.domain.inflow.IdleDollarTrigger;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

interface IdleDollarTriggerRepository extends JpaRepository<IdleDollarTrigger, Long> {
    List<IdleDollarTrigger> findByUserIdOrderByDetectedAtDesc(Long userId);
    Optional<IdleDollarTrigger> findByAccountIdAndTriggerStatus(Long accountId, String triggerStatus);
    Optional<IdleDollarTrigger> findByUserIdAndTriggerStatus(Long userId, String triggerStatus);
}
