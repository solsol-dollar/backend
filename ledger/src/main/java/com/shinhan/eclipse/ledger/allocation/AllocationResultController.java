package com.shinhan.eclipse.ledger.allocation;

import com.shinhan.eclipse.common.exception.BusinessException;
import com.shinhan.eclipse.common.exception.ErrorCode;
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
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/subscription-results")
@RequiredArgsConstructor
public class AllocationResultController {

    private final SubscriptionFacade subscriptionFacade;
    private final ReturnPlanFacade returnPlanFacade;
    private final QuoteClient quoteClient;

    // ALLOC-001 (from/to/statusGroup: 명세 외 추가 — 조회 조건 설정 모달용)
    @GetMapping
    public ResponseEntity<ApiResponse<AllocationResultListRes>> getAllocationResults(
            @UserHeader Long userId,
            @RequestParam(name = "subscriptionId", required = false) Long subscriptionId,
            @RequestParam(name = "from", required = false) LocalDate from,
            @RequestParam(name = "to", required = false) LocalDate to,
            @RequestParam(name = "statusGroup", required = false) String statusGroup) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "from은 to보다 늦을 수 없습니다.");
        }
        List<AllocationResultRes> results = subscriptionFacade.getSubscriptionResults(userId, subscriptionId)
                .stream()
                .filter(s -> matchesDateRange(s, from, to))
                .filter(s -> matchesStatusGroup(s, statusGroup))
                .map(this::toRes)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(AllocationResultListRes.builder().results(results).build()));
    }

    private boolean matchesDateRange(IpoSubscription subscription, LocalDate from, LocalDate to) {
        if (from == null && to == null) return true;
        LocalDate reference = subscription.getSubscribedAt().toLocalDate();
        if (from != null && reference.isBefore(from)) return false;
        if (to != null && reference.isAfter(to)) return false;
        return true;
    }

    /**
     * statusGroup: ALL(전체) | REQUESTED(청약신청/취소완료) | ALLOCATED(배정완료) | LISTED(상장완료)
     * resultStatus는 배정 시점에 "COMPLETED" 하나로만 채워지고, 배정과 상장 거래 가능 시점이
     * 사실상 같은 순간으로 통일됐기 때문에 ALLOCATED/LISTED 둘 다 같은 값으로 매칭한다.
     */
    private boolean matchesStatusGroup(IpoSubscription subscription, String statusGroup) {
        if (!StringUtils.hasText(statusGroup) || "ALL".equalsIgnoreCase(statusGroup)) return true;
        if ("REQUESTED".equalsIgnoreCase(statusGroup)) {
            return "REQUESTED".equals(subscription.getSubscriptionStatus())
                    || "CANCELLED".equals(subscription.getSubscriptionStatus());
        }
        if ("ALLOCATED".equalsIgnoreCase(statusGroup) || "LISTED".equalsIgnoreCase(statusGroup)) {
            return "COMPLETED".equals(subscription.getResultStatus());
        }
        return false;
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
