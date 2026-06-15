package com.shinhan.eclipse.ledger.returnplan.internal;

import com.shinhan.eclipse.domain.returnplan.ReturnPlan;
import com.shinhan.eclipse.ledger.returnplan.ReturnPlanFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
class ReturnPlanFacadeImpl implements ReturnPlanFacade {

    private final ReturnPlanRepository returnPlanRepository;

    @Override
    public ReturnPlan createReturnPlan(Long userId, Long subscriptionId) {
        // TODO: 구현
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public ReturnPlan confirmReturnPlan(Long returnPlanId, Long userId) {
        // TODO: 구현
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public List<ReturnPlan> getReturnPlans(Long userId) {
        // TODO: 구현
        throw new UnsupportedOperationException("TODO");
    }
}
