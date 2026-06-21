package com.shinhan.eclipse.ledger.exchange.internal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shinhan.eclipse.common.exchange.ExchangeRateInfo;
import com.shinhan.eclipse.common.exchange.ExchangeRateKeys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
class LedgerRateReader {

    private static final String KEY_PREFIX = ExchangeRateKeys.RATE_KEY_PREFIX;

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper        objectMapper;

    Optional<ExchangeRateInfo> read(String currencyCode) {
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