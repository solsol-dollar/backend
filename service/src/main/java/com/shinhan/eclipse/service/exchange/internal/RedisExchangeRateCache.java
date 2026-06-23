package com.shinhan.eclipse.service.exchange.internal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shinhan.eclipse.common.redis.exchange.ExchangeRateInfo;
import com.shinhan.eclipse.common.redis.exchange.ExchangeRateKeys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Locale;
import java.util.Optional;

/**
 * Cache-Aside 패턴으로 환율 정보를 Redis에 저장한다.
 * TTL을 72시간으로 설정하여 금요일 10시 갱신 후 월요일까지 폴백 사용 가능.
 */
@Slf4j
@Component
@RequiredArgsConstructor
class RedisExchangeRateCache implements ExchangeRateCache {

    private static final String   KEY_PREFIX      = ExchangeRateKeys.RATE_KEY_PREFIX;
    private static final String   PREV_KEY_PREFIX = ExchangeRateKeys.PREV_RATE_KEY_PREFIX;
    private static final Duration TTL             = Duration.ofHours(72);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper        objectMapper;

    @Override
    public void put(ExchangeRateInfo info) {
        try {
            String key  = KEY_PREFIX + info.currencyCode().toUpperCase(Locale.ROOT);
            String json = objectMapper.writeValueAsString(info);
            redisTemplate.opsForValue().set(key, json, TTL);
        } catch (JsonProcessingException e) {
            log.warn("환율 캐시 직렬화 실패 [{}]: {}", info.currencyCode(), e.getMessage());
        }
    }

    @Override
    public Optional<ExchangeRateInfo> get(String currencyCode) {
        return getByKey(KEY_PREFIX + currencyCode.toUpperCase(Locale.ROOT), currencyCode);
    }

    @Override
    public void putPrev(ExchangeRateInfo info) {
        try {
            String key  = PREV_KEY_PREFIX + info.currencyCode().toUpperCase(Locale.ROOT);
            String json = objectMapper.writeValueAsString(info);
            redisTemplate.opsForValue().set(key, json, TTL);
        } catch (JsonProcessingException e) {
            log.warn("이전 환율 캐시 직렬화 실패 [{}]: {}", info.currencyCode(), e.getMessage());
        }
    }

    @Override
    public Optional<ExchangeRateInfo> getPrev(String currencyCode) {
        return getByKey(PREV_KEY_PREFIX + currencyCode.toUpperCase(Locale.ROOT), currencyCode);
    }

    private Optional<ExchangeRateInfo> getByKey(String key, String currencyCode) {
        String json = redisTemplate.opsForValue().get(key);
        if (json == null) return Optional.empty();
        try {
            return Optional.of(objectMapper.readValue(json, ExchangeRateInfo.class));
        } catch (JsonProcessingException e) {
            log.warn("환율 캐시 역직렬화 실패 [{}]: {}", currencyCode, e.getMessage());
            return Optional.empty();
        }
    }
}