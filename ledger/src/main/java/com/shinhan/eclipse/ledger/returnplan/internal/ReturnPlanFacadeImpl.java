package com.shinhan.eclipse.ledger.returnplan.internal;

import com.shinhan.eclipse.common.exception.BusinessException;
import com.shinhan.eclipse.common.exception.ErrorCode;
import com.shinhan.eclipse.domain.account.FinancialAccount;
import com.shinhan.eclipse.domain.ipo.Ipo;
import com.shinhan.eclipse.domain.returnplan.ReturnPlan;
import com.shinhan.eclipse.domain.returnplan.ReturnPlanAllocation;
import com.shinhan.eclipse.domain.returnplan.ReturnPlanPreset;
import com.shinhan.eclipse.domain.subscription.IpoSubscription;
import com.shinhan.eclipse.ledger.accountlink.AccountLinkService;
import com.shinhan.eclipse.ledger.event.AllocationCompletedEvent;
import com.shinhan.eclipse.ledger.returnplan.ReturnPlanFacade;
import com.shinhan.eclipse.ledger.returnplan.dto.AllocationItem;
import com.shinhan.eclipse.ledger.returnplan.dto.ImmediateAllocationRes;
import com.shinhan.eclipse.ledger.subscription.SubscriptionFacade;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
class ReturnPlanFacadeImpl implements ReturnPlanFacade {

    private static final Logger log = LoggerFactory.getLogger(ReturnPlanFacadeImpl.class);
    private static final List<String> DESTINATION_TYPES = List.of("SECURITIES", "SAVINGS", "DEPOSIT");
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    /** 환불일(D+1) 21:00 KST 정산 배치보다 1시간 앞선 수정 마감 시각. */
    private static final LocalTime EDIT_CUTOFF_TIME = LocalTime.of(20, 0);

    private final ReturnPlanRepository returnPlanRepository;
    private final ReturnPlanAllocationRepository allocationRepository;
    private final ReturnPlanPresetRepository presetRepository;
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

        FinancialAccount savingsAccount = requireLinkedAccount(userId, "SAVINGS", "외화적립예금");
        requireLinkedAccount(userId, "DEPOSIT", "외화통장");

        Ipo nextIpo = subscriptionFacade.findNextUpcomingIpo().orElse(null);

        ReturnPlan plan = ReturnPlan.create(
                userId,
                subscriptionId,
                subscription.getRefundAmount(),
                nextIpo == null ? null : nextIpo.getId(),
                null,
                savingsAccount.getInterestRate());

        ReturnPlan saved;
        try {
            saved = returnPlanRepository.saveAndFlush(plan);
        } catch (DataIntegrityViolationException e) {
            // existsBySubscriptionId 체크와 INSERT 사이의 동시 요청 race condition은
            // DB unique 제약(subscription_id)이 최종 방어선이 된다.
            throw new BusinessException(ErrorCode.RETURN_PLAN_ALREADY_EXISTS);
        }

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
        return allocationRepository.findByReturnPlanIdOrderByIdAsc(returnPlanId);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, List<ReturnPlanAllocation>> getAllocationsByPlanIds(List<Long> returnPlanIds) {
        if (returnPlanIds.isEmpty()) {
            return Map.of();
        }
        return allocationRepository.findByReturnPlanIdIn(returnPlanIds).stream()
                .collect(Collectors.groupingBy(ReturnPlanAllocation::getReturnPlanId));
    }

    @Override
    @Transactional
    public ReturnPlan updateRatios(Long returnPlanId, Long userId, List<AllocationItem> items) {
        ReturnPlan plan = returnPlanRepository.findByIdAndUserIdForUpdate(returnPlanId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RETURN_PLAN_NOT_FOUND));

        if (!plan.isDraft()) {
            throw new BusinessException(ErrorCode.RETURN_PLAN_CONFLICT);
        }
        validateEditWindow(plan, userId);

        items.forEach(item -> {
            ReturnPlanAllocation allocation = allocationRepository
                    .findByReturnPlanIdAndDestinationType(plan.getId(), item.destinationType())
                    .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT, "지원하지 않는 분배 대상입니다: " + item.destinationType()));
            allocation.updateRatio(item.ratio(), plan.getTotalRefundAmount());
        });

        // 요청에 일부 destinationType이 빠져있어도(과거 비율이 그대로 남는 경우) 합이 깨지지 않는지
        // 플랜에 속한 전체 allocation 기준으로 재검증한다.
        List<ReturnPlanAllocation> allAllocations = allocationRepository.findByReturnPlanIdOrderByIdAsc(plan.getId());
        validateRatioSum(allAllocations.stream().mapToInt(a -> a.getAllocationRatio().intValue()).sum());

        log.info("리턴 플랜 비율 수정: returnPlanId={}, userId={}", returnPlanId, userId);
        return plan;
    }

    @Override
    @Transactional
    public ReturnPlan executeReturnPlan(Long returnPlanId) {
        ReturnPlan plan = returnPlanRepository.findByIdForUpdate(returnPlanId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RETURN_PLAN_NOT_FOUND));

        if (!plan.isDraft()) {
            throw new BusinessException(ErrorCode.RETURN_PLAN_CONFLICT);
        }

        // 생성 이후 사용자가 계좌 연동을 끊었을 수 있으니 실제 자금 이동 직전에 한 번 더 확인한다.
        requireLinkedAccount(plan.getUserId(), "SAVINGS", "외화적립예금");
        requireLinkedAccount(plan.getUserId(), "DEPOSIT", "외화통장");

        List<ReturnPlanAllocation> allocations = allocationRepository.findByReturnPlanIdOrderByIdAsc(plan.getId());
        int ratioSum = allocations.stream().mapToInt(a -> a.getAllocationRatio().intValue()).sum();
        if (ratioSum != 100) {
            log.warn("리턴 플랜 비율 미설정으로 기본값(SECURITIES 100%) 적용: returnPlanId={}", plan.getId());
            applyDefaultAllocation(plan, allocations);
        }

        creditAllocations(plan, allocations);
        plan.execute();

        eventPublisher.publishEvent(new AllocationCompletedEvent(plan.getSubscriptionId(), plan.getUserId(), plan.getTotalRefundAmount()));

        log.info("리턴 플랜 실행: returnPlanId={}, userId={}", returnPlanId, plan.getUserId());
        return plan;
    }

    /** 목적지 타입별 계좌에 분배 금액을 실제로 적립한다 (환불금 자체가 여기서 처음 계좌에 들어간다). */
    private void creditAllocations(ReturnPlan plan, List<ReturnPlanAllocation> allocations) {
        for (ReturnPlanAllocation allocation : allocations) {
            if (allocation.getAllocationAmount().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            FinancialAccount account = accountLinkService.findAccountByType(plan.getUserId(), allocation.getDestinationType())
                    .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_LINKED,
                            allocation.getDestinationType() + " 계좌가 연동되어 있지 않아 분배를 실행할 수 없습니다."));
            accountLinkService.credit(plan.getUserId(), account.getId(), allocation.getAllocationAmount());
            allocation.markExecuted(account.getId());
        }
    }

    /** SAVINGS/DEPOSIT 둘 중 하나라도 연동되어 있지 않으면 리턴 플랜 생성/실행 자체를 막는다. */
    private FinancialAccount requireLinkedAccount(Long userId, String accountType, String displayName) {
        return accountLinkService.findAccountByType(userId, accountType)
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_LINKED,
                        displayName + " 계좌가 연동되어 있지 않아 리턴 플랜을 진행할 수 없습니다."));
    }

    private void applyDefaultAllocation(ReturnPlan plan, List<ReturnPlanAllocation> allocations) {
        for (ReturnPlanAllocation allocation : allocations) {
            int ratio = "SECURITIES".equals(allocation.getDestinationType()) ? 100 : 0;
            allocation.updateRatio(ratio, plan.getTotalRefundAmount());
        }
    }

    /**
     * 환불일(D+1) 21:00 KST에 정산 배치가 실행되므로, 그 1시간 전인 20:00까지만 비율 수정을 허용한다.
     * 정확히 그 순간에 몰려도 같은 row를 비관적 락으로 잡기 때문에 데이터가 깨지지는 않지만,
     * 사용자가 "방금 수정한 게 반영됐는지 애매한" 혼란을 막기 위한 버퍼다.
     */
    private void validateEditWindow(ReturnPlan plan, Long userId) {
        IpoSubscription subscription = subscriptionFacade.getSubscriptionResult(plan.getSubscriptionId(), userId);
        Ipo ipo = subscriptionFacade.getIpo(subscription.getIpoId());
        if (ipo.getRefundDate() == null) {
            return;
        }
        LocalDateTime editDeadline = LocalDateTime.of(ipo.getRefundDate(), EDIT_CUTOFF_TIME);
        if (!LocalDateTime.now(KST).isBefore(editDeadline)) {
            throw new BusinessException(ErrorCode.RETURN_PLAN_EDIT_WINDOW_CLOSED);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReturnPlan> getReturnPlans(Long userId, LocalDate from, LocalDate to, String status, Pageable pageable) {
        String normalizedStatus = (!StringUtils.hasText(status) || "ALL".equalsIgnoreCase(status)) ? null : status;
        LocalDateTime fromDateTime = from == null ? null : from.atStartOfDay();
        LocalDateTime toDateTime = to == null ? null : to.atTime(LocalTime.MAX);
        return returnPlanRepository.search(userId, normalizedStatus, fromDateTime, toDateTime, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReturnPlan> getAllReturnPlans(Long userId) {
        return returnPlanRepository.findByUserId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsBySubscriptionId(Long subscriptionId) {
        return returnPlanRepository.existsBySubscriptionId(subscriptionId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReturnPlanPreset> getPresets() {
        return presetRepository.findAllByOrderByDisplayOrderAsc();
    }

    @Override
    @Transactional
    public ReturnPlan applyPreset(Long returnPlanId, Long userId, String presetCode) {
        ReturnPlanPreset preset = presetRepository.findByPresetCode(presetCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.RETURN_PLAN_PRESET_NOT_FOUND));

        List<AllocationItem> items = List.of(
                new AllocationItem("SECURITIES", preset.getSecuritiesRatio().intValue()),
                new AllocationItem("SAVINGS", preset.getSavingsRatio().intValue()),
                new AllocationItem("DEPOSIT", preset.getAccountRatio().intValue()));

        log.info("리턴 플랜 프리셋 적용: returnPlanId={}, userId={}, presetCode={}", returnPlanId, userId, presetCode);
        return updateRatios(returnPlanId, userId, items);
    }

    private void validateRatioSum(int sum) {
        if (sum != 100) {
            throw new BusinessException(ErrorCode.RATIO_SUM_INVALID);
        }
    }

    @Override
    @Transactional
    public ImmediateAllocationRes executeImmediateAllocation(Long userId, List<AllocationItem> items) {
        validateRatioSum(items.stream().mapToInt(AllocationItem::ratio).sum());

        FinancialAccount cmaAccount = accountLinkService.findAccountByType(userId, "SECURITIES")
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_LINKED, "증권 CMA 계좌가 연동되지 않았습니다."));

        BigDecimal available = cmaAccount.availableBalance();
        if (available.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_BALANCE, "분배 가능한 예수금이 없습니다.");
        }

        List<ImmediateAllocationRes.AllocationView> views = new ArrayList<>();
        for (AllocationItem item : items) {
            BigDecimal amount = available
                    .multiply(BigDecimal.valueOf(item.ratio()))
                    .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);

            if (!"SECURITIES".equals(item.destinationType()) && item.ratio() > 0) {
                FinancialAccount destAccount = accountLinkService.findAccountByType(userId, item.destinationType())
                        .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_LINKED,
                                item.destinationType() + " 계좌가 연동되지 않았습니다."));
                accountLinkService.deduct(userId, cmaAccount.getId(), amount);
                accountLinkService.credit(userId, destAccount.getId(), amount);
            }

            views.add(new ImmediateAllocationRes.AllocationView(item.destinationType(), item.ratio(), amount));
        }

        log.info("즉시 분배 실행: userId={}, totalAmount={}", userId, available);
        return new ImmediateAllocationRes(available, views);
    }
}
