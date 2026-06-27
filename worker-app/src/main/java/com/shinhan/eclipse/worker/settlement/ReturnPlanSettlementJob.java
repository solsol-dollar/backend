package com.shinhan.eclipse.worker.settlement;

import com.shinhan.eclipse.domain.notification.Notification;
import com.shinhan.eclipse.domain.returnplan.ReturnPlan;
import com.shinhan.eclipse.domain.returnplan.ReturnPlanAllocation;
import com.shinhan.eclipse.domain.subscription.IpoSubscription;
import com.shinhan.eclipse.worker.allocation.repository.WorkerNotificationRepository;
import com.shinhan.eclipse.worker.settlement.repository.WorkerReturnPlanAllocationRepository;
import com.shinhan.eclipse.worker.settlement.repository.WorkerReturnPlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 환불일(상장일+1일) 정산 배치. "DRAFT" 상태인 리턴 플랜 중 출처 청약의 IPO refundDate가 오늘이거나
 * 지난 것들을 찾아 ledger-app의 내부 실행 API({@code PUT /internal/return-plans/{id}/execute})를 호출한다.
 * refundDate가 지난 건도 포함하는 이유: 직전 배치 실행이 통째로 실패했을 때 다음날 자동으로 재시도되게 하는
 * 안전망이다 (그날만 찾으면 하루 놓치면 영원히 안 실행됨).
 *
 * <p>매일 한국시간 21:00 1회 실행. 리턴 플랜 수정 마감(20:00, {@code ReturnPlanFacadeImpl.EDIT_CUTOFF_TIME})
 * 보다 1시간 뒤로 잡아, 사용자가 환불일 당일 저녁까지 비율을 조정할 시간을 충분히 준 다음 실행한다.
 * 실행 자체(기본값 적용, 계좌 적립, 락)는 ledger-app 쪽 비즈니스 로직을 그대로 재사용하므로 여기서는
 * "오늘 실행해야 할 대상을 찾아 호출"하는 역할만 한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReturnPlanSettlementJob {

    private static final int MAX_ATTEMPTS = 3;
    private static final Duration RETRY_BACKOFF = Duration.ofSeconds(1);
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final WorkerReturnPlanRepository returnPlanRepository;
    private final WorkerReturnPlanAllocationRepository allocationRepository;
    private final ReturnPlanExecutionClient executionClient;
    private final WorkerNotificationRepository notificationRepository;

    private static final List<String> DESTINATION_TYPES = List.of("SECURITIES", "SAVINGS", "DEPOSIT");

    @Scheduled(cron = "0 0 21 * * *", zone = "Asia/Seoul")
    public void run() {
        LocalDate today = LocalDate.now(KST);
        autoCreateMissingReturnPlans(today);
        List<ReturnPlan> targets = returnPlanRepository.findDraftPlansDueForSettlement(today);
        if (targets.isEmpty()) {
            return;
        }

        log.info("ReturnPlanSettlementJob 시작: 대상 {}건", targets.size());

        List<Long> subscriptionIds = targets.stream().map(ReturnPlan::getSubscriptionId).toList();
        Map<Long, String> companyBySubscriptionId = returnPlanRepository.findCompanyNamesBySubscriptionIds(subscriptionIds)
                .stream().collect(Collectors.toMap(row -> (Long) row[0], row -> (String) row[1]));

        int executed = 0;
        for (ReturnPlan plan : targets) {
            if (executeWithRetry(plan.getId())) {
                executed++;
                String companyName = companyBySubscriptionId.getOrDefault(plan.getSubscriptionId(), "IPO");
                notificationRepository.save(Notification.create(
                        plan.getUserId(),
                        "IPO_REFUND",
                        "리턴 플랜 완료",
                        companyName + " 청약 환불금 $" + plan.getTotalRefundAmount().stripTrailingZeros().toPlainString() + "가 리턴 플랜되었어요!",
                        "RETURN_PLAN", plan.getId()
                ));
            }
        }
        log.info("ReturnPlanSettlementJob 완료: {}건 실행", executed);
    }

    /** 일시적인 네트워크/타임아웃 오류에 대비해 짧은 backoff로 몇 번 더 시도한다. */
    private boolean executeWithRetry(Long returnPlanId) {
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                executionClient.execute(returnPlanId);
                return true;
            } catch (HttpClientErrorException e) {
                // 409(이미 실행됨)는 직전 시도가 사실은 성공했는데 응답만 못 받은 경우와 같은 결과이므로
                // 실패로 보지 않고 그대로 성공 처리한다 — 재시도/실패 로그가 불필요한 잡음이 된다.
                if (e.getStatusCode() == HttpStatus.CONFLICT) {
                    return true;
                }
                if (!retryOrFail(returnPlanId, attempt, e)) {
                    return false;
                }
            } catch (Exception e) {
                if (!retryOrFail(returnPlanId, attempt, e)) {
                    return false;
                }
            }
        }
        return false;
    }

    /** @return 재시도를 이어가도 되면 true, 마지막 시도까지 실패해서 더 이상 재시도하지 않으면 false */
    private boolean retryOrFail(Long returnPlanId, int attempt, Exception e) {
        if (attempt == MAX_ATTEMPTS) {
            log.error("리턴 플랜 정산 실행 실패 (재시도 {}회 모두 실패): returnPlanId={}", MAX_ATTEMPTS, returnPlanId, e);
            return false;
        }
        log.warn("리턴 플랜 정산 실행 실패, 재시도합니다: returnPlanId={}, attempt={}", returnPlanId, attempt);
        sleep(RETRY_BACKOFF.multipliedBy(attempt));
        return true;
    }

    private void sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 기한 내에 리턴 플랜을 생성하지 않은 청약에 대해 SECURITIES 100% 기본 플랜을 자동 생성한다.
     * 이후 기존 정산 배치가 DRAFT 플랜을 찾아 실행하므로 별도 실행 로직은 불필요.
     */
    @Transactional
    void autoCreateMissingReturnPlans(LocalDate today) {
        List<IpoSubscription> targets = returnPlanRepository.findSubscriptionsNeedingAutoReturnPlan(today);
        if (targets.isEmpty()) {
            return;
        }

        log.info("리턴 플랜 자동 생성 대상: {}건", targets.size());
        for (IpoSubscription subscription : targets) {
            try {
                ReturnPlan plan = ReturnPlan.create(
                        subscription.getUserId(),
                        subscription.getId(),
                        subscription.getRefundAmount(),
                        null,
                        null,
                        null);
                ReturnPlan saved = returnPlanRepository.save(plan);

                // SECURITIES 100%, 나머지 0%로 allocation 생성
                for (String type : DESTINATION_TYPES) {
                    ReturnPlanAllocation allocation = ReturnPlanAllocation.initZero(saved.getId(), type);
                    allocationRepository.save(allocation);
                }
                allocationRepository.findByReturnPlanIdAndDestinationType(saved.getId(), "SECURITIES")
                        .ifPresent(a -> a.updateRatio(100, subscription.getRefundAmount()));

                log.info("리턴 플랜 자동 생성: returnPlanId={}, subscriptionId={}, refundAmount={}",
                        saved.getId(), subscription.getId(), subscription.getRefundAmount());
            } catch (Exception e) {
                log.error("리턴 플랜 자동 생성 실패: subscriptionId={}", subscription.getId(), e);
            }
        }
    }
}
