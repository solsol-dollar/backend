package com.shinhan.eclipse.ledger.returnplan;

import com.shinhan.eclipse.common.exception.BusinessException;
import com.shinhan.eclipse.common.exception.ErrorCode;
import com.shinhan.eclipse.common.resolver.UserHeader;
import com.shinhan.eclipse.common.response.ApiResponse;
import com.shinhan.eclipse.domain.ipo.Ipo;
import com.shinhan.eclipse.domain.returnplan.ReturnPlan;
import com.shinhan.eclipse.domain.returnplan.ReturnPlanAllocation;
import com.shinhan.eclipse.domain.subscription.IpoSubscription;
import com.shinhan.eclipse.ledger.returnplan.dto.ReturnPlanCreateReq;
import com.shinhan.eclipse.ledger.returnplan.dto.ReturnPlanListItemRes;
import com.shinhan.eclipse.ledger.returnplan.dto.ReturnPlanListRes;
import com.shinhan.eclipse.ledger.returnplan.dto.ReturnPlanPresetReq;
import com.shinhan.eclipse.ledger.returnplan.dto.ReturnPlanPresetRes;
import com.shinhan.eclipse.ledger.returnplan.dto.ReturnPlanRes;
import com.shinhan.eclipse.ledger.returnplan.dto.ReturnPlanSummaryRes;
import com.shinhan.eclipse.ledger.returnplan.dto.ReturnPlanUpdateReq;
import com.shinhan.eclipse.ledger.subscription.SubscriptionFacade;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/return-plans")
@RequiredArgsConstructor
public class ReturnPlanController {

    private static final Logger log = LoggerFactory.getLogger(ReturnPlanController.class);

    private final ReturnPlanFacade returnPlanFacade;
    private final SubscriptionFacade subscriptionFacade;

    // RP-001
    @PostMapping
    public ResponseEntity<ApiResponse<ReturnPlanRes>> createReturnPlan(
            @UserHeader Long userId,
            @Valid @RequestBody ReturnPlanCreateReq request) {
        log.info("리턴 플랜 생성 요청: userId={}, subscriptionId={}", userId, request.subscriptionId());
        ReturnPlan plan = returnPlanFacade.createReturnPlan(userId, request.subscriptionId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(toRes(plan, userId)));
    }

    // RP-002
    @PutMapping("/{returnPlanId}")
    public ResponseEntity<ApiResponse<ReturnPlanRes>> updateRatios(
            @UserHeader Long userId,
            @PathVariable("returnPlanId") Long returnPlanId,
            @Valid @RequestBody ReturnPlanUpdateReq request) {
        ReturnPlan plan = returnPlanFacade.updateRatios(returnPlanId, userId, request.allocations());
        return ResponseEntity.ok(ApiResponse.success(toRes(plan, userId)));
    }

    // 단건 조회 (명세 외 추가) — 수정/상세 화면이 마운트 시 현재 비율을 불러오기 위해 필요
    @GetMapping("/{returnPlanId}")
    public ResponseEntity<ApiResponse<ReturnPlanRes>> getReturnPlan(
            @UserHeader Long userId,
            @PathVariable("returnPlanId") Long returnPlanId) {
        ReturnPlan plan = returnPlanFacade.getReturnPlan(returnPlanId, userId);
        return ResponseEntity.ok(ApiResponse.success(toRes(plan, userId)));
    }

    // RP-004 (from/to/status: 명세 외 추가 — 조회 조건 설정 모달용)
    @GetMapping
    public ResponseEntity<ApiResponse<ReturnPlanListRes>> getReturnPlans(
            @UserHeader Long userId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "from", required = false) LocalDate from,
            @RequestParam(name = "to", required = false) LocalDate to,
            @RequestParam(name = "status", required = false) String status) {
        if (page < 0 || size < 1 || size > 100) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "page는 0 이상, size는 1~100 사이여야 합니다.");
        }
        if (from != null && to != null && from.isAfter(to)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "from은 to보다 늦을 수 없습니다.");
        }
        Pageable pageable = PageRequest.of(page, size);
        Page<ReturnPlan> plans = returnPlanFacade.getReturnPlans(userId, from, to, status, pageable);
        List<ReturnPlanListItemRes> items = plans.getContent().stream()
                .map(plan -> ReturnPlanListItemRes.from(plan, getSourceIpo(plan, userId)))
                .toList();
        return ResponseEntity.ok(ApiResponse.success(ReturnPlanListRes.builder().returnPlans(items).build()));
    }

    // RP-005 (명세 외 추가): 분배 프리셋 목록 (return_plan_presets 테이블 기반)
    @GetMapping("/presets")
    public ResponseEntity<ApiResponse<List<ReturnPlanPresetRes>>> getPresets() {
        List<ReturnPlanPresetRes> presets = returnPlanFacade.getPresets().stream()
                .map(ReturnPlanPresetRes::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(presets));
    }

    // RP-006 (명세 외 추가): 프리셋 적용
    @PutMapping("/{returnPlanId}/preset")
    public ResponseEntity<ApiResponse<ReturnPlanRes>> applyPreset(
            @UserHeader Long userId,
            @PathVariable("returnPlanId") Long returnPlanId,
            @Valid @RequestBody ReturnPlanPresetReq request) {
        ReturnPlan plan = returnPlanFacade.applyPreset(returnPlanId, userId, request.presetCode());
        return ResponseEntity.ok(ApiResponse.success(toRes(plan, userId)));
    }

    // RP-007 (명세 외 추가): 리턴플랜 대시보드 요약
    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<ReturnPlanSummaryRes>> getSummary(@UserHeader Long userId) {
        List<ReturnPlan> plans = returnPlanFacade.getAllReturnPlans(userId);
        Map<Long, List<ReturnPlanAllocation>> allocationsByPlanId = returnPlanFacade.getAllocationsByPlanIds(
                plans.stream().map(ReturnPlan::getId).toList());
        return ResponseEntity.ok(ApiResponse.success(ReturnPlanSummaryRes.of(plans,
                id -> allocationsByPlanId.getOrDefault(id, List.of()),
                subscriptionFacade.findNextUpcomingIpo().orElse(null))));
    }

    private ReturnPlanRes toRes(ReturnPlan plan, Long userId) {
        List<ReturnPlanAllocation> allocations = returnPlanFacade.getAllocations(plan.getId());
        Ipo nextIpo = plan.getNextIpoId() == null ? null : subscriptionFacade.getIpo(plan.getNextIpoId());
        IpoSubscription sourceSubscription = subscriptionFacade.getSubscriptionResult(plan.getSubscriptionId(), userId);
        Ipo sourceIpo = subscriptionFacade.getIpo(sourceSubscription.getIpoId());
        return ReturnPlanRes.of(plan, allocations, nextIpo, sourceIpo, sourceSubscription);
    }

    private Ipo getSourceIpo(ReturnPlan plan, Long userId) {
        IpoSubscription subscription = subscriptionFacade.getSubscriptionResult(plan.getSubscriptionId(), userId);
        return subscriptionFacade.getIpo(subscription.getIpoId());
    }
}
