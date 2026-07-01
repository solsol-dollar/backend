package com.shinhan.eclipse.service.app.api;

import com.shinhan.eclipse.common.exception.BusinessException;
import com.shinhan.eclipse.common.exception.ErrorCode;
import com.shinhan.eclipse.common.response.ApiResponse;
import com.shinhan.eclipse.service.card.SpendingReportService;
import com.shinhan.eclipse.service.ipo.internal.FinnhubSyncScheduler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
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
     * worker-app 내부 잡 트리거 프록시용 베이스 URL 후보. QA 데모 전용 — 운영 노출 금지.
     * 배포(도커)는 서비스명 worker-app:8082, 로컬은 localhost:8082로 접근.
     * 설정 파일 변경 없이 앞에서부터 연결되는 주소를 사용한다.
     */
    private static final java.util.List<String> WORKER_URLS =
            java.util.List.of("http://worker-app:8082", "http://localhost:8082");

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

    /** 단건: 특정 IPO만 배정 (worker /internal/jobs/allocation/{ipoId}) */
    @PostMapping("/allocation/{ipoId}")
    public ResponseEntity<ApiResponse<Object>> triggerAllocationForIpo(@PathVariable("ipoId") Long ipoId) {
        return forwardToWorker("/internal/jobs/allocation/" + ipoId);
    }

    /** 단건: 특정 IPO만 입고(입고완료) (worker /internal/jobs/listing/{ipoId}) */
    @PostMapping("/listing/{ipoId}")
    public ResponseEntity<ApiResponse<Object>> triggerListingForIpo(@PathVariable("ipoId") Long ipoId) {
        return forwardToWorker("/internal/jobs/listing/" + ipoId);
    }

    /** 단건: 특정 리턴플랜만 실행 (worker /internal/jobs/settlement/{returnPlanId}) */
    @PostMapping("/settlement/{returnPlanId}")
    public ResponseEntity<ApiResponse<Object>> triggerSettlementForPlan(@PathVariable("returnPlanId") Long returnPlanId) {
        return forwardToWorker("/internal/jobs/settlement/" + returnPlanId);
    }

    private ResponseEntity<ApiResponse<Object>> forwardToWorker(String path) {
        Exception lastError = null;
        for (String baseUrl : WORKER_URLS) {
            log.info("[QA] worker 잡 트리거 프록시 호출 시도: {}{}", baseUrl, path);
            try {
                Object body = RestClient.create()
                        .post()
                        .uri(baseUrl + path)
                        .retrieve()
                        .body(Object.class);
                return ResponseEntity.ok(ApiResponse.success(body));
            } catch (org.springframework.web.client.RestClientResponseException e) {
                // worker가 응답은 했으나 4xx/5xx (예: IPO 없음) — 다음 후보로 넘기지 않고 그대로 반환
                log.warn("[QA] worker 잡 응답 오류: {}{} status={}", baseUrl, path, e.getStatusCode());
                return ResponseEntity.status(e.getStatusCode())
                        .body(ApiResponse.success(Map.of("status", "worker-error", "body", e.getResponseBodyAsString())));
            } catch (Exception e) {
                // 연결 실패(주소 미해결 등) — 다음 후보 주소로 재시도
                log.warn("[QA] worker 연결 실패, 다음 후보 시도: {}{} ({})", baseUrl, path, e.getMessage());
                lastError = e;
            }
        }
        log.error("[QA] worker 잡 트리거 전체 실패: {}", path, lastError);
        return ResponseEntity.internalServerError()
                .body(ApiResponse.success(Map.of(
                        "status", "failed",
                        "error", lastError != null ? String.valueOf(lastError.getMessage()) : "unknown")));
    }
}
