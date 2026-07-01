package com.shinhan.eclipse.worker.allocation;

import com.shinhan.eclipse.domain.ipo.Ipo;
import com.shinhan.eclipse.worker.allocation.repository.WorkerIpoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 상장일 개장(22:30 KST, EDT 기준 9:30 AM ET)에 배정된 주식을 보유 계좌에 입고시킨다.
 * IpoAllocationJob(21:30 KST)이 먼저 실행돼 resultStatus = "COMPLETED"를 기록한 뒤
 * 이 Job이 실행된다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IpoListingJob {

    private final WorkerIpoRepository ipoRepository;
    private final IpoListingDeliveryService deliveryService;

    private static final ZoneId NEW_YORK = ZoneId.of("America/New_York");
    private final AtomicBoolean running = new AtomicBoolean(false);

    /** 이미 실행 중이면 true. 수동 트리거가 중복 실행을 막기 위해 사용. */
    public boolean isRunning() {
        return running.get();
    }

    private boolean isNewYorkInDst() {
        return NEW_YORK.getRules().isDaylightSavings(Instant.now());
    }

    /** EDT(써머타임): 개장 = 22:30 KST (9:30 AM ET) */
    @Scheduled(cron = "0 30 22 * * *", zone = "Asia/Seoul")
    public void runEdt() {
        if (isNewYorkInDst()) run();
    }

    /** EST(겨울): 개장 = 23:30 KST (9:30 AM ET) */
    @Scheduled(cron = "0 30 23 * * *", zone = "Asia/Seoul")
    public void runEst() {
        if (!isNewYorkInDst()) run();
    }

    /** 특정 IPO 한 건만 입고 (QA 단건 트리거용). deliverForIpo는 별도 빈이라 @Transactional이 적용된다. */
    public int runForIpo(Long ipoId) {
        Ipo ipo = ipoRepository.findById(ipoId)
                .orElseThrow(() -> new IllegalArgumentException("IPO를 찾을 수 없습니다: ipoId=" + ipoId));
        if (ipo.getProductId() == null) {
            throw new IllegalStateException("productId가 없어 입고할 수 없습니다: ipoId=" + ipoId);
        }
        return deliveryService.deliverForIpo(ipo);
    }

    public void run() {
        if (!running.compareAndSet(false, true)) {
            log.warn("IpoListingJob 이미 실행 중 — 이번 실행은 건너뜀");
            return;
        }
        try {
            LocalDate today = LocalDate.now();
            List<Ipo> listingTodayIpos = ipoRepository.findByListingDate(today);
            if (listingTodayIpos.isEmpty()) {
                return;
            }

            log.info("IpoListingJob 시작: 대상 IPO {}건", listingTodayIpos.size());
            int totalDelivered = 0;
            for (Ipo ipo : listingTodayIpos) {
                if (ipo.getProductId() == null) {
                    log.warn("IpoListingJob: productId 없음 — 건너뜀 ipoId={}, ticker={}", ipo.getId(), ipo.getTicker());
                    continue;
                }
                try {
                    totalDelivered += deliveryService.deliverForIpo(ipo);
                } catch (Exception e) {
                    log.error("IpoListingJob 입고 실패: ipoId={}, ticker={}", ipo.getId(), ipo.getTicker(), e);
                }
            }
            log.info("IpoListingJob 완료: {}건 입고", totalDelivered);
        } finally {
            running.set(false);
        }
    }
}
