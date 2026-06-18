package com.shinhan.eclipse.service.securities.internal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shinhan.eclipse.common.exception.BusinessException;
import com.shinhan.eclipse.common.exception.ErrorCode;
import com.shinhan.eclipse.domain.product.InvestmentProduct;
import com.shinhan.eclipse.domain.product.PriceCandle;
import com.shinhan.eclipse.service.securities.ChartResponse;
import com.shinhan.eclipse.service.securities.ChartResponse.CandleItem;
import com.shinhan.eclipse.service.securities.ChartService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
class ChartServiceImpl implements ChartService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final Duration MINUTE_CACHE_TTL = Duration.ofSeconds(86400);

    // period → candleType 매핑
    private static final Map<String, String> PERIOD_TO_CANDLE_TYPE = Map.of(
            "1D", "MINUTE",
            "1W", "DAY",
            "1M", "DAY",
            "3M", "WEEK",
            "6M", "WEEK",
            "1Y", "MONTH",
            "5Y", "MONTH"
    );

    // period → 조회 기간(일) — 여유분 포함
    private static final Map<String, Integer> PERIOD_TO_DAYS = Map.of(
            "1W",  10,
            "1M",  30,
            "3M",  90,
            "6M",  180,
            "1Y",  365,
            "5Y",  1825
    );

    private final ProductRepository      productRepository;
    private final PriceCandleRepository  priceCandleRepository;
    private final LsRestClient           lsRestClient;
    private final KisRestClient          kisRestClient;
    private final StringRedisTemplate    redisTemplate;
    private final ObjectMapper           objectMapper;

    @Override
    public ChartResponse getChart(Long productId, String period) {
        InvestmentProduct product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND,
                        "종목을 찾을 수 없습니다: " + productId));

        String candleType = PERIOD_TO_CANDLE_TYPE.get(period);
        // period 유효성은 컨트롤러에서 검증하므로 여기서는 NPE 방어만
        if (candleType == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "지원하지 않는 period: " + period);
        }

        if ("1D".equals(period)) {
            return getMinuteChart(product, period);
        } else {
            return getDailyChart(product, period, candleType);
        }
    }

    // ── 1D: 분봉 (Redis → g3203 폴백) ──────────────────────────────────────

    private ChartResponse getMinuteChart(InvestmentProduct product, String period) {
        String today = LocalDate.now().format(DATE_FMT);
        String redisKey = "chart:min:5:" + product.getTicker() + ":" + today;

        // 1. Redis 조회
        String cached = redisTemplate.opsForValue().get(redisKey);
        if (cached != null) {
            try {
                List<CandleItem> candles = objectMapper.readValue(cached,
                        new TypeReference<>() {});
                log.debug("차트 Redis 캐시 HIT: {}", redisKey);
                return new ChartResponse(product.getTicker(), period, "MINUTE", candles);
            } catch (JsonProcessingException e) {
                log.warn("분봉 캐시 역직렬화 실패 [{}]: {}", redisKey, e.getMessage());
            }
        }

        // 2. KIS HHDFS76950200 호출
        log.debug("차트 Redis 캐시 MISS: {} → KIS 분봉 호출", redisKey);
        List<CandleItem> candles = kisRestClient.getMinuteCandles(product.getTicker(), product.getExchangeName())
                .map(resp -> resp.getCandles().stream()
                        .map(c -> new CandleItem(
                                c.getXymd(),
                                c.getXhms(),
                                parseBD(c.getOpen()),
                                parseBD(c.getHigh()),
                                parseBD(c.getLow()),
                                parseBD(c.getLast()),
                                parseLong(c.getEvol()),
                                null  // 분봉에는 sign 없음
                        ))
                        .toList())
                .orElse(Collections.emptyList());

        // 3. Redis 캐싱
        if (!candles.isEmpty()) {
            try {
                String json = objectMapper.writeValueAsString(candles);
                redisTemplate.opsForValue().set(redisKey, json, MINUTE_CACHE_TTL);
            } catch (JsonProcessingException e) {
                log.warn("분봉 캐시 직렬화 실패 [{}]: {}", redisKey, e.getMessage());
            }
        }

        return new ChartResponse(product.getTicker(), period, "MINUTE", candles);
    }

    // ── 1W~5Y: DB 조회 ────────────────────────────────────────────────────────

    private ChartResponse getDailyChart(InvestmentProduct product, String period, String candleType) {
        int days = PERIOD_TO_DAYS.getOrDefault(period, 30);
        LocalDate from = LocalDate.now().minusDays(days);
        LocalDate to   = LocalDate.now();

        List<PriceCandle> candles = priceCandleRepository
                .findByProductIdAndCandleTypeAndCandleAtBetween(product.getId(), candleType, from, to);

        List<CandleItem> items = candles.stream()
                .map(c -> new CandleItem(
                        c.getCandleAt().format(DATE_FMT),
                        null,  // 일봉 이상은 time 없음
                        c.getOpenPrice(),
                        c.getHighPrice(),
                        c.getLowPrice(),
                        c.getClosePrice(),
                        c.getVolume(),
                        c.getSign()
                ))
                .toList();

        return new ChartResponse(product.getTicker(), period, candleType, items);
    }

    // ── 유틸 ──────────────────────────────────────────────────────────────────

    private BigDecimal parseBD(String s) {
        if (s == null || s.isBlank()) return BigDecimal.ZERO;
        try { return new BigDecimal(s.trim()); }
        catch (NumberFormatException e) { return BigDecimal.ZERO; }
    }

    private Long parseLong(String s) {
        if (s == null || s.isBlank()) return 0L;
        try { return Long.parseLong(s.trim()); }
        catch (NumberFormatException e) { return 0L; }
    }
}
