package com.shinhan.eclipse.ledger.allocation;

import com.shinhan.eclipse.common.resolver.UserHeader;
import com.shinhan.eclipse.common.response.ApiResponse;
import com.shinhan.eclipse.domain.ipo.Ipo;
import com.shinhan.eclipse.domain.subscription.IpoSubscription;
import com.shinhan.eclipse.ledger.allocation.dto.AllocationResultDetailRes;
import com.shinhan.eclipse.ledger.allocation.dto.AllocationResultListRes;
import com.shinhan.eclipse.ledger.allocation.dto.AllocationResultRes;
import com.shinhan.eclipse.ledger.returnplan.ReturnPlanFacade;
import com.shinhan.eclipse.ledger.subscription.SubscriptionFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/subscription-results")
@RequiredArgsConstructor
public class AllocationResultController {

    private final SubscriptionFacade subscriptionFacade;
    private final ReturnPlanFacade returnPlanFacade;
    private final QuoteClient quoteClient;

    // ALLOC-001
    @GetMapping
    public ResponseEntity<ApiResponse<AllocationResultListRes>> getAllocationResults(
            @UserHeader Long userId,
            @RequestParam(name = "subscriptionId", required = false) Long subscriptionId) {
        List<AllocationResultRes> results = subscriptionFacade.getSubscriptionResults(userId, subscriptionId)
                .stream()
                .map(this::toRes)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(AllocationResultListRes.builder().results(results).build()));
    }

    // ALLOC-002
    @GetMapping("/{subscriptionResultId}")
    public ResponseEntity<ApiResponse<AllocationResultDetailRes>> getAllocationResultDetail(
            @UserHeader Long userId,
            @PathVariable("subscriptionResultId") Long subscriptionResultId) {
        IpoSubscription subscription = subscriptionFacade.getSubscriptionResult(subscriptionResultId, userId);
        Ipo ipo = subscriptionFacade.getIpo(subscription.getIpoId());
        BigDecimal currentPrice = ipo.getProductId() == null ? null : quoteClient.getCurrentPrice(ipo.getProductId());
        boolean hasReturnPlan = returnPlanFacade.existsBySubscriptionId(subscription.getId());
        return ResponseEntity.ok(ApiResponse.success(
                AllocationResultDetailRes.of(subscription, ipo, currentPrice, hasReturnPlan)));
    }

    private AllocationResultRes toRes(IpoSubscription subscription) {
        Ipo ipo = subscriptionFacade.getIpo(subscription.getIpoId());
        BigDecimal currentPrice = ipo.getProductId() == null ? null : quoteClient.getCurrentPrice(ipo.getProductId());
        return AllocationResultRes.of(subscription, ipo, currentPrice);
    }
}
