package com.shinhan.eclipse.ledger.returnplan;

import com.shinhan.eclipse.domain.returnplan.ReturnPlan;
import java.util.List;

public interface ReturnPlanFacade {
    ReturnPlan createReturnPlan(Long userId, Long subscriptionId);
    ReturnPlan confirmReturnPlan(Long returnPlanId, Long userId);
    List<ReturnPlan> getReturnPlans(Long userId);
}
