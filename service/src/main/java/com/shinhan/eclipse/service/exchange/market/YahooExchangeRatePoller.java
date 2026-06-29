package com.shinhan.eclipse.service.exchange.market;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Slf4j
@Component
@RequiredArgsConstructor
public class YahooExchangeRatePoller {

    private static final ZoneId    KST         = ZoneId.of("Asia/Seoul");
    private static final BigDecimal SPREAD_RATE = new BigDecimal("0.00955");

    private static final String YAHOO_URL =
            "https://query1.finance.yahoo.com/v8/finance/chart/USDKRW=X?interval=1m&range=1d";

    private final MarketRateRedisStore redisStore;
    private final ObjectMapper         objectMapper;

    @Scheduled(fixedDelay = 300_000, initialDelay = 10_000)
    void poll() {
        // LS WebSocket이 살아있으면 스킵 (Redis에 LS_CUR 데이터가 있음)
        MarketRateData current = redisStore.get().orElse(null);
        if (current != null && "LS_CUR".equals(current.source())) {
            log.debug("[Yahoo폴링] LS 연결 중 — 스킵");
            return;
        }
        try {
            String body = WebClient.builder()
                    .build()
                    .get()
                    .uri(YAHOO_URL)
                    .header("User-Agent", "Mozilla/5.0")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (body == null) return;

            JsonNode root   = objectMapper.readTree(body);
            JsonNode result = root.path("chart").path("result");
            if (!result.isArray() || result.isEmpty()) return;

            JsonNode meta = result.get(0).path("meta");
            BigDecimal price = new BigDecimal(meta.path("regularMarketPrice").asText("0"));
            if (price.compareTo(BigDecimal.ZERO) == 0) return;

            BigDecimal high  = new BigDecimal(meta.path("regularMarketDayHigh").asText("0"));
            BigDecimal low   = new BigDecimal(meta.path("regularMarketDayLow").asText("0"));
            BigDecimal prev  = new BigDecimal(meta.path("previousClose").asText("0"));
            BigDecimal open  = prev.compareTo(BigDecimal.ZERO) != 0 ? prev : price;
            BigDecimal change     = prev != null && prev.compareTo(BigDecimal.ZERO) != 0
                    ? price.subtract(prev)
                    : BigDecimal.ZERO;
            BigDecimal changeRate = prev != null && prev.compareTo(BigDecimal.ZERO) != 0
                    ? change.divide(prev, 6, RoundingMode.HALF_UP)
                            .multiply(new BigDecimal("100"))
                            .setScale(4, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            String sign = change.compareTo(BigDecimal.ZERO) >= 0 ? "2" : "5";

            BigDecimal spread = price.multiply(SPREAD_RATE).setScale(1, RoundingMode.HALF_UP);
            BigDecimal tts    = price.add(spread);
            BigDecimal ttb    = price.subtract(spread);

            MarketRateData data = new MarketRateData(price, tts, ttb, spread,
                    change, changeRate, sign,
                    high != null ? high : price,
                    low  != null ? low  : price,
                    open != null ? open : price,
                    "YAHOO", LocalDateTime.now(KST));

            redisStore.save(data);
            log.info("[Yahoo폴링] USD/KRW = {} (TTS={}, TTB={})", price, tts, ttb);

        } catch (Exception e) {
            log.warn("[Yahoo폴링] 실패: {}", e.getMessage());
        }
    }
}
