package com.shinhan.eclipse.service.app.api;

import com.shinhan.eclipse.service.card.SpendingReportService;
import com.shinhan.eclipse.service.ipo.internal.FinnhubSyncScheduler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequestMapping("/internal/jobs")
@RequiredArgsConstructor
public class AdminController {

    private final FinnhubSyncScheduler finnhubSyncScheduler;
    private final SpendingReportService spendingReportService;

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

    /** 소비 리포트 알림 수동 트리거 */
    @PostMapping("/spending-report")
    public ResponseEntity<Map<String, Object>> triggerSpendingReport(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        if ((year == null) != (month == null)) {
            return ResponseEntity.badRequest().body(Map.of("error", "year, month 둘 다 입력하거나 둘 다 생략해야 합니다."));
        }
        if (month != null && (month < 1 || month > 12)) {
            return ResponseEntity.badRequest().body(Map.of("error", "month는 1~12 사이여야 합니다."));
        }
        YearMonth target = (year != null)
                ? YearMonth.of(year, month)
                : YearMonth.now().minusMonths(1);
        log.info("[MANUAL] 소비 리포트 발송 시작: {}년 {}월", target.getYear(), target.getMonthValue());
        CompletableFuture.runAsync(() -> {
            try {
                spendingReportService.generateAll(target.getYear(), target.getMonthValue());
            } catch (Exception e) {
                log.error("[MANUAL] 소비 리포트 발송 실패", e);
            }
        });
        return ResponseEntity.accepted().body(Map.of(
                "status", "started",
                "job", "spending-report",
                "year", target.getYear(),
                "month", target.getMonthValue()
        ));
    }
}
