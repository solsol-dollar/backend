package com.shinhan.eclipse.ledger.returnplan;

import com.shinhan.eclipse.domain.returnplan.ReturnPlan;
import com.shinhan.eclipse.domain.returnplan.ReturnPlanAllocation;
import com.shinhan.eclipse.ledger.returnplan.dto.AllocationItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ReturnPlanFacade {
    ReturnPlan createReturnPlan(Long userId, Long subscriptionId);
    ReturnPlan getReturnPlan(Long returnPlanId, Long userId);
    List<ReturnPlanAllocation> getAllocations(Long returnPlanId);
    ReturnPlan updateRatios(Long returnPlanId, Long userId, List<AllocationItem> items);
    ReturnPlan confirmReturnPlan(Long returnPlanId, Long userId);
    Page<ReturnPlan> getReturnPlans(Long userId, Pageable pageable);

    /** ALLOC-002 의 hasReturnPlan 플래그 계산용. */
    boolean existsBySubscriptionId(Long subscriptionId);
}
