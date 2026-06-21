package com.shinhan.eclipse.ledger.subscription;

import com.shinhan.eclipse.common.resolver.UserHeader;
import com.shinhan.eclipse.common.response.ApiResponse;
import com.shinhan.eclipse.domain.subscription.IpoSubscription;
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

import java.util.List;

@RestController
@RequestMapping("/api/v1/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionController.class);

    private final SubscriptionFacade subscriptionFacade;

    // SUB-001
    @PostMapping
    public ResponseEntity<ApiResponse<SubscriptionRes>> requestSubscription(
            @UserHeader Long userId,
            @Valid @RequestBody SubscriptionReq request) {
        log.info("청약 신청 요청: userId={}, ipoId={}", userId, request.ipoId());
        IpoSubscription subscription = subscriptionFacade.requestSubscription(request.toEntity(userId));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(SubscriptionRes.from(subscription)));
    }

    // SUB-002
    @PutMapping("/{subscriptionId}/confirm")
    public ResponseEntity<ApiResponse<SubscriptionRes>> confirmSubscription(
            @UserHeader Long userId,
            @PathVariable("subscriptionId") Long subscriptionId) {
        log.info("청약 확정 요청: userId={}, subscriptionId={}", userId, subscriptionId);
        IpoSubscription subscription = subscriptionFacade.confirmSubscription(subscriptionId, userId);
        return ResponseEntity.ok(ApiResponse.success(SubscriptionRes.from(subscription)));
    }

    // SUB-003
    @DeleteMapping("/{subscriptionId}")
    public ResponseEntity<Void> cancelSubscription(
            @UserHeader Long userId,
            @PathVariable("subscriptionId") Long subscriptionId) {
        log.info("청약 취소 요청: userId={}, subscriptionId={}", userId, subscriptionId);
        subscriptionFacade.cancelSubscription(subscriptionId, userId);
        return ResponseEntity.noContent().build();
    }

    // SUB-004
    @GetMapping
    public ResponseEntity<ApiResponse<SubscriptionListRes>> getSubscriptions(
            @UserHeader Long userId,
            @RequestParam(name = "ipoId", required = false) Long ipoId,
            @RequestParam(name = "status", required = false) String status) {
        List<SubscriptionRes> subscriptions = subscriptionFacade.getSubscriptions(userId, ipoId, status)
                .stream()
                .map(SubscriptionRes::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(SubscriptionListRes.builder().subscriptions(subscriptions).build()));
    }
}
