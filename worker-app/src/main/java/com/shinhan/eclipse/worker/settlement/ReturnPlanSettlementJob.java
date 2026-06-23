package com.shinhan.eclipse.worker.settlement;

import com.shinhan.eclipse.domain.returnplan.ReturnPlan;
import com.shinhan.eclipse.worker.settlement.repository.WorkerReturnPlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

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

    private final WorkerReturnPlanRepository returnPlanRepository;
    private final ReturnPlanExecutionClient executionClient;

    @Scheduled(cron = "0 0 21 * * *", zone = "Asia/Seoul")
    public void run() {
        List<ReturnPlan> targets = returnPlanRepository.findDraftPlansDueForSettlement(LocalDate.now());
        if (targets.isEmpty()) {
            return;
        }

        log.info("ReturnPlanSettlementJob 시작: 대상 {}건", targets.size());
        int executed = 0;
        for (ReturnPlan plan : targets) {
            if (executeWithRetry(plan.getId())) {
                executed++;
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
            } catch (Exception e) {
                if (attempt == MAX_ATTEMPTS) {
                    log.error("리턴 플랜 정산 실행 실패 (재시도 {}회 모두 실패): returnPlanId={}", MAX_ATTEMPTS, returnPlanId, e);
                    return false;
                }
                log.warn("리턴 플랜 정산 실행 실패, 재시도합니다: returnPlanId={}, attempt={}", returnPlanId, attempt);
                sleep(RETRY_BACKOFF.multipliedBy(attempt));
            }
        }
        return false;
    }

    private void sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
