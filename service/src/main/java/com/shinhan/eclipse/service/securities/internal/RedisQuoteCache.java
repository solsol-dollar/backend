package com.shinhan.eclipse.service.securities.internal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shinhan.eclipse.service.securities.QuoteCache;
import com.shinhan.eclipse.service.securities.QuoteSnapshot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
class RedisQuoteCache implements QuoteCache {

    private static final String KEY_PREFIX = "quote:";
    private static final Duration TTL      = Duration.ofSeconds(60);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper        objectMapper;

    @Override
    public void put(String ticker, QuoteSnapshot snapshot) {
        try {
            String json = objectMapper.writeValueAsString(snapshot);
            redisTemplate.opsForValue().set(KEY_PREFIX + ticker, json, TTL);
        } catch (JsonProcessingException e) {
            log.warn("시세 캐시 직렬화 실패 [{}]: {}", ticker, e.getMessage());
        }
    }

    @Override
    public Optional<QuoteSnapshot> get(String ticker) {
        String json = redisTemplate.opsForValue().get(KEY_PREFIX + ticker);
        if (json == null) return Optional.empty();
        try {
            return Optional.of(objectMapper.readValue(json, QuoteSnapshot.class));
        } catch (JsonProcessingException e) {
            log.warn("시세 캐시 역직렬화 실패 [{}]: {}", ticker, e.getMessage());
            return Optional.empty();
        }
    }
}
