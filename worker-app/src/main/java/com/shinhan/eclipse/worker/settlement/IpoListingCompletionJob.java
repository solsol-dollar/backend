package com.shinhan.eclipse.worker.settlement;

import com.shinhan.eclipse.domain.ipo.Ipo;
import com.shinhan.eclipse.worker.allocation.repository.WorkerIpoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 환불금 분배(refundDate, {@link ReturnPlanSettlementJob})가 끝난 다음날, 해당 IPO를
 * "LISTED"(상장완료) 상태로 전환한다. refundDate가 오늘보다 이전인 건을 모두 대상으로 삼아
 * (단순히 "어제"만 보지 않음) 배치가 하루 놓쳐도 다음 실행에서 자동으로 따라잡게 한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IpoListingCompletionJob {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final String LISTED_STATUS = "LISTED";

    private final WorkerIpoRepository ipoRepository;

    private final AtomicBoolean running = new AtomicBoolean(false);

    /** 이미 실행 중이면 true. 수동 트리거가 중복 실행을 막기 위해 사용. */
    public boolean isRunning() {
        return running.get();
    }

    @Scheduled(cron = "0 0 6 * * *", zone = "Asia/Seoul")
    public void run() {
        if (!running.compareAndSet(false, true)) {
            log.warn("IpoListingCompletionJob 이미 실행 중 — 이번 실행은 건너뜀");
            return;
        }
        try {
            execute(LocalDate.now(KST));
        } finally {
            running.set(false);
        }
    }

    void execute(LocalDate today) {
        List<Ipo> targets = ipoRepository.findByRefundDateLessThanAndIpoStatusNot(today, LISTED_STATUS);
        if (targets.isEmpty()) {
            return;
        }

        log.info("IpoListingCompletionJob 시작: 대상 {}건", targets.size());
        for (Ipo ipo : targets) {
            ipo.markAsListed();
        }
        ipoRepository.saveAll(targets);
        log.info("IpoListingCompletionJob 완료: {}건 LISTED 전환", targets.size());
    }
}
