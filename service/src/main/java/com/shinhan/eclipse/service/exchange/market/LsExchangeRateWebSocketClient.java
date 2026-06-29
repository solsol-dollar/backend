package com.shinhan.eclipse.service.exchange.market;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.util.retry.Retry;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class LsExchangeRateWebSocketClient {

    private static final ZoneId   KST          = ZoneId.of("Asia/Seoul");
    private static final String   TR_KEY       = "USD   ";
    private static final BigDecimal SPREAD_RATE = new BigDecimal("0.00955");

    private final LsCurProperties    props;
    private final LsTokenManager     tokenManager;
    private final MarketRateRedisStore redisStore;
    private final ObjectMapper        objectMapper;

    private volatile boolean    running      = false;
    private volatile String     lastPrice    = null;
    private volatile Disposable subscription = null;

    // TTS/TTB는 5분 스케줄러에서만 갱신 — 매 tick마다 바꾸지 않음
    private volatile BigDecimal cachedTts    = null;
    private volatile BigDecimal cachedTtb    = null;
    private volatile BigDecimal cachedSpread = null;

    @PostConstruct
    void init() {
        if (!props.isConfigured()) {
            log.warn("[LS WebSocket] LS 설정 없음(appKey 미설정) — 비활성화");
            return;
        }
        String token = tokenManager.getToken();
        if (!token.isBlank()) {
            connect(token);
        } else {
            log.warn("[LS WebSocket] 토큰 없음 — 장 시작(08:55) 시 자동 연결");
        }
    }

    @Scheduled(cron = "0 55 8 * * MON-FRI", zone = "Asia/Seoul")
    void connectAtMarketOpen() {
        if (!props.isConfigured()) return;
        log.info("[LS WebSocket] 장 시작 연결");
        connect(tokenManager.getToken());
    }

    @Scheduled(cron = "0 30 15 * * MON-FRI", zone = "Asia/Seoul")
    void disconnectAtMarketClose() {
        log.info("[LS WebSocket] 장 종료 연결 해제");
        disconnect();
    }

    @EventListener
    void onTokenRefreshed(LsTokenRefreshedEvent event) {
        log.info("[LS WebSocket] 토큰 갱신 — 재연결");
        disconnect();
        connect(event.getNewToken());
    }

    public void connect(String token) {
        if (token == null || token.isBlank()) {
            log.warn("[LS WebSocket] 토큰 없음 — 연결 생략");
            return;
        }
        running = true;
        lastPrice = null;

        try {
            SslContext ssl = SslContextBuilder.forClient()
                    .trustManager(InsecureTrustManagerFactory.INSTANCE)
                    .build();

            ReactorNettyWebSocketClient client = new ReactorNettyWebSocketClient(
                    HttpClient.create().secure(spec -> spec.sslContext(ssl))
            );

            String subscribeMsg = objectMapper.writeValueAsString(Map.of(
                    "header", Map.of("token", token, "tr_type", "3"),
                    "body",   Map.of("tr_cd", "CUR", "tr_key", TR_KEY)
            ));

            subscription = client.execute(URI.create(props.getWsUrl()), session -> {
                    log.info("[LS WebSocket] 세션 열림 — 구독 메시지 전송");
                    return session.send(Mono.just(session.textMessage(subscribeMsg)))
                            .doOnSuccess(v -> log.info("[LS WebSocket] 구독 메시지 전송 완료"))
                            .doOnError(e -> log.warn("[LS WebSocket] 구독 메시지 전송 실패: {}", e.getMessage()))
                            .thenMany(session.receive()
                                    .doOnNext(msg -> handleMessage(msg.getPayloadAsText()))
                                    .doOnError(e -> log.warn("[LS WebSocket] 수신 오류: {}", e.getMessage()))
                            )
                            .then();
            })
            .retryWhen(Retry.backoff(Long.MAX_VALUE, Duration.ofSeconds(5))
                    .maxBackoff(Duration.ofSeconds(60))
                    .doBeforeRetry(s -> {
                        if (!running) throw new RuntimeException("연결 종료 요청");
                        log.info("[LS WebSocket] 재연결 시도 #{}", s.totalRetries() + 1);
                    })
                    .filter(e -> running)
            )
            .subscribe(
                    null,
                    e -> log.warn("[LS WebSocket] 연결 종료: {}", e.getMessage()),
                    ()  -> log.info("[LS WebSocket] 스트림 완료")
            );

            log.info("[LS WebSocket] 연결 시작: {}", props.getWsUrl());
        } catch (Exception e) {
            log.error("[LS WebSocket] 연결 실패: {}", e.getMessage());
        }
    }

    public void disconnect() {
        running = false;
        lastPrice = null;
        if (subscription != null && !subscription.isDisposed()) {
            subscription.dispose();
        }
    }

    private void handleMessage(String payload) {
        try {
            JsonNode root   = objectMapper.readTree(payload);
            JsonNode header = root.path("header");
            String   rspCd  = header.path("rsp_cd").asText(null);

            if (rspCd != null) {
                if ("00000".equals(rspCd)) {
                    log.info("[LS WebSocket] CUR 구독 성공");
                } else {
                    log.warn("[LS WebSocket] 구독 실패 rsp_cd={} msg={}", rspCd, header.path("rsp_msg").asText());
                }
                return;
            }

            JsonNode body = root.path("body").isMissingNode() ? root : root.path("body");

            String priceStr = body.path("price").asText("").trim();
            if (priceStr.isBlank() || "0".equals(priceStr)) return;
            if (priceStr.equals(lastPrice)) return;
            lastPrice = priceStr;

            MarketRateData data = buildData(body);
            redisStore.save(data);

        } catch (Exception e) {
            log.debug("[LS WebSocket] 메시지 파싱 오류: {} — payload={}", e.getMessage(), payload.length() > 200 ? payload.substring(0, 200) : payload);
        }
    }

    /** 5분마다 현재 price 기준으로 TTS/TTB 재계산 */
    @Scheduled(fixedDelay = 300_000, initialDelay = 5_000)
    void refreshRates() {
        if (lastPrice == null) return;
        try {
            BigDecimal price  = new BigDecimal(lastPrice);
            BigDecimal spread = price.multiply(SPREAD_RATE).setScale(1, RoundingMode.HALF_UP);
            cachedSpread = spread;
            cachedTts    = price.add(spread);
            cachedTtb    = price.subtract(spread);
            log.info("[LS 고시환율] TTS={} TTB={} ({})", cachedTts, cachedTtb, lastPrice);

            // Redis에 저장된 최신 데이터에 TTS/TTB 반영
            redisStore.get().ifPresent(current -> {
                MarketRateData updated = new MarketRateData(
                        current.price(), cachedTts, cachedTtb, cachedSpread,
                        current.change(), current.changeRate(), current.sign(),
                        current.high(), current.low(), current.open(),
                        current.source(), LocalDateTime.now(KST));
                redisStore.save(updated);
            });
        } catch (Exception e) {
            log.warn("[LS 고시환율] 계산 실패: {}", e.getMessage());
        }
    }

    private MarketRateData buildData(JsonNode body) {
        BigDecimal price  = bd(body, "price");
        BigDecimal change = bd(body, "change");
        BigDecimal drate  = bd(body, "drate");
        BigDecimal high   = bd(body, "high");
        BigDecimal low    = bd(body, "low");
        BigDecimal open   = bd(body, "open");
        String     sign   = body.path("sign").asText("3");

        // TTS/TTB는 캐시값 사용 (없으면 최초 1회만 계산)
        BigDecimal spread = cachedSpread != null ? cachedSpread
                : price.multiply(SPREAD_RATE).setScale(1, RoundingMode.HALF_UP);
        BigDecimal tts    = cachedTts != null ? cachedTts : price.add(spread);
        BigDecimal ttb    = cachedTtb != null ? cachedTtb : price.subtract(spread);

        return new MarketRateData(price, tts, ttb, spread, change, drate, sign,
                high, low, open, "LS_CUR", LocalDateTime.now(KST));
    }

    private BigDecimal bd(JsonNode node, String field) {
        String val = node.path(field).asText("0").trim();
        try {
            return new BigDecimal(val);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }
}
