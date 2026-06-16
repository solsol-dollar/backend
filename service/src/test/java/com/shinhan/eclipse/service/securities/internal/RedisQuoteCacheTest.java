package com.shinhan.eclipse.service.securities.internal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.shinhan.eclipse.service.securities.QuoteSnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RedisQuoteCacheTest {

    @Mock StringRedisTemplate redisTemplate;
    @Mock ValueOperations<String, String> valueOps;

    RedisQuoteCache cache;
    ObjectMapper    objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        given(redisTemplate.opsForValue()).willReturn(valueOps);
        cache = new RedisQuoteCache(redisTemplate, objectMapper);
    }

    @Test
    void put_Redis에_JSON으로_저장하고_TTL_60초를_설정한다() throws Exception {
        QuoteSnapshot snapshot = new QuoteSnapshot(
                "TSLA", new BigDecimal("250.50"), new BigDecimal("3.20"),
                new BigDecimal("1.29"), 1234567L, "2", Instant.now()
        );

        cache.put("TSLA", snapshot);

        ArgumentCaptor<String> keyCaptor  = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);

        verify(valueOps).set(keyCaptor.capture(), jsonCaptor.capture(), ttlCaptor.capture());

        assertThat(keyCaptor.getValue()).isEqualTo("quote:TSLA");
        assertThat(ttlCaptor.getValue()).isEqualTo(Duration.ofSeconds(60));
        QuoteSnapshot deserialized = objectMapper.readValue(jsonCaptor.getValue(), QuoteSnapshot.class);
        assertThat(deserialized.ticker()).isEqualTo("TSLA");
        assertThat(deserialized.price()).isEqualByComparingTo("250.50");
    }

    @Test
    void get_Redis에_값이_있으면_QuoteSnapshot을_반환한다() throws Exception {
        QuoteSnapshot snapshot = new QuoteSnapshot(
                "AAPL", new BigDecimal("180.00"), BigDecimal.ZERO,
                BigDecimal.ZERO, 5000000L, "3", Instant.EPOCH
        );
        String json = objectMapper.writeValueAsString(snapshot);
        given(valueOps.get("quote:AAPL")).willReturn(json);

        Optional<QuoteSnapshot> result = cache.get("AAPL");

        assertThat(result).isPresent();
        assertThat(result.get().ticker()).isEqualTo("AAPL");
        assertThat(result.get().price()).isEqualByComparingTo("180.00");
    }

    @Test
    void get_Redis에_값이_없으면_빈_Optional을_반환한다() {
        given(valueOps.get("quote:NVDA")).willReturn(null);

        Optional<QuoteSnapshot> result = cache.get("NVDA");

        assertThat(result).isEmpty();
    }

    @Test
    void put_key_형식은_quote_콜론_ticker이다() {
        QuoteSnapshot snapshot = new QuoteSnapshot(
                "QQQ", BigDecimal.TEN, BigDecimal.ZERO,
                BigDecimal.ZERO, 0L, "3", Instant.now()
        );

        cache.put("QQQ", snapshot);

        verify(valueOps).set(eq("quote:QQQ"), anyString(), any(Duration.class));
    }
}
