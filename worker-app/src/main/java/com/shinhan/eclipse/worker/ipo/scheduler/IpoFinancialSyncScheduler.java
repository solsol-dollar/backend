package com.shinhan.eclipse.worker.ipo.scheduler;

import com.shinhan.eclipse.worker.ipo.sync.IpoFinancialSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class IpoFinancialSyncScheduler {

    private final IpoFinancialSyncService ipoFinancialSyncService;

    @Scheduled(cron = "0 0 3 1 */3 *")
    public void triggerIpoFinancialSync() {
        log.info("IPO 재무데이터 수집 시작 (SEC EDGAR)");
        try {
            var results = ipoFinancialSyncService.syncAll();
            log.info("IPO 재무데이터 수집 완료: {}", results);
        } catch (Exception e) {
            log.error("IPO 재무데이터 수집 실패", e);
        }
    }
}
