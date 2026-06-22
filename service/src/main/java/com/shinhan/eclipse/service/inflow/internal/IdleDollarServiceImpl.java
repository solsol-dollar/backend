package com.shinhan.eclipse.service.inflow.internal;

import com.shinhan.eclipse.domain.account.FinancialAccount;
import com.shinhan.eclipse.domain.inflow.IdleDollarTrigger;
import com.shinhan.eclipse.service.inflow.IdleDollarService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    private static final int IDLE_THRESHOLD_HOURS = 1;
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final IdleDollarTriggerRepository triggerRepository;
    private final IdleDetectionAccountRepository accountRepository;
    private final IdleDetectionTradeOrderRepository tradeOrderRepository;
    private final IdleDetectionFxTransactionRepository fxTransactionRepository;
    private final IdleDetectionTransferRepository transferRepository;

    @Override
    @Transactional
    public void detectAll() {
        List<FinancialAccount> accounts = accountRepository.findActiveSecuritiesAccountsWithBalance();
        log.info("유휴 달러 감지 시작: 대상 계좌 {}개", accounts.size());

        int invalidated = 0;
        int triggered = 0;
        int skipped = 0;

        LocalDateTime threshold = LocalDateTime.now(KST).minusHours(IDLE_THRESHOLD_HOURS);

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
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public List<IdleDollarTrigger> getTriggerHistory(Long userId) {
        return triggerRepository.findByUserIdOrderByDetectedAtDesc(userId);
    }

    private int invalidateExistingTriggers(Long accountId) {
        List<IdleDollarTrigger> triggers =
                triggerRepository.findByAccountIdAndTriggerStatus(accountId, "TRIGGERED");
        triggers.forEach(IdleDollarTrigger::invalidate);
        triggerRepository.saveAll(triggers);
        if (!triggers.isEmpty()) {
            log.info("유휴 달러 트리거 무효화 [accountId={}, count={}]", accountId, triggers.size());
        }
        return triggers.size();
    }

    private boolean createTriggerIfAbsent(FinancialAccount account, Optional<LocalDateTime> lastActivity) {
        // 이미 TRIGGERED 상태 트리거가 존재하면 재생성하지 않음
        boolean alreadyTriggered = !triggerRepository
                .findByAccountIdAndTriggerStatus(account.getId(), "TRIGGERED").isEmpty();
        if (alreadyTriggered) return false;

        int idleDays = lastActivity
                .map(la -> (int) ChronoUnit.HOURS.between(la, LocalDateTime.now(KST)))
                .orElse(IDLE_THRESHOLD_HOURS);

        triggerRepository.save(
                IdleDollarTrigger.detect(account.getUserId(), account.getId(), account.getBalance(), idleDays));

        log.info("유휴 달러 트리거 생성 [accountId={}, userId={}, balance={}, idleDays={}]",
                account.getId(), account.getUserId(), account.getBalance(), idleDays);
        return true;
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