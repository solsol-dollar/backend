package com.shinhan.eclipse.worker.admin;

import com.shinhan.eclipse.worker.candle.CandleSyncService;
import com.shinhan.eclipse.worker.candle.job.DailyCandleJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequestMapping("/internal/jobs")
@RequiredArgsConstructor
public class JobTriggerController {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final DailyCandleJob dailyCandleJob;
    private final CandleSyncService candleSyncService;
    private final JobLauncher jobLauncher;

    @Qualifier("ipoNewsFetchOnlyJob")
    private final Job ipoNewsFetchOnlyJob;

    /** IPO 뉴스 수집 Spring Batch 잡 */
    @PostMapping("/ipo-news-sync")
    public ResponseEntity<Map<String, Object>> triggerIpoNewsSync() {
        log.info("[MANUAL] IPO 뉴스 수집 잡 시작");
        CompletableFuture.runAsync(() -> {
            try {
                JobParameters params = new JobParametersBuilder()
                        .addLong("runAt", System.currentTimeMillis())
                        .toJobParameters();
                jobLauncher.run(ipoNewsFetchOnlyJob, params);
                log.info("[MANUAL] IPO 뉴스 수집 잡 완료");
            } catch (Exception e) {
                log.error("[MANUAL] IPO 뉴스 수집 잡 실패", e);
            }
        });
        return ResponseEntity.accepted().body(Map.of("status", "started", "job", "ipo-news-sync"));
    }

    /** 일봉 수동 실행 */
    @PostMapping("/candle/daily")
    public ResponseEntity<Map<String, Object>> triggerDailyCandle() {
        log.info("[MANUAL] 일봉 잡 시작");
        CompletableFuture.runAsync(() -> {
            try {
                dailyCandleJob.run();
                log.info("[MANUAL] 일봉 잡 완료");
            } catch (Exception e) {
                log.error("[MANUAL] 일봉 잡 실패", e);
            }
        });
        return ResponseEntity.accepted().body(Map.of("status", "started", "job", "candle-daily"));
    }

    /** 5년치 캔들 전체 백필 */
    @PostMapping("/candle/backfill")
    public ResponseEntity<Map<String, Object>> triggerBackfill(
            @RequestParam(required = false) String tickers) {
        log.info("[MANUAL] 캔들 백필 시작: tickers={}", tickers != null ? tickers : "ALL");
        CompletableFuture.runAsync(() -> {
            try {
                LocalDate today = LocalDate.now();
                String sdate = today.minusDays(1825).format(DATE_FMT);
                String edate = today.format(DATE_FMT);

                var products = candleSyncService.findActiveProducts();
                if (tickers != null && !tickers.isBlank()) {
                    var filter = Set.of(tickers.toUpperCase().split(","));
                    products = products.stream()
                            .filter(p -> filter.contains(p.getTicker()))
                            .toList();
                }
                log.info("[MANUAL] 백필 대상 종목: {}개", products.size());

                int total = 0;
                for (var product : products) {
                    total += candleSyncService.syncCandlesFull(product,
                            CandleSyncService.GUBUN_DAY, CandleSyncService.TYPE_DAY, sdate, edate, 1000L);
                    total += candleSyncService.syncCandlesFull(product,
                            CandleSyncService.GUBUN_WEEK, CandleSyncService.TYPE_WEEK, sdate, edate, 1000L);
                    total += candleSyncService.syncCandlesFull(product,
                            CandleSyncService.GUBUN_MONTH, CandleSyncService.TYPE_MONTH, sdate, edate, 1000L);
                    Thread.sleep(1000L);
                }
                log.info("[MANUAL] 캔들 백필 완료: 총 {}건", total);
            } catch (Exception e) {
                log.error("[MANUAL] 캔들 백필 실패", e);
            }
        });
        return ResponseEntity.accepted().body(Map.of("status", "started", "job", "candle-backfill"));
    }
}
