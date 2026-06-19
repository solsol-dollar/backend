package com.shinhan.eclipse.service.app.api;

import com.shinhan.eclipse.auth.dto.AuthUser;
import com.shinhan.eclipse.common.response.ApiResponse;
import com.shinhan.eclipse.service.securities.TradeOrderRequest;
import com.shinhan.eclipse.service.securities.TradeOrderResponse;
import com.shinhan.eclipse.service.securities.TradeOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

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
}