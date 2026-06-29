package com.shinhan.eclipse.service.securities.internal;

import com.shinhan.eclipse.service.securities.QuoteSnapshot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
class YahooFinanceClient {

    // 내부 ticker → Yahoo Finance 심볼 매핑
    private static final Map<String, String> SYMBOL_MAP = Map.of(
            "SPY",    "^GSPC",
            "QQQ",    "^IXIC",
            "USDKRW", "USDKRW=X"
    );

    private final WebClient client = WebClient.builder()
            .baseUrl("https://query1.finance.yahoo.com")
            .defaultHeader("User-Agent", "Mozilla/5.0")
            .build();

    Optional<QuoteSnapshot> fetch(String ticker) {
        String symbol = SYMBOL_MAP.getOrDefault(ticker, ticker);
        try {
            Map<?, ?> body = client.get()
                    .uri(u -> u.path("/v8/finance/chart/{symbol}")
                            .queryParam("interval", "1d")
                            .queryParam("range", "1d")
                            .build(symbol))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (body == null) return Optional.empty();

            Map<?, ?> chart = (Map<?, ?>) body.get("chart");
            List<?> results = (List<?>) chart.get("result");
            if (results == null || results.isEmpty()) return Optional.empty();

            Map<?, ?> meta = (Map<?, ?>) ((Map<?, ?>) results.get(0)).get("meta");
            if (meta == null) return Optional.empty();

            BigDecimal price = toBD(meta.get("regularMarketPrice"));
            if (price == null) return Optional.empty();

            BigDecimal prevClose = toBD(meta.get("chartPreviousClose"));
            BigDecimal change = prevClose != null
                    ? price.subtract(prevClose).setScale(4, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            BigDecimal changeRate = (prevClose != null && prevClose.compareTo(BigDecimal.ZERO) != 0)
                    ? change.divide(prevClose, 4, RoundingMode.HALF_UP)
                             .multiply(BigDecimal.valueOf(100))
                             .setScale(2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            Number vol = (Number) meta.get("regularMarketVolume");
            long volume = vol != null ? vol.longValue() : 0L;
            String sign = change.compareTo(BigDecimal.ZERO) >= 0 ? "2" : "5";

            log.debug("Yahoo Finance 조회 성공: {} = {}", ticker, price);
            return Optional.of(new QuoteSnapshot(ticker, price, change, changeRate, volume, sign, Instant.now()));

        } catch (Exception e) {
            log.warn("Yahoo Finance 조회 실패 [{}]: {}", ticker, e.getMessage());
            return Optional.empty();
        }
    }

    private BigDecimal toBD(Object val) {
        if (val instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        return null;
    }
}
