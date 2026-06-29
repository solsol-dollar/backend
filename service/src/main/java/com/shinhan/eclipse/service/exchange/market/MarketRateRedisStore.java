package com.shinhan.eclipse.service.exchange.market;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class MarketRateRedisStore {

    public static final String KEY            = "exchange_rate:market:USD";
    public static final String PUBSUB_CHANNEL = "exchange_rate:update";

    private static final Duration LS_TTL    = Duration.ofSeconds(30);
    private static final Duration YAHOO_TTL = Duration.ofMinutes(6);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper        objectMapper;
    private final SseEmitterRegistry  sseRegistry;

    public void save(MarketRateData data) {
        try {
            String json = objectMapper.writeValueAsString(data);
            Duration ttl = "YAHOO".equals(data.source()) ? YAHOO_TTL : LS_TTL;
            redisTemplate.opsForValue().set(KEY, json, ttl);
            redisTemplate.convertAndSend(PUBSUB_CHANNEL, json);
            sseRegistry.broadcast(json);
        } catch (JsonProcessingException e) {
            log.warn("[시장환율] 직렬화 실패: {}", e.getMessage());
        }
    }

    public Optional<MarketRateData> get() {
        String json = redisTemplate.opsForValue().get(KEY);
        if (json == null) return Optional.empty();
        try {
            return Optional.of(objectMapper.readValue(json, MarketRateData.class));
        } catch (JsonProcessingException e) {
            log.warn("[시장환율] 역직렬화 실패: {}", e.getMessage());
            return Optional.empty();
        }
    }
}
