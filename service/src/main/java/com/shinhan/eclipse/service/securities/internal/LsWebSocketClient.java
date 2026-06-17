package com.shinhan.eclipse.service.securities.internal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shinhan.eclipse.service.securities.QuoteReceivedEvent;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
@RequiredArgsConstructor
class LsWebSocketClient {

    private static final String EXCHCD_NASDAQ = "82";
    private static final int    TR_KEY_LEN    = 18;

    private final LsProperties           props;
    private final LsTokenManager         tokenManager;
    private final ApplicationEventPublisher publisher;
    private final ObjectMapper           objectMapper;
    private final ProductRepository      productRepository;

    private final AtomicBoolean          running       = new AtomicBoolean(false);
    private final List<String>           subscribedKeys = new CopyOnWriteArrayList<>();

    @EventListener(ApplicationReadyEvent.class)
    void onReady() {
        if (!props.isConfigured()) {
            log.warn("LS 시세 WebSocket 비활성화: app-key/app-secret 미설정");
            return;
        }
        connect();
    }

    private void connect() {
        if (running.getAndSet(true)) return;

        List<String> tickers = productRepository.findByProductTypeAndStatus("OVERSEAS", "ACTIVE")
                .stream()
                .map(com.shinhan.eclipse.domain.product.InvestmentProduct::getTicker)
                .toList();

        if (tickers.isEmpty()) {
            log.warn("구독할 종목 없음 — WebSocket 연결 건너뜀");
            running.set(false);
            return;
        }

        subscribedKeys.clear();
        tickers.forEach(t -> subscribedKeys.add(trKey(EXCHCD_NASDAQ, t)));
        log.info("LS 시세 WebSocket 연결 시도: {} 종목", subscribedKeys.size());

        ReactorNettyWebSocketClient wsClient = new ReactorNettyWebSocketClient();

        wsClient.execute(URI.create(props.getWsUrl()), session -> {
            // 구독 요청 전송
            String token = tokenManager.getAccessToken();
            List<WebSocketMessage> subscriptions = subscribedKeys.stream()
                    .map(key -> buildSubscribeMsg(session, token, key))
                    .toList();

            Mono<Void> send = session.send(
                    reactor.core.publisher.Flux.fromIterable(subscriptions)
            );

            Mono<Void> receive = session.receive()
                    .doOnNext(msg -> handleMessage(msg.getPayloadAsText()))
                    .then();

            return send.then(receive);
        })
        .doOnError(e -> {
            log.warn("LS WebSocket 연결 끊김: {} — 30초 후 재연결", e.getMessage());
            running.set(false);
        })
        .subscribe(
                null,
                e -> scheduleReconnect(),
                () -> {
                    log.info("LS WebSocket 세션 종료 — 재연결");
                    running.set(false);
                    scheduleReconnect();
                }
        );
    }

    private void scheduleReconnect() {
        Mono.delay(Duration.ofSeconds(30))
                .subscribe(ignored -> connect());
    }

    private WebSocketMessage buildSubscribeMsg(
            org.springframework.web.reactive.socket.WebSocketSession session,
            String token, String trKey) {

        Map<String, Object> msg = Map.of(
                "header", Map.of(
                        "token",      token,
                        "tr_type",    "3"  // 실시간 등록
                ),
                "body", Map.of(
                        "tr_cd",  "GSH",
                        "tr_key", trKey
                )
        );
        try {
            return session.textMessage(objectMapper.writeValueAsString(msg));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("구독 메시지 직렬화 실패", e);
        }
    }

    private void handleMessage(String payload) {
        try {
            WsMessage msg = objectMapper.readValue(payload, WsMessage.class);
            if (msg.getBody() == null) return;

            String rspCd = msg.getHeader() != null ? msg.getHeader().getRspCd() : null;
            if ("00000".equals(rspCd)) {
                log.debug("GSH 구독 성공: {}", msg.getHeader().getRspMsg());
                return;
            }

            WsMessage.Body body = msg.getBody();
            if (body.getTrCd() == null || body.getData() == null) return;

            WsMessage.Body.GshData data = body.getData();
            if (data.getSymbol() == null) return;

            publisher.publishEvent(QuoteReceivedEvent.of(
                    data.getSymbol(),
                    parseDecimal(data.getPrice()),
                    parseDecimal(data.getDiff()),
                    parseDecimal(data.getRate()),
                    parseLong(data.getVolume()),
                    data.getSign()
            ));
        } catch (Exception e) {
            log.debug("WebSocket 메시지 파싱 오류: {}", e.getMessage());
        }
    }

    private static String trKey(String exchcd, String ticker) {
        String raw = exchcd + ticker;
        return String.format("%-" + TR_KEY_LEN + "s", raw);
    }

    private static BigDecimal parseDecimal(String s) {
        if (s == null || s.isBlank()) return BigDecimal.ZERO;
        try { return new BigDecimal(s.trim()); } catch (NumberFormatException e) { return BigDecimal.ZERO; }
    }

    private static long parseLong(String s) {
        if (s == null || s.isBlank()) return 0L;
        try { return Long.parseLong(s.trim()); } catch (NumberFormatException e) { return 0L; }
    }

    // ---- 내부 메시지 파싱 구조체 ----

    @Getter @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class WsMessage {
        private Header header;
        private Body   body;

        @Getter @Setter
        @JsonIgnoreProperties(ignoreUnknown = true)
        static class Header {
            @JsonProperty("rsp_cd")  String rspCd;
            @JsonProperty("rsp_msg") String rspMsg;
        }

        @Getter @Setter
        @JsonIgnoreProperties(ignoreUnknown = true)
        static class Body {
            @JsonProperty("tr_cd")  String trCd;
            @JsonProperty("tr_key") String trKey;
            private GshData data;

            @Getter @Setter
            @JsonIgnoreProperties(ignoreUnknown = true)
            static class GshData {
                @JsonProperty("symbol") String symbol;
                @JsonProperty("price")  String price;
                @JsonProperty("sign")   String sign;
                @JsonProperty("diff")   String diff;
                @JsonProperty("rate")   String rate;
                @JsonProperty("volume") String volume;
                @JsonProperty("open")   String open;
                @JsonProperty("high")   String high;
                @JsonProperty("low")    String low;
            }
        }
    }
}
