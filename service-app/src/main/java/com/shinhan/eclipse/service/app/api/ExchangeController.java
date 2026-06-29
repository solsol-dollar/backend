package com.shinhan.eclipse.service.app.api;

import com.shinhan.eclipse.common.response.ApiResponse;
import com.shinhan.eclipse.service.exchange.market.MarketRateData;
import com.shinhan.eclipse.service.exchange.market.MarketRateRedisStore;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/exchange")
@RequiredArgsConstructor
public class ExchangeController {

    private final MarketRateRedisStore marketRateRedisStore;

    /** EX-001: 환율 조회 (실시간 시장 환율) */
    @GetMapping("/rate")
    public ResponseEntity<ApiResponse<MarketRateData>> getExchangeRate() {
        return marketRateRedisStore.get()
                .map(data -> ResponseEntity.ok(ApiResponse.success(data)))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
