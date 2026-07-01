package com.shinhan.eclipse.service.app.api;

import com.shinhan.eclipse.common.exception.BusinessException;
import com.shinhan.eclipse.common.exception.ErrorCode;
import com.shinhan.eclipse.common.response.ApiResponse;
import com.shinhan.eclipse.service.card.SpendingReportService;
import com.shinhan.eclipse.service.ipo.internal.FinnhubSyncScheduler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequestMapping("/api/v1/internal/jobs")
@RequiredArgsConstructor
public class AdminController {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final FinnhubSyncScheduler finnhubSyncScheduler;
    private final SpendingReportService spendingReportService;

    /**
     * worker-app 내부 잡 트리거 프록시용 베이스 URL. QA 데모 전용 — 운영 노출 금지.
     * 도커 네트워크에서 worker-app 서비스명으로 접근(설정 파일 변경 없이 하드코딩).
     */
    private static final String WORKER_URL = "http://worker-app:8082";

    /** Finnhub IPO 캘린더 동기화 수동 트리거 */
    @PostMapping("/finnhub-sync")
    public ResponseEntity<ApiResponse<Void>> triggerFinnhubSync() {
        log.info("[MANUAL] Finnhub IPO 동기화 시작");
        CompletableFuture.runAsync(() -> {
            try {
                int count = finnhubSyncScheduler.sync();
                log.info("[MANUAL] Finnhub IPO 동기화 완료: {}건 신규", count);
            } catch (Exception e) {
                log.error("[MANUAL] Finnhub IPO 동기화 실패", e);
            }
        });
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /** 소비 리포트 알림 수동 트리거 */
    @PostMapping("/spending-report")
    public ResponseEntity<ApiResponse<Void>> triggerSpendingReport(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        if ((year == null) != (month == null)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "year, month 둘 다 입력하거나 둘 다 생략해야 합니다.");
        }
        if (month != null && (month < 1 || month > 12)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "month는 1~12 사이여야 합니다.");
        }
        YearMonth target = (year != null)
                ? YearMonth.of(year, month)
                : YearMonth.now(KST).minusMonths(1);
        log.info("[MANUAL] 소비 리포트 발송 시작: {}년 {}월", target.getYear(), target.getMonthValue());
        CompletableFuture.runAsync(() -> {
            try {
                spendingReportService.generateAll(target.getYear(), target.getMonthValue());
            } catch (Exception e) {
                log.error("[MANUAL] 소비 리포트 발송 실패", e);
            }
        });
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // ───────────────────────────────────────────────────────────────
    // worker-app 잡 트리거 프록시 (QA 데모 전용). worker-app은 ALB 미노출이라
    // service-app을 경유해 /internal/jobs/* 를 호출한다. 인증 가드 없음 — 운영 금지.
    // ───────────────────────────────────────────────────────────────

    /** 청약 배정 잡 (worker /internal/jobs/allocation) */
    @PostMapping("/allocation")
    public ResponseEntity<ApiResponse<Object>> triggerAllocation() {
        return forwardToWorker("/internal/jobs/allocation");
    }

    /** IPO 상장 입고 잡 — resultStatus DEPOSITED(입고완료) 전환 (worker /internal/jobs/listing) */
    @PostMapping("/listing")
    public ResponseEntity<ApiResponse<Object>> triggerListing() {
        return forwardToWorker("/internal/jobs/listing");
    }

    /** 리턴플랜 정산(실행) 잡 (worker /internal/jobs/settlement) */
    @PostMapping("/settlement")
    public ResponseEntity<ApiResponse<Object>> triggerSettlement() {
        return forwardToWorker("/internal/jobs/settlement");
    }

    private ResponseEntity<ApiResponse<Object>> forwardToWorker(String path) {
        log.info("[QA] worker 잡 트리거 프록시 호출: {}{}", WORKER_URL, path);
        try {
            Object body = RestClient.create()
                    .post()
                    .uri(WORKER_URL + path)
                    .retrieve()
                    .body(Object.class);
            return ResponseEntity.ok(ApiResponse.success(body));
        } catch (Exception e) {
            log.error("[QA] worker 잡 트리거 실패: {}{}", WORKER_URL, path, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.success(Map.of("status", "failed", "error", String.valueOf(e.getMessage()))));
        }
    }
}
