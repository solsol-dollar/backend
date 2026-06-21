package com.shinhan.eclipse.service.exchange.internal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shinhan.eclipse.common.exchange.ExchangeRateInfo;
import com.shinhan.eclipse.common.exchange.ExchangeRateKeys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

/**
 * Cache-Aside 패턴으로 환율 정보를 Redis에 저장한다.
 * TTL을 25시간으로 설정하여 주말·공휴일 이후에도 마지막 환율을 폴백으로 사용할 수 있다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
class RedisExchangeRateCache implements ExchangeRateCache {

    private static final String KEY_PREFIX = ExchangeRateKeys.RATE_KEY_PREFIX;
    private static final Duration TTL = Duration.ofHours(25);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void put(ExchangeRateInfo info) {
        try {
            String json = objectMapper.writeValueAsString(info);
            redisTemplate.opsForValue().set(KEY_PREFIX + info.currencyCode(), json, TTL);
        } catch (JsonProcessingException e) {
            log.warn("환율 캐시 직렬화 실패 [{}]: {}", info.currencyCode(), e.getMessage());
        }
    }

    @Override
    public Optional<ExchangeRateInfo> get(String currencyCode) {
        String json = redisTemplate.opsForValue().get(KEY_PREFIX + currencyCode);
        if (json == null) return Optional.empty();
        try {
            return Optional.of(objectMapper.readValue(json, ExchangeRateInfo.class));
        } catch (JsonProcessingException e) {
            log.warn("환율 캐시 역직렬화 실패 [{}]: {}", currencyCode, e.getMessage());
            return Optional.empty();
        }
    }
}