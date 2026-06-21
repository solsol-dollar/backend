package com.shinhan.eclipse.service.app.api;

import com.shinhan.eclipse.common.response.ApiResponse;
import com.shinhan.eclipse.common.exchange.ExchangeRateInfo;
import com.shinhan.eclipse.service.exchange.ExchangeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;

@RestController
@RequestMapping("/api/v1/exchange")
@RequiredArgsConstructor
public class ExchangeController {

    private final ExchangeService exchangeService;

    /** EX-001: 환율 조회 */
    @GetMapping("/rate")
    public ResponseEntity<ApiResponse<ExchangeRateInfo>> getExchangeRate(
            @RequestParam(defaultValue = "USD") String currency) {
        return ResponseEntity.ok(ApiResponse.success(
                exchangeService.getExchangeRate(currency.trim().toUpperCase(Locale.ROOT))));
    }
}
