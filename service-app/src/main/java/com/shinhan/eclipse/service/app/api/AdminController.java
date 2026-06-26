package com.shinhan.eclipse.service.app.api;

import com.shinhan.eclipse.service.ipo.internal.FinnhubSyncScheduler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequestMapping("/internal/jobs")
@RequiredArgsConstructor
public class AdminController {

    private final FinnhubSyncScheduler finnhubSyncScheduler;

    /** Finnhub IPO 캘린더 동기화 수동 트리거 */
    @PostMapping("/finnhub-sync")
    public ResponseEntity<Map<String, Object>> triggerFinnhubSync() {
        log.info("[MANUAL] Finnhub IPO 동기화 시작");
        CompletableFuture.runAsync(() -> {
            try {
                int count = finnhubSyncScheduler.sync();
                log.info("[MANUAL] Finnhub IPO 동기화 완료: {}건 신규", count);
            } catch (Exception e) {
                log.error("[MANUAL] Finnhub IPO 동기화 실패", e);
            }
        });
        return ResponseEntity.accepted().body(Map.of("status", "started", "job", "finnhub-sync"));
    }
}
