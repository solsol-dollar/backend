package com.shinhan.eclipse.worker.admin;

import com.shinhan.eclipse.worker.allocation.IpoAllocationJob;
import com.shinhan.eclipse.worker.allocation.IpoListingJob;
import com.shinhan.eclipse.worker.ipo.sync.IpoFinancialSyncService;
import com.shinhan.eclipse.worker.candle.CandleSyncService;
import com.shinhan.eclipse.worker.candle.job.CandleGapFillJob;
import com.shinhan.eclipse.worker.candle.job.DailyCandleJob;
import com.shinhan.eclipse.worker.settlement.IpoListingCompletionJob;
import com.shinhan.eclipse.worker.settlement.ReturnPlanSettlementJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
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

    private final IpoAllocationJob ipoAllocationJob;
    private final IpoListingJob ipoListingJob;
    private final ReturnPlanSettlementJob returnPlanSettlementJob;
    private final IpoListingCompletionJob ipoListingCompletionJob;
    private final DailyCandleJob dailyCandleJob;
    private final CandleGapFillJob candleGapFillJob;
    private final CandleSyncService candleSyncService;
    private final JobLauncher jobLauncher;
    private final IpoFinancialSyncService ipoFinancialSyncService;

    @Qualifier("ipoNewsFetchOnlyJob")
    private final Job ipoNewsFetchOnlyJob;

    /** IPO 배정 수동 실행 */
    @PostMapping("/allocation")
    public ResponseEntity<Map<String, Object>> triggerAllocation() {
        log.info("[MANUAL] IPO 배정 잡 시작");
        CompletableFuture.runAsync(() -> {
            try {
                ipoAllocationJob.run();
                log.info("[MANUAL] IPO 배정 잡 완료");
            } catch (Exception e) {
                log.error("[MANUAL] IPO 배정 잡 실패", e);
            }
        });
        return ResponseEntity.accepted().body(Map.of("status", "started", "job", "ipo-allocation"));
    }

    /** IPO 상장 입고 수동 실행 */
    @PostMapping("/listing")
    public ResponseEntity<Map<String, Object>> triggerListing() {
        if (ipoListingJob.isRunning()) {
            log.warn("[MANUAL] IPO 입고 잡 이미 실행 중 — 요청 거절");
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("status", "already-running", "job", "ipo-listing"));
        }
        log.info("[MANUAL] IPO 입고 잡 시작");
        CompletableFuture.runAsync(() -> {
            try {
                ipoListingJob.run();
                log.info("[MANUAL] IPO 입고 잡 완료");
            } catch (Exception e) {
                log.error("[MANUAL] IPO 입고 잡 실패", e);
            }
        });
        return ResponseEntity.accepted().body(Map.of("status", "started", "job", "ipo-listing"));
    }

    /** 리턴플랜 정산 수동 실행 */
    @PostMapping("/settlement")
    public ResponseEntity<Map<String, Object>> triggerSettlement() {
        if (returnPlanSettlementJob.isRunning()) {
            log.warn("[MANUAL] 리턴플랜 정산 잡 이미 실행 중 — 요청 거절");
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("status", "already-running", "job", "return-plan-settlement"));
        }
        log.info("[MANUAL] 리턴플랜 정산 잡 시작");
        CompletableFuture.runAsync(() -> {
            try {
                returnPlanSettlementJob.run();
                log.info("[MANUAL] 리턴플랜 정산 잡 완료");
            } catch (Exception e) {
                log.error("[MANUAL] 리턴플랜 정산 잡 실패", e);
            }
        });
        return ResponseEntity.accepted().body(Map.of("status", "started", "job", "return-plan-settlement"));
    }

    /** 단건: 특정 IPO만 배정 (QA용) */
    @PostMapping("/allocation/{ipoId}")
    public ResponseEntity<Map<String, Object>> triggerAllocationForIpo(@PathVariable("ipoId") Long ipoId) {
        log.info("[MANUAL] 단건 IPO 배정 시작: ipoId={}", ipoId);
        try {
            int allocated = ipoAllocationJob.runForIpo(ipoId);
            return ResponseEntity.ok(Map.of("status", "completed", "job", "ipo-allocation", "ipoId", ipoId, "allocated", allocated));
        } catch (Exception e) {
            log.error("[MANUAL] 단건 IPO 배정 실패: ipoId={}", ipoId, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("status", "failed", "ipoId", ipoId, "error", String.valueOf(e.getMessage())));
        }
    }

    /** 단건: 특정 IPO만 입고 (QA용) */
    @PostMapping("/listing/{ipoId}")
    public ResponseEntity<Map<String, Object>> triggerListingForIpo(@PathVariable("ipoId") Long ipoId) {
        log.info("[MANUAL] 단건 IPO 입고 시작: ipoId={}", ipoId);
        try {
            int delivered = ipoListingJob.runForIpo(ipoId);
            return ResponseEntity.ok(Map.of("status", "completed", "job", "ipo-listing", "ipoId", ipoId, "delivered", delivered));
        } catch (Exception e) {
            log.error("[MANUAL] 단건 IPO 입고 실패: ipoId={}", ipoId, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("status", "failed", "ipoId", ipoId, "error", String.valueOf(e.getMessage())));
        }
    }

    /** 단건: 특정 리턴플랜만 실행 (QA용) */
    @PostMapping("/settlement/{returnPlanId}")
    public ResponseEntity<Map<String, Object>> triggerSettlementForPlan(@PathVariable("returnPlanId") Long returnPlanId) {
        log.info("[MANUAL] 단건 리턴플랜 실행 시작: returnPlanId={}", returnPlanId);
        try {
            boolean executed = returnPlanSettlementJob.executeOne(returnPlanId);
            return ResponseEntity.ok(Map.of("status", executed ? "completed" : "skipped",
                    "job", "return-plan-settlement", "returnPlanId", returnPlanId));
        } catch (Exception e) {
            log.error("[MANUAL] 단건 리턴플랜 실행 실패: returnPlanId={}", returnPlanId, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("status", "failed", "returnPlanId", returnPlanId, "error", String.valueOf(e.getMessage())));
        }
    }

    /** LISTED이지만 product_id 미연결인 IPO 보정 */
    @PostMapping("/listing-completion/link-products")
    public ResponseEntity<Map<String, Object>> triggerLinkMissingProducts() {
        log.info("[MANUAL] LISTED IPO product_id 보정 시작");
        try {
            int linked = ipoListingCompletionJob.linkMissingProducts();
            return ResponseEntity.ok(Map.of("status", "completed", "linked", linked));
        } catch (Exception e) {
            log.error("[MANUAL] LISTED IPO product_id 보정 실패", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("status", "failed", "error", e.getMessage()));
        }
    }

    /** IPO 상장완료 상태 전환 수동 실행 */
    @PostMapping("/listing-completion")
    public ResponseEntity<Map<String, Object>> triggerListingCompletion() {
        if (ipoListingCompletionJob.isRunning()) {
            log.warn("[MANUAL] IPO 상장완료 전환 잡 이미 실행 중 — 요청 거절");
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("status", "already-running", "job", "ipo-listing-completion"));
        }
        log.info("[MANUAL] IPO 상장완료 전환 잡 시작");
        CompletableFuture.runAsync(() -> {
            try {
                ipoListingCompletionJob.run();
                log.info("[MANUAL] IPO 상장완료 전환 잡 완료");
            } catch (Exception e) {
                log.error("[MANUAL] IPO 상장완료 전환 잡 실패", e);
            }
        });
        return ResponseEntity.accepted().body(Map.of("status", "started", "job", "ipo-listing-completion"));
    }

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

    /** IPO 재무데이터 수집 (SEC EDGAR 424B4/S-1/F-1/20-F 파싱) */
    @PostMapping("/ipo-financials-sync")
    public ResponseEntity<Map<String, Object>> triggerIpoFinancialSync() {
        log.info("[MANUAL] IPO 재무데이터 수집 시작");
        CompletableFuture.runAsync(() -> {
            try {
                Map<String, String> results = ipoFinancialSyncService.syncAll();
                log.info("[MANUAL] IPO 재무데이터 수집 완료: {}", results);
            } catch (Exception e) {
                log.error("[MANUAL] IPO 재무데이터 수집 실패", e);
            }
        });
        return ResponseEntity.accepted().body(Map.of("status", "started", "job", "ipo-financials-sync"));
    }

    /** 캔들 Gap Fill — 종목별 누락 구간만 적재 */
    @PostMapping("/candle/gap-fill")
    public ResponseEntity<Map<String, Object>> triggerGapFill(
            @RequestParam(required = false) String tickers) {
        log.info("[MANUAL] 캔들 Gap Fill 시작: tickers={}", tickers != null ? tickers : "ALL");
        CompletableFuture.runAsync(() -> {
            try {
                var products = candleSyncService.findActiveProducts();
                if (tickers != null && !tickers.isBlank()) {
                    var filter = Set.of(tickers.toUpperCase().split(","));
                    products = products.stream()
                            .filter(p -> filter.contains(p.getTicker()))
                            .toList();
                }
                log.info("[MANUAL] Gap Fill 대상 종목: {}개", products.size());
                candleGapFillJob.run(products);
                log.info("[MANUAL] 캔들 Gap Fill 완료");
            } catch (Exception e) {
                log.error("[MANUAL] 캔들 Gap Fill 실패", e);
            }
        });
        return ResponseEntity.accepted().body(Map.of("status", "started", "job", "candle-gap-fill"));
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
