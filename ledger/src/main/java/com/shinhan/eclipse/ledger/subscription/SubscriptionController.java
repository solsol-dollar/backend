package com.shinhan.eclipse.ledger.subscription;

import com.shinhan.eclipse.common.exception.BusinessException;
import com.shinhan.eclipse.common.exception.ErrorCode;
import com.shinhan.eclipse.auth.AuthUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.shinhan.eclipse.common.response.ApiResponse;
import com.shinhan.eclipse.domain.account.FinancialAccount;
import com.shinhan.eclipse.domain.ipo.Ipo;
import com.shinhan.eclipse.domain.subscription.IpoSubscription;
import com.shinhan.eclipse.ledger.accountlink.AccountLinkService;
import com.shinhan.eclipse.ledger.subscription.dto.SubscriptionCancelRes;
import com.shinhan.eclipse.ledger.subscription.dto.SubscriptionListRes;
import com.shinhan.eclipse.ledger.subscription.dto.SubscriptionReq;
import com.shinhan.eclipse.ledger.subscription.dto.SubscriptionRes;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionController.class);

    private final SubscriptionFacade subscriptionFacade;
    private final AccountLinkService accountLinkService;

    // SUB-001
    @PostMapping
    public ResponseEntity<ApiResponse<SubscriptionRes>> requestSubscription(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody SubscriptionReq request) {
        Long userId = authUser.userId();
        log.info("청약 신청 요청: userId={}, ipoId={}", userId, request.ipoId());
        IpoSubscription subscription = subscriptionFacade.requestSubscription(
                userId, request.ipoId(), request.securitiesAccountId(), request.subscriptionAmount(), request.offerPrice());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(toRes(subscription)));
    }

    // SUB-002
    @PutMapping("/{subscriptionId}/confirm")
    public ResponseEntity<ApiResponse<SubscriptionRes>> confirmSubscription(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable("subscriptionId") Long subscriptionId) {
        Long userId = authUser.userId();
        log.info("청약 확정 요청: userId={}, subscriptionId={}", userId, subscriptionId);
        IpoSubscription subscription = subscriptionFacade.confirmSubscription(subscriptionId, userId);
        return ResponseEntity.ok(ApiResponse.success(toRes(subscription)));
    }

    // SUB-003 (환불금액/환불계좌 포함: 명세 외 변경 — 기존엔 204 No Content였음)
    @DeleteMapping("/{subscriptionId}")
    public ResponseEntity<ApiResponse<SubscriptionCancelRes>> cancelSubscription(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable("subscriptionId") Long subscriptionId) {
        Long userId = authUser.userId();
        log.info("청약 취소 요청: userId={}, subscriptionId={}", userId, subscriptionId);
        IpoSubscription subscription = subscriptionFacade.cancelSubscription(subscriptionId, userId);
        FinancialAccount refundAccount = accountLinkService.getLinkedAccount(userId, subscription.getSecuritiesAccountId());
        return ResponseEntity.ok(ApiResponse.success(SubscriptionCancelRes.of(subscription, refundAccount)));
    }

    // SUB-005 복권 긁기 완료 처리 (멱등)
    @PatchMapping("/{subscriptionId}/scratch")
    public ResponseEntity<ApiResponse<SubscriptionRes>> revealScratch(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable("subscriptionId") Long subscriptionId) {
        Long userId = authUser.userId();
        log.info("복권 긁기 완료 요청: userId={}, subscriptionId={}", userId, subscriptionId);
        IpoSubscription subscription = subscriptionFacade.revealScratch(subscriptionId, userId);
        return ResponseEntity.ok(ApiResponse.success(toRes(subscription)));
    }

    // SUB-004 (from/to: 명세 외 추가 — 조회 조건 설정 모달용)
    @GetMapping
    public ResponseEntity<ApiResponse<SubscriptionListRes>> getSubscriptions(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestParam(name = "ipoId", required = false) Long ipoId,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "from", required = false) LocalDate from,
            @RequestParam(name = "to", required = false) LocalDate to) {
        Long userId = authUser.userId();
        if (from != null && to != null && from.isAfter(to)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "from은 to보다 늦을 수 없습니다.");
        }
        List<SubscriptionRes> subscriptions = subscriptionFacade.getSubscriptions(userId, ipoId, status, from, to)
                .stream()
                .map(this::toRes)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(SubscriptionListRes.builder().subscriptions(subscriptions).build()));
    }

    private SubscriptionRes toRes(IpoSubscription subscription) {
        Ipo ipo = subscriptionFacade.getIpo(subscription.getIpoId());
        return SubscriptionRes.from(subscription, ipo);
    }
}
