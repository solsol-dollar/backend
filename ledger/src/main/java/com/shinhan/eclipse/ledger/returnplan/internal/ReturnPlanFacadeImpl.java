package com.shinhan.eclipse.ledger.returnplan.internal;

import com.shinhan.eclipse.common.exception.BusinessException;
import com.shinhan.eclipse.common.exception.ErrorCode;
import com.shinhan.eclipse.domain.account.FinancialAccount;
import com.shinhan.eclipse.domain.ipo.Ipo;
import com.shinhan.eclipse.domain.returnplan.ReturnPlan;
import com.shinhan.eclipse.domain.returnplan.ReturnPlanAllocation;
import com.shinhan.eclipse.domain.subscription.IpoSubscription;
import com.shinhan.eclipse.ledger.accountlink.AccountLinkService;
import com.shinhan.eclipse.ledger.event.AllocationCompletedEvent;
import com.shinhan.eclipse.ledger.returnplan.ReturnPlanFacade;
import com.shinhan.eclipse.ledger.returnplan.dto.AllocationItem;
import com.shinhan.eclipse.ledger.subscription.SubscriptionFacade;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
class ReturnPlanFacadeImpl implements ReturnPlanFacade {

    private static final Logger log = LoggerFactory.getLogger(ReturnPlanFacadeImpl.class);
    private static final List<String> DESTINATION_TYPES = List.of("SECURITIES", "FX_SAVINGS", "FX_ACCOUNT");

    private final ReturnPlanRepository returnPlanRepository;
    private final ReturnPlanAllocationRepository allocationRepository;
    private final SubscriptionFacade subscriptionFacade;
    private final AccountLinkService accountLinkService;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public ReturnPlan createReturnPlan(Long userId, Long subscriptionId) {
        IpoSubscription subscription = subscriptionFacade.getSubscriptionResult(subscriptionId, userId);
        if (subscription.getRefundAmount() == null) {
            throw new BusinessException(ErrorCode.ALLOCATION_NOT_FOUND);
        }
        if (returnPlanRepository.existsBySubscriptionId(subscriptionId)) {
            throw new BusinessException(ErrorCode.RETURN_PLAN_ALREADY_EXISTS);
        }

        Ipo nextIpo = subscriptionFacade.findNextUpcomingIpo().orElse(null);
        FinancialAccount fxSavings = accountLinkService.findAccountByType(userId, "FX_SAVINGS").orElse(null);

        ReturnPlan plan = ReturnPlan.create(
                userId,
                subscriptionId,
                subscription.getRefundAmount(),
                nextIpo == null ? null : nextIpo.getId(),
                null,
                fxSavings == null ? null : fxSavings.getInterestRate());
        ReturnPlan saved = returnPlanRepository.save(plan);

        DESTINATION_TYPES.forEach(type ->
                allocationRepository.save(ReturnPlanAllocation.initZero(saved.getId(), type)));

        log.info("리턴 플랜 생성: returnPlanId={}, userId={}, subscriptionId={}", saved.getId(), userId, subscriptionId);
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public ReturnPlan getReturnPlan(Long returnPlanId, Long userId) {
        return returnPlanRepository.findByIdAndUserId(returnPlanId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RETURN_PLAN_NOT_FOUND));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReturnPlanAllocation> getAllocations(Long returnPlanId) {
        return allocationRepository.findByReturnPlanId(returnPlanId);
    }

    @Override
    @Transactional
    public ReturnPlan updateRatios(Long returnPlanId, Long userId, List<AllocationItem> items) {
        ReturnPlan plan = returnPlanRepository.findByIdAndUserIdForUpdate(returnPlanId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RETURN_PLAN_NOT_FOUND));

        validateRatioSum(items.stream().mapToInt(AllocationItem::ratio).sum());

        items.forEach(item -> {
            ReturnPlanAllocation allocation = allocationRepository
                    .findByReturnPlanIdAndDestinationType(plan.getId(), item.destinationType())
                    .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT, "지원하지 않는 분배 대상입니다: " + item.destinationType()));
            allocation.updateRatio(item.ratio(), plan.getTotalRefundAmount());
        });

        log.info("리턴 플랜 비율 수정: returnPlanId={}, userId={}", returnPlanId, userId);
        return plan;
    }

    @Override
    @Transactional
    public ReturnPlan confirmReturnPlan(Long returnPlanId, Long userId) {
        ReturnPlan plan = returnPlanRepository.findByIdAndUserIdForUpdate(returnPlanId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RETURN_PLAN_NOT_FOUND));

        List<ReturnPlanAllocation> allocations = allocationRepository.findByReturnPlanId(plan.getId());
        validateRatioSum(allocations.stream().mapToInt(a -> a.getAllocationRatio().intValue()).sum());

        plan.confirm();

        eventPublisher.publishEvent(new AllocationCompletedEvent(plan.getSubscriptionId(), userId, plan.getTotalRefundAmount()));

        log.info("리턴 플랜 확정: returnPlanId={}, userId={}", returnPlanId, userId);
        return plan;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReturnPlan> getReturnPlans(Long userId, Pageable pageable) {
        return returnPlanRepository.findByUserId(userId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsBySubscriptionId(Long subscriptionId) {
        return returnPlanRepository.existsBySubscriptionId(subscriptionId);
    }

    private void validateRatioSum(int sum) {
        if (sum != 100) {
            throw new BusinessException(ErrorCode.RATIO_SUM_INVALID);
        }
    }
}
