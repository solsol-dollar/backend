package com.shinhan.eclipse.service.app.api;

import com.shinhan.eclipse.common.response.ApiResponse;
import com.shinhan.eclipse.service.exchange.market.LsExchangeRateWebSocketClient;
import com.shinhan.eclipse.service.exchange.market.LsTokenManager;
import com.shinhan.eclipse.service.exchange.market.MarketExchangeRateService;
import com.shinhan.eclipse.service.exchange.market.MarketRateData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@RestController
@RequestMapping("/api/v1/exchange")
@RequiredArgsConstructor
public class ExchangeRateSseController {

    private final MarketExchangeRateService       marketExchangeRateService;
    private final LsExchangeRateWebSocketClient   wsClient;
    private final LsTokenManager                  lsExchangeTokenManager;

    /** EX-002: 실시간 환율 스트림 (SSE) */
    @GetMapping(value = "/rate/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        return marketExchangeRateService.subscribe();
    }

    /** EX-003: 현재 시장 환율 스냅샷 */
    @GetMapping("/rate/market")
    public ResponseEntity<ApiResponse<MarketRateData>> marketRate() {
        return ResponseEntity.ok(ApiResponse.success(marketExchangeRateService.getCurrent().orElse(null)));
    }

    /** TEST: 웹소켓 수동 연결 트리거 */
    @PostMapping("/rate/connect")
    public ResponseEntity<ApiResponse<String>> connect() {
        String token = lsExchangeTokenManager.getToken();
        if (token == null || token.isBlank()) {
            return ResponseEntity.ok(ApiResponse.success("토큰 없음 — LS_APP_KEY 확인 필요"));
        }
        wsClient.connect(token);
        return ResponseEntity.ok(ApiResponse.success("WebSocket 연결 요청 완료"));
    }
}
