package com.shinhan.eclipse.service.app.api;

import com.shinhan.eclipse.auth.AuthUser;
import com.shinhan.eclipse.common.response.ApiResponse;
import com.shinhan.eclipse.service.securities.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/trade-orders")
@RequiredArgsConstructor
public class TradeOrderController {

    private final TradeOrderService tradeOrderService;

    /** ORD-001: 매수/매도 주문 */
    @PostMapping
    public ResponseEntity<ApiResponse<TradeOrderResponse>> placeOrder(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestBody TradeOrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(tradeOrderService.placeOrder(authUser.userId(), request)));
    }

    /** B-02: 주문 내역 */
    @GetMapping
    public ResponseEntity<ApiResponse<List<OrderHistoryItem>>> getOrders(
            @AuthenticationPrincipal AuthUser authUser) {
        return ResponseEntity.ok(ApiResponse.success(
                tradeOrderService.getOrders(authUser.userId())));
    }

    /** B-03: 판매 수익 */
    @GetMapping("/profits")
    public ResponseEntity<ApiResponse<SellProfitsSummary>> getProfits(
            @AuthenticationPrincipal AuthUser authUser) {
        return ResponseEntity.ok(ApiResponse.success(
                tradeOrderService.getProfits(authUser.userId())));
    }
}
