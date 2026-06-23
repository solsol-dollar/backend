package com.shinhan.eclipse.service.inflow.internal;

import com.shinhan.eclipse.common.exception.BusinessException;
import com.shinhan.eclipse.common.exception.ErrorCode;
import com.shinhan.eclipse.domain.account.FinancialAccount;
import com.shinhan.eclipse.domain.inflow.IdleDollarTrigger;
import com.shinhan.eclipse.service.inflow.IdleDollarService;
import com.shinhan.eclipse.service.inflow.IdleDollarStatusResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
class IdleDollarServiceImpl implements IdleDollarService {

    private static final int IDLE_THRESHOLD_DAYS = 14;
    private static final ZoneId KST                 = ZoneId.of("Asia/Seoul");

    private final IdleDollarTriggerRepository        triggerRepository;
    private final IdleDetectionAccountRepository     accountRepository;
    private final IdleDetectionTradeOrderRepository  tradeOrderRepository;
    private final IdleDetectionFxTransactionRepository fxTransactionRepository;
    private final IdleDetectionTransferRepository    transferRepository;
    private final IdleDetectionFavoriteIpoRepository favoriteIpoRepository;

    @Override
    @Transactional
    public void detectAll() {
        List<FinancialAccount> accounts = accountRepository.findActiveSecuritiesAccountsWithBalance();
        log.info("유휴 달러 감지 시작: 대상 계좌 {}개", accounts.size());

        int invalidated = 0;
        int triggered = 0;
        int skipped = 0;

        LocalDateTime threshold = LocalDateTime.now(KST).minusDays(IDLE_THRESHOLD_DAYS);

        for (FinancialAccount account : accounts) {
            try {
                Optional<LocalDateTime> lastActivity = resolveLastActivity(account.getId());
                boolean isActive = lastActivity.isPresent() && lastActivity.get().isAfter(threshold);

                if (isActive) {
                    // 최근 거래 발생 → 기존 TRIGGERED 트리거 무효화
                    invalidated += invalidateExistingTriggers(account.getId());
                } else {
                    // 14일 이상 유휴 → TRIGGERED 트리거가 없을 때만 신규 생성
                    if (createTriggerIfAbsent(account, lastActivity)) triggered++;
                    else skipped++;
                }
            } catch (Exception e) {
                log.error("유휴 달러 감지 오류 [accountId={}]: {}", account.getId(), e.getMessage());
            }
        }

        log.info("유휴 달러 감지 완료: 무효화={}, 신규 트리거={}, 이미 트리거 중(스킵)={}", invalidated, triggered, skipped);
    }

    @Override
    @Transactional
    public void detectAndNotify(Long userId) {
        // TODO: 특정 사용자 계좌 검사 후 알림 발송
        throw new BusinessException(ErrorCode.INTERNAL_ERROR, "아직 구현되지 않은 기능입니다.");
    }

    @Override
    @Transactional(readOnly = true)
    public List<IdleDollarTrigger> getTriggerHistory(Long userId) {
        return triggerRepository.findByUserIdOrderByDetectedAtDesc(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<IdleDollarStatusResponse> getIdleStatus(Long userId) {
        return triggerRepository.findByUserIdAndTriggerStatus(userId, "TRIGGERED")
                .map(trigger -> {
                    int currentIdleDays = resolveLastActivity(trigger.getAccountId())
                            .map(la -> (int) ChronoUnit.DAYS.between(la.toLocalDate(), LocalDate.now(KST)))
                            .orElse(trigger.getIdleDays());
                    return new IdleDollarStatusResponse(
                            true,
                            trigger.getAccountId(),
                            trigger.getIdleBalance(),
                            currentIdleDays,
                            trigger.getDetectedAt()
                    );
                });
    }

    private int invalidateExistingTriggers(Long accountId) {
        return triggerRepository.findByAccountIdAndTriggerStatus(accountId, "TRIGGERED")
                .map(trigger -> {
                    trigger.invalidate();
                    triggerRepository.save(trigger);
                    log.info("유휴 달러 트리거 무효화 [accountId={}]", accountId);
                    return 1;
                })
                .orElse(0);
    }

    private boolean createTriggerIfAbsent(FinancialAccount account, Optional<LocalDateTime> lastActivity) {
        if (triggerRepository.findByAccountIdAndTriggerStatus(account.getId(), "TRIGGERED").isPresent()) {
            return false;
        }

        if (!passesIpoCondition(account.getUserId())) {
            log.info("유휴 달러 트리거 스킵 — IPO 조건 미충족 [userId={}]", account.getUserId());
            return false;
        }

        int idleDays = lastActivity
                .map(la -> (int) ChronoUnit.DAYS.between(la.toLocalDate(), LocalDate.now(KST)))
                .orElse(IDLE_THRESHOLD_DAYS);

        triggerRepository.save(
                IdleDollarTrigger.detect(account.getUserId(), account.getId(), account.getBalance(), idleDays));

        log.info("유휴 달러 트리거 생성 [accountId={}, userId={}, balance={}, idleDays={}]",
                account.getId(), account.getUserId(), account.getBalance(), idleDays);
        return true;
    }

    /**
     * 관심 IPO가 없으면 무조건 통과.
     * 관심 IPO가 있으면 청약 마감일이 14일 이상 남은 항목이 하나라도 있어야 통과.
     */
    private boolean passesIpoCondition(Long userId) {
        if (!favoriteIpoRepository.existsByUserId(userId)) {
            return true;
        }
        LocalDate cutoff = LocalDate.now(KST).plusDays(IDLE_THRESHOLD_DAYS);
        return favoriteIpoRepository.existsFavoriteWithSubscriptionEndAfter(userId, cutoff);
    }

    private Optional<LocalDateTime> resolveLastActivity(Long accountId) {
        Optional<LocalDateTime> latestTrade = tradeOrderRepository.findLatestOrderedAt(accountId);
        Optional<LocalDateTime> latestFx = fxTransactionRepository.findLatestRequestedAt(accountId);
        Optional<LocalDateTime> latestTransfer = transferRepository.findLatestRequestedAt(accountId);

        return Stream.of(latestTrade, latestFx, latestTransfer)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .max(Comparator.naturalOrder());
    }
}