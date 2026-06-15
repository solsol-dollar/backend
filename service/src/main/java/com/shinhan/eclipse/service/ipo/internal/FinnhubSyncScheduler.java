package com.shinhan.eclipse.service.ipo.internal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
class FinnhubSyncScheduler {

    // IPO 캘린더 동기화 (단순 upsert — @Scheduled 로 충분)
    // 뉴스 수집 + AI 요약은 worker-app의 IpoNewsSyncJob 이 담당
    @Scheduled(cron = "0 0 6 * * *")
    public void syncIpoCalendar() {
        // TODO: Finnhub /calendar/ipo API 호출 → ipos 테이블 upsert
        log.info("Finnhub IPO calendar sync triggered");
    }
}
