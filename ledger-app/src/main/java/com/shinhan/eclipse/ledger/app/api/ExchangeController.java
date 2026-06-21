package com.shinhan.eclipse.ledger.app.api;

import com.shinhan.eclipse.auth.AuthUser;
import com.shinhan.eclipse.common.response.ApiResponse;
import com.shinhan.eclipse.ledger.exchange.ExchangeExecutionService;
import com.shinhan.eclipse.ledger.exchange.ExchangeRequest;
import com.shinhan.eclipse.ledger.exchange.ExchangeResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/exchange")
@RequiredArgsConstructor
public class ExchangeController {

    private final ExchangeExecutionService exchangeExecutionService;

    @PostMapping
    public ResponseEntity<ApiResponse<ExchangeResult>> execute(
            @RequestBody ExchangeRequest request,
            @AuthenticationPrincipal AuthUser authUser) {
        return ResponseEntity.ok(ApiResponse.success(exchangeExecutionService.execute(request, authUser.userId())));
    }
}