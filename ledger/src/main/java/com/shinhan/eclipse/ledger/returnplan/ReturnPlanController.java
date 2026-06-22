package com.shinhan.eclipse.ledger.returnplan;

import com.shinhan.eclipse.common.resolver.UserHeader;
import com.shinhan.eclipse.common.response.ApiResponse;
import com.shinhan.eclipse.domain.ipo.Ipo;
import com.shinhan.eclipse.domain.returnplan.ReturnPlan;
import com.shinhan.eclipse.domain.returnplan.ReturnPlanAllocation;
import com.shinhan.eclipse.domain.subscription.IpoSubscription;
import com.shinhan.eclipse.ledger.returnplan.dto.ReturnPlanConfirmRes;
import com.shinhan.eclipse.ledger.returnplan.dto.ReturnPlanCreateReq;
import com.shinhan.eclipse.ledger.returnplan.dto.ReturnPlanListItemRes;
import com.shinhan.eclipse.ledger.returnplan.dto.ReturnPlanListRes;
import com.shinhan.eclipse.ledger.returnplan.dto.ReturnPlanRes;
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

import java.util.List;

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

    // RP-003
    @PutMapping("/{returnPlanId}/confirm")
    public ResponseEntity<ApiResponse<ReturnPlanConfirmRes>> confirmReturnPlan(
            @UserHeader Long userId,
            @PathVariable("returnPlanId") Long returnPlanId) {
        ReturnPlan plan = returnPlanFacade.confirmReturnPlan(returnPlanId, userId);
        return ResponseEntity.ok(ApiResponse.success(ReturnPlanConfirmRes.from(plan)));
    }

    // RP-004
    @GetMapping
    public ResponseEntity<ApiResponse<ReturnPlanListRes>> getReturnPlans(
            @UserHeader Long userId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ReturnPlan> plans = returnPlanFacade.getReturnPlans(userId, pageable);
        List<ReturnPlanListItemRes> items = plans
                .map(plan -> ReturnPlanListItemRes.from(plan, getSourceIpo(plan, userId)))
                .getContent();
        return ResponseEntity.ok(ApiResponse.success(ReturnPlanListRes.builder().returnPlans(items).build()));
    }

    private ReturnPlanRes toRes(ReturnPlan plan, Long userId) {
        List<ReturnPlanAllocation> allocations = returnPlanFacade.getAllocations(plan.getId());
        Ipo nextIpo = plan.getNextIpoId() == null ? null : subscriptionFacade.getIpo(plan.getNextIpoId());
        Ipo sourceIpo = getSourceIpo(plan, userId);
        return ReturnPlanRes.of(plan, allocations, nextIpo, sourceIpo);
    }

    private Ipo getSourceIpo(ReturnPlan plan, Long userId) {
        IpoSubscription subscription = subscriptionFacade.getSubscriptionResult(plan.getSubscriptionId(), userId);
        return subscriptionFacade.getIpo(subscription.getIpoId());
    }
}
